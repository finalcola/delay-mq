package org.finalcola.delay.mq.broker;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.finalcola.dalay.mq.common.constants.MqType;
import org.finalcola.dalay.mq.common.utils.MoreFunctions;
import org.finalcola.dalay.mq.common.utils.RetryUtils;
import org.finalcola.delay.mq.broker.config.ExecutorDef;
import org.finalcola.delay.mq.broker.config.MqConfig;
import org.finalcola.delay.mq.broker.model.ScanResult;
import org.finalcola.delay.mq.broker.producer.Producer;
import org.finalcola.delay.mq.broker.producer.RocketProducer;
import org.finalcola.delay.mq.common.proto.DelayMsg;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: finalcola
 * @date: 2023/3/19 13:51
 */
@Slf4j
public class MessageOutput implements Runnable {

    private static final String DEFAULT_START_KEY = StringUtils.repeat("0", 13);
    @Getter
    private final int partitionId;
    private final MetaHolder metaHolder;
    private final MqConfig mqConfig;
    private final Scanner scanner;
    private Producer producer;
    private volatile boolean isRunning = false;

    public MessageOutput(int partitionId, MetaHolder metaHolder, MqConfig mqConfig, Scanner scanner) {
        this.partitionId = partitionId;
        this.metaHolder = metaHolder;
        this.mqConfig = mqConfig;
        this.scanner = scanner;
    }


    public void start() {
        producer = createProducer();
        producer.start(mqConfig);
        isRunning = true;

        ExecutorDef.MSG_OUTPUT_EXECUTOR.submit(() -> {
            if (isRunning) {
                try {
                    this.run();
                } catch (Exception e) {
                    log.error("message output error", e);
                    MoreFunctions.runCatching(() -> TimeUnit.SECONDS.sleep(1));
                }
            }
        });
    }

    public void stop() {
        isRunning = false;
        producer.stop();
        producer = null;
    }

    @Override
    @SneakyThrows
    public void run() {
        String lastHandledKey = metaHolder.getLastHandledKey();
        String startKey = StringUtils.firstNonEmpty(lastHandledKey, DEFAULT_START_KEY);
        ScanResult scanResult = scanner.scan(partitionId, startKey);
        String lastMsgStoreKey = scanResult.getLastMsgStoreKey();

        // 发送消息
        List<DelayMsg> delayMsgs = scanResult.getDelayMsgs();
        if (CollectionUtils.isEmpty(delayMsgs)) {
            return;
        }
        Boolean sendResult = RetryUtils.retry(10, () -> producer.send(delayMsgs));
        if (sendResult) {
            metaHolder.setLastHandledKey(lastMsgStoreKey);
        }
    }

    private Producer createProducer() {
        MqType mqType = mqConfig.getMqType();
        assert mqType != null;
        switch (mqType) {
            case ROCKET_MQ:
                return new RocketProducer();
            case KAFKA:
            default:
                throw new RuntimeException(String.format("mqType:%s not support", mqType.name()));
        }
    }
}
