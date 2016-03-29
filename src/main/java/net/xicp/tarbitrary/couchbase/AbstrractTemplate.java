package net.xicp.tarbitrary.couchbase;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;

public abstract class AbstrractTemplate {

	protected static final Logger logger = LoggerFactory
			.getLogger(CouchBaseTest.class);

	private static final Cluster cluster;

	private static final Bucket bucket;

	static {
		CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
				.connectTimeout(20 * 1000).build();
		URL resource = Thread.currentThread().getContextClassLoader().getResource("/");
		System.out.println(resource + ">>>>>>>>>>>>>>>>>>>>>>>>");
		cluster = CouchbaseCluster.create(env, "192.168.46.31");

		bucket = cluster.openBucket();

	}

	public Bucket getBucket() {
		return bucket;
	}

	public Cluster getCluster() {
		return cluster;
	}

	abstract void stepOne();

	abstract void stepTwo();

	abstract void stepThree();

	public void init() {
		stepOne();
		stepTwo();
		stepThree();
	}

}
