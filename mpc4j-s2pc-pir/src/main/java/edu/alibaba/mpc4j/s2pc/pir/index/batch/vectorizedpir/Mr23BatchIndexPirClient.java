package edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirClient;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirPtoDesc.*;

/**
 * vectorized batch Index PIR client.
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
    private Mr23BatchIndexPirParams params;
    /**
     * no stash cuckoo hash bin
     */
    private IntNoStashCuckooHashBin cuckooHashBin;
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
    List<List<HashBinEntry<Integer>>> completeHashBins = new ArrayList<>();
    /**
     * group bin size
     */
    private int groupBinSize;
    /**
     * dimension size
     */
    private int[] dimensionsSize;

    public Mr23BatchIndexPirClient(Rpc clientRpc, Party serverParty, Mr23BatchIndexPirConfig config) {
        super(Mr23BatchIndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        params = Mr23BatchIndexPirParams.getParams(serverElementSize, maxRetrievalSize);
        MpcAbortPreconditions.checkArgument(params != null);
        setInitInput(serverElementSize, elementBitLength, params.getMaxRetrievalSize());
        this.partitionBitLength = params.getPlainModulusBitLength() - 1;
        this.partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive hash keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getHashNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);

        // client generate simple hash bin
        stopWatch.start();
        int maxBinSize = generateSimpleHashBin();
        checkDimensionLength(maxBinSize);
        dimensionsSize = new int[] {
            params.getThirdDimensionSize(), params.getFirstTwoDimensionSize(), params.getFirstTwoDimensionSize()
        };
        groupBinSize = (params.getPolyModulusDegree() / 2) / params.getFirstTwoDimensionSize();
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
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cuckooHashKeyTime, "Client generates cuckoo hash bin");

        stopWatch.start();
        // client generate queries for each hash bin
        IntStream intStream = IntStream.range(0, cuckooHashBin.binNum());
        intStream = parallel ? intStream.parallel() : intStream;
        List<long[][]> queries = intStream
            .mapToObj(i -> binIndexList.get(i) != -1 ? generateVectorizedPirQuery(i, binIndexList.get(i))
                    : new long[params.getDimension()][params.getPolyModulusDegree()])
            .collect(Collectors.toList());
        // merge queries
        List<long[][]> mergedQueries = mergeQueries(queries);
        // encrypt merged queries
        Stream<long[][]> queryStream = mergedQueries.stream();
        queryStream = parallel ? queryStream.parallel() : queryStream;
        List<byte[]> clientQueryPayload = queryStream
            .map(query ->
                Mr23BatchIndexPirNativeUtils.generateQuery(params.getEncryptionParams(), publicKey, secretKey, query)
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, genQueryTime, "Client generates query");

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
        logStepInfo(PtoState.PTO_STEP, 3, 3, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return retrievalResult;
    }

    /**
     * update retrieval index list.
     *
     * @return bin index list.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<Integer> updateBinIndex() throws MpcAbortException {
        int[] indicesArray = IntStream.range(0, retrievalSize).map(indexList::get).toArray();
        cuckooHashBin.insertItems(indicesArray);
        List<Integer> binIndex = new ArrayList<>(cuckooHashBin.binNum());
        binIndexRetrievalIndexMap = new HashMap<>(retrievalSize);
        for (int i = 0; i < cuckooHashBin.binNum(); i++) {
            if (cuckooHashBin.getBinHashIndex(i) == -1) {
                binIndex.add(-1);
            } else {
                for (int j = 0; j < completeHashBins.get(i).size(); j++) {
                    if (completeHashBins.get(i).get(j).getItem() == cuckooHashBin.getBinEntry(i)) {
                        binIndex.add(j);
                        binIndexRetrievalIndexMap.put(i, cuckooHashBin.getBinEntry(i));
                        break;
                    }
                }
            }
        }
        MpcAbortPreconditions.checkArgument(
            binIndex.size() == cuckooHashBin.binNum() && binIndexRetrievalIndexMap.size() == retrievalSize
        );
        return binIndex;
    }

    /**
     * client generate query.
     *
     * @param binIndex          bin index.
     * @param binRetrievalIndex bin retrieval index.
     * @return query.
     */
    private long[][] generateVectorizedPirQuery(int binIndex, int binRetrievalIndex) {
        int[] temp = PirUtils.computeIndices(binRetrievalIndex, dimensionsSize);
        int[] permutedIndices = IntStream.range(0, params.getDimension())
            .map(i -> temp[params.getDimension() - 1 - i])
            .toArray();
        int[] indices = new int[params.getDimension()];
        int offset = (binIndex < (cuckooHashBin.binNum() / 2)) ? 0 : params.getPolyModulusDegree() / 2;
        for (int i = 0; i < params.getDimension(); i++) {
            indices[i] = permutedIndices[i];
            for (int j = 0; j < i; j++) {
                indices[i] = (indices[i] + permutedIndices[j]) % params.getFirstTwoDimensionSize();
            }
        }
        long[][] query = new long[params.getDimension()][params.getPolyModulusDegree()];
        IntStream.range(0, params.getDimension()).forEach(i -> query[i][indices[i] * groupBinSize + offset] = 1L);
        return query;
    }

    /**
     * merge queries ciphertext.
     *
     * @param queries queries.
     * @return merged query.
     */
    private List<long[][]> mergeQueries(List<long[][]> queries) {
        int binNum = cuckooHashBin.binNum();
        int count = CommonUtils.getUnitNum(binNum / 2, groupBinSize);
        List<long[][]> mergeQueries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long[][] query = new long[params.getDimension()][params.getPolyModulusDegree()];
            for (int j = 0; j < groupBinSize; j++) {
                if ((i * groupBinSize + j) >= (binNum / 2)) {
                    break;
                } else {
                    long[][] temp = PirUtils.plaintextRotate(queries.get(i * groupBinSize + j), j);
                    for (int l = 0; l < params.getDimension(); l++) {
                        for (int k = 0; k < params.getPolyModulusDegree(); k++) {
                            query[l][k] += temp[l][k];
                        }
                    }
                    temp = PirUtils.plaintextRotate(queries.get(i * groupBinSize + j + binNum / 2), j);
                    for (int l = 0; l < params.getDimension(); l++) {
                        for (int k = 0; k < params.getPolyModulusDegree(); k++) {
                            query[l][k] += temp[l][k];
                        }
                    }
                }
            }
            mergeQueries.add(query);
        }
        return mergeQueries;
    }

    /**
     * generate key pair payload.
     *
     * @return key pair payload.
     */
    private List<byte[]> generateKeyPairPayload() {
        List<byte[]> keyPair = Mr23BatchIndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 4);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
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
     * @throws MpcAbortException the protocol failure aborts.
     */
    private int generateSimpleHashBin() throws MpcAbortException {
        int binNum = IntCuckooHashBinFactory.getBinNum(
            IntCuckooHashBinType.NO_STASH_NAIVE, params.getMaxRetrievalSize()
        );
        if (binNum % 2 == 1) {
            binNum = binNum + 1;
        }
        cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType, IntCuckooHashBinType.NO_STASH_NAIVE, params.getMaxRetrievalSize(), binNum, hashKeys
        );
        MpcAbortPreconditions.checkArgument(params.getPolyModulusDegree() >= binNum);
        List<Integer> totalIndexList = IntStream.range(0, serverElementSize)
            .boxed()
            .collect(Collectors.toCollection(() -> new ArrayList<>(serverElementSize)));
        RandomPadHashBin<Integer> completeHash = new RandomPadHashBin<>(envType, binNum, serverElementSize, hashKeys);
        completeHash.insertItems(totalIndexList);
        int maxBinSize = completeHash.binSize(0);
        for (int i = 1; i < completeHash.binNum(); i++) {
            if (completeHash.binSize(i) > maxBinSize) {
                maxBinSize = completeHash.binSize(i);
            }
        }
        completeHashBins = new ArrayList<>();
        HashBinEntry<Integer> paddingEntry = HashBinEntry.fromEmptyItem(-1);
        for (int i = 0; i < completeHash.binNum(); i++) {
            List<HashBinEntry<Integer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        return maxBinSize;
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
        MpcAbortPreconditions.checkArgument(serverResponse.size() == partitionSize);
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        int byteL = CommonUtils.getByteLength(partitionBitLength);
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i -> {
            long[] coeffs = Mr23BatchIndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(), secretKey, serverResponse.get(i)
            );
            byte[][] partitionItems = new byte[binIndex.size()][];
            for (int j = 0; j < binIndex.size(); j++) {
                if (binIndex.get(j) != -1) {
                    int offset =  (j < (binIndex.size() / 2)) ? 0 : (params.getPolyModulusDegree() - binIndex.size())/2;
                    partitionItems[j] = IntUtils.nonNegIntToFixedByteArray(Math.toIntExact(coeffs[j + offset]), byteL);
                } else {
                    partitionItems[j] = new byte[byteL];
                }
            }
            databases[i] = ZlDatabase.create(partitionBitLength, partitionItems);
        });
        NaiveDatabase database = NaiveDatabase.createFromZl(elementBitLength, databases);
        // generate retrieval index and retrieval result map
        return IntStream.range(0, binIndex.size())
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

    /**
     * check the validity of the dimension length.
     *
     * @param elementSize bin element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void checkDimensionLength(int elementSize) throws MpcAbortException {
        int product =
            params.getFirstTwoDimensionSize() * params.getFirstTwoDimensionSize() * params.getThirdDimensionSize();
        MpcAbortPreconditions.checkArgument(product >= elementSize);
    }
}