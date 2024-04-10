package edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirParams;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirPtoDesc.*;

/**
 * Vectorized Batch PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Mr23BatchIndexPirClient extends AbstractBatchIndexPirClient {

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
     * secret key
     */
    private byte[] secretKey;
    /**
     * bin index and retrieval index map
     */
    private Map<Integer, Integer> binIndexRetrievalIndexMap;
    /**
     * simple hash bin
     */
    private int[][] hashBin;
    /**
     * entry slot
     */
    int[][] entrySlot;
    /**
     * bin num
     */
    private int binNum;
    /**
     * cuckoo hash bin type
     */
    private final IntCuckooHashBinType cuckooHashBinType;
    /**
     * hash num
     */
    private final int hashNum;
    /**
     * cuckoo hash factor
     */
    private final double cuckooFactor;
    /**
     * client instance num
     */
    private int clientNum;

    public Mr23BatchIndexPirClient(Rpc clientRpc, Party serverParty, Mr23BatchIndexPirConfig config) {
        super(Mr23BatchIndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        cuckooHashBinType = IntCuckooHashBinType.NO_STASH_NAIVE;
        hashNum = IntCuckooHashBinFactory.getHashNum(cuckooHashBinType);
        cuckooFactor = 1.2;
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(serverElementSize, elementBitLength, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive hash keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == hashNum);
        hashKeys = hashKeyPayload.toArray(new byte[0][]);

        // client generate simple hash bin
        stopWatch.start();
        binNum = (int) Math.ceil(cuckooFactor * maxRetrievalSize);
        int maxBinSize = generateSimpleHashBin();
        initBatchPirParams(maxBinSize);
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        // client generate key pair
        stopWatch.start();
        List<byte[]> keyPairPayload = generateKeyPairPayload();
        DataPacketHeader keyPairHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keyPairHeader, keyPairPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyGenTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<Integer, byte[]> pir(List<Integer> indexList) throws MpcAbortException {
        setPtoInput(indexList);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // convert database index to bucket index
        List<Integer> binIndexList = updateBinIndex();
        List<byte[]> clientQueryPayload = generateQuery(binIndexList);
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        // receive server response
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        // decode response
        stopWatch.start();
        Map<Integer, byte[]> retrievalResult = handleServerResponse(responsePayload, binIndexList);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return retrievalResult;
    }

    /**
     * init batch PIR params.
     *
     * @param binSize bin size.
     */
    private void initBatchPirParams(int binSize) {
        params = Mr23SingleIndexPirParams.DEFAULT_PARAMS;
        partitionBitLength = params.getPlainModulusBitLength() - 1;
        partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        params.calculateDimensions(binSize);
    }

    /**
     * update retrieval index list.
     *
     * @return bin index list.
     */
    private List<Integer> updateBinIndex() {
        IntNoStashCuckooHashBin cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType, cuckooHashBinType, maxRetrievalSize, binNum, hashKeys
        );
        int[] indicesArray = IntStream.range(0, retrievalSize).map(indexList::get).toArray();
        cuckooHashBin.insertItems(indicesArray);
        List<Integer> binIndex = new ArrayList<>(binNum);
        binIndexRetrievalIndexMap = new HashMap<>(retrievalSize);
        for (int i = 0; i < binNum; i++) {
            if (cuckooHashBin.getBinHashIndex(i) == -1) {
                binIndex.add(-1);
            } else {
                for (int j = 0; j < hashBin[i].length; j++) {
                    if (hashBin[i][j] == cuckooHashBin.getBinEntry(i)) {
                        binIndex.add(j);
                        binIndexRetrievalIndexMap.put(i, cuckooHashBin.getBinEntry(i));
                        break;
                    }
                }
            }
        }
        return binIndex;
    }

    /**
     * generate query.
     *
     * @param retrievalIndex retrieval index.
     * @return client query.
     */
    private List<byte[]> generateQuery(List<Integer> retrievalIndex) {
        List<byte[]> clientQuery = new ArrayList<>();
        int perClientCapacity = params.getPolyModulusDegree() / params.firstTwoDimensionSize;
        clientNum = CommonUtils.getUnitNum(binNum, perClientCapacity);
        entrySlot = new int[clientNum][];
        int previousIdx = 0;
        for (int i = 0; i < clientNum; i++) {
            int offset = Math.min(perClientCapacity, binNum - previousIdx);
            entrySlot[i] = new int[offset];
            long[][][] plainQueries = new long[offset][params.getDimension()][params.getPolyModulusDegree()];
            for (int j = previousIdx; j < previousIdx + offset; j++) {
                int currentSlot = 0;
                if (retrievalIndex.get(j) != -1) {
                    int[] slotPositions = computeIndices(retrievalIndex.get(j));
                    for (int k = 0; k < params.getDimension(); k++) {
                        int slotPos = slotPositions[k];
                        int rotatedSlot = (currentSlot + slotPos) % params.firstTwoDimensionSize;
                        plainQueries[j - previousIdx][k][(rotatedSlot * params.gap) % params.rowSize] = 1;
                        currentSlot = (currentSlot + slotPos) % params.firstTwoDimensionSize;
                    }
                }
                // saving first chunk location to be used for decoding at the end
                entrySlot[i][j - previousIdx] = currentSlot;
            }
            previousIdx += offset;
            clientQuery.addAll(mergePirQueries(plainQueries));
        }
        return clientQuery;
    }

    /**
     * merge client queries.
     *
     * @param plainQueries plain queries.
     * @return merged queries.
     */
    private List<byte[]> mergePirQueries(long[][][] plainQueries) {
        long[][] query = new long[params.getDimension()][params.getPolyModulusDegree()];
        for (int j = 0; j < params.getDimension(); j++) {
            for (int i = 0; i < plainQueries.length; i++) {
                int rotateAmount = i;
                if (i >= params.gap) {
                    plainQueries[i][j] = PirUtils.rotateVectorCol(plainQueries[i][j]);
                    rotateAmount = rotateAmount - params.gap;
                }
                long[] rotated = PirUtils.plaintextRotate(plainQueries[i][j], rotateAmount);
                for (int k = 0; k < params.getPolyModulusDegree(); k++) {
                    query[j][k] = query[j][k] + rotated[k];
                }
            }
        }
        return Mr23BatchIndexPirNativeUtils.generateQuery(params.getEncryptionParams(), publicKey, secretKey, query);
    }

    /**
     * generate key pair payload.
     *
     * @return key pair payload.
     */
    private List<byte[]> generateKeyPairPayload() {
        List<byte[]> keyPair = Mr23BatchIndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 4);
        publicKey = keyPair.remove(0);
        secretKey = keyPair.remove(0);
        byte[] relinKeys = keyPair.remove(0);
        byte[] galoisKeys = keyPair.remove(0);
        List<byte[]> keyPairPayload = new ArrayList<>();
        keyPairPayload.add(publicKey);
        keyPairPayload.add(relinKeys);
        keyPairPayload.add(galoisKeys);
        return keyPairPayload;
    }

    /**
     * generate simple hash bin.
     *
     * @return max bin size.
     */
    private int generateSimpleHashBin() {
        int[] totalIndex = IntStream.range(0, serverElementSize).toArray();
        IntHashBin intHashBin = new SimpleIntHashBin(envType, binNum, serverElementSize, hashKeys);
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
     * compute indices.
     *
     * @param desiredIndex desired index.
     * @return indices.
     */
    private int[] computeIndices(int desiredIndex) {
        int dimension = params.getDimension();
        int[] dimensionSize = params.getDimensionSize();
        int[] indices = new int[dimension];
        indices[2] = desiredIndex / (dimensionSize[0] * dimensionSize[1]);
        indices[1] = (desiredIndex - indices[2] * dimensionSize[0] * dimensionSize[1]) / dimensionSize[0];
        indices[0] = desiredIndex % dimensionSize[0];
        return indices;
    }

    /**
     * handle server response.
     *
     * @param serverResponse response ciphertext.
     * @param binIndex       bin index.
     * @return retrieval result map.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private Map<Integer, byte[]> handleServerResponse(List<byte[]> serverResponse, List<Integer> binIndex)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverResponse.size() % clientNum == 0);
        ZlDatabase[] databases;
        int byteL = CommonUtils.getByteLength(partitionBitLength);
        IntStream intStream = IntStream.range(0, serverResponse.size());
        intStream = parallel ? intStream.parallel() : intStream;
        List<long[]> coeffs = intStream
            .mapToObj(index -> Mr23BatchIndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(), secretKey, serverResponse.get(index))
            )
            .collect(Collectors.toList());
        int maxEmptySlots = params.firstTwoDimensionSize;
        int perClientCapacity = params.getPolyModulusDegree() / maxEmptySlots;
        byte[][][] items = new byte[partitionSize][binIndex.size()][];
        int numChunkCtx = CommonUtils.getUnitNum(partitionSize, maxEmptySlots);
        int previousIdx = 0, idx;
        for (int i = 0; i < clientNum; i++) {
            int startIdx = i * numChunkCtx;
            int numQueries = Math.min(perClientCapacity, binNum - previousIdx);
            int remainingSlotsEntry = partitionSize;
            for (int j = 0; j < numChunkCtx; j++) {
                int loop = Math.min(maxEmptySlots, remainingSlotsEntry);
                for (int l = 0; l < numQueries; l++) {
                    int tmp = l;
                    if (tmp >= params.gap) {
                        idx = params.rowSize;
                        tmp = tmp - params.gap;
                    } else {
                        idx = 0;
                    }
                    int entryOffset = entrySlot[i][l] * params.gap + tmp;
                    for (int k = 0; k < loop; k++) {
                        int chunkOffset = (entryOffset + (k * params.gap)) % params.rowSize;
                        byte[] item = IntUtils.nonNegIntToFixedByteArray(
                            Math.toIntExact(coeffs.get(j + startIdx)[idx + chunkOffset]), byteL
                        );
                        items[j * maxEmptySlots + k][l + previousIdx] = item;
                    }
                }
                remainingSlotsEntry -= maxEmptySlots;
            }
            previousIdx += numQueries;
        }
        databases = IntStream.range(0, partitionSize)
            .mapToObj(i -> ZlDatabase.create(partitionBitLength, items[i]))
            .toArray(ZlDatabase[]::new);
        NaiveDatabase database = NaiveDatabase.createFromZl(elementBitLength, databases);
        // generate retrieval index and retrieval item map
        return IntStream.range(0, binNum)
            .filter(i -> binIndex.get(i) != -1)
            .boxed()
            .collect(
                Collectors.toMap(
                    integer -> binIndexRetrievalIndexMap.get(integer),
                    database::getBytesData,
                    (a, b) -> b,
                    () -> new HashMap<>(retrievalSize)
                )
            );
    }
}