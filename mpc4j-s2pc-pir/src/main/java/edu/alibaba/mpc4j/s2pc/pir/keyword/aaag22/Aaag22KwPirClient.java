package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22.Aaag22KwPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * AAAG22 keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/6/20
 */
public class Aaag22KwPirClient extends AbstractKwPirClient {
    /**
     * AAAG22 keyword PIR params
     */
    private Aaag22KwPirParams params;
    /**
     * prf key
     */
    private byte[] prfKey;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * secret key
     */
    private byte[] secretKey;
    /**
     * is padding
     */
    private boolean isPadding;

    public Aaag22KwPirClient(Rpc clientRpc, Party serverParty, Aaag22KwPirConfig config) {
        super(Aaag22KwPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(KwPirParams kwPirParams, int serverElementSize, int maxRetrievalSize, int labelByteLength)
        throws MpcAbortException {
        setInitInput(maxRetrievalSize, serverElementSize, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        assert (kwPirParams instanceof Aaag22KwPirParams);
        params = (Aaag22KwPirParams) kwPirParams;
        assert maxRetrievalSize <= kwPirParams.maxRetrievalSize();

        DataPacketHeader prfHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(prfHashKeyHeader).getPayload();

        stopWatch.start();
        if (CommonUtils.getUnitNum(labelByteLength * Byte.SIZE, params.getPlainModulusSize()) % 2 == 1) {
            isPadding = true;
        }
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.get(0);
        // generate key pair
        List<byte[]> publicKeysPayload = generateKeyPair();
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
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
        params = Aaag22KwPirParams.DEFAULT_PARAMS;
        assert maxRetrievalSize <= params.maxRetrievalSize();

        DataPacketHeader prfHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> prfKeyPayload = rpc.receive(prfHashKeyHeader).getPayload();

        stopWatch.start();
        if (CommonUtils.getUnitNum(labelByteLength * Byte.SIZE, params.getPlainModulusSize()) % 2 == 1) {
            isPadding = true;
        }
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.get(0);
        // generate key pair
        List<byte[]> publicKeysPayload = generateKeyPair();
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
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

        // run PRF
        stopWatch.start();
        ByteBuffer keywordPrf = computePrf();
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, oprfTime, "Client runs PRF");

        // generate query
        stopWatch.start();
        byte[] query = generateQuery(keywordPrf);
        DataPacketHeader queryDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryDataPacketHeader, Collections.singletonList(query)));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, genQueryTime, "Client generate query");

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        stopWatch.start();
        Map<ByteBuffer, byte[]> pirResult = handleResponse(responsePayload);
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, decodeResponseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * handle server response.
     *
     * @param responsePayload response payload.
     * @return retrieval result map.
     */
    private Map<ByteBuffer, byte[]> handleResponse(List<byte[]> responsePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(responsePayload.size() == 1);
        byte[] response = responsePayload.get(0);
        int slotCount = params.getPolyModulusDegree() / 2;
        long[] coeffs = Aaag22KwPirNativeUtils.decodeReply(params.encryptionParams, secretKey, response);
        long[] items = new long[params.pirColumnNumPerObj];
        int index = IntStream.range(0, slotCount).filter(i -> coeffs[i] != 0).findFirst().orElse(-1);
        if (index > 0) {
            for (int i = 0; i < params.pirColumnNumPerObj / 2; i++) {
                items[i] = coeffs[index + i];
                items[i + params.pirColumnNumPerObj / 2] = coeffs[index + i + slotCount];
            }
            byte[] bytes = PirUtils.convertCoeffsToBytes(items, params.getPlainModulusSize());
            HashMap<ByteBuffer, byte[]> result = new HashMap<>(1);
            int start = isPadding ? params.getPlainModulusSize() / Byte.SIZE : 0;
            result.put(retrievalKeyList.get(0), BytesUtils.clone(bytes, start, valueByteLength));
            return result;
        } else {
            return new HashMap<>(0);
        }
    }

    /**
     * generate query.
     *
     * @param keywordPrf keyword PRF.
     * @return client query.
     */
    private byte[] generateQuery(ByteBuffer keywordPrf) {
        long[] query = new long[params.getPolyModulusDegree()];
        long[] coeffs = PirUtils.convertBytesToCoeffs(
            params.getPlainModulusSize(), 0, params.keywordPrfByteLength, keywordPrf.array()
        );
        assert coeffs.length == params.colNum * 2;
        int size = params.getPolyModulusDegree() / (params.colNum * 2);
        int slotCount = params.getPolyModulusDegree() / 2;
        for (int i = 0; i < params.colNum; i++) {
            for (int j = i * size; j < (i + 1) * size; j++) {
                query[j] = coeffs[2 * i];
                query[j + slotCount] = coeffs[2 * i + 1];
            }
        }
        return Aaag22KwPirNativeUtils.generateQuery(params.encryptionParams, publicKey, secretKey, query);
    }

    /**
     * generate prf element.
     *
     * @return prf element.
     */
    private ByteBuffer computePrf() {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prf.setKey(prfKey);
        return ByteBuffer.wrap(prf.getBytes(retrievalKeyList.get(0).array()));
    }

    /**
     * client generates key pair.
     *
     * @return public keys.
     */
    private List<byte[]> generateKeyPair() {
        List<byte[]> keyPair = Aaag22KwPirNativeUtils.keyGen(
            params.encryptionParams, params.pirColumnNumPerObj, params.colNum
        );
        assert (keyPair.size() == 4);
        this.secretKey = keyPair.get(0);
        this.publicKey = keyPair.get(1);
        return keyPair.subList(1, 4);
    }
}