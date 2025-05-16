package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.AbstractStdKwPirServer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon.PantheonStdKwPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon.PantheonStdKwPirPtoDesc.getInstance;

/**
 * Pantheon standard keyword PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
public class PantheonStdKwPirServer<T> extends AbstractStdKwPirServer<T> {
    /**
     * Pantheon KSPIR params
     */
    private final PantheonStdKwPirParams params;
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

    public PantheonStdKwPirServer(Rpc serverRpc, Party clientParty, PantheonStdKwPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        params = config.getParams();
    }

    @Override
    public void init(Map<T, byte[]> keyValueMap, int l, int maxBatchNum) throws MpcAbortException {
        assert maxBatchNum <= params.maxRetrievalSize();
        setInitInput(keyValueMap, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        params.initPirParams(n, byteL * Byte.SIZE);
        if (CommonUtils.getUnitNum(byteL * Byte.SIZE, params.getPlainModulusSize()) % 2 == 1) {
            isPadding = true;
        }
        ArrayList<T> keysList = new ArrayList<>(keyValueMap.keySet());
        List<ByteBuffer> keysPrf = computeKeysPrf(keysList);
        Map<ByteBuffer, byte[]> keywordPrfLabelMap = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toMap(keysPrf::get, i -> keyValueMap.get(keysList.get(i)), (a, b) -> b));
        sendOtherPartyPayload(PtoStep.SERVER_SEND_PRF_KEY.ordinal(), Collections.singletonList(prfKey));
        stopWatch.stop();
        long prfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, prfTime, "Server compute keyword prf");

        stopWatch.start();
        encodedKeyword = encodeKeyword(keysPrf);
        stopWatch.stop();
        long keywordEncodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, keywordEncodeTime, "Server encodes keyword");

        stopWatch.start();
        encodedLabel = encodeLabel(keysPrf, keywordPrfLabelMap);
        stopWatch.stop();
        long labelEncodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, labelEncodeTime, "Server encodes label");

        List<byte[]> serverKeysPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(serverKeysPayload.size() == 3);
        this.publicKey = serverKeysPayload.get(0);
        this.relinKeys = serverKeysPayload.get(1);
        this.galoisKeys = serverKeysPayload.get(2);
        masks = PantheonStdKwPirNativeUtils.preprocessMask(params.encryptionParams, params.colNum);
        stopWatch.stop();
        long initPublicKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, initPublicKeyTime);

        logPhaseInfo(PtoState.INIT_END);
    }


    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            answer();
        }
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void answer() throws MpcAbortException {
        List<byte[]> queryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(queryPayload.size() == 1);
        byte[] query = queryPayload.get(0);
        List<byte[]> expandedQuery = PantheonStdKwPirNativeUtils.expandQuery(
            params.encryptionParams, galoisKeys, masks, query, params.colNum
        );
        IntStream rowStream = parallel ? IntStream.range(0, params.rowNum).parallel() : IntStream.range(0, params.rowNum);
        List<byte[]> rowResults = rowStream.mapToObj(i -> {
            IntStream colStream = parallel ? IntStream.range(0, params.colNum).parallel() : IntStream.range(0, params.colNum);
            List<byte[]> columnResults = colStream
                .mapToObj(j -> PantheonStdKwPirNativeUtils.processColumn(
                    params.encryptionParams, publicKey, relinKeys, encodedKeyword.get(i)[j], expandedQuery.get(j)))
                .collect(Collectors.toList());
            return PantheonStdKwPirNativeUtils.processRow(params.encryptionParams, relinKeys, galoisKeys, columnResults);
        }).collect(Collectors.toList());
        byte[] responsePayload = PantheonStdKwPirNativeUtils.processPir(
            params.encryptionParams, galoisKeys, encodedLabel, rowResults, params.pirColumnNumPerObj
        );
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), Collections.singletonList(responsePayload));
    }

    private List<ByteBuffer> computeKeysPrf(ArrayList<T> keys) {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prfKey = BlockUtils.randomBlock(secureRandom);
        prf.setKey(prfKey);
        Stream<T> keysStream = parallel ? keys.stream().parallel() : keys.stream();
        return keysStream
            .map(ObjectUtils::objectToByteArray)
            .map(prf::getBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
    }

    private List<long[][]> encodeKeyword(List<ByteBuffer> keysPrf) {
        int slotNum = params.getPolyModulusDegree() / 2;
        List<long[][]> encodedCoeffs = IntStream.range(0, params.rowNum)
            .mapToObj(i -> new long[params.colNum][params.getPolyModulusDegree()])
            .collect(Collectors.toList());
        for (int i = 0; i < n; i++) {
            long[] coeffs = PirUtils.convertBytesToCoeffs(
                params.getPlainModulusSize(), 0, params.keywordPrfByteLength, keysPrf.get(i).array()
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

    private List<byte[]> encodeLabel(List<ByteBuffer> keywordPrf, Map<ByteBuffer, byte[]> keywordPrfLabelMap) {
        long[][] labelCoeffs = new long[params.pirDbRowNum][params.getPolyModulusDegree()];
        for (int i = 0; i < params.pirDbRowNum; i++) {
            for (int j = 0; j < params.getPolyModulusDegree(); j++) {
                labelCoeffs[i][j] = 1L;
            }
        }
        int slotCount = params.getPolyModulusDegree() / 2;
        for (int i = 0; i < n; i++) {
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
        return PantheonStdKwPirNativeUtils.nttTransform(params.encryptionParams, labelCoeffs);
    }
}