package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirPtoDesc.PtoStep;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * XPIR client.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class XpirStdIdxPirClient extends AbstractStdIdxPirClient implements PbcableStdIdxPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR params
     */
    private final XpirStdIdxPirParams params;
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

    public XpirStdIdxPirClient(Rpc clientRpc, Party serverParty, XpirStdIdxPirConfig config) {
        super(XpirStdIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        params = config.getStdIdxPirParams();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        Pair<List<byte[]>, List<byte[]>> keyPair = keyGen();
        // we do not need to send public keys in XPIR since server does not need to do
        // ciphertext-ciphertext multiplication
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
            dimensionSize = PirUtils.computeDimensionLength(plaintextSize, params.getDimension());
            for (int j : dimensionSize) {
                MpcAbortPreconditions.checkArgument(j <= params.getPolyModulusDegree());
            }
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
        List<byte[]> keyPair = XpirStdIdxPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 2);
        List<byte[]> clientKeys = new ArrayList<>();
        clientKeys.add(keyPair.get(0));
        clientKeys.add(keyPair.get(1));
        return Pair.of(clientKeys, new ArrayList<>());
    }

    @Override
    public void query(int x) {
        int indexOfPlaintext = x / elementSizeOfPlaintext;
        // compute indices for each dimension
        int[] indices = PirUtils.decomposeIndex(indexOfPlaintext, dimensionSize);
        List<byte[]> queryPayload = XpirStdIdxPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, indices, dimensionSize
        );
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    @Override
    public byte[] recover(int x) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        int partitionResponseSize = IntStream.range(0, params.getDimension() - 1)
            .map(i -> params.getExpansionRatio())
            .reduce(1, (a, b) -> a * b);
        MpcAbortPreconditions.checkArgument(responsePayload.size() == partitionResponseSize * partitionSize);
        BigInteger[] partitionEntries = new BigInteger[partitionSize];
        IntStream intStream = parallel ? IntStream.range(0, partitionSize).parallel() : IntStream.range(0, partitionSize);
        intStream.forEach(partitionIndex -> {
            long[] coeffs = XpirStdIdxPirNativeUtils.decryptReply(
                params.getEncryptionParams(),
                secretKey,
                responsePayload.subList(partitionIndex * partitionResponseSize, (partitionIndex + 1) * partitionResponseSize),
                params.getDimension()
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
        int partitionResponseSize = IntStream.range(0, params.getDimension() - 1)
            .map(i -> params.getExpansionRatio())
            .reduce(1, (a, b) -> a * b);
        MpcAbortPreconditions.checkArgument(responsePayload.size() == partitionResponseSize * partitionSize);
    }
}
