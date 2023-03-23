package org.finalcola.delay.mq.broker.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: finalcola
 * @date: 2023/3/18 21:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BrokerConfig {

  private Integer scanMsgBatchSize;
}
