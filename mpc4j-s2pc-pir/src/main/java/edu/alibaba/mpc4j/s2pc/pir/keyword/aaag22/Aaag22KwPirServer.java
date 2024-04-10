package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22.Aaag22KwPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * AAAG22 keyword PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
public class Aaag22KwPirServer extends AbstractKwPirServer {
    /**
     * AAAG22 keyword PIR params
     */
    private Aaag22KwPirParams params;
    /**
     * encoded keyword
     */
    private List<long[][]> encodedKeyword;
    /**
     * encoded label
     */
    private List<byte[]> encodedLabel;
    /**
     * prf key
     */
    private byte[] prfKey;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * relinearization keys
     */
    private byte[] relinKeys;
    /**
     * Galois keys
     */
    private byte[] galoisKeys;
    /**
     * masks
     */
    private List<byte[]> masks;
    /**
     * is padding
     */
    private boolean isPadding = false;

    public Aaag22KwPirServer(Rpc serverRpc, Party clientParty, Aaag22KwPirConfig config) {
        super(Aaag22KwPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(KwPirParams kwPirParams, Map<ByteBuffer, byte[]> serverKeywordLabelMap, int maxRetrievalSize,
                     int labelByteLength) throws MpcAbortException {
        setInitInput(serverKeywordLabelMap, maxRetrievalSize, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        assert (kwPirParams instanceof Aaag22KwPirParams);
        params = (Aaag22KwPirParams) kwPirParams;
        assert maxRetrievalSize <= params.maxRetrievalSize();

        stopWatch.start();
        params.initPirParams(keywordSize, labelByteLength * Byte.SIZE);
        if (CommonUtils.getUnitNum(labelByteLength * Byte.SIZE, params.getPlainModulusSize()) % 2 == 1) {
            isPadding = true;
        }
        List<ByteBuffer> keywordPrf = computeKeywordPrf();
        Map<ByteBuffer, byte[]> keywordPrfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(Collectors.toMap(
                keywordPrf::get, i -> serverKeywordLabelMap.get(keywordList.get(i)), (a, b) -> b)
            );
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(prfKeyHeader, Collections.singletonList(prfKey)));
        stopWatch.stop();
        long prfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, prfTime, "Server compute keyword prf");

        stopWatch.start();
        encodedKeyword = encodeKeyword(keywordPrf);
        stopWatch.stop();
        long keywordEncodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, keywordEncodeTime, "Server encodes keyword");

        stopWatch.start();
        encodedLabel = encodeLabel(keywordPrf, keywordPrfLabelMap);
        stopWatch.stop();
        long labelEncodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, labelEncodeTime, "Server encodes label");

        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        setPublicKey(publicKeyPayload);
        masks = Aaag22KwPirNativeUtils.preprocessMask(params.encryptionParams, params.colNum);
        stopWatch.stop();
        long initPublicKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, initPublicKeyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(Map<ByteBuffer, byte[]> serverKeywordLabelMap, int maxRetrievalSize, int labelByteLength)
        throws MpcAbortException {
        setInitInput(serverKeywordLabelMap, maxRetrievalSize, labelByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);
        params = Aaag22KwPirParams.DEFAULT_PARAMS;
        assert maxRetrievalSize <= params.maxRetrievalSize();

        stopWatch.start();
        params.initPirParams(keywordSize, labelByteLength * Byte.SIZE);
        if (CommonUtils.getUnitNum(labelByteLength * Byte.SIZE, params.getPlainModulusSize()) % 2 == 1) {
            isPadding = true;
        }
        List<ByteBuffer> keywordPrf = computeKeywordPrf();
        Map<ByteBuffer, byte[]> keywordPrfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(Collectors.toMap(
                keywordPrf::get, i -> serverKeywordLabelMap.get(keywordList.get(i)), (a, b) -> b)
            );
        DataPacketHeader prfKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRF_KEY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(prfKeyHeader, Collections.singletonList(prfKey)));
        stopWatch.stop();
        long prfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, prfTime, "Server compute keyword prf");

        stopWatch.start();
        encodedKeyword = encodeKeyword(keywordPrf);
        stopWatch.stop();
        long keywordEncodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, keywordEncodeTime, "Server encodes keyword");

        stopWatch.start();
        encodedLabel = encodeLabel(keywordPrf, keywordPrfLabelMap);
        stopWatch.stop();
        long labelEncodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, labelEncodeTime, "Server encodes label");

        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        setPublicKey(publicKeyPayload);
        masks = Aaag22KwPirNativeUtils.preprocessMask(params.encryptionParams, params.colNum);
        stopWatch.stop();
        long initPublicKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, initPublicKeyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();

        stopWatch.start();
        byte[] response = generateResponse(queryPayload);
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, Collections.singletonList(response)));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * server generate response.
     *
     * @param queryPayload client query.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private byte[] generateResponse(List<byte[]> queryPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(queryPayload.size() == 1);
        byte[] query = queryPayload.get(0);
        List<byte[]> expandedQuery = Aaag22KwPirNativeUtils.expandQuery(
            params.encryptionParams, galoisKeys, masks, query, params.colNum
        );
        IntStream rowStream = IntStream.range(0, params.rowNum);
        rowStream = parallel ? rowStream.parallel() : rowStream;
        List<byte[]> rowResults = rowStream.mapToObj(i -> {
            IntStream colStream = IntStream.range(0, params.colNum);
            colStream = parallel ? colStream.parallel() : colStream;
            List<byte[]> columnResults = colStream
                .mapToObj(j -> Aaag22KwPirNativeUtils.processColumn(
                    params.encryptionParams, publicKey, relinKeys, encodedKeyword.get(i)[j], expandedQuery.get(j)))
                .collect(Collectors.toList());
            return Aaag22KwPirNativeUtils.processRow(params.encryptionParams, relinKeys, galoisKeys, columnResults);
        }).collect(Collectors.toList());
        return Aaag22KwPirNativeUtils.processPir(
            params.encryptionParams, galoisKeys, encodedLabel, rowResults, params.pirColumnNumPerObj
        );
    }

    /**
     * compute keyword prf.
     *
     * @return keyword prf.
     */
    private List<ByteBuffer> computeKeywordPrf() {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prfKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(prfKey);
        prf.setKey(prfKey);
        Stream<ByteBuffer> keywordStream = keywordList.stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        return keywordStream
            .map(byteBuffer -> prf.getBytes(byteBuffer.array()))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
    }

    /**
     * preprocess keyword encoding.
     *
     * @param keywordPrf keyword prf.
     * @return encoded keyword.
     */
    private List<long[][]> encodeKeyword(List<ByteBuffer> keywordPrf) {
        int slotNum = params.getPolyModulusDegree() / 2;
        List<long[][]> encodedCoeffs = IntStream.range(0, params.rowNum)
            .mapToObj(i -> new long[params.colNum][params.getPolyModulusDegree()])
            .collect(Collectors.toList());
        for (int i = 0; i < keywordSize; i++) {
            long[] coeffs = PirUtils.convertBytesToCoeffs(
                params.getPlainModulusSize(), 0, params.keywordPrfByteLength, keywordPrf.get(i).array()
            );
            assert coeffs.length == params.colNum * 2;
            int index = i / slotNum;
            int rowIndex = i % slotNum;
            for (int j = 0; j < params.colNum; j++) {
                encodedCoeffs.get(index)[j][rowIndex] = coeffs[2 * j];
                encodedCoeffs.get(index)[j][rowIndex + slotNum] = coeffs[2 * j + 1];
            }
        }
        return encodedCoeffs;
    }

    /**
     * preprocess label encoding.
     *
     * @param keywordPrf keyword prf.
     * @param keywordPrfLabelMap keyword prf label map.
     * @return encoded label.
     */
    private List<byte[]> encodeLabel(List<ByteBuffer> keywordPrf, Map<ByteBuffer, byte[]> keywordPrfLabelMap) {
        long[][] labelCoeffs = new long[params.pirDbRowNum][params.getPolyModulusDegree()];
        for (int i = 0; i < params.pirDbRowNum; i++) {
            for (int j = 0; j < params.getPolyModulusDegree(); j++) {
                labelCoeffs[i][j] = 1L;
            }
        }
        int slotCount = params.getPolyModulusDegree() / 2;
        for (int i = 0; i < keywordSize; i++) {
            byte[] label = keywordPrfLabelMap.get(keywordPrf.get(i));
            if (isPadding) {
                label = BytesUtils.paddingByteArray(label, label.length + 2);
                label[0] = 0x01;
                label[1] = 0x01;
            }
            long[] coeffs = PirUtils.convertBytesToCoeffs(params.getPlainModulusSize(), 0, label.length, label);
            int row = i / slotCount;
            int col = i % slotCount;
            for (int j = 0; j < params.pirColumnNumPerObj / 2; j++) {
                labelCoeffs[row][col] = coeffs[j];
                if (j + (params.pirColumnNumPerObj / 2) >= coeffs.length) {
                    labelCoeffs[row][col + slotCount] = 0L;
                } else {
                    labelCoeffs[row][col + slotCount] = coeffs[j + (params.pirColumnNumPerObj / 2)];
                }
                row += params.queryCiphertextNum;
            }
        }
        return Aaag22KwPirNativeUtils.nttTransform(params.encryptionParams, labelCoeffs);
    }

    /**
     * set public keys.
     *
     * @param clientPublicKeysPayload client public keys payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void setPublicKey(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 3);
        this.publicKey = clientPublicKeysPayload.get(0);
        this.relinKeys = clientPublicKeysPayload.get(1);
        this.galoisKeys = clientPublicKeysPayload.get(2);
    }
}