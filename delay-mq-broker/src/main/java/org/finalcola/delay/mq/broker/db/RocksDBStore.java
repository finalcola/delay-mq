package org.finalcola.delay.mq.broker.db;

import lombok.Getter;
import org.finalcola.delay.mq.broker.config.RocksDBConfig;
import org.finalcola.delay.mq.broker.model.KeyValuePair;
import org.rocksdb.*;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author: finalcola
 * @date: 2023/3/14 23:07
 */
public class RocksDBStore {

    @Getter
    private final RocksDBConfig config;
    @Getter
    private volatile boolean isRunning;
    private RocksDB rocksDB;
    private DBOptions dbOptions;
    private List<ColumnFamilyDescriptor> cfDescriptors;
    private List<ColumnFamilyHandle> cfHandles;
    private ColumnFamilyOptions cfOptions;
    private Map<Integer, RocksDBAccess> rocksDBAccessMap;

    public RocksDBStore(@Nonnull RocksDBConfig config) {
        this.config = config;
    }

    public synchronized void start() throws RocksDBException {
        initDbOptions();
        initCfDescriptors();
        cfHandles = new ArrayList<>();
        rocksDB = RocksDB.open(dbOptions, config.getPath(), cfDescriptors, cfHandles);
        rocksDBAccessMap = IntStream.range(0, config.getPartitionCount())
                .mapToObj(index -> new RocksDBAccess(index, rocksDB, cfHandles.get(index + 1)))
                .collect(Collectors.toMap(RocksDBAccess::getPartitionId, Function.identity()));
        isRunning = true;
    }

    public synchronized void stop() {
        isRunning = false;
        cfHandles.forEach(AbstractImmutableNativeReference::close);
        rocksDB.close();
    }

    public void put(int partitionId, @Nonnull List<KeyValuePair> keyValuePairs) throws RocksDBException {
        rocksDBAccessMap.get(partitionId).batchPut(keyValuePairs);
    }

    public void deleteRange(int partitionId, @Nonnull ByteBuffer start, @Nonnull ByteBuffer end) throws RocksDBException {
        rocksDBAccessMap.get(partitionId).deleteRange(start, end);
    }

    public RocksDBRangeIterator range(int partitionId, @Nonnull ByteBuffer start, @Nonnull ByteBuffer end) {
        return rocksDBAccessMap.get(partitionId).range(start, end);
    }

    private void initCfDescriptors() {
        // 初始化cf配置
        initCfOptions();
        int partitionCount = config.getPartitionCount();
        // 每个分区对应一个表
        List<ColumnFamilyDescriptor> descriptors = IntStream.range(0, partitionCount)
                .mapToObj(index -> {
                    String cfName = ColumnFamilyType.MSG_PARTITION.getName().toLowerCase() + "_" + index;
                    ColumnFamilyOptions cfOptions = new ColumnFamilyOptions();
                    return new ColumnFamilyDescriptor(cfName.getBytes(UTF_8), cfOptions);
                })
                .collect(Collectors.toList());
        descriptors.add(0, new ColumnFamilyDescriptor(ColumnFamilyType.DEFAULT.getName().getBytes(UTF_8)));
        cfDescriptors = descriptors;
    }

    private void initCfOptions() {
        cfOptions = new ColumnFamilyOptions();
    }

    private void initDbOptions() {
        dbOptions = new DBOptions()
                .setCreateIfMissing(true)
                .setCreateMissingColumnFamilies(true);
    }
}