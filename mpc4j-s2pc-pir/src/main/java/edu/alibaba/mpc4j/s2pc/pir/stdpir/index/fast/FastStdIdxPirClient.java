package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast;

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

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirPtoDesc.getInstance;

/**
 * FastPIR client.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class FastStdIdxPirClient extends AbstractStdIdxPirClient implements PbcableStdIdxPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Fast PIR params
     */
    private final FastStdIdxPirParams params;
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
    /**
     * query ciphertext size
     */
    private int querySize;

    public FastStdIdxPirClient(Rpc clientRpc, Party serverParty, FastStdIdxPirConfig config) {
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
            int elementByteLength = CommonUtils.getByteLength(l);
            isPadding = elementByteLength % 2 == 1;
            int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
            int paddingElementBitLength = isPadding ? l + Byte.SIZE : l;
            partitionBitLength = Math.min(maxPartitionBitLength, paddingElementBitLength);
            partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
            partitionSize = CommonUtils.getUnitNum(paddingElementBitLength, partitionBitLength);
            querySize = CommonUtils.getUnitNum(n, params.getPolyModulusDegree() / 2);
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
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = recover(xs[i]);
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        return entries;
    }

    @Override
    public Pair<List<byte[]>, List<byte[]>> keyGen() {
        int maxPartitionByteLength = CommonUtils.getByteLength(
            params.getPolyModulusDegree() * params.getPlainModulusBitLength()
        );
        int columnLength = CommonUtils.getUnitNum(
            (maxPartitionByteLength / 2) * Byte.SIZE, params.getPlainModulusBitLength()
        );
        List<Integer> steps = IntStream.iterate(1, i -> i < columnLength, i -> i << 1).mapToObj(i -> -i).toList();
        List<byte[]> keyPair = FastStdIdxPirNativeUtils.keyGen(
            params.getEncryptionParams(), steps.stream().mapToInt(step -> step).toArray()
        );
        assert keyPair.size() == 3;
        List<byte[]> clientKeys = new ArrayList<>();
        clientKeys.add(keyPair.get(0));
        clientKeys.add(keyPair.get(1));
        List<byte[]> serverKeys = new ArrayList<>();
        // add Galois keys
        serverKeys.add(keyPair.get(2));
        return Pair.of(clientKeys, serverKeys);
    }

    @Override
    public void query(int x) {
        List<byte[]> queryPayload = FastStdIdxPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, x, querySize
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
            long[] coeffs = FastStdIdxPirNativeUtils.decodeResponse(
                params.getEncryptionParams(), secretKey, responsePayload.get(partitionIndex)
            );
            int rowCount = coeffs.length / 2;
            long[][] rotatedCoeffs = new long[2][rowCount];
            IntStream.range(0, rowCount).forEach(i -> {
                rotatedCoeffs[0][i] = coeffs[(x + i) % rowCount];
                rotatedCoeffs[1][i] = coeffs[rowCount + ((x + i) % rowCount)];
            });
            byte[] upperBytes = PirUtils.convertCoeffsToBytes(rotatedCoeffs[0], params.getPlainModulusBitLength());
            byte[] lowerBytes = PirUtils.convertCoeffsToBytes(rotatedCoeffs[1], params.getPlainModulusBitLength());
            byte[] partitionBytes = new byte[partitionByteLength];
            System.arraycopy(upperBytes, 0, partitionBytes, 0, partitionByteLength / 2);
            System.arraycopy(lowerBytes, 0, partitionBytes, partitionByteLength / 2, partitionByteLength / 2);
            partitionEntries[partitionIndex] = BigIntegerUtils.byteArrayToNonNegBigInteger(partitionBytes);
        });
        int paddingElementBitLength = isPadding ? l + Byte.SIZE : l;
        BigInteger entry = BigInteger.ZERO;
        for (BigInteger partitionEntry : partitionEntries) {
            entry = entry.shiftLeft(partitionBitLength).or(partitionEntry);
        }
        byte[] temp = BigIntegerUtils.nonNegBigIntegerToByteArray(entry, CommonUtils.getByteLength(paddingElementBitLength));
        byte[] element;
        if (isPadding) {
            element = new byte[CommonUtils.getByteLength(paddingElementBitLength) - 1];
            System.arraycopy(temp, 1, element, 0, CommonUtils.getByteLength(paddingElementBitLength) - 1);
        } else {
            element = temp;
        }
        return element;
    }

    @Override
    public void dummyRecover() throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == partitionSize);
    }
}