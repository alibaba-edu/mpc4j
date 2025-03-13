package edu.alibaba.work.femur.demo.naive;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.work.femur.demo.AbstractFemurDemoPirServer;
import edu.alibaba.work.femur.demo.FemurStatus;
import edu.alibaba.work.femur.redis.RedisHeaderBytesArray;
import edu.alibaba.work.femur.redis.RedisHeaderSet;
import gnu.trove.map.TLongObjectMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.IntStream;

/**
 * Naive Femur demo PIR server.
 *
 * @author Weiran Liu
 * @date 2024/9/19
 */
public class NaiveFemurDemoRedisPirServer extends AbstractFemurDemoPirServer {
    /**
     * host
     */
    private final String host;
    /**
     * port
     */
    private final int port;
    /**
     * time out
     */
    private final int timeout;
    /**
     * key-value array
     */
    private RedisHeaderBytesArray redisHeaderKeyValueArray;
    /**
     * clients
     */
    private RedisHeaderSet redisHeaderClientSet;
    /**
     * write lock
     */
    private final WriteLock writeLock;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;

    public NaiveFemurDemoRedisPirServer(NaiveFemurDemoRedisPirConfig config) {
        super(config);
        host = config.getHost();
        port = config.getPort();
        timeout = config.getTimeout();
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        pgmIndexLeafEpsilon = config.getPgmIndexLeafEpsilon();
    }

    @Override
    public void init(int n, int l) {
        setInitInput(n, l);
        redisHeaderKeyValueArray = new RedisHeaderBytesArray(host, port, timeout, "NAIVE_REDIS_DEMO_DATA");
        redisHeaderKeyValueArray.clear();
        redisHeaderClientSet = new RedisHeaderSet(host, port, timeout, "NAIVE_REDIS_DEMO_CLIENTS");
        redisHeaderClientSet.clear();
    }

    @Override
    public void setDatabase(TLongObjectMap<byte[]> keyValueDatabase) {
        checkSetDatabaseInput(keyValueDatabase);
        long[] keyArray = keyValueDatabase.keys();
        Arrays.sort(keyArray);
        // set data
        byte[][] keyValueArray = IntStream.range(0, n)
            .mapToObj(i -> {
                long key = keyArray[i];
                byte[] keyEntry = new byte[Long.BYTES + byteL];
                byte[] keyBytes = LongUtils.longToByteArray(key);
                System.arraycopy(keyBytes, 0, keyEntry, 0, Long.BYTES);
                System.arraycopy(keyValueDatabase.get(key), 0, keyEntry, Long.BYTES, byteL);
                return keyEntry;
            })
            .toArray(byte[][]::new);
        // create PGM-index
        LongApproxPgmIndex.LongApproxPgmIndexBuilder builder = new LongApproxPgmIndex.LongApproxPgmIndexBuilder()
            .setSortedKeys(keyArray)
            .setEpsilon(pgmIndexLeafEpsilon)
            .setEpsilonRecursive(CommonConstants.PGM_INDEX_RECURSIVE_EPSILON);
        LongApproxPgmIndex pgmIndex = builder.build();
        writeLock.lock();
        redisHeaderKeyValueArray.putArray(keyValueArray);
        pgmIndexPair = Pair.of(Long.toUnsignedString(redisHeaderKeyValueArray.getVersion()), pgmIndex);
        writeLock.unlock();
    }

    @Override
    public boolean updateValue(long key, byte[] value) {
        int[] index = pgmIndexPair.getValue().approximateIndexRangeOf(key);
        if (index[0] < 0) {
            return false;
        }
        byte[] keyBytes = LongUtils.longToByteArray(key);
        int[] indexes = IntStream.range(index[1], index[2]).toArray();
        writeLock.lock();
        byte[][] keyEntries = redisHeaderKeyValueArray.gets(indexes);
        writeLock.unlock();
        for (int i = 0; i < index[2] - index[1]; i++) {
            byte[] keyEntry = keyEntries[i];
            if (Arrays.equals(keyBytes, 0, Long.BYTES, keyEntry, 0, Long.BYTES)) {
                // find the target key, update value
                byte[] updateKeyEntry = new byte[Long.BYTES + byteL];
                System.arraycopy(keyBytes, 0, updateKeyEntry, 0, Long.BYTES);
                System.arraycopy(value, 0, updateKeyEntry, Long.BYTES, byteL);
                writeLock.lock();
                redisHeaderKeyValueArray.updateAt(index[1] + i, updateKeyEntry);
                writeLock.unlock();
                return true;
            }
        }
        return false;
    }

    @Override
    public Pair<FemurStatus, List<byte[]>> register(List<byte[]> registerPayload) {
        if (!init) {
            return Pair.of(FemurStatus.SERVER_NOT_INIT, new LinkedList<>());
        }
        if (pgmIndexPair == null) {
            return Pair.of(FemurStatus.SERVER_NOT_KVDB, new LinkedList<>());
        }
        MathPreconditions.checkEqual("registerPayload.size()", "1", registerPayload.size(), 1);
        String clientId = new String(registerPayload.get(0), CommonConstants.DEFAULT_CHARSET);
        writeLock.lock();
        redisHeaderClientSet.add(clientId);
        writeLock.unlock();
        List<byte[]> paramsPayload = new ArrayList<>();
        paramsPayload.add(IntUtils.intToByteArray(n));
        paramsPayload.add(IntUtils.intToByteArray(l));
        return Pair.of(FemurStatus.SERVER_SUCC_RES, paramsPayload);
    }

    @Override
    public Pair<FemurStatus, List<byte[]>> response(List<byte[]> queryPayload) {
        MathPreconditions.checkEqual("queryPayload.size()", "1", queryPayload.size(), 4);
        String clientId = new String(queryPayload.get(0), CommonConstants.DEFAULT_CHARSET);
        // client does not register
        writeLock.lock();
        boolean containsId = redisHeaderClientSet.contains(clientId);
        writeLock.unlock();
        if (!containsId) {
            return Pair.of(FemurStatus.CLIENT_NOT_REGS, new LinkedList<>());
        }
        int leftBound = IntUtils.byteArrayToInt(queryPayload.get(2));
        int range = IntUtils.byteArrayToInt(queryPayload.get(3));
        while (leftBound < 0) {
            leftBound += n;
        }
        int[] indexes;
        if (leftBound + range < n) {
            indexes = IntStream.range(leftBound, leftBound + range).toArray();
        } else {
            indexes = new int[range];
            int[] leftIndexes = IntStream.range(leftBound, n).toArray();
            System.arraycopy(leftIndexes, 0, indexes, 0, leftIndexes.length);
            int[] rightIndexes = IntStream.range(0, leftBound + range - n)
                .map(index -> index % n)
                .toArray();
            System.arraycopy(rightIndexes, 0, indexes, leftIndexes.length, rightIndexes.length);
        }
        String version = new String(queryPayload.get(1), CommonConstants.DEFAULT_CHARSET);
        writeLock.lock();
        // PGM-index version mismatch
        if (!version.equals(pgmIndexPair.getKey())) {
            writeLock.unlock();
            return Pair.of(FemurStatus.HINT_V_MISMATCH, new LinkedList<>());
        } else {
            byte[][] responses = redisHeaderKeyValueArray.gets(indexes);
            writeLock.unlock();
            List<byte[]> responsePayload = Arrays.stream(responses)
                .peek(bytes -> Preconditions.checkArgument(Objects.nonNull(bytes)))
                .toList();
            return Pair.of(FemurStatus.SERVER_SUCC_RES, responsePayload);
        }
    }

    @Override
    public void reset() {
        writeLock.lock();
        innerReset();
        if (redisHeaderKeyValueArray != null) {
            redisHeaderKeyValueArray.clear();
            redisHeaderKeyValueArray.close();
            redisHeaderKeyValueArray = null;
        }
        if (redisHeaderClientSet != null) {
            redisHeaderClientSet.clear();
            redisHeaderClientSet.close();
            redisHeaderClientSet = null;
        }
        writeLock.unlock();
    }
}