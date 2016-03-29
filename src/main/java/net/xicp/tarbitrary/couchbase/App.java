package net.xicp.tarbitrary.couchbase;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.SerializableDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.util.retry.RetryBuilder;

public class App {
	public static void main(String[] args) {
		CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
				.connectTimeout(20 * 1000).build();
		final Cluster cluster = CouchbaseCluster.create(env, "192.168.46.31");
		URL resource = Thread.currentThread().getContextClassLoader()
				.getResource("/");
		System.out.println(resource + ">>>>>>>>>>>>>>>>>>>>>>>>");

		final Bucket bucket = cluster.openBucket();

		JsonDocument jsonDocument = bucket.get("0000001626");
		System.out.println(jsonDocument.content());
//		long cas = document.cas();

//		FundMoney dfm = (FundMoney) document.content();
//		System.out.println("results" + dfm.getCurrBalance());

	/*	
		 * SerializableDocument sm = SerializableDocument.create("135246", fm);
		 * bucket.insert(sm);
		 
		final AsyncBucket syncBucket = bucket.async();
		Observable.defer(new Func0<Observable<JsonDocument>>() {

			@Override
			public Observable<JsonDocument> call() {
				// TODO Auto-generated method stub
				return syncBucket.get("135248");
			}

		}).map(new Func1<JsonDocument, JsonDocument>() {
			@Override
			public JsonDocument call(JsonDocument document) {
				modifyDocumentSomehow(document);
				return document;
			}

			private void modifyDocumentSomehow(JsonDocument document) {
				// TODO Auto-generated method stub

			}

		})
				.flatMap(new Func1<JsonDocument, Observable<JsonDocument>>() {
					@Override
					public Observable<JsonDocument> call(JsonDocument document) {
						return syncBucket.replace(document);
					}
				})
				.retryWhen(
						RetryBuilder.anyOf(CASMismatchException.class)
								.delay(Delay.fixed(1, TimeUnit.SECONDS))
								.max(100).build()).subscribe();
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						SerializableDocument document = bucket.get("135246",
								SerializableDocument.class);
						long cas = document.cas();

						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						FundMoney dfm = (FundMoney) document.content();
						dfm.setCurrBalance(18888L);
						SerializableDocument
								.create("125246", SerializableDocument.create(
										"135246", dfm), cas);
						bucket.replace(document);
					} catch (CASMismatchException e) {
						continue;
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}

			}
		}).start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				SerializableDocument document = bucket.get("135246",
						SerializableDocument.class);
				long cas = document.cas();

				FundMoney dfm = (FundMoney) document.content();
				dfm.setCurrBalance(9999L);
				SerializableDocument.create("125246",
						SerializableDocument.create("135246", dfm), cas);
				bucket.replace(document);

			}
		}).start();

		
		 * while (true) { try { SerializableDocument document =
		 * bucket.get("135246", SerializableDocument.class); long cas =
		 * document.cas();
		 * 
		 * Thread.sleep(30000); FundMoney dfm = (FundMoney) document.content();
		 * dfm.setCurrBalance(dfm.getCurrBalance() + 100);
		 * SerializableDocument.create("125246",
		 * SerializableDocument.create("135246", dfm), cas);
		 * bucket.upsert(document); } catch (Exception e) { continue; }
		 * 
		 * break; }
		 
		System.out.println("强转试试");*/
		/*
		 * System.out.println("result:" + dfm.getCurrBalance() + "---" +
		 * dfm.getUseBalance());
		 */

		// bucket.counter("0000001622", -20000);
		/*
		 * final Bucket bucket2 = cluster.openBucket();
		 * System.out.println("refe" + (bucket == bucket2)); AsyncBucket async =
		 * bucket.async(); async.counter("test::id", 1, 1);
		 * 
		 * async.counter("user::id", 1, 1) .map(new Func1<JsonLongDocument,
		 * String>() {
		 * 
		 * @Override public String call(JsonLongDocument counter) { return
		 * "user::" + counter.content(); } }).flatMap(new Func1<String,
		 * Observable<JsonDocument>>() {
		 * 
		 * @Override public Observable<JsonDocument> call(String id) { return
		 * bucket.async().insert( JsonDocument.create(id, JsonObject.empty()));
		 * } }).toBlocking().single(new Func1<JsonDocument, Boolean>() {
		 * 
		 * public Boolean call(JsonDocument arg0) { System.out.println(arg0);
		 * return true; } });
		 * 
		 * int form = 0, count = 10; Observable.range(form, count) .flatMap(new
		 * Func1<Integer, Observable<JsonDocument>>() { public
		 * Observable<JsonDocument> call(Integer id) { return
		 * bucket.async().get("user::" + id); } }).last().toBlocking()
		 * .single(new Func1<JsonDocument, Boolean>() {
		 * 
		 * public Boolean call(JsonDocument arg0) { System.out.println(arg0);
		 * cluster.disconnect(); return true; } }); JsonDocument jsonDocument =
		 * bucket.get("tarbitrary"); System.out.println("result:" +
		 * jsonDocument); jsonDocument.content().put("name", "diy");
		 * JsonDocument upsert = bucket.upsert(jsonDocument);
		 * 
		 * JsonDocument jsonDocument2 = bucket2.get("tarbitrary");
		 * System.out.println("update" + jsonDocument2); cluster.disconnect();
		 */

	}
}
