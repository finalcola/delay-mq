package org.finalcola.delay.mq.broker;

import lombok.SneakyThrows;
import org.finalcola.delay.mq.broker.config.BrokerConfig;
import org.finalcola.delay.mq.broker.config.MqConfig;
import org.finalcola.delay.mq.broker.config.RocksDBConfig;
import org.finalcola.delay.mq.broker.db.RocksDBStore;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author: finalcola
 * @date: 2023/3/18 22:49
 */
public class DelayMessageBroker {

    private final BrokerConfig brokerConfig;
    private final MqConfig mqConfig;
    private final RocksDBConfig rocksDBConfig;
    private MetaHolder metaHolder = new MetaHolder();

    private RocksDBStore rocksDBStore;
    private Scanner scanner;
    private Map<Integer, MessageInput> messageInputMap;
    private Map<Integer, MessageOutput> messageOutputMap;

    public DelayMessageBroker(BrokerConfig brokerConfig, MqConfig mqConfig, RocksDBConfig rocksDBConfig) {
        this.brokerConfig = brokerConfig;
        this.mqConfig = mqConfig;
        this.rocksDBConfig = rocksDBConfig;
    }

    @SneakyThrows
    public synchronized void start() {
        // 初始化DB
        rocksDBStore = new RocksDBStore(rocksDBConfig);
        rocksDBStore.start();
        // 初始化scanner
        scanner = new Scanner(brokerConfig, rocksDBStore);
        initMessageInput();
        initMessageOutput();

        // 启动组件
        messageInputMap.values().forEach(MessageInput::start);
        messageOutputMap.values().forEach(MessageOutput::start);
    }

    public void stop() {
        messageInputMap.values().forEach(MessageInput::stop);
        messageInputMap.values().forEach(MessageInput::stop);
        rocksDBStore.stop();
    }

    private void initMessageInput() {
        int partitionCount = rocksDBConfig.getPartitionCount();
        messageInputMap = IntStream.range(0, partitionCount)
                .mapToObj(partitionId -> new MessageInput(partitionId, mqConfig, rocksDBStore))
                .collect(Collectors.toMap(MessageInput::getPartitionId, Function.identity()));
    }

    private void initMessageOutput() {
        int partitionCount = rocksDBConfig.getPartitionCount();
        messageOutputMap = IntStream.range(0, partitionCount)
                .mapToObj(partitionId -> new MessageOutput(partitionId, metaHolder, mqConfig, scanner))
                .collect(Collectors.toMap(MessageOutput::getPartitionId, Function.identity()));
    }
}
