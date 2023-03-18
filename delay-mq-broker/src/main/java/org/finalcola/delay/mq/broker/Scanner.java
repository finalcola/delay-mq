package org.finalcola.delay.mq.broker;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.finalcola.delay.mq.broker.config.BrokerConfig;
import org.finalcola.delay.mq.broker.db.RocksDBRangeIterator;
import org.finalcola.delay.mq.broker.db.RocksDBStore;
import org.finalcola.delay.mq.broker.model.KevValuePair;
import org.finalcola.delay.mq.broker.model.ScanResult;
import org.finalcola.delay.mq.common.proto.DelayMsg;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: finalcola
 * @date: 2023/3/18 13:00
 */
@AllArgsConstructor
public class Scanner {

    private final BrokerConfig brokerConfig;
    private final RocksDBStore rocksDBStore;

    @SneakyThrows
    public ScanResult scan(int partitionId, String startKey) {
        ByteBuffer endKey = ByteBuffer.wrap(
                String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
//      RocksDBRangeIterator rangeIterator = ;
        final ByteBuffer startKeyBuffer = ByteBuffer.wrap(startKey.getBytes(StandardCharsets.UTF_8));
        String lastStoreKey = null;
        int counter = 0;
        List<DelayMsg> delayMsgs = new ArrayList<>();
        try (RocksDBRangeIterator range = rocksDBStore.range(partitionId, startKeyBuffer, endKey)) {
            while (range.hasNext()) {
                final KevValuePair kevValuePair = range.next();
                lastStoreKey = new String(kevValuePair.getKey().array(), StandardCharsets.UTF_8);
                delayMsgs.add(DelayMsg.parseFrom(kevValuePair.getValue()));
                counter++;
                if (counter > brokerConfig.getScanMsgBatchSize()) {
                    break;
                }
            }
        }
        return new ScanResult(lastStoreKey, delayMsgs);
    }
}
