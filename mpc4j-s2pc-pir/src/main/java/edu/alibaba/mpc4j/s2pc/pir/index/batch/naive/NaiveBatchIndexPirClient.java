package edu.alibaba.mpc4j.s2pc.pir.index.batch.naive;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory.IntCuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.naive.NaiveBatchIndexPirPtoDesc.PtoStep;

/**
 * naive batch Index PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class NaiveBatchIndexPirClient extends AbstractBatchIndexPirClient {

    /**
     * cuckoo hash bin type
     */
    private final IntCuckooHashBinType cuckooHashBinType;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * bin index and database index map
     */
    private Map<Integer, Integer> binIndexRetrievalIndexMap;
    /**
     * simple hash bin
     */
    private List<List<HashBinEntry<Integer>>> completeHashBins = new ArrayList<>();
    /**
     * single index PIR client
     */
    private final SingleIndexPirClient indexPirClient;
    /**
     * bin num
     */
    private int binNum;

    public NaiveBatchIndexPirClient(Rpc clientRpc, Party serverParty, NaiveBatchIndexPirConfig config) {
        super(NaiveBatchIndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        indexPirClient = SingleIndexPirFactory.createClient(clientRpc, serverParty, config.getSingleIndexPirConfig());
        addSubPtos(indexPirClient);
        cuckooHashBinType = config.getCuckooHashBinType();
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        setInitInput(serverElementSize, elementBitLength, maxRetrievalSize);

        // receive hash keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        // client generate simple hash bin
        binNum = IntCuckooHashBinFactory.getBinNum(cuckooHashBinType, maxRetrievalSize);
        int hashNum = IntCuckooHashBinFactory.getHashNum(cuckooHashBinType);
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == hashNum);
        hashKeys = hashKeyPayload.toArray(new byte[0][]);
        int maxBinSize = generateSimpleHashBin();
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        stopWatch.start();
        // client init single index PIR client
        List<byte[]> publicKeysPayload = indexPirClient.clientSetup(maxBinSize, elementBitLength);
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long initIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initIndexPirTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<Integer, byte[]> pir(List<Integer> indexList) throws MpcAbortException {
        setPtoInput(indexList);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // generate bin index list
        List<Integer> binIndexList = updateBinIndex();
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cuckooHashKeyTime, "Client generates cuckoo hash bin");

        stopWatch.start();
        Stream<Integer> stream = binIndexList.stream();
        stream = parallel ? stream.parallel() : stream;
        List<byte[]> clientQueryPayload = stream
            .map(indexPirClient::generateQuery)
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
        logStepInfo(PtoState.PTO_STEP, 2, 3, genQueryTime, "Client executes single PIR for each bucket");

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(responsePayload.size() % binNum == 0);

        stopWatch.start();
        int binResponseSize = responsePayload.size() / binNum;
        IntStream intStream = IntStream.range(0, binNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> decodedItemList = intStream.mapToObj(i -> {
            try {
                if (binIndexList.get(i) != -1) {
                    return indexPirClient.decodeResponse(
                        responsePayload.subList(i * binResponseSize, (i + 1) * binResponseSize), binIndexList.get(i)
                    );
                } else {
                    return new byte[0];
                }
            } catch (MpcAbortException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, decodeResponseTime, "Client decodes response");

        logPhaseInfo(PtoState.PTO_END);
        return IntStream.range(0, binIndexList.size())
            .filter(i -> binIndexList.get(i) != -1)
            .boxed()
            .collect(
                Collectors.toMap(
                    integer -> binIndexRetrievalIndexMap.get(integer),
                    decodedItemList::get,
                    (a, b) -> b,
                    () -> new HashMap<>(retrievalSize)
                )
            );
    }

    /**
     * update retrieval index list.
     *
     * @return bin index list.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<Integer> updateBinIndex() throws MpcAbortException {
        IntNoStashCuckooHashBin cuckooHashBin = IntCuckooHashBinFactory.createInstance(
            envType, cuckooHashBinType, maxRetrievalSize, binNum, hashKeys
        );
        int[] indicesArray = IntStream.range(0, retrievalSize).map(indexList::get).toArray();
        cuckooHashBin.insertItems(indicesArray);
        List<Integer> binIndex = new ArrayList<>(binNum);
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
     * generate simple hash bin.
     *
     * @return max bin size.
     */
    private int generateSimpleHashBin() {
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
}