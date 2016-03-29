package net.xicp.tarbitrary.couchbase;

import net.xicp.tarbitrary.enums.EnumStepStatus;

/**
 * @author tarbitrary
 *
 * @param <T>
 */
public class CouchBaseStamp<T> {
	//进程状态
	public EnumStepStatus enumStatus;
	
	// 资金号+流水号为key
	public String key;
	
	//资金账号
	public String account;
	
	//相关资金表对应的pojo
	public T t ;

	public EnumStepStatus getEnumStatus() {
		return enumStatus;
	}

	public void setEnumStatus(EnumStepStatus enumStatus) {
		this.enumStatus = enumStatus;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public T getT() {
		return t;
	}

	public void setT(T t) {
		this.t = t;
	}
	
	
	
	

}
