package net.xicp.tarbitrary.couchbase;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import net.xicp.tarbitrary.enums.EnumStepStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.SerializableDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;

/**
 * Hello world!
 * 
 */
public class CopyOfCouchBaseTest {
	private static final Logger logger = LoggerFactory
			.getLogger(CopyOfCouchBaseTest.class);

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

			SerializableDocument jsonDocument = bucket.get(account,
					SerializableDocument.class);
			if (null == jsonDocument) {
				// lock.lock();
				try {
					// 查询数据库获取资金账户 余额, 余额用currentAmount表示
					{
						// 查询资金余额代码
					}
					long currentAmount = 1000;// 没有获取的代码,默认给1000的初始值
					FundMoney fm = new FundMoney();
					fm.setCurrBalance(1000l + amount);
					fm.setUseBalance(0l);
					SerializableDocument create = SerializableDocument.create(
							account, fm);

					bucket.insert(create);
				} catch (DocumentAlreadyExistsException e) {
					while (true) {
						try {
							SerializableDocument document = bucket.get(account,
									SerializableDocument.class);
							if (null != document) {
								long cas = document.cas();
								FundMoney dfm = (FundMoney) document.content();
								dfm.setCurrBalance(dfm.getCurrBalance()
										+ amount);
								SerializableDocument create = SerializableDocument
										.create(account, dfm, cas);
								SerializableDocument replace = bucket
										.replace(create);
								FundMoney fm = (FundMoney) replace.content();
								logger.debug("account result : ----->{}",
										fm.getCurrBalance());
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
				// lock.unlock();
			} else {
				while (true) {
					try {
						SerializableDocument document = bucket.get(account,
								SerializableDocument.class);
						if (null != document) {
							long cas = document.cas();
							FundMoney dfm = (FundMoney) document.content();
							dfm.setCurrBalance(dfm.getCurrBalance() + amount);
							SerializableDocument create = SerializableDocument
									.create(account, dfm, cas);
							SerializableDocument replace = bucket
									.replace(create);

							FundMoney fm = (FundMoney) replace.content();
							logger.debug("account result : ----->{}",
									fm.getCurrBalance());
						} else {
							logger.debug("is empty now ");
							continue;
						}
					} catch (CASMismatchException e) {
						logger.debug("continue");
						continue;
					}
					break;
				}

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

	private void updateDBError(Exception e, String account, String key) {
		JsonDocument jd = bucket.get(key);
		if (null != jd) {
			Long currBalance = (jd.content().getLong(key)) * -1;

			while (true) {
				try {
					SerializableDocument document = bucket.get(account,
							SerializableDocument.class);
					if (null != document) {
						long cas = document.cas();
						FundMoney dfm = (FundMoney) document.content();
						dfm.setCurrBalance(dfm.getCurrBalance() + currBalance);
						SerializableDocument create = SerializableDocument
								.create(account, dfm, cas);
						SerializableDocument replace = bucket.replace(create);
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
			// 缓存资金余额回滚
			// 删除缓存操作日志
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
		final CopyOfCouchBaseTest test = new CopyOfCouchBaseTest();
		for (int i = 0; i < 10; i++) {
			fixedThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					for (int j = 0; j < 101; j++) {
						test.init("0000001622", "843",-1*Long.parseLong(j + "")
								* 1, UUID.randomUUID().toString());
					}
				}
			});
		}

		fixedThreadPool.shutdown();

	}
}
