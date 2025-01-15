package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import com.carrotsearch.hppc.LongArrayList;
import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.HintCpKsPirServer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex.LongApproxPgmIndexBuilder;
import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimplePgmCpKsPirDesc.*;

/**
 * PGM-index client-specific preprocessing KSPIR server.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SimplePgmCpKsPirServer<T> extends AbstractCpKsPirServer<T> implements HintCpKsPirServer<T> {
    /**
     * LWE dimension
     */
    private final int dimension;
    /**
     * transpose database database
     */
    private IntMatrix[] tdbs;
    /**
     * rows
     */
    private int rows;
    /**
     * data rows
     */
    private int dataRows;
    /**
     * columns
     */
    private int columns;
    /**
     * partition count
     */
    private int partition;

    public SimplePgmCpKsPirServer(Rpc serverRpc, Party clientParty, SimplePgmCpKsPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        dimension = gaussianLweParam.getDimension();
    }

    @Override
    public void init(Map<T, byte[]> keyValueMap, int l, int matchBatchNum) throws MpcAbortException {
        setInitInput(keyValueMap, l, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // concat Hash(key) and value
        Hash keyHash = HashFactory.createInstance(envType, DIGEST_BYTE_L);
        Map<T, byte[]> keyValueConcatMap = new HashMap<>();
        keyValueMap.keySet().forEach(key -> {
            byte[] digest = keyHash.digestToBytes(ObjectUtils.objectToByteArray(key));
            keyValueConcatMap.put(key, Bytes.concat(digest, keyValueMap.get(key)));
        });
        stopWatch.stop();
        long hashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, hashBinTime, "Server generates Hash(key)||value and value map");

        stopWatch.start();
        Hash indexHash = HashFactory.createInstance(envType, Long.BYTES);
        Map<Long, T> idxKeyMap = new HashMap<>();
        keyValueMap.keySet().forEach(key -> {
            byte[] byteIdx = indexHash.digestToBytes(ObjectUtils.objectToByteArray(key));
            long idx = LongUtils.byteArrayToLong(byteIdx);
            idxKeyMap.put(idx, key);
        });
        long[] keys = idxKeyMap.keySet().stream().mapToLong(i -> i).toArray();
        Arrays.sort(keys);
        LongArrayList keyList = new LongArrayList();
        keyList.add(keys, 0, keys.length);
        LongApproxPgmIndexBuilder builder = new LongApproxPgmIndexBuilder()
            .setSortedKeys(keyList)
            .setEpsilon(EPSILON)
            .setEpsilonRecursive(EPSILON_RECURSIVE);
        LongApproxPgmIndex pgmIndex = builder.build();
        int[] sizes = SimplePgmCpKsPirDesc.getMatrixSize(n, byteL);
        // rows returned is rows + 2 * EPSILON + 3
        rows = sizes[0];
        dataRows = rows - (2 * EPSILON + 3);
        columns = sizes[1];
        partition = sizes[2];
        sendOtherPartyPayload(PtoStep.SERVER_SEND_PGM_INFO.ordinal(), Collections.singletonList(pgmIndex.toByteArray()));
        // create database
        IntMatrix[] dbs = genDbs(keyValueConcatMap, idxKeyMap, keys);
        stopWatch.stop();
        long pgmIdxTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, pgmIdxTime, "Server init PGM index");

        stopWatch.start();
        // server generates and sends the seed for the random matrix A.
        byte[] seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        List<byte[]> seedPayload = Collections.singletonList(seed);
        sendOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal(), seedPayload);
        IntMatrix matrixA = IntMatrix.createRandom(columns, dimension, seed);
        // create hint
        tdbs = new IntMatrix[partition];
        IntStream intStream = parallel ? IntStream.range(0, partition).parallel() : IntStream.range(0, partition);
        IntMatrix[] hint = intStream.mapToObj(p -> dbs[p].mul(matrixA)).toArray(IntMatrix[]::new);
        // transpose database
        IntStream.range(0, partition).forEach(p -> {
            List<byte[]> hintPayload = IntStream.range(0, rows)
                .mapToObj(rowIndex -> IntUtils.intArrayToByteArray(hint[p].getRow(rowIndex).getElements()))
                .collect(Collectors.toList());
            sendOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal(), hintPayload);
            tdbs[p] = dbs[p].transpose();
        });
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, hintTime, "Server generates hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            answer();
        }
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses query");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void answer() throws MpcAbortException {
        List<byte[]> clientQueryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == 1);
        // parse qu
        IntVector qu = IntVector.create(IntUtils.byteArrayToIntArray(clientQueryPayload.get(0)));
        MpcAbortPreconditions.checkArgument(qu.getNum() == columns);
        // generate response
        IntStream pIntStream = parallel ? IntStream.range(0, partition).parallel() : IntStream.range(0, partition);
        List<byte[]> responsePayload = pIntStream
            .mapToObj(p -> tdbs[p].leftMul(qu))
            .map(ans -> IntUtils.intArrayToByteArray(ans.getElements()))
            .toList();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }

    private IntMatrix[] genDbs(Map<T, byte[]> keyValueConcatMap, Map<Long, T> idxKeyMap, long[] keys) {
        byte[] bot = new byte[partition];
        Arrays.fill(bot, (byte) 0xFF);
        IntMatrix[] dbs = IntStream.range(0, partition)
            .mapToObj(p -> IntMatrix.createZeros(rows, columns))
            .toArray(IntMatrix[]::new);
        for (int i = 0; i < n; i++) {
            T key = idxKeyMap.get(keys[i]);
            byte[] element = keyValueConcatMap.get(key);
            assert element.length == partition;
            int colIdx = i / dataRows;
            int rowIdx = (i % dataRows) + EPSILON + 1;
            for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                dbs[entryIndex].set(rowIdx, colIdx, element[entryIndex] & 0xFF);
            }
        }
        for (int i = n; i < dataRows * columns; i++) {
            int colIdx = i / dataRows;
            int rowIdx = (i % dataRows) + EPSILON + 1;
            for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                dbs[entryIndex].set(rowIdx, colIdx, bot[entryIndex] & 0xFF);
            }
        }
        if (columns > 1) {
            for (int i = 0; i < columns; i++) {
                if (i == 0) {
                    for (int j = 0; j < EPSILON + 1; j++) {
                        for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                            dbs[entryIndex].set(j, i, bot[entryIndex] & 0xFF);
                        }
                    }
                    for (int j = 0; j < EPSILON + 2; j++) {
                        for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                            dbs[entryIndex].set(
                                j + EPSILON + 1 + dataRows, i, dbs[entryIndex].get(j + EPSILON + 1, i + 1) & 0xFF
                            );
                        }
                    }
                } else if (i == columns - 1) {
                    for (int j = 0; j < EPSILON + 1; j++) {
                        for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                            dbs[entryIndex].set(j, i, dbs[entryIndex].get(EPSILON + 1 + dataRows - j, i - 1) & 0xFF);
                        }
                    }
                    for (int j = 0; j < EPSILON + 2; j++) {
                        for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                            dbs[entryIndex].set(j + EPSILON + 1 + dataRows, i, bot[entryIndex] & 0xFF);
                        }
                    }
                } else {
                    for (int j = 0; j < EPSILON + 1; j++) {
                        for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                            dbs[entryIndex].set(j, i, dbs[entryIndex].get(EPSILON + 1 + dataRows - j, i - 1) & 0xFF);
                        }
                    }
                    for (int j = 0; j < EPSILON + 2; j++) {
                        for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                            dbs[entryIndex].set(
                                j + EPSILON + 1 + dataRows, i, dbs[entryIndex].get(j + EPSILON + 1, i + 1) & 0xFF
                            );
                        }
                    }
                }
            }
        } else {
            for (int j = 0; j < EPSILON + 1; j++) {
                for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                    dbs[entryIndex].set(j, 0, bot[entryIndex] & 0xFF);
                }
            }
            for (int j = 0; j < EPSILON + 2; j++) {
                for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                    dbs[entryIndex].set(j + EPSILON + 1 + dataRows, 0, bot[entryIndex] & 0xFF);
                }
            }
        }
        return dbs;
    }
}
