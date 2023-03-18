package org.finalcola.delay.mq.broker.config;

import lombok.Data;

/**
 * @author: finalcola
 * @date: 2023/3/17 23:27
 */
@Data
public class MqConfig {
    private String brokerAddr; // broker地址
    private Integer pullBatchSize; // 每次pull的消息数量
    private Integer sendRetryTimes;
}
