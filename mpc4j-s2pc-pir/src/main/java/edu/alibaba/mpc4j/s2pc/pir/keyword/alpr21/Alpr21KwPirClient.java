package edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;

/**
 * ALPR21 keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/6/20
 */
public class Alpr21KwPirClient extends AbstractKwPirClient {
    /**
     * ALPR21 keyword PIR params
     */
    private Alpr21KwPirParams params;
    /**
     * index PIR client
     */
    private final BatchIndexPirClient indexPirClient;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * prf key
     */
    private byte[] prfKey;
    /**
     * hash keys
     */
    private byte[][] hashKeys;

    public Alpr21KwPirClient(Rpc clientRpc, Party serverParty, Alpr21KwPirConfig config) {
        super(Alpr21KwPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        indexPirClient = BatchIndexPirFactory.createClient(clientRpc, serverParty, config.getBatchIndexPirConfig());
        addSubPto(indexPirClient);
        cuckooHashBinType = config.getCuckooHashBinType();
    }

    @Override
    public void init(KwPirParams kwPirParams, int serverElementSize, int maxRetrievalSize, int labelByteLength)
        throws MpcAbortException {
        setInitInput(maxRetrievalSize, serverElementSize, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        assert (kwPirParams instanceof Alpr21KwPirParams);
        params = (Alpr21KwPirParams) kwPirParams;
        params.setMaxRetrievalSize(maxRetrievalSize);

        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(prfKeyHeader).getPayload();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.get(0);
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == getHashNum(cuckooHashBinType));
        hashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);
        int binNum = getBinNum(cuckooHashBinType, serverElementSize);
        int elementBitLength = (params.truncationByteLength + labelByteLength) * Byte.SIZE;
        indexPirClient.init(binNum, elementBitLength, hashKeys.length * maxRetrievalSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxRetrievalSize, int serverElementSize, int labelByteLength) throws MpcAbortException {
        setInitInput(maxRetrievalSize, serverElementSize, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        params = Alpr21KwPirParams.DEFAULT_PARAMS;
        params.setMaxRetrievalSize(maxRetrievalSize);

        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(prfKeyHeader).getPayload();
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.get(0);
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == getHashNum(cuckooHashBinType));
        hashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);
        int binNum = getBinNum(cuckooHashBinType, serverElementSize);
        int elementBitLength = (params.truncationByteLength + labelByteLength) * Byte.SIZE;
        indexPirClient.init(binNum, elementBitLength, hashKeys.length * maxRetrievalSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<ByteBuffer, byte[]> pir(Set<ByteBuffer> retrievalKeySet) throws MpcAbortException {
        setPtoInput(retrievalKeySet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<ByteBuffer> prfOutput = computePrf();
        List<Integer> indexList = computeIndex(prfOutput);
        Map<Integer, byte[]> retrievalMap = indexPirClient.pir(indexList);
        Map<ByteBuffer, byte[]> pirResult = handleResponse(retrievalMap, prfOutput);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, oprfTime, "Client runs PIR");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * handle server response.
     *
     * @param retrievalMap retrieval map.
     * @param keyPrf       key prf.
     * @return retrieval result map.
     */
    private Map<ByteBuffer, byte[]> handleResponse(Map<Integer, byte[]> retrievalMap, List<ByteBuffer> keyPrf) {
        Map<ByteBuffer, byte[]> result = new HashMap<>(retrievalKeySize);
        retrievalMap.forEach((index, item) -> {
            try {
                MpcAbortPreconditions.checkArgument(item.length == params.truncationByteLength + valueByteLength);
            } catch (MpcAbortException e) {
                e.printStackTrace();
            }
            byte[] retrievalKeyBytes = BytesUtils.clone(item, 0, params.truncationByteLength);
            IntStream.range(0, retrievalKeySize).forEach(i -> {
                byte[] localKeyBytes = BytesUtils.clone(keyPrf.get(i).array(), 0, params.truncationByteLength);
                if (ByteBuffer.wrap(retrievalKeyBytes).equals(ByteBuffer.wrap(localKeyBytes))) {
                    result.put(
                        retrievalKeyList.get(i),
                        BytesUtils.clone(item, params.truncationByteLength, valueByteLength)
                    );
                }
            });
        });
        return result;
    }

    /**
     * generate prf element.
     *
     * @return prf element.
     */
    private List<ByteBuffer> computePrf() {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prf.setKey(prfKey);
        Stream<ByteBuffer> keywordStream = retrievalKeyList.stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        return keywordStream
            .map(byteBuffer -> prf.getBytes(byteBuffer.array()))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
    }

    /**
     * compute index.
     *
     * @param prfOutput retrieval prf.
     * @return retrieval index list.
     */
    private List<Integer> computeIndex(List<ByteBuffer> prfOutput) {
        int binNum = getBinNum(cuckooHashBinType, serverElementSize);
        Prf[] hashes = Arrays.stream(hashKeys)
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        Set<Integer> indexList = new HashSet<>();
        for (ByteBuffer byteBuffer : prfOutput) {
            for (int hashIndex = 0; hashIndex < hashKeys.length; hashIndex++) {
                HashBinEntry<ByteBuffer> hashBinEntry = HashBinEntry.fromRealItem(hashIndex, byteBuffer);
                indexList.add(hashes[hashIndex].getInteger(hashBinEntry.getItemByteArray(), binNum));
            }
        }
        return new ArrayList<>(indexList);
    }
}