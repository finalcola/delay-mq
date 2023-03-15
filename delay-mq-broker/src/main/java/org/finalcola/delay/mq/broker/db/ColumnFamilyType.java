package org.finalcola.delay.mq.broker.db;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: finalcola
 * @date: 2023/3/15 22:28
 */
@Getter
@AllArgsConstructor
public enum ColumnFamilyType {
    DEFAULT("default"),
    MSG_PARTITION("msg_partition"),
    ;
    private final String name;
}
