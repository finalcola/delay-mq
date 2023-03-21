package org.finalcola.delay.mq.broker;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.finalcola.delay.mq.broker.config.BrokerConfig;
import org.finalcola.delay.mq.broker.convert.MsgConverter;
import org.finalcola.delay.mq.broker.db.RocksDBRangeIterator;
import org.finalcola.delay.mq.broker.db.RocksDBStore;
import org.finalcola.delay.mq.broker.model.KeyValuePair;
import org.finalcola.delay.mq.broker.model.ScanResult;
import org.finalcola.delay.mq.common.proto.DelayMsg;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.finalcola.delay.mq.broker.convert.MsgConverter.toByteBuffer;

/**
 * @author: finalcola
 * @date: 2023/3/18 13:00
 */
@AllArgsConstructor
public class Scanner {

    private final BrokerConfig brokerConfig;
    private final RocksDBStore rocksDBStore;

    @SneakyThrows
    public ScanResult scan(int partitionId, String startKey, boolean includeStart) {
        ByteBuffer endKey = toByteBuffer(String.valueOf(System.currentTimeMillis()));
        final ByteBuffer startKeyBuffer = toByteBuffer(startKey);
        int counter = 0;
        List<DelayMsg> delayMsgs = new ArrayList<>();
        ByteBuffer lastStoreKey = null;
        try (RocksDBRangeIterator range = rocksDBStore.range(partitionId, startKeyBuffer, endKey)) {
            while (range.hasNext()) {
                if (!includeStart) {
                    includeStart = true;
                    continue;
                }
                final KeyValuePair keyValuePair = range.next();
                lastStoreKey = keyValuePair.getKey();
                delayMsgs.add(DelayMsg.parseFrom(keyValuePair.getValue()));
                counter++;
                if (counter > brokerConfig.getScanMsgBatchSize()) {
                    break;
                }
            }
        }
        return new ScanResult(MsgConverter.toString(lastStoreKey), delayMsgs);
    }
}