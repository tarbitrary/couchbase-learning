package net.xicp.tarbitrary.couchbase;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import net.xicp.tarbitrary.enums.EnumStepStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.cluster.BucketSettings;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;

/**
 * Hello world!
 *
 */
public class CopyOfCouchBaseTest2 {
    private static final Logger logger = LoggerFactory.getLogger(CopyOfCouchBaseTest2.class);

    private static final Cluster cluster;

    private static final Bucket bucket;

    private static final AsyncBucket asBucket;

    private final String currBalanceKey = "currrBalance";

    private static ReentrantLock lock = new ReentrantLock();

    static {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder().connectTimeout(20 * 1000).build();
        cluster = CouchbaseCluster.create(env, "192.168.46.31");

        bucket = cluster.openBucket();
        asBucket = bucket.async();
    }

    @SuppressWarnings("unchecked")
    public void init(final String account, final String userid, final Long amount, final String flowid) {
        EnumStepStatus status = EnumStepStatus.INIT;
        String key = null;
        JsonDocument js = null;
        try {

            // step one,��¼�ʽ���ˮ,�õ���ˮid
            {
                // ��¼�ʽ���ˮ����
            }
            // ��¼�ʽ���ˮ�ɹ������״̬
            status = EnumStepStatus.FUNDFLOWLOG;

            // step two �����ʽ�����־, �����ʽ��˺�+��ˮ����key
            key = account + "->" + flowid;
            logger.debug("flowid:{}", key);
            js = JsonDocument.create(key, JsonObject.empty().put(key, amount));
            bucket.insert(js);
            // ����״̬
            status = EnumStepStatus.CACHELOG;

            // step three �����ʽ����, �ʽ��˺�Ϊkey

            RawJsonDocument jsonDocument = bucket.get(account, RawJsonDocument.class);
            if (null == jsonDocument) {
                // lock.lock();
                try {
                    // ��ѯ���ݿ��ȡ�ʽ��˻� ���, �����currentAmount��ʾ
                    {
                        // ��ѯ�ʽ�������
                    }
                    long currentAmount = 1000;// û�л�ȡ�Ĵ���,Ĭ�ϸ�1000�ĳ�ʼֵ
                    FundMoney fm = new FundMoney();
                    fm.setCurrBalance(1000l);
                    fm.setUseBalance(0l);
                    RawJsonDocument create = RawJsonDocument.create(account, JSON.toJSONString(fm));

                    bucket.insert(create);
                } catch (DocumentAlreadyExistsException e) {
                    while (true) {
                        try {
                            RawJsonDocument document = bucket.get(account, RawJsonDocument.class);
                            if (null != document) {
                                long cas = document.cas();
                                FundMoney dfm = JSON.parseObject(document.content(), FundMoney.class);
                                dfm.setCurrBalance(dfm.getCurrBalance() + amount);
                                RawJsonDocument create = RawJsonDocument.create(account, JSON.toJSONString(dfm), cas);
                                RawJsonDocument replace = bucket.replace(create);
                                FundMoney fm = JSON.parseObject(replace.content(), FundMoney.class);
                                logger.debug("account result : ----->{}", fm.getCurrBalance());
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
                        RawJsonDocument document = bucket.get(account, RawJsonDocument.class);
                        if (null != document) {
                            long cas = document.cas();
                            FundMoney dfm = JSON.parseObject(document.content(), FundMoney.class);
                            dfm.setCurrBalance(dfm.getCurrBalance() + amount);
                            RawJsonDocument create = RawJsonDocument.create(account, JSON.toJSONString(dfm), cas);
                            RawJsonDocument replace = bucket.replace(create);

                            FundMoney fm = JSON.parseObject(replace.content(), FundMoney.class);
                            logger.debug("account result : ----->{}", fm.getCurrBalance());
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

            // step four ����������ص����ݿ�
            {
                // ��ش���
            }
            status = EnumStepStatus.UPDATEDB;

            // step five ɾ�������ʽ�����־
            logger.debug("*******************start*****************");
            bucket.remove(js);
            logger.debug("*******************end*****************");
            status = EnumStepStatus.SUCCESS;
        } catch (Exception e) {
            logger.error(e.getMessage());
            switch (status.getResult()) {
                case EnumStepStatus.init:
                    logger.error("��¼�ʽ���ˮʧ��, ʧ����Ϣ", e);
                    initError(e, js, key);
                    break;
                case EnumStepStatus.fundFlowLog:
                    logger.error("�����ʽ�����־ʧ��, ʧ����Ϣ", e);
                    fundFlowLogError(e, key);
                    break;
                case EnumStepStatus.cacheLog:
                    logger.error("�����ʽ����ʧ��, ʧ��ԭ�� ", e);
                    cacheLogError(e, js, key);
                    break;
                case EnumStepStatus.updateDB:
                    logger.error("��ص����ݿ�ʧ��, ʧ��ԭ��", e);
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
                    RawJsonDocument document = bucket.get(account, RawJsonDocument.class);
                    if (null != document) {
                        long cas = document.cas();
                        FundMoney dfm = JSON.parseObject(document.content(), FundMoney.class);
                        dfm.setCurrBalance(dfm.getCurrBalance() + currBalance);
                        RawJsonDocument create = RawJsonDocument.create(account, JSON.toJSONString(dfm), cas);
                        RawJsonDocument replace = bucket.replace(create);
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
            // �����ʽ����ع�
            // ɾ�����������־
        }
    }

    private void cacheLogError(Exception e, JsonDocument js, String key) {
        // ֱ��ɾ��ǰ��Ļ�������־
        bucket.remove(js);
    }

    private void initError(Exception e, JsonDocument js, String key) {

    }

    private void fundFlowLogError(Exception e, String key) {

    }

    public static void main(String[] args) {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);
        final CopyOfCouchBaseTest2 test = new CopyOfCouchBaseTest2();
        for (int i = 0; i < 10; i++) {
            fixedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 101; j++) {
                        test.init("0000001626", "843", Long.parseLong(j + "") * 1, UUID.randomUUID().toString());
                    }
                }
            });
        }

        fixedThreadPool.shutdown();

    }
}
