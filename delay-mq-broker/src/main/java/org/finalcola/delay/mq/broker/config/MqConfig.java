package org.finalcola.delay.mq.broker.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.finalcola.dalay.mq.common.constants.MqType;

/**
 * @author: finalcola
 * @date: 2023/3/17 23:27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MqConfig {
    private String brokerAddr; // broker地址
    private Integer pullBatchSize; // 每次pull的消息数量
    private Integer sendRetryTimes; // 发送消息的重试次数
    private MqType mqType; // 消息队列类型

}
