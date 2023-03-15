package org.finalcola.delay.mq.broker.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.RocksDBException;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * @author: shanshan
 * @date: 2023/3/15 23:01
 */
public class RocksDBStoreTest {

    private RocksDBStore store;

    @Before
    public void before() throws RocksDBException {
        store = new RocksDBStore(createDBConfig());
        store.start();
    }

    @After
    public void stop() {
        store.stop();
    }

    @Test
    public void putAndRange() throws RocksDBException {
        List<KevValuePair> pairList = IntStream.range(0, 10)
                .boxed()
                .flatMap(i -> {
                    String key = i + "_k";
                    String key2 = i + "_t";
                    String value = i + "";
                    KevValuePair pair1 = new KevValuePair(ByteBuffer.wrap(key.getBytes()), ByteBuffer.wrap(value.getBytes()));
                    KevValuePair pair2 = new KevValuePair(ByteBuffer.wrap(key2.getBytes()), ByteBuffer.wrap(value.getBytes()));
                    return Stream.of(pair1, pair2);
                })
                .collect(Collectors.toList());
        store.put(0, pairList);
        RocksDBRangeIterator iterator = store.range(0, ByteBuffer.wrap("1".getBytes()), ByteBuffer.wrap("5".getBytes()));
        iterator.forEachRemaining(pair -> {
            String key = new String(pair.getKey().array());
            String value = new String(pair.getValue().array());
            System.out.printf("%s -> %s%n", key, value);
        });
    }

    @Test
    public void put() {
    }

    @Test
    public void range() {
    }

    private RocksDBConfig createDBConfig() {
        return RocksDBConfig.builder()
                .partitionCount(10)
                .path("D:\\workspace\\runenv\\delay-server\\rocksdb")
                .build();
    }
}