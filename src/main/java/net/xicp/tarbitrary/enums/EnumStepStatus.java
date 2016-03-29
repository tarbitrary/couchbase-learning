package net.xicp.tarbitrary.enums;

public enum EnumStepStatus {
	INIT("初始状态", 0), FUNDFLOWLOG("记资金流水日志", 1), CACHELOG("缓存资金操作", 2), UPDATEFUNDCAHE(
			"更新内存资金余额", 3), UPDATEDB("更新数据库", 4), SUCCESS("更新成功", 5);

	public static final int init = 0;
	public static final int fundFlowLog = 1;
	public static final int cacheLog = 2;
	public static final int updateFundCache = 3;
	public static final int updateDB = 4;
	public static final int success = 5;
	
	
	private String value;

	private int result;

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	private EnumStepStatus(String value, int result) {
		this.value = value;
		this.result = result;
	}
}
