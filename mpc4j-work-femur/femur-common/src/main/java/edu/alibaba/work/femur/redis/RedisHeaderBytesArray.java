package edu.alibaba.work.femur.redis;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Redis header array for long[].
 *
 * @author Weiran Liu
 * @date 2024/12/9
 */
public class RedisHeaderBytesArray {
    /**
     * max batch size, we test on Ubuntu and find that 2^19 does not work (throw invalid multibulk length Exception)
     * while MAC works fine. So, here we set 2^18, which works fine on both platforms.
     */
    private static final int MAX_BATCH_SIZE = 1 << 18;
    /**
     * Java Redis API
     */
    private final Jedis jedis;
    /**
     * header
     */
    private final String header;
    /**
     * inner version
     */
    private long version;
    /**
     * array length
     */
    private int length;

    public RedisHeaderBytesArray(String host, int port, String header) {
        this(host, port, Protocol.DEFAULT_TIMEOUT, header);
    }

    public RedisHeaderBytesArray(String host, int port, int timeOut, String header) {
        jedis = new Jedis(host, port, timeOut);
        this.header = header;
        version = 0L;
        length = 0;
    }

    public long getVersion() {
        return version;
    }

    private String getVersionHeader(long version, int counterLabel) {
        return header + "_" + version + "_" + counterLabel;
    }

    public void putArray(byte[][] bytesArray) {
        int newLength = bytesArray.length;
        long oldVersion = version;
        long newVersion = version + 1;
        Map<Integer, List<Integer>> groupIndexes = IntStream.range(0, newLength)
            .boxed()
            .collect(Collectors.groupingBy(index -> (index / MAX_BATCH_SIZE)));
        for (int counterLabel : groupIndexes.keySet()) {
            int[] batchIndexes = groupIndexes.get(counterLabel).stream().mapToInt(i -> i).toArray();
            Map<byte[], byte[]> hash = new HashMap<>(batchIndexes.length);
            for (int index : batchIndexes) {
                hash.put(IntUtils.intToByteArray(index), bytesArray[index]);
            }
            String newVersionHeader = getVersionHeader(newVersion, counterLabel);
            jedis.hmset(newVersionHeader.getBytes(), hash);
        }
        // switch to new version and delete old version
        version = newVersion;
        int oldLength = length;
        length = newLength;
        // delete old version
        for (int oldCounterLabel = 0; oldCounterLabel <= oldLength / MAX_BATCH_SIZE; oldCounterLabel++) {
            String oldVersionHeader = getVersionHeader(oldVersion, oldCounterLabel);
            jedis.del(oldVersionHeader.getBytes());
        }
    }

    /**
     * Set the specified hash field to the specified value.
     *
     * @param index index.
     * @param value value.
     * @return If the field already exists, and put just produced an update of the value, false is returned, otherwise
     * if a new field is created true is returned.
     */
    public boolean updateAt(int index, byte[] value) {
        MathPreconditions.checkNonNegativeInRange("index", index, length);
        int counterLabel = index / MAX_BATCH_SIZE;
        String versionHeader = getVersionHeader(version, counterLabel);
        jedis.hset(versionHeader.getBytes(), IntUtils.intToByteArray(index), value);
        return true;
    }

    /**
     * If key holds a hash, retrieve the value associated to the specified field. If the field is not found or the key
     * does not exist, null is returned.
     *
     * @param index index.
     * @return Bulk reply.
     */
    public byte[] get(int index) {
        MathPreconditions.checkNonNegativeInRange("index", index, length);
        String versionHeader = getVersionHeader(version, index / MAX_BATCH_SIZE);
        return jedis.hget(versionHeader.getBytes(), IntUtils.intToByteArray(index));
    }

    public byte[][] gets(int[] indexes) {
        Arrays.stream(indexes).forEach(index -> MathPreconditions.checkNonNegativeInRange("index", index, length));
        Map<Integer, List<Integer>> groupIndexes = Arrays.stream(indexes)
            .boxed()
            .collect(Collectors.groupingBy(index -> (index / MAX_BATCH_SIZE)));
        TIntObjectMap<byte[]> map = new TIntObjectHashMap<>(indexes.length);
        for (int counterLabel : groupIndexes.keySet()) {
            int[] batchIndexes = groupIndexes.get(counterLabel).stream().mapToInt(i -> i).toArray();
            byte[][] queryRange = Arrays.stream(batchIndexes)
                .mapToObj(IntUtils::intToByteArray)
                .toArray(byte[][]::new);
            String versionHeader = getVersionHeader(version, counterLabel);
            byte[][] batchResult = jedis.hmget(versionHeader.getBytes(), queryRange).toArray(new byte[0][]);
            MathPreconditions.checkEqual("result.length", "index.length", batchResult.length, batchIndexes.length);
            for (int i = 0; i < batchIndexes.length; i++) {
                Preconditions.checkNotNull(batchResult[i], batchIndexes[i] + "-th entry is null");
                map.put(batchIndexes[i], batchResult[i]);
            }
        }
        byte[][] results = new byte[indexes.length][];
        for (int i = 0; i < indexes.length; i++) {
            byte[] entry = map.get(indexes[i]);
            results[i] = Arrays.copyOf(entry, entry.length);
        }
        for (int i = 0; i < indexes.length; i++) {
            Preconditions.checkArgument(results[i] != null, i + "-th entry is null");
        }
        return results;
    }

    /**
     * Remove the specified header map. If a given header does not exist, no operation is performed.
     *
     * @return true if the key was removed, false if the key does not exist.
     */
    public boolean clear() {
        boolean success = true;
        for (int counterLabel = 0; counterLabel < length / MAX_BATCH_SIZE; counterLabel++) {
            String versionHeader = getVersionHeader(version, counterLabel);
            long result = jedis.del(versionHeader);
            success = success & (result > 0);
        }
        return success;
    }

    /**
     * Closes this stream and releases any system resources associated with it. If the stream is already closed then
     * invoking this method has no effect.
     */
    public void close() {
        jedis.close();
    }
}
