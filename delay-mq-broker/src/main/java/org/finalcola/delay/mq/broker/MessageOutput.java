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
import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: finalcola
 * @date: 2023/3/19 13:51
 */
@Slf4j
public class MessageOutput {

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
                    long count = this.sendMsg();
                    if (count <= 0) {
                        // 到期消息已经处理完成或者出现异常进行退让
                        long nextExecuteTime = DateTime.now().plusSeconds(1).withMillisOfSecond(0).getMillis();
                        long gap = nextExecuteTime - System.currentTimeMillis();
                        if (gap > 0) {
                            TimeUnit.MILLISECONDS.sleep(gap);
                        }
                    }
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

    @SneakyThrows
    public long sendMsg() {
        String lastHandledKey = metaHolder.getLastHandledKey();
        String startKey = StringUtils.firstNonEmpty(lastHandledKey, DEFAULT_START_KEY);
        int counter = 0;
        boolean sendFail = false;
        while (true) {
            ScanResult scanResult = scanner.scan(partitionId, startKey, false);
            String lastMsgStoreKey = scanResult.getLastMsgStoreKey();
            if (lastMsgStoreKey == null || CollectionUtils.isEmpty(scanResult.getDelayMsgs())) {
                break;
            }
            // 发送消息
            List<DelayMsg> delayMsgs = scanResult.getDelayMsgs();
            Boolean sendResult = RetryUtils.retry(10, () -> producer.send(delayMsgs));
            if (!sendResult) {
                sendFail = true;
                break;
            }
            metaHolder.setLastHandledKey(lastHandledKey, lastMsgStoreKey);
            counter += delayMsgs.size();
        }
        return sendFail ? -1 : counter;
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
