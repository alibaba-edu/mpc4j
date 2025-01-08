package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirServer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirPtoDesc.PtoStep;

/**
 * Vectorized PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class VectorizedStdIdxPirServer extends AbstractStdIdxPirServer implements StdIdxPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Vectorized PIR params
     */
    private final VectorizedStdIdxPirParams params;
    /**
     * partition size
     */
    protected int partitionSize;
    /**
     * partition bit-length
     */
    protected int partitionBitLength;
    /**
     * partition byte length
     */
    protected int partitionByteLength;
    /**
     * BFV plaintext in NTT form
     */
    private List<List<byte[]>> encodedDatabase;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * relin keys
     */
    private byte[] relinKeys;
    /**
     * Galois keys
     */
    private byte[] galoisKeys;
    /**
     * query payload size
     */
    private int queryPayloadSize;
    /**
     * simple hash bin
     */
    int[][] hashBin;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * cuckoo hash factor
     */
    private final double cuckooFactor;
    /**
     * database
     */
    private long[][][] db;
    /**
     * instance num
     */
    private int serverNum;

    public VectorizedStdIdxPirServer(Rpc serverRpc, Party clientParty, VectorizedStdIdxPirConfig config) {
        super(VectorizedStdIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        hashNum = IntCuckooHashBinFactory.getHashNum(IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE);
        cuckooFactor = 1.2;
        params = config.getStdIdxPirParams();
    }

    @Override
    public void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        setInitInput(database, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int binNum = (int) Math.ceil(cuckooFactor * maxBatchNum);
        byte[][] hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        int[] totalIndex = IntStream.range(0, n).toArray();
        IntHashBin intHashBin = new SimpleIntHashBin(envType, binNum, n, hashKeys);
        intHashBin.insertItems(totalIndex);
        int maxBinSize = IntStream.range(0, binNum)
            .mapToObj(intHashBin::binSize)
            .max(Integer::compare)
            .orElse(0);
        assert maxBinSize > 0;
        hashBin = new int[binNum][];
        for (int i = 0; i < binNum; i++) {
            hashBin[i] = new int[intHashBin.binSize(i)];
            for (int j = 0; j < intHashBin.binSize(i); j++) {
                hashBin[i][j] = intHashBin.getBin(i)[j];
            }
        }
        partitionSize = CommonUtils.getUnitNum(l, params.getPlainModulusBitLength() - 1);
        params.calculateDimensions(maxBinSize);
        ZlDatabase[] databases = database.partitionZl(params.getPlainModulusBitLength() - 1);
        sendOtherPartyPayload(
            PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), Arrays.stream(hashKeys).collect(Collectors.toList())
        );
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        List<byte[]> publicKeysPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal());
        MpcAbortPreconditions.checkArgument(publicKeysPayload.size() == 3);

        stopWatch.start();
        publicKey = publicKeysPayload.remove(0);
        relinKeys = publicKeysPayload.remove(0);
        galoisKeys = publicKeysPayload.remove(0);
        // vectorized batch pir setup
        int perServerCapacity = params.getPolyModulusDegree() / params.firstTwoDimensionSize;
        serverNum = CommonUtils.getUnitNum(binNum, perServerCapacity);
        queryPayloadSize = serverNum * params.getDimension();
        db = new long[serverNum][][];
        int previousIdx = 0;
        for (int i = 0; i < serverNum; i++) {
            int offset = Math.min(perServerCapacity, binNum - previousIdx);
            for (int j = previousIdx; j < previousIdx + offset; j++) {
                long[][] coeffs = vectorizedPirSetup(hashBin[j], databases);
                mergeToDb(coeffs, j - previousIdx, i);
            }
            previousIdx += offset;
            rotateDbCols(i);
        }
        IntStream intStream = parallel ? IntStream.range(0, serverNum).parallel() : IntStream.range(0, serverNum);
        encodedDatabase = intStream
            .mapToObj(i -> VectorizedStdIdxPirNativeUtils.preprocessDatabase(params.getEncryptionParams(), db[i]))
            .collect(Collectors.toList());
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, encodeTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        List<byte[]> clientQueryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == queryPayloadSize);

        stopWatch.start();
        IntStream intStream = parallel ? IntStream.range(0, serverNum).parallel() : IntStream.range(0, serverNum);
        List<byte[]> responsePayload = intStream.mapToObj(i ->
                VectorizedStdIdxPirNativeUtils.generateReply(
                    params.getEncryptionParams(),
                    clientQueryPayload.subList(i * params.getDimension(), (i + 1) * params.getDimension()),
                    encodedDatabase.get(i),
                    publicKey,
                    relinKeys,
                    galoisKeys,
                    params.firstTwoDimensionSize,
                    params.thirdDimensionSize,
                    partitionSize))
            .flatMap(Collection::stream).toList();
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    private long[][] vectorizedPirSetup(int[] binItems, ZlDatabase[] databases) {
        int roundedNum = (int) (Math.pow(params.firstTwoDimensionSize, 2) * params.thirdDimensionSize);
        long[][] items = new long[roundedNum][partitionSize];
        for (int i = 0; i < roundedNum; i++) {
            if (i < binItems.length) {
                for (int j = 0; j < partitionSize; j++) {
                    items[i][j] = IntUtils.fixedByteArrayToNonNegInt(databases[j].getBytesData(binItems[i]));
                }
            } else {
                items[i] = IntStream.range(0, partitionSize).mapToLong(j -> 1L).toArray();
            }
        }
        int plaintextNum = CommonUtils.getUnitNum(roundedNum, params.firstTwoDimensionSize) * partitionSize;
        int plaintextsPerChunk = CommonUtils.getUnitNum(roundedNum, params.firstTwoDimensionSize);
        long[][] coeffs = new long[plaintextNum][params.getPolyModulusDegree()];
        for (int i = 0; i < roundedNum; i++) {
            int plaintextIndex = i / params.firstTwoDimensionSize;
            int slot = (i * params.gap) % params.rowSize;
            for (int j = 0; j < partitionSize; j++) {
                if (plaintextIndex >= plaintextNum) {
                    throw new AssertionError(String.format(
                        "Error: Out-of-bounds access at ciphertext_idx = %d, slot = %d", plaintextIndex, slot
                    ));
                }
                coeffs[plaintextIndex][slot] = items[i][j];
                plaintextIndex += plaintextsPerChunk;
            }
        }
        return coeffs;
    }

    private void mergeToDb(long[][] plaintexts, int rotationIndex, int instanceIndex) {
        int roundedNum = (int) (Math.pow(params.firstTwoDimensionSize, 2) * params.thirdDimensionSize);
        int plaintextNum = CommonUtils.getUnitNum(roundedNum, params.firstTwoDimensionSize) * partitionSize;
        int rotateAmount = rotationIndex;
        if (rotateAmount == 0) {
            db[instanceIndex] = new long[plaintextNum][params.getPolyModulusDegree()];
        }
        if (rotateAmount >= params.gap) {
            rotateAmount = rotateAmount - params.gap;
        }
        for (int j = 0; j < plaintextNum; j++) {
            if (rotationIndex >= params.gap) {
                plaintexts[j] = PirUtils.rotateVectorCol(plaintexts[j]);
            }
            long[] rotated = PirUtils.plaintextRotate(plaintexts[j], rotateAmount);
            for (int k = 0; k < db[instanceIndex][j].length; k++) {
                db[instanceIndex][j][k] = db[instanceIndex][j][k] + rotated[k];
            }
        }
    }

    private void rotateDbCols(int instanceIndex) {
        int roundedNum = (int) (Math.pow(params.firstTwoDimensionSize, 2) * params.thirdDimensionSize);
        int plaintextNum = CommonUtils.getUnitNum(roundedNum, params.firstTwoDimensionSize) * partitionSize;
        for (int idx = 0; idx < plaintextNum; idx += params.firstTwoDimensionSize) {
            for (int i = 0; i < params.firstTwoDimensionSize; i++) {
                db[instanceIndex][idx + i] = PirUtils.plaintextRotate(db[instanceIndex][idx + i], i * params.gap);
            }
        }
    }
}