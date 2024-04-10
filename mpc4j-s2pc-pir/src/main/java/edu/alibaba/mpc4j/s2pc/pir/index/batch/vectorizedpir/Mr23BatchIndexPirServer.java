package edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirParams;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirPtoDesc.*;

/**
 * Vectorized Batch PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Mr23BatchIndexPirServer extends AbstractBatchIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * vectorized Batch PIR params
     */
    private Mr23SingleIndexPirParams params;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * Galois keys
     */
    private byte[] galoisKeys;
    /**
     * Relin Keys
     */
    private byte[] relinKeys;
    /**
     * plaintext in NTT form
     */
    private List<List<byte[]>> encodedDatabase;
    /**
     * simple hash bin
     */
    int[][] hashBin;
    /**
     * bin num
     */
    private int binNum;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * plaintexts coefficient
     */
    private long[][][] db;
    /**
     * cuckoo hash factor
     */
    private final double cuckooFactor;
    /**
     * server instance num
     */
    private int serverNum;

    public Mr23BatchIndexPirServer(Rpc serverRpc, Party clientParty, Mr23BatchIndexPirConfig config) {
        super(Mr23BatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        hashNum = IntCuckooHashBinFactory.getHashNum(IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE);
        cuckooFactor = 1.2;
    }

    @Override
    public void init(NaiveDatabase database, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(database, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        binNum = (int) Math.ceil(cuckooFactor * maxRetrievalSize);
        hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        int maxBinSize = generateSimpleHashBin();
        initBatchPirParams(database, maxBinSize);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        DataPacketHeader keyPairHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> keyPairPayload = rpc.receive(keyPairHeader).getPayload();
        handleKeyPairPayload(keyPairPayload);

        stopWatch.start();
        // vectorized batch pir setup
        encodedDatabase = new ArrayList<>();
        int perServerCapacity = params.getPolyModulusDegree() / params.firstTwoDimensionSize;
        serverNum = CommonUtils.getUnitNum(binNum, perServerCapacity);
        db = new long[serverNum][][];
        int previousIdx = 0;
        for (int i = 0; i < serverNum; i++) {
            int offset = Math.min(perServerCapacity, binNum - previousIdx);
            for (int j = previousIdx; j < previousIdx + offset; j++) {
                long[][] coeffs = vectorizedPirSetup(hashBin[j]);
                mergeToDb(coeffs, j - previousIdx, i);
            }
            previousIdx += offset;
            rotateDbCols(i);
        }
        IntStream intStream = IntStream.range(0, serverNum);
        intStream = parallel ? intStream.parallel() : intStream;
        encodedDatabase = intStream
            .mapToObj(i -> Mr23BatchIndexPirNativeUtils.preprocessDatabase(params.getEncryptionParams(), db[i]))
            .collect(Collectors.toList());
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, encodeTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive client query
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == serverNum * params.getDimension());

        // generate response
        stopWatch.start();
        IntStream intStream = IntStream.range(0, serverNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> responsePayload = intStream.mapToObj(i ->
            Mr23BatchIndexPirNativeUtils.generateReply(
                params.getEncryptionParams(),
                clientQueryPayload.subList(i * params.getDimension(), (i + 1) * params.getDimension()),
                encodedDatabase.get(i),
                publicKey,
                relinKeys,
                galoisKeys,
                params.firstTwoDimensionSize,
                params.thirdDimensionSize,
                partitionSize))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates response");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * generate simple hash bin.
     *
     * @return max bin size.
     */
    private int generateSimpleHashBin() {
        int[] totalIndex = IntStream.range(0, num).toArray();
        IntHashBin intHashBin = new SimpleIntHashBin(envType, binNum, num, hashKeys);
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
        intHashBin.clear();
        return maxBinSize;
    }

    /**
     * rotate plaintexts.
     *
     * @param index server instance index.
     */
    private void rotateDbCols(int index) {
        int roundedNum = (int) (Math.pow(params.firstTwoDimensionSize, 2) * params.thirdDimensionSize);
        int plaintextNum = CommonUtils.getUnitNum(roundedNum, params.firstTwoDimensionSize) * partitionSize;
        for (int idx = 0; idx < plaintextNum; idx += params.firstTwoDimensionSize) {
            for (int i = 0; i < params.firstTwoDimensionSize; i++) {
                db[index][idx + i] = PirUtils.plaintextRotate(db[index][idx + i], i * params.gap);
            }
        }
    }

    /**
     * vectorized PIR setup.
     *
     * @param binItems bin items.
     * @return plaintexts.
     */
    private long[][] vectorizedPirSetup(int[] binItems) {
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

    /**
     * merge plaintexts.
     *
     * @param plaintexts    plaintexts.
     * @param rotationIndex rotation index.
     * @param index         server instance index
     */
    private void mergeToDb(long[][] plaintexts, int rotationIndex, int index) {
        int roundedNum = (int) (Math.pow(params.firstTwoDimensionSize, 2) * params.thirdDimensionSize);
        int plaintextNum = CommonUtils.getUnitNum(roundedNum, params.firstTwoDimensionSize) * partitionSize;
        int rotateAmount = rotationIndex;
        if (rotateAmount == 0) {
            db[index] = new long[plaintextNum][params.getPolyModulusDegree()];
        }
        if (rotateAmount >= params.gap) {
            rotateAmount = rotateAmount - params.gap;
        }
        for (int j = 0; j < plaintextNum; j++) {
            if (rotationIndex >= params.gap) {
                plaintexts[j] = PirUtils.rotateVectorCol(plaintexts[j]);
            }
            long[] rotated = PirUtils.plaintextRotate(plaintexts[j], rotateAmount);
            for (int k = 0; k < db[index][j].length; k++) {
                db[index][j][k] = db[index][j][k] + rotated[k];
            }
        }
    }

    /**
     * handle key pair payload.
     *
     * @param keyPairPayload key pair payload.
     * @exception MpcAbortException the protocol failure aborts.
     */
    private void handleKeyPairPayload(List<byte[]> keyPairPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(keyPairPayload.size() == 3);
        publicKey = keyPairPayload.remove(0);
        relinKeys = keyPairPayload.remove(0);
        galoisKeys = keyPairPayload.remove(0);
    }

    /**
     * init batch PIR params.
     *
     * @param database database
     * @param binSize  bin size
     */
    private void initBatchPirParams(NaiveDatabase database, int binSize) {
        params = Mr23SingleIndexPirParams.DEFAULT_PARAMS;
        partitionSize = CommonUtils.getUnitNum(elementBitLength, params.getPlainModulusBitLength() - 1);
        params.calculateDimensions(binSize);
        databases = database.partitionZl(params.getPlainModulusBitLength() - 1);
    }
}
