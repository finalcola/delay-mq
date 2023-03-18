package org.finalcola.delay.mq.broker;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.finalcola.dalay.mq.common.constants.MqType;
import org.finalcola.dalay.mq.common.utils.RetryUtils;
import org.finalcola.delay.mq.broker.config.MqConfig;
import org.finalcola.delay.mq.broker.consumer.Consumer;
import org.finalcola.delay.mq.broker.consumer.RocketConsumer;
import org.finalcola.delay.mq.broker.convert.MsgConverter;
import org.finalcola.delay.mq.broker.db.RocksDBStore;
import org.finalcola.delay.mq.broker.model.KevValuePair;
import org.finalcola.delay.mq.common.proto.DelayMsg;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: finalcola
 * @date: 2023/3/18 23:47
 */
@AllArgsConstructor
public class MessageInput implements Runnable {

    private final RocksDBStore store;
    private final MqConfig mqConfig;
    private final int partitionId;
    private volatile Consumer consumer;
    private volatile boolean isRunning = false;

    public synchronized void start() {
        consumer = createConsumer(mqConfig);
        isRunning = true;
    }

    public synchronized void stop() {
        Consumer oldConsumer = this.consumer;
        oldConsumer.stop();
        this.consumer = null;
        this.isRunning = false;
    }

    @Override
    public void run() {
        Consumer consumer = this.consumer;
        if (!isRunning || consumer == null) {
            return;
        }
        List<DelayMsg> delayMsgs = consumer.poll();
        if (CollectionUtils.isEmpty(delayMsgs)) {
            return;
        }
        List<KevValuePair> kevValuePairs = delayMsgs.stream()
                .map(msg -> {
                    ByteBuffer key = MsgConverter.buildKey(msg);
                    ByteBuffer value = ByteBuffer.wrap(msg.toByteArray());
                    return new KevValuePair(key, value);
                })
                .collect(Collectors.toList());
        RetryUtils.retry(10, () -> {
            store.put(partitionId, kevValuePairs);
        });
    }

    private Consumer createConsumer(MqConfig mqConfig) {
        MqType mqType = mqConfig.getMqType();
        assert mqType != null;
        switch (mqType) {
            case ROCKET_MQ:
                return new RocketConsumer();
            case KAFKA:
            default:
                throw new RuntimeException(String.format("mqType:%s not support", mqType.name()));
        }
    }
}
