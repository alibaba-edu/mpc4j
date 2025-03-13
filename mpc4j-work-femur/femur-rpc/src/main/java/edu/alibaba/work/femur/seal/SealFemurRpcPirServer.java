package edu.alibaba.work.femur.seal;

import com.carrotsearch.hppc.LongArrayList;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex.LongApproxPgmIndexBuilder;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.work.femur.AbstractFemurRpcPirServer;
import edu.alibaba.work.femur.FemurSealPirNativeUtils;
import edu.alibaba.work.femur.FemurSealPirParams;
import gnu.trove.map.TLongObjectMap;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.work.femur.seal.SealFemurRpcPirPtoDesc.*;

/**
 * PGM-index range SEAL PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public class SealFemurRpcPirServer extends AbstractFemurRpcPirServer {

    static {
        System.loadLibrary("femur-native-fhe");
    }

    /**
     * params
     */
    private final FemurSealPirParams params;
    /**
     * plaintext size
     */
    private int plaintextSize;
    /**
     * element size per plaintext
     */
    private int elementSizeOfPlaintext;
    /**
     * BFV plaintext in NTT form
     */
    private List<byte[][]> encodedDatabase;
    /**
     * query payload size
     */
    private int queryPayloadSize;
    /**
     * Galois Keys
     */
    private byte[] galoisKeys;
    /**
     * response time
     */
    private long genResponseTime;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;
    /**
     * partition num
     */
    private int partitionNum;
    /**
     * partition byte l
     */
    private int partitionByteL;

    public SealFemurRpcPirServer(Rpc serverRpc, Party clientParty, SealFemurRpcPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        params = config.getParams();
        pgmIndexLeafEpsilon = config.getPgmIndexLeafEpsilon();
    }

    @Override
    public void init(TLongObjectMap<byte[]> keyValueMap, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(keyValueMap, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // receive Galois keys
        List<byte[]> serverKeysPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal());
        MpcAbortPreconditions.checkArgument(serverKeysPayload.size() == 1);
        galoisKeys = serverKeysPayload.get(0);
        // create PGM-index
        long[] keys = keyValueMap.keys();
        Arrays.sort(keys);
        LongArrayList keyList = new LongArrayList();
        keyList.add(keys, 0, n);
        LongApproxPgmIndexBuilder builder = new LongApproxPgmIndexBuilder()
            .setSortedKeys(keyList)
            .setEpsilon(pgmIndexLeafEpsilon)
            .setEpsilonRecursive(CommonConstants.PGM_INDEX_RECURSIVE_EPSILON);
        LongApproxPgmIndex pgmIndex = builder.build();
        partitionNum = 0;
        int partitionL;
        do {
            partitionNum++;
            partitionL = CommonUtils.getUnitNum(l + Long.SIZE, partitionNum);
            partitionByteL = CommonUtils.getByteLength(partitionL);
            int coeffSizeOfElement = CommonUtils.getUnitNum(partitionByteL * Byte.SIZE, params.getPlainModulusBitLength());
            elementSizeOfPlaintext = (params.getPolyModulusDegree() / 2) / coeffSizeOfElement;
        } while (elementSizeOfPlaintext < LongApproxPgmIndex.bound(pgmIndexLeafEpsilon));
        plaintextSize = CommonUtils.getUnitNum(n, elementSizeOfPlaintext);

        // preprocess database
        List<byte[][]> partitionDatabase = new ArrayList<>();
        byte[][] database = new byte[n][partitionNum * partitionByteL];
        for (int i = 0; i < n; i++) {
            byte[] key = LongUtils.longToByteArray(keys[i]);
            assert key.length == Long.BYTES;
            byte[] value = keyValueMap.get(keys[i]);
            assert value.length == byteL;
            System.arraycopy(key, 0, database[i], 0, Long.BYTES);
            System.arraycopy(value, 0, database[i], 8, byteL);
        }
        for (int i = 0; i < partitionNum; i++) {
            byte[][] temp = new byte[n][partitionByteL];
            for (int j = 0; j < n; j++) {
                System.arraycopy(database[j], i * partitionByteL, temp[j], 0, partitionByteL);
            }
            partitionDatabase.add(temp);
        }
        encodedDatabase = IntStream.range(0, partitionNum)
            .mapToObj(i -> preprocessDatabase(partitionDatabase.get(i)))
            .toList();
        // query size
        queryPayloadSize = params.getDimension();
        // send PGM-index seed
        sendOtherPartyPayload(PtoStep.SERVER_SEND_PGM_INFO.ordinal(), Collections.singletonList(pgmIndex.toByteArray()));
        genResponseTime = 0;
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

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
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    private byte[][] preprocessDatabase(byte[][] partitionDatabase) {
        // number of FV plaintexts needed to create the d-dimensional matrix
        int[] dimensionSize = FemurSealPirNativeUtils.computeDimensionLength(plaintextSize, params.getDimension());
        int prod = Arrays.stream(dimensionSize).reduce(1, (a, b) -> a * b);
        assert (plaintextSize <= prod);
        int byteSizeOfPlaintext = elementSizeOfPlaintext * partitionByteL;
        List<long[]> coeffsList = new ArrayList<>();
        for (int i = 0; i < plaintextSize; i++) {
            byte[] processByte = new byte[byteSizeOfPlaintext * 2];
            for (int j = 0; j < elementSizeOfPlaintext; j++) {
                byte[] bytes = new byte[partitionByteL];
                if (i * elementSizeOfPlaintext + j < n) {
                    System.arraycopy(partitionDatabase[i * elementSizeOfPlaintext + j],
                        0,
                        bytes,
                        0,
                        partitionByteL);
                } else {
                    secureRandom.nextBytes(bytes);
                }
                System.arraycopy(bytes, 0, processByte, j * partitionByteL, partitionByteL);
            }
            if (i < plaintextSize - 1) {
                for (int j = 0; j < elementSizeOfPlaintext; j++) {
                    byte[] bytes = new byte[partitionByteL];
                    if (elementSizeOfPlaintext + i * elementSizeOfPlaintext + j < n) {
                        System.arraycopy(partitionDatabase[elementSizeOfPlaintext + i * elementSizeOfPlaintext + j],
                            0,
                            bytes,
                            0,
                            partitionByteL);
                    } else {
                        secureRandom.nextBytes(bytes);
                    }
                    System.arraycopy(bytes,
                        0,
                        processByte,
                        byteSizeOfPlaintext + j * partitionByteL,
                        partitionByteL);
                }
            } else {
                for (int j = 0; j < elementSizeOfPlaintext; j++) {
                    byte[] bytes = new byte[partitionByteL];
                    secureRandom.nextBytes(bytes);
                    System.arraycopy(bytes,
                        0,
                        processByte,
                        byteSizeOfPlaintext + j * partitionByteL,
                        partitionByteL);
                }
            }
            // Get the coefficients of the elements that will be packed in plaintext i
            long[] coeffs = FemurSealPirNativeUtils.convertBytesToCoeffs(
                byteSizeOfPlaintext * 2, params.getPlainModulusBitLength(), processByte
            );
            assert (coeffs.length <= params.getPolyModulusDegree());
            long[] paddingCoeffsArray = new long[params.getPolyModulusDegree()];
            System.arraycopy(coeffs, 0, paddingCoeffsArray, 0, coeffs.length);
            // Pad the rest with 1s
            IntStream.range(coeffs.length, params.getPolyModulusDegree()).forEach(j -> paddingCoeffsArray[j] = 1L);
            coeffsList.add(paddingCoeffsArray);
        }
        // Add padding plaintext to make database a matrix
        int currentPlaintextSize = coeffsList.size();
        assert (currentPlaintextSize <= plaintextSize);
        IntStream.range(0, (prod - currentPlaintextSize))
            .mapToObj(i -> IntStream.range(0, params.getPolyModulusDegree()).mapToLong(j -> 1L).toArray())
            .forEach(coeffsList::add);
        return FemurSealPirNativeUtils.transformToNtt(params.getEncryptionParams(), coeffsList).toArray(new byte[0][]);
    }

    private void answer() throws MpcAbortException {
        List<byte[]> queryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(queryPayload.size() == queryPayloadSize + 2);
        StopWatch genResponseStopWatch = new StopWatch();
        genResponseStopWatch.start();
        int leftRange = IntUtils.byteArrayToInt(queryPayload.get(queryPayloadSize));
        int rangePlaintextSize = IntUtils.byteArrayToInt(queryPayload.get(queryPayloadSize + 1));
        int[] rangeDimensionSize = FemurSealPirNativeUtils.computeDimensionLength(rangePlaintextSize, params.getDimension());
        int prod = Arrays.stream(rangeDimensionSize).reduce(1, (a, b) -> a * b);
        byte[][] rangeEncodeDatabase = new byte[prod][];
        List<byte[]> responsePayload = new ArrayList<>();
        for (int i = 0; i < partitionNum; i++) {
            int finalI = i;
            IntStream.range(0, prod)
                .forEach(j -> {
                    int idx = (leftRange + j) % plaintextSize;
                    if (idx < 0) {
                        idx = idx + plaintextSize;
                    }
                    rangeEncodeDatabase[j] = Arrays.copyOf(encodedDatabase.get(finalI)[idx],
                        encodedDatabase.get(finalI)[idx].length);
                });
            responsePayload.addAll(FemurSealPirNativeUtils.generateReply(
                params.getEncryptionParams(),
                galoisKeys,
                queryPayload.subList(0, queryPayloadSize),
                rangeEncodeDatabase,
                rangeDimensionSize
            ));
        }
        genResponseStopWatch.stop();
        genResponseTime += genResponseStopWatch.getTime(TimeUnit.MILLISECONDS);
        genResponseStopWatch.reset();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }

    public long getGenResponseTime() {
        return genResponseTime;
    }
}