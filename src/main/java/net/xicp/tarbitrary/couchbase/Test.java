package net.xicp.tarbitrary.couchbase;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.RawJsonDocument;

public class Test {
	public static void main(String[] args) {
		ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);

		final CouchBaseService<FundMoney> cbs = new CouchBaseServiceImpl<FundMoney>();
		for (int i = 0; i < 10; i++) {
			fixedThreadPool.execute(new Runnable() {
				@Override
				public void run() {
					for (int j = 0; j < 100; j++) {
						try {
							JsonDocument cacheLog = cbs.cacheLog("0000001686", UUID.randomUUID().toString(), Long.parseLong(j + "") * 1);
							FundMoney t = new FundMoney();
							RawJsonDocument upsertFundCache = cbs.upsertFundCache(cacheLog, "0000001686", t, new CouchBaseUpsertFundCacheCall<FundMoney>() {

								/*
								 * (non-Javadoc) 获取初始资金余额方法
								 * 
								 * @see
								 * net.xicp.tarbitrary.couchbase.CouchBaseExpand
								 * #onInit(java.lang.Object)
								 */
								@Override
								public FundMoney onInit(FundMoney t) {
									FundMoney fm = new FundMoney();
									fm.setCurrBalance(1010l);
									return fm;
								}

								/*
								 * (non-Javadoc) 资金原子操作方法
								 * 
								 * @see
								 * net.xicp.tarbitrary.couchbase.CouchBaseExpand
								 * #onNext(java.lang.Object)
								 */
								@Override
								public FundMoney onNext(FundMoney t) {
									t.setCurrBalance(t.getCurrBalance() + 10l);
									return t;
								}
							});

							cbs.updateDB(cacheLog, upsertFundCache, t, new CouchBaseUpdateDBCall<FundMoney>() {

								/*
								 * (non-Javadoc) 资金变更入库方法
								 * 
								 * @see
								 * net.xicp.tarbitrary.couchbase.CouchBaseExpand
								 * #onNext(java.lang.Object)
								 */
								@Override
								public FundMoney onNext(FundMoney t) {
									return t;
								}

								/*
								 * (non-Javadoc) 落库失败回滚资金余额方法
								 * 
								 * @see
								 * net.xicp.tarbitrary.couchbase.CouchBaseExpand
								 * #onUpdateDBError(java.lang.Object)
								 */
								@Override
								public FundMoney onUpdateDBError(FundMoney t) {
									t.setCurrBalance(t.getCurrBalance() - 10);
									return t;
								}
							});

						} catch (NumberFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}
			});
		}

		fixedThreadPool.shutdown();
		// Thread.sleep(10000000);
		// cbs.getFixedThreadPool().shutdown();
	}

}
