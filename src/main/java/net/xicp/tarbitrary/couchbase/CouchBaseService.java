package net.xicp.tarbitrary.couchbase;

import java.util.concurrent.ExecutorService;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.RawJsonDocument;

/**
 * @author Administrator
 *
 * @param <T>
 */
public interface CouchBaseService<T> {

	
	/** 缓存操作日志
	 * @param account 资金账号
	 * @param flowid 流水id
	 * @param amount 变动数量
	 * @return 缓存操作日志的JsonDocument
	 * @throws Exception 
	 */
	public JsonDocument cacheLog(String account, String flowid, Long amount)
			throws Exception;

	/** 缓存资金账户更新
	 * @param js cacheLog操作中得到的返回值JsonDocument
	 * @param account 资金账号
	 * @param t  资金系统中需要操作表对应的pojo
	 * @param cbe 定义资金账号在缓存中不存在时执行的方法(onInit)以及资金账号已存在时的处理逻辑(onNext)
	 * @return couchBase中资金信息的缓存对象(json格式) 返回null表明更新失败
	 */
	public RawJsonDocument upsertFundCache(JsonDocument js, String account, T t,
			CouchBaseExpand<T> cbe);

	/**更新入库
	 * @param js 缓存操作日志文档
	 * @param rjd 缓存资金更新文档
	 * @param t 资金相关对象
	 * @param cbe 回调接口
	 */
	public void updateDB(JsonDocument js, RawJsonDocument rjd, T t,
			CouchBaseExpand<T> cbe);


}
