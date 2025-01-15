package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.HintCpKsPirServer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleBinCpKsPirDesc.*;
import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleBinCpKsPirDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleBinCpKsPirDesc.getInstance;

/**
 * Simple bin client-specific preprocessing KSPIR server.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SimpleBinCpKsPirServer<T> extends AbstractCpKsPirServer<T> implements HintCpKsPirServer<T> {
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
     * columns
     */
    private int columns;
    /**
     * partition count
     */
    private int partition;

    public SimpleBinCpKsPirServer(Rpc serverRpc, Party clientParty, SimpleBinCpKsPirConfig config) {
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
        Hash hash = HashFactory.createInstance(envType, DIGEST_BYTE_L);
        Map<T, byte[]> keyValueConcatMap = new HashMap<>();
        keyValueMap.keySet().forEach(key -> {
            byte[] digest = hash.digestToBytes(ObjectUtils.objectToByteArray(key));
            keyValueConcatMap.put(key, Bytes.concat(digest, keyValueMap.get(key)));
        });
        partition = byteL + DIGEST_BYTE_L;
        int binNum = (int) Math.ceil(Math.sqrt((long) n * (long) partition));
        byte[] hashKey = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        RandomPadHashBin<T> hashBin = new RandomPadHashBin<>(envType, binNum, n, new byte[][]{hashKey});
        hashBin.insertItems(keyValueMap.keySet());
        int maxBinSize = IntStream.range(0, binNum).map(hashBin::binSize).filter(i -> i >= 0).max().orElse(0);
        List<byte[]> hashBinInfoPayload = new ArrayList<>();
        hashBinInfoPayload.add(hashKey);
        hashBinInfoPayload.add(IntUtils.intToByteArray(maxBinSize));
        sendOtherPartyPayload(PtoStep.SERVER_SEND_HASH_BIN_INFO.ordinal(), hashBinInfoPayload);
        stopWatch.stop();
        long initHashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initHashBinTime, "Server generates hash bin");

        stopWatch.start();
        rows = maxBinSize;
        columns = binNum;
        // server generates and sends the seed for the random matrix A.
        byte[] seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        List<byte[]> seedPayload = Collections.singletonList(seed);
        sendOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal(), seedPayload);
        // create database
        byte[] bot = new byte[partition];
        Arrays.fill(bot, (byte) 0xFF);
        IntMatrix[] dbs = IntStream.range(0, partition)
            .mapToObj(p -> IntMatrix.createZeros(rows, columns))
            .toArray(IntMatrix[]::new);
        for (int i = 0; i < columns; i++) {
            Set<HashBinEntry<T>> binSet = hashBin.getBin(i);
            int j = 0;
            for (HashBinEntry<T> entry : binSet) {
                byte[] element = keyValueConcatMap.get(entry.getItem());
                assert element.length == partition;
                for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                    dbs[entryIndex].set(j, i, element[entryIndex] & 0xFF);
                }
                j++;
            }
            for (; j < rows; j++) {
                for (int entryIndex = 0; entryIndex < partition; entryIndex++) {
                    dbs[entryIndex].set(j, i, bot[entryIndex] & 0xFF);
                }
            }
        }
        // create hint
        IntMatrix matrixA = IntMatrix.createRandom(columns, dimension, seed);
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
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "Server generates hints");

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
}
