package net.xicp.tarbitrary.couchbase;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import net.xicp.tarbitrary.enums.EnumStepStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.util.retry.RetryBuilder;

/**
 * Hello world!
 * 
 */
public class CouchBaseTest {
	private static final Logger logger = LoggerFactory
			.getLogger(CouchBaseTest.class);

	private static final Cluster cluster;

	private static final Bucket bucket;

	private static final AsyncBucket asBucket;

	private final String currBalanceKey = "currrBalance";

	private static ReentrantLock lock = new ReentrantLock();

	static {
		CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
				.connectTimeout(20 * 1000).build();
		cluster = CouchbaseCluster.create(env, "192.168.46.31");

		bucket = cluster.openBucket();
		asBucket = bucket.async();
	}

	@SuppressWarnings("unchecked")
	public void init(final String account, final String userid,
			final Long amount, final String flowid) {
		// 将状态置为初始状态
		EnumStepStatus status = EnumStepStatus.INIT;
		String key = null;
		JsonDocument js = null;
		try {

			// step one,记录资金流水,得到流水id
			{
				// 记录资金流水代码
			}
			// 记录资金流水成功后更新状态
			status = EnumStepStatus.FUNDFLOWLOG;

			// step two 缓存资金变更日志, 根据资金账号+流水号作key
			key = account + "->" + flowid;
			logger.debug("flowid:{}", key);
			js = JsonDocument.create(key, JsonObject.empty().put(key, amount));
			bucket.insert(js);
			// 更新状态
			status = EnumStepStatus.CACHELOG;

			// step three 缓存资金更新, 资金账号为key

			JsonDocument jsonDocument = bucket.get(account);
			if (null == jsonDocument) {
				lock.lock();
				if (bucket.get(account) == null) {
					// 查询数据库获取资金账户 余额, 余额用currentAmount表示
					{
						// 查询资金余额代码
					}
					Long currentAmount = 1000l;// 没有获取的代码,默认给1000的初始值

					JsonDocument create = JsonDocument.create(
							account,
							JsonObject.empty().put(currBalanceKey,
									currentAmount + amount));
					bucket.insert(create);
				} else {
					Observable
							.defer(new Func0<Observable<JsonDocument>>() {

								@Override
								public Observable<JsonDocument> call() {
									return asBucket.get(account);
								}

							})
							.map(new Func1<JsonDocument, JsonDocument>() {
								@Override
								public JsonDocument call(JsonDocument document) {
									modifyDocumentSomehow(document);
									return document;
								}

								private void modifyDocumentSomehow(
										JsonDocument document) {
									Long currBalance = document.content()
											.getLong(currBalanceKey);
									currBalance = currBalance + amount;
									document.content().put(currBalanceKey,
											currBalance);

								}

							})
							.flatMap(
									new Func1<JsonDocument, Observable<JsonDocument>>() {
										@Override
										public Observable<JsonDocument> call(
												JsonDocument document) {
											return asBucket.replace(document);
										}
									})
							.retryWhen(
									RetryBuilder.anyOf(
											CASMismatchException.class).delay(Delay.fixed(1, TimeUnit.MILLISECONDS)).max(999999999).build())
							.subscribe();
				}
				lock.unlock();
			} else {
				Observable
						.defer(new Func0<Observable<JsonDocument>>() {

							@Override
							public Observable<JsonDocument> call() {
								return asBucket.get(account);
							}

						})
						.map(new Func1<JsonDocument, JsonDocument>() {
							@Override
							public JsonDocument call(JsonDocument document) {
								logger.debug(
										"before{}, amount{}, flowid{}",
										document.content().getLong(
												currBalanceKey), amount, flowid);
								modifyDocumentSomehow(document);
								return document;
							}

							private void modifyDocumentSomehow(
									JsonDocument document) {
								Long currBalance = document.content().getLong(
										currBalanceKey);
								currBalance = currBalance + amount;
								document.content().put(currBalanceKey,
										currBalance);
								logger.debug(
										"after{}, amount{}, flowid{}",
										document.content().getLong(
												currBalanceKey), amount, flowid);

							}

						})
						.flatMap(
								new Func1<JsonDocument, Observable<JsonDocument>>() {
									@Override
									public Observable<JsonDocument> call(
											JsonDocument document) {
										logger.debug(
												"end{}, amount{}, flowid{}",
												document.content().getLong(
														currBalanceKey),
												amount, flowid);
										return asBucket.replace(document);
									}
								})
						.retryWhen(
								RetryBuilder.anyOf(CASMismatchException.class)
										.delay(Delay.fixed(1, TimeUnit.MILLISECONDS)).max(999999999).build())
						.subscribe(new Action1<JsonDocument>() {

							@Override
							public void call(JsonDocument t1) {
								logger.debug("fund money result:"
										+ t1.content().getLong(currBalanceKey));
							}
						}, new Action1<Throwable>() {

							@Override
							public void call(Throwable t1) {
								logger.error("异常", t1);

							}
						});

			}
			status = EnumStepStatus.UPDATEFUNDCAHE;

			// step four 缓存数据落地到数据库
			{
				// 落地代码
			}
			status = EnumStepStatus.UPDATEDB;

			// step five 删除缓存资金变更日志
			logger.debug("*******************start*****************");
			bucket.remove(js);
			logger.debug("*******************end*****************");
			status = EnumStepStatus.SUCCESS;
		} catch (Exception e) {
			logger.error(e.getMessage());
			switch (status.getResult()) {
			case EnumStepStatus.init:
				logger.error("记录资金流水失败, 失败信息{}", e);
				initError(e, js, key);
				break;
			case EnumStepStatus.fundFlowLog:
				logger.error("缓存资金变更日志失败, 失败信息", e);
				fundFlowLogError(e, key);
				break;
			case EnumStepStatus.cacheLog:
				logger.error("缓存资金更新失败, 失败原因 {}", e);
				cacheLogError(e, js, key);
				break;
			case EnumStepStatus.updateDB:
				logger.error("落地到数据库失败, 失败原因{}", e);
				updateDBError(e, account, key);
				break;
			default:
				e.printStackTrace();
				;

			}
		}

	}

	private void updateDBError(Exception e, final String account,
			final String key) {
		JsonDocument jd = bucket.get(key);
		if (null != jd) {
			Long currBalance = jd.content().getLong(key);

			Observable
					.defer(new Func0<Observable<JsonDocument>>() {

						@Override
						public Observable<JsonDocument> call() {
							return asBucket.get(account);
						}

					})
					.map(new Func1<JsonDocument, JsonDocument>() {
						@Override
						public JsonDocument call(JsonDocument document) {
							modifyDocumentSomehow(document);
							return document;
						}

						private void modifyDocumentSomehow(JsonDocument document) {
							Long currBalance = document.content().getLong(
									currBalanceKey);
							currBalance = currBalance
									- bucket.get(key).content().getLong(key);
							document.content().put(currBalanceKey, currBalance);
						}

					})
					.flatMap(
							new Func1<JsonDocument, Observable<JsonDocument>>() {
								@Override
								public Observable<JsonDocument> call(
										JsonDocument document) {

									return asBucket.replace(document);
								}
							})
					.retryWhen(
							RetryBuilder.anyOf(CASMismatchException.class)
									.delay(Delay.fixed(1, TimeUnit.MILLISECONDS)).max(999999999).build())
					.subscribe(new Action1<JsonDocument>() {

						@Override
						public void call(JsonDocument t1) {
							logger.debug("fund money result:"
									+ t1.content().getLong(currBalanceKey));
						}
					}, new Action1<Throwable>() {

						@Override
						public void call(Throwable t1) {
							logger.error("异常", t1);

						}
					});

			// 缓存资金余额回滚
			bucket.counter(account, currBalance * -1);
			// 删除缓存操作日志
			bucket.remove(jd);
		}
	}

	private void cacheLogError(Exception e, JsonDocument js, String key) {
		// 直接删除前面的缓存变更日志
		bucket.remove(js);
	}

	private void initError(Exception e, JsonDocument js, String key) {

	}

	private void fundFlowLogError(Exception e, String key) {

	}

	public static void main(String[] args) {
		ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);
		final CouchBaseTest test = new CouchBaseTest();
		for (int i = 0; i < 10; i++) {
			fixedThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					for (int j = 0; j < 101; j++) {
						test.init("0000001623", "843", Long.parseLong(j + ""),
								UUID.randomUUID().toString());
					}
				}
			});
		}

		fixedThreadPool.shutdown();

	}
}
