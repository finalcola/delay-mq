package org.finalcola.delay.mq.broker;

import org.finalcola.delay.mq.broker.config.BrokerConfig;
import org.finalcola.delay.mq.broker.config.MqConfig;
import org.finalcola.delay.mq.broker.config.RocksDBConfig;
import org.finalcola.delay.mq.broker.consumer.Consumer;
import org.finalcola.delay.mq.broker.db.RocksDBStore;
import org.finalcola.delay.mq.broker.producer.Producer;

/**
 * @author: finalcola
 * @date: 2023/3/18 22:49
 */
public class DelayMessageBroker {

    private final BrokerConfig brokerConfig;
    private final MqConfig mqConfig;
    private final RocksDBConfig rocksDBConfig;

    private RocksDBStore rocksDBStore;

    private Consumer consumer;
    private Producer producer;
    private Scanner scanner;
    private MetaHolder metaHolder = new MetaHolder();

    public DelayMessageBroker(BrokerConfig brokerConfig, MqConfig mqConfig, RocksDBConfig rocksDBConfig) {
        this.brokerConfig = brokerConfig;
        this.mqConfig = mqConfig;
        this.rocksDBConfig = rocksDBConfig;
    }

    public synchronized void start() {
        // 初始化DB
        rocksDBStore = new RocksDBStore(rocksDBConfig);
        // 初始化mq组件
        initMq();
        // 初始化scanner
        scanner = new Scanner(brokerConfig, rocksDBStore);

        producer.start(mqConfig);
        // TODO: 2023/3/18 启动其他组件
    }

    private void initMq() {

    }
}
