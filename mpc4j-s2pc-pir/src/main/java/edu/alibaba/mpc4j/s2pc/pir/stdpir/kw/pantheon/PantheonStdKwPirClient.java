package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.AbstractStdKwPirClient;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon.PantheonStdKwPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon.PantheonStdKwPirPtoDesc.getInstance;

/**
 * Pantheon standard keyword PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/6/20
 */
public class PantheonStdKwPirClient<T> extends AbstractStdKwPirClient<T> {
    /**
     * Pantheon KSPIR params
     */
    private final PantheonStdKwPirParams params;
    /**
     * prf key
     */
    private byte[] prfKey;
    /**
     * client keys
     */
    private List<byte[]> clientKeys;
    /**
     * is padding
     */
    private boolean isPadding = false;

    public PantheonStdKwPirClient(Rpc clientRpc, Party serverParty, PantheonStdKwPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        params = config.getParams();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        assert maxBatchNum <= params.maxRetrievalSize();
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        params.initPirParams(n, byteL * Byte.SIZE);
        List<byte[]> prfKeyPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_PRF_KEY.ordinal());
        MpcAbortPreconditions.checkArgument(prfKeyPayload.size() == 1);
        prfKey = prfKeyPayload.get(0);
        if (CommonUtils.getUnitNum(byteL * Byte.SIZE, params.getPlainModulusSize()) % 2 == 1) {
            isPadding = true;
        }
        Pair<List<byte[]>, List<byte[]>> keyPair = generateKeyPair();
        clientKeys = keyPair.getLeft();
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), keyPair.getRight());
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
        List<ByteBuffer> keysPrf = computeKeysPrf(keys);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, oprfTime, "Client runs PRF");

        // generate query
        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(keysPrf.get(i));
        }
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, genQueryTime, "Client generate query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = recover();
        }
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, decodeResponseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private byte[] recover() throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == 1);
        byte[] response = responsePayload.get(0);
        int slotCount = params.getPolyModulusDegree() / 2;
        long[] coeffs = PantheonStdKwPirNativeUtils.decodeReply(params.encryptionParams, clientKeys.get(0), response);
        long[] items = new long[params.pirColumnNumPerObj];
        int index = IntStream.range(0, slotCount).filter(i -> coeffs[i] != 0).findFirst().orElse(-1);
        if (index >= 0) {
            for (int i = 0; i < params.pirColumnNumPerObj / 2; i++) {
                items[i] = coeffs[index + i];
                items[i + params.pirColumnNumPerObj / 2] = coeffs[index + i + slotCount];
            }
            byte[] bytes = PirUtils.convertCoeffsToBytes(items, params.getPlainModulusSize());
            int start = isPadding ? params.getPlainModulusSize() / Byte.SIZE : 0;
            return BytesUtils.clone(bytes, start, byteL);
        } else {
            return new byte[0];
        }
    }

    private void query(ByteBuffer keyPrf) {
        long[] query = new long[params.getPolyModulusDegree()];
        long[] coeffs = PirUtils.convertBytesToCoeffs(
            params.getPlainModulusSize(), 0, params.keywordPrfByteLength, keyPrf.array()
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
        byte[] queryPayload = PantheonStdKwPirNativeUtils.generateQuery(
            params.encryptionParams, clientKeys.get(1), clientKeys.get(0), query
        );
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), Collections.singletonList(queryPayload));
    }

    private List<ByteBuffer> computeKeysPrf(ArrayList<T> keys) {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prf.setKey(prfKey);
        Stream<T> intStream = parallel ? keys.stream().parallel() : keys.stream();
        return intStream.map(ObjectUtils::objectToByteArray)
            .map(prf::getBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
    }

    private Pair<List<byte[]>, List<byte[]>> generateKeyPair() {
        List<byte[]> keyPair = PantheonStdKwPirNativeUtils.keyGen(
            params.encryptionParams, params.pirColumnNumPerObj, params.colNum
        );
        assert (keyPair.size() == 4);
        List<byte[]> clientKeys = new ArrayList<>();
        clientKeys.add(keyPair.get(0));
        clientKeys.add(keyPair.get(1));
        List<byte[]> serverKeys = new ArrayList<>(keyPair.subList(1, 4));
        return Pair.of(clientKeys, serverKeys);
    }
}