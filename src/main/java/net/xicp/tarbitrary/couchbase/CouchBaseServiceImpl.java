package net.xicp.tarbitrary.couchbase;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;

public class CouchBaseServiceImpl<T> implements CouchBaseService<T> {
	private static final Logger logger = LoggerFactory
			.getLogger(CopyOfCouchBaseTest2.class);

	private static final Cluster cluster;

	private static final Bucket bucket;


	private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);

	
	
	static {
		CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
				.connectTimeout(20 * 1000).build();
		cluster = CouchbaseCluster.create(env, "192.168.46.31");

		bucket = cluster.openBucket();
	}

	@Override
	public JsonDocument cacheLog(String account, String flowid, Long amount)
			throws Exception {
		String key = account + "->" + flowid;
		JsonDocument js = JsonDocument.create(key,
				JsonObject.empty().put(key, amount));
		return bucket.insert(js);
	}

	@Override
	public RawJsonDocument upsertFundCache(JsonDocument js, String account,
			T t, CouchBaseExpand<T> cbe) {
		try {
			RawJsonDocument jsonDocument = bucket.get(account,
					RawJsonDocument.class);
			if (null == jsonDocument) {
				try {
					RawJsonDocument create = RawJsonDocument.create(account,
							JSON.toJSONString(cbe.onInit(t)));

					return bucket.insert(create);
				} catch (DocumentAlreadyExistsException e) {
					return casUpdate(account, cbe, t);
				}
			} else {
				return casUpdate(account, cbe, t);
			}
		} catch (Exception e2) {
			logger.error("update fund cache error, error info {}", e2);
			bucket.remove(js);
			return null;
		}

	}
	
	@Override
	public void updateDB(final JsonDocument js, final RawJsonDocument rjd,
			final T t, final CouchBaseExpand<T> cbe) {
		try {
			Future<T> future = fixedThreadPool.submit(new Callable<T>() {
				@Override
				public T call() throws Exception {
					return cbe.onNext(t);
				}
			});

			@SuppressWarnings("unused")
			T t2 = future.get();
			
			//如果没有错误的话直接删除缓存操作日志
			bucket.remove(js);
		} catch (Exception e) {
			logger.error("落库失败,{}", e);
			JsonDocument jd = bucket.get(js);
			if (null != jd) {
				// 缓存资金余额回滚
				casRollBack(rjd, t, cbe);
				// 删除缓存操作日志
//				bucket.remove(js);
			}
		}

	}

	private void casRollBack(final RawJsonDocument rjd, final T t,
			final CouchBaseExpand<T> cbe) {
		while (true) {
			RawJsonDocument document = null;
			try {
				document = bucket.get(rjd.id(), RawJsonDocument.class);
				if (null != document) {
					long cas = document.cas();
					@SuppressWarnings("unchecked")
					T parseObject = (T) JSON.parseObject(document.content(), t.getClass());
					RawJsonDocument create = RawJsonDocument.create(document.id(),
							JSON.toJSONString(cbe.onUpdateDBError(parseObject)), cas);
					document = bucket.replace(create);
				} else {
					logger.debug("is empty now ");
					continue;
				}
			} catch (CASMismatchException e1) {
				logger.debug("continue");
				continue;
			}

			break;
		}
	}

	private RawJsonDocument casUpdate(String account, CouchBaseExpand<T> cbe,
			T t) {
		RawJsonDocument result = null;
		while (true) {
			RawJsonDocument document = null;
			try {
				document = bucket.get(account, RawJsonDocument.class);
				if (null != document) {
					long cas = document.cas();
					@SuppressWarnings("unchecked")
					T parseObject = (T) JSON.parseObject(document.content(), t.getClass());
					RawJsonDocument create = RawJsonDocument.create(account,
							JSON.toJSONString(cbe.onNext(parseObject)), cas);
					result = bucket.replace(create);
				} else {
					logger.debug("is empty now ");
					continue;
				}
			} catch (CASMismatchException e1) {
				logger.debug("continue");
				continue;
			}
			
			logger.debug("counter over");
			return result;
		}
	}

}
