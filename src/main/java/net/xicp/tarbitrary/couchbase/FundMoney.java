package net.xicp.tarbitrary.couchbase;

import java.io.Serializable;

public class FundMoney implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5478410435724361048L;

	private Long currBalance;
	
	private Long useBalance;

	public Long getCurrBalance() {
		return currBalance;
	}

	public void setCurrBalance(Long currBalance) {
		this.currBalance = currBalance;
	}

	public Long getUseBalance() {
		return useBalance;
	}

	public void setUseBalance(Long useBalance) {
		this.useBalance = useBalance;
	}
	
	
	
}
