package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirClient;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion.OnionStdIdxPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion.OnionStdIdxPirPtoDesc.getInstance;

/**
 * OnionPIR client.
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class OnionStdIdxPirClient extends AbstractStdIdxPirClient implements PbcableStdIdxPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * OnionPIR params
     */
    private final OnionStdIdxPirParams params;
    /**
     * partition size
     */
    protected int partitionSize;
    /**
     * partition bit-length
     */
    protected int partitionBitLength;
    /**
     * partition byte length
     */
    protected int partitionByteLength;
    /**
     * element size per BFV plaintext
     */
    private int elementSizeOfPlaintext;
    /**
     * dimension size
     */
    private int[] dimensionSize;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * private key
     */
    private byte[] secretKey;

    public OnionStdIdxPirClient(Rpc clientRpc, Party serverParty, OnionStdIdxPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        params = config.getStdIdxPirParams();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        Pair<List<byte[]>, List<byte[]>> keyPair = keyGen();
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), keyPair.getRight());
        init(keyPair.getLeft(), n, l, maxBatchNum);
    }

    @Override
    public void init(List<byte[]> clientKeys, int n, int l, int maxBatchNum) throws MpcAbortException {
        if (clientKeys == null) {
            init(n, l, maxBatchNum);
        } else {
            setInitInput(n, l, maxBatchNum);
            logPhaseInfo(PtoState.INIT_BEGIN);

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(clientKeys.size() == 2);
            this.publicKey = clientKeys.get(0);
            this.secretKey = clientKeys.get(1);
            int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
            partitionBitLength = Math.min(maxPartitionBitLength, l);
            partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
            partitionSize = CommonUtils.getUnitNum(l, partitionBitLength);
            elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
                partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
            );
            int plaintextSize = CommonUtils.getUnitNum(n, elementSizeOfPlaintext);
            dimensionSize = PirUtils.computeDimensionLength(
                plaintextSize, params.getFirstDimensionSize(), params.SUBSEQUENT_DIMENSION_SIZE
            );
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

            logPhaseInfo(PtoState.INIT_END);
        }
    }

    @Override
    public byte[][] pir(int[] xs) throws MpcAbortException {
        setPtoInput(xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(xs[i]);
        }
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = recover(xs[i]);
        }
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Server handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    @Override
    public Pair<List<byte[]>, List<byte[]>> keyGen() {
        List<byte[]> keyPair = OnionStdIdxPirNativeUtils.keyGen(params.getEncryptionParams());
        assert keyPair.size() == 3;
        List<byte[]> clientKeys = new ArrayList<>();
        clientKeys.add(keyPair.get(0));
        clientKeys.add(keyPair.get(1));
        List<byte[]> serverKeys = new ArrayList<>();
        // add public key and Galois keys
        serverKeys.add(keyPair.get(0));
        serverKeys.add(keyPair.get(2));
        serverKeys.addAll(
            OnionStdIdxPirNativeUtils.encryptSecretKey(params.getEncryptionParams(), keyPair.get(0), keyPair.get(1))
        );
        return Pair.of(clientKeys, serverKeys);
    }

    @Override
    public void query(int x) {
        int indexOfPlaintext = x / elementSizeOfPlaintext;
        // compute indices for each dimension
        int[] indices = PirUtils.decomposeIndex(indexOfPlaintext, dimensionSize);
        List<byte[]> queryPayload =  OnionStdIdxPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, indices, dimensionSize
        );
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    @Override
    public byte[] recover(int x) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == partitionSize);
        BigInteger[] partitionEntries = new BigInteger[partitionSize];
        IntStream intStream = parallel ? IntStream.range(0, partitionSize).parallel() : IntStream.range(0, partitionSize);
        intStream.forEach(partitionIndex -> {
            long[] coeffs = OnionStdIdxPirNativeUtils.decryptReply(
                params.getEncryptionParams(), secretKey, responsePayload.get(partitionIndex)
            );
            byte[] bytes = PirUtils.convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
            int offset = x % elementSizeOfPlaintext;
            byte[] partitionBytes = new byte[partitionByteLength];
            System.arraycopy(bytes, offset * partitionByteLength, partitionBytes, 0, partitionByteLength);
            partitionEntries[partitionIndex] = BigIntegerUtils.byteArrayToNonNegBigInteger(partitionBytes);
        });
        BigInteger entry = BigInteger.ZERO;
        for (BigInteger partitionEntry : partitionEntries) {
            entry = entry.shiftLeft(partitionBitLength).or(partitionEntry);
        }
        return BigIntegerUtils.nonNegBigIntegerToByteArray(entry, byteL);
    }

    @Override
    public void dummyRecover() throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == partitionSize);
    }
}