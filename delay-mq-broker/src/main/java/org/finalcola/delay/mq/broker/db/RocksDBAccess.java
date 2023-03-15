package org.finalcola.delay.mq.broker.db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.rocksdb.*;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author: finalcola
 * @date: 2023/3/15 22:30
 */
@AllArgsConstructor
public class RocksDBAccess {
    @Getter
    private final int partitionId;
    private final RocksDB rocksDB;
    private final ColumnFamilyHandle handle;
    private final WriteOptions writeOptions = new WriteOptions()
            .setSync(false);

    public void put(@Nonnull KevValuePair pair) throws RocksDBException {
        rocksDB.put(handle, writeOptions, pair.getKey().array(), pair.getValue().array());
    }

    public void batchPut(@Nonnull List<KevValuePair> kevValuePairs) throws RocksDBException {
        if (CollectionUtils.isEmpty(kevValuePairs)) {
            return;
        }
        WriteBatch writeBatch = new WriteBatch();
        for (KevValuePair pair : kevValuePairs) {
            writeBatch.put(handle, pair.getKey().array(), pair.getValue().array());
        }
        rocksDB.write(writeOptions, writeBatch);
    }

    public RocksDBRangeIterator range(@Nonnull ByteBuffer start, @Nonnull ByteBuffer end) {
        RocksIterator iterator = rocksDB.newIterator(handle);
        return new RocksDBRangeIterator(iterator, start, end);
    }

    public void deleteRange(@Nonnull ByteBuffer start, @Nonnull ByteBuffer end) throws RocksDBException {
        rocksDB.deleteRange(handle, writeOptions, start.array(), end.array());
    }
}
