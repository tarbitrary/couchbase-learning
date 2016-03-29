package net.xicp.tarbitrary.couchbase;


public interface  CouchBaseExpand<T> {

	public T onInit(T t);
	
	
	public  T onNext(T t);
	
	public T onUpdateDBError(T t);
	
	
	
	
}
