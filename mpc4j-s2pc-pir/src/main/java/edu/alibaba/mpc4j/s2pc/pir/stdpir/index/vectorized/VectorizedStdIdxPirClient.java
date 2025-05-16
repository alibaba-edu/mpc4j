package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.DynamicSimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirClient;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.poi.util.IntList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirPtoDesc.getInstance;

/**
 * Vectorized Batch PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class VectorizedStdIdxPirClient extends AbstractStdIdxPirClient implements StdIdxPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Vectorized PIR params
     */
    private final VectorizedStdIdxPirParams params;
    /**
     * partition bit-length
     */
    private int partitionBitLength;
    /**
     * partition size
     */
    private int partitionSize;
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
    private TIntIntMap binIndexRetrievalIndexMap;
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
    private final IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType;
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

    public VectorizedStdIdxPirClient(Rpc clientRpc, Party serverParty, VectorizedStdIdxPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        cuckooHashBinType = IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE;
        hashNum = IntCuckooHashBinFactory.getHashNum(cuckooHashBinType);
        cuckooFactor = 1.2;
        params = config.getStdIdxPirParams();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        List<byte[]> hashKeyPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal());
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == hashNum);
        hashKeys = hashKeyPayload.toArray(new byte[0][]);

        stopWatch.start();
        binNum = (int) Math.ceil(cuckooFactor * maxBatchNum);
        int[] totalIndex = IntStream.range(0, n).toArray();
        DynamicSimpleIntHashBin intHashBin = new DynamicSimpleIntHashBin(envType, binNum, n, hashKeys);
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
        partitionBitLength = params.getPlainModulusBitLength() - 1;
        partitionSize = CommonUtils.getUnitNum(l, partitionBitLength);
        params.calculateDimensions(maxBinSize);
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        stopWatch.start();
        List<byte[]> keyPair = VectorizedStdIdxPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 4);
        publicKey = keyPair.remove(0);
        secretKey = keyPair.remove(0);
        byte[] relinKeys = keyPair.remove(0);
        byte[] galoisKeys = keyPair.remove(0);
        List<byte[]> publicKeysPayload = new ArrayList<>();
        publicKeysPayload.add(publicKey);
        publicKeysPayload.add(relinKeys);
        publicKeysPayload.add(galoisKeys);
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), publicKeysPayload);
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyGenTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(int[] xs) throws MpcAbortException {
        setPtoInput(xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // convert database index to bucket index
        IntList indexList = new IntList();
        IntStream.range(0, batchNum).filter(i -> !indexList.contains(xs[i])).forEach(i -> indexList.add(xs[i]));
        IntList binIndexList = updateBinIndex(indexList);
        List<byte[]> clientQueryPayload = generateQuery(binIndexList);
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), clientQueryPayload);
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());

        stopWatch.start();
        byte[][] entries = handleServerResponse(responsePayload, binIndexList, xs);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private List<byte[]> generateQuery(IntList retrievalIndex) {
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

    private IntList updateBinIndex(IntList xs) {
        IntNoStashCuckooHashBin cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType, cuckooHashBinType, maxBatchNum, binNum, hashKeys
        );
        cuckooHashBin.insertItems(xs.toArray());
        IntList binIndex = new IntList(binNum);
        binIndexRetrievalIndexMap = new TIntIntHashMap(batchNum);
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

    private int[] computeIndices(int desiredIndex) {
        int dimension = params.getDimension();
        int[] dimensionSize = params.getDimensionSize();
        int[] indices = new int[dimension];
        indices[2] = desiredIndex / (dimensionSize[0] * dimensionSize[1]);
        indices[1] = (desiredIndex - indices[2] * dimensionSize[0] * dimensionSize[1]) / dimensionSize[0];
        indices[0] = desiredIndex % dimensionSize[0];
        return indices;
    }

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
        return VectorizedStdIdxPirNativeUtils.generateQuery(params.getEncryptionParams(), publicKey, secretKey, query);
    }

    private byte[][] handleServerResponse(List<byte[]> serverResponse, IntList binIndex, int[] xs)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverResponse.size() % clientNum == 0);
        ZlDatabase[] databases;
        int byteL = CommonUtils.getByteLength(partitionBitLength);
        IntStream intStream = parallel ? IntStream.range(0, serverResponse.size()).parallel() : IntStream.range(0, serverResponse.size());
        List<long[]> coeffs = intStream
            .mapToObj(index ->
                VectorizedStdIdxPirNativeUtils.decryptReply(
                    params.getEncryptionParams(), secretKey, serverResponse.get(index)
                )
            ).toList();
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
        NaiveDatabase database = NaiveDatabase.createFromZl(l, databases);
        // generate retrieval index and retrieval item map
        byte[][] entries = new byte[batchNum][];
        IntStream.range(0, binNum)
            .filter(i -> binIndex.get(i) != -1)
            .forEach(i -> {
                int item = binIndexRetrievalIndexMap.get(i);
                for (int j = 0; j < batchNum; j++) {
                    if (item == xs[j]) {
                        entries[j] = database.getBytesData(i).clone();
                    }
                }
            });
        return entries;
    }
}
