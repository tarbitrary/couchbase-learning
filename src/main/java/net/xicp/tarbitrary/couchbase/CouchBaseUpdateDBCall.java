package net.xicp.tarbitrary.couchbase;

public abstract class CouchBaseUpdateDBCall<T> implements CouchBaseExpand<T> {
	
	@Override
	public T onInit(T t) {
		return null;
	}

}
