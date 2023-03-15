package org.finalcola.delay.mq.broker.db;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.ByteBuffer;

/**
 * @author: finalcola
 * @date: 2023/3/15 22:44
 */
@Data
@AllArgsConstructor
public class KevValuePair {
    private final ByteBuffer key;
    private final ByteBuffer value;
}
