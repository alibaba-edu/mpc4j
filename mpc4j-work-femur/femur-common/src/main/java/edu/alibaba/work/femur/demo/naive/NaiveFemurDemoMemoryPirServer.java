package edu.alibaba.work.femur.demo.naive;


import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.work.femur.demo.AbstractFemurDemoPirServer;
import edu.alibaba.work.femur.demo.FemurStatus;
import gnu.trove.map.TLongObjectMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex.LongApproxPgmIndexBuilder;

/**
 * Naive Femur demo PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class NaiveFemurDemoMemoryPirServer extends AbstractFemurDemoPirServer {
    /**
     * version
     */
    private long version;
    /**
     * key-value array, which should be stored in Redis
     */
    private byte[][] keyValueArray;
    /**
     * clients, which should be stored in Redis
     */
    private Set<String> clients;
    /**
     * write lock
     */
    private final WriteLock writeLock;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;

    public NaiveFemurDemoMemoryPirServer(NaiveFemurDemoMemoryPirConfig config) {
        super(config);
        version = 0L;
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
        pgmIndexLeafEpsilon = config.getPgmIndexLeafEpsilon();
    }

    @Override
    public void init(int n, int l) {
        setInitInput(n, l);
        clients = new HashSet<>();
    }

    @Override
    public void setDatabase(TLongObjectMap<byte[]> keyValueDatabase) {
        checkSetDatabaseInput(keyValueDatabase);
        long[] keyArray = keyValueDatabase.keys();
        Arrays.sort(keyArray);
        // create PGM-index
        LongApproxPgmIndexBuilder builder = new LongApproxPgmIndexBuilder()
            .setSortedKeys(keyArray)
            .setEpsilon(pgmIndexLeafEpsilon)
            .setEpsilonRecursive(CommonConstants.PGM_INDEX_RECURSIVE_EPSILON);
        byte[][] newKeyValueArray = new byte[n][Long.BYTES + byteL];
        IntStream.range(0, n).forEach(i -> {
            long key = keyArray[i];
            byte[] keyBytes = LongUtils.longToByteArray(key);
            System.arraycopy(keyBytes, 0, newKeyValueArray[i], 0, Long.BYTES);
            System.arraycopy(keyValueDatabase.get(key), 0, newKeyValueArray[i], Long.BYTES, byteL);
        });
        LongApproxPgmIndex pgmIndex = builder.build();
        writeLock.lock();
        pgmIndexPair = Pair.of(Long.toUnsignedString(version), pgmIndex);
        keyValueArray = newKeyValueArray;
        version++;
        writeLock.unlock();
    }

    @Override
    public boolean updateValue(long key, byte[] value) {
        int[] index = pgmIndexPair.getValue().approximateIndexRangeOf(key);
        if (index[0] < 0) {
            return false;
        }
        byte[] keyBytes = LongUtils.longToByteArray(key);
        for (int i = index[1]; i < index[2]; i++) {
            if (Arrays.equals(keyBytes, 0, Long.BYTES, keyValueArray[i], 0, Long.BYTES)) {
                // find the target key, update value
                System.arraycopy(value, 0, keyValueArray[i], Long.BYTES, byteL);
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
        clients.add(clientId);
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
        if (!clients.contains(clientId)) {
            return Pair.of(FemurStatus.CLIENT_NOT_REGS, new LinkedList<>());
        }
        String version = new String(queryPayload.get(1), CommonConstants.DEFAULT_CHARSET);
        int leftRange = IntUtils.byteArrayToInt(queryPayload.get(2));
        int rangeBound = IntUtils.byteArrayToInt(queryPayload.get(3));
        writeLock.lock();
        // PGM-index version mismatch
        if (!version.equals(pgmIndexPair.getKey())) {
            writeLock.unlock();
            return Pair.of(FemurStatus.HINT_V_MISMATCH, new LinkedList<>());
        } else {
            ArrayList<byte[]> responsePayload = new ArrayList<>(rangeBound);
            responsePayload.ensureCapacity(rangeBound);
            for (int i = 0; i < rangeBound; i++) {
                byte[] response = new byte[Long.BYTES + byteL];
                int idx = (leftRange + i) % n;
                if (idx < 0) {
                    idx = idx + n;
                }
                System.arraycopy(keyValueArray[idx], 0, response, 0, Long.BYTES + byteL);
                responsePayload.add(response);
            }
            writeLock.unlock();
            return Pair.of(FemurStatus.SERVER_SUCC_RES, responsePayload);
        }
    }

    @Override
    public void reset() {
        writeLock.lock();
        innerReset();
        version = 0;
        keyValueArray = null;
        if (clients != null) {
            clients.clear();
            clients = null;
        }
        writeLock.unlock();
    }
}