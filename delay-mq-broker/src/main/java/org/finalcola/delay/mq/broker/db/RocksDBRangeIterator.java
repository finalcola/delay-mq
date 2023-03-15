package org.finalcola.delay.mq.broker.db;

import org.rocksdb.RocksIterator;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * @author: finalcola
 * @date: 2023/3/15 22:48
 */
public class RocksDBRangeIterator implements Iterator<KevValuePair> {
    private final RocksIterator rocksIterator;
    private final ByteBuffer start;
    private final ByteBuffer end;
    private KevValuePair next;

    public RocksDBRangeIterator(RocksIterator rocksIterator, ByteBuffer start, ByteBuffer end) {
        this.rocksIterator = rocksIterator;
        this.start = start;
        this.end = end;
        rocksIterator.seek(start);
    }

    @Override
    public boolean hasNext() {
        KevValuePair makeResult = makeNext();
        this.next = makeResult;
        return makeResult != null;
    }

    @Override
    public KevValuePair next() {
        try {
            return next;
        } finally {
            next = null;
        }
    }

    private KevValuePair makeNext() {
        byte[] key = rocksIterator.key();
        if (key == null || key.length <= 0) {
            return null;
        }
        ByteBuffer value = ByteBuffer.wrap(rocksIterator.value());
        if (value.compareTo(end) > 0) {
            return null;
        }
        rocksIterator.next();
        return new KevValuePair(ByteBuffer.wrap(key), value);
    }
}
