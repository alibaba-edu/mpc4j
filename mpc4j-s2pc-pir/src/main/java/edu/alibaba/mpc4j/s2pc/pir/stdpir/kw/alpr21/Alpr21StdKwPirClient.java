package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.AbstractStdKwPirClient;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21.Alpr21StdKwPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21.Alpr21StdKwPirPtoDesc.getInstance;

/**
 * ALPR21 standard keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/6/20
 */
public class Alpr21StdKwPirClient<T> extends AbstractStdKwPirClient<T> {
    /**
     * ALPR21 standard KS PIR params
     */
    private final Alpr21StdKwPirParams params;
    /**
     * index PIR client
     */
    private final PbcableStdIdxPirClient pirClient;
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

    public Alpr21StdKwPirClient(Rpc clientRpc, Party serverParty, Alpr21StdKwPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        pirClient = StdIdxPirFactory.createPbcableClient(clientRpc, serverParty, config.getPbcableStdIdxPirConfig());
        addSubPto(pirClient);
        cuckooHashBinType = config.getCuckooHashBinType();
        params = config.getParams();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);
        params.setMaxRetrievalSize(maxBatchNum);

        stopWatch.start();
        List<byte[]> prfKeyPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_PRF_KEY.ordinal());
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.getFirst();
        List<byte[]> cuckooHashKeysPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal());
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == getHashNum(cuckooHashBinType));
        hashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);
        int binNum = getBinNum(cuckooHashBinType, n);
        int elementBitLength = (params.truncationByteLength + byteL) * Byte.SIZE;
        pirClient.init(binNum, elementBitLength, hashKeys.length * maxBatchNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(ArrayList<T> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<ByteBuffer> prfOutput = computePrf(keys);
        int[] indices = computeIndex(prfOutput);
        byte[][] pirOutput = pirClient.pir(indices);
        byte[][] entries = handleResponse(pirOutput, prfOutput);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, oprfTime, "Client runs PIR");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private byte[][] handleResponse(byte[][] pirOutput, List<ByteBuffer> keyPrf) {
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            byte[] localKeyBytes = BytesUtils.clone(keyPrf.get(i).array(), 0, params.truncationByteLength);
            for (byte[] bytes : pirOutput) {
                byte[] retrievalKeyBytes = BytesUtils.clone(bytes, 0, params.truncationByteLength);
                if (ByteBuffer.wrap(retrievalKeyBytes).equals(ByteBuffer.wrap(localKeyBytes))) {
                    entries[i] = BytesUtils.clone(bytes, params.truncationByteLength, byteL);
                }
            }
        }
        return entries;
    }

    private List<ByteBuffer> computePrf(ArrayList<T> keys) {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prf.setKey(prfKey);
        Stream<T> keywordStream = parallel ? keys.stream().parallel() : keys.stream();
        return keywordStream
            .map(ObjectUtils::objectToByteArray)
            .map(prf::getBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
    }

    private int[] computeIndex(List<ByteBuffer> prfOutput) {
        int binNum = getBinNum(cuckooHashBinType, n);
        Prf[] hashes = Arrays.stream(hashKeys)
            .map(key -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(key);
                return prf;
            })
            .toArray(Prf[]::new);
        List<Integer> indices = new ArrayList<>();
        for (ByteBuffer byteBuffer : prfOutput) {
            for (int hashIndex = 0; hashIndex < hashKeys.length; hashIndex++) {
                HashBinEntry<ByteBuffer> hashBinEntry = HashBinEntry.fromRealItem(hashIndex, byteBuffer);
                int index = hashes[hashIndex].getInteger(hashBinEntry.getItemByteArray(), binNum);
                indices.add(index);
            }
        }
        return indices.stream().mapToInt(integer -> integer).toArray();
    }
}