package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw;

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

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirPtoDesc.getInstance;

/**
 * Constant-weight PIR client
 *
 * @author Qixian Zhou
 * @date 2023/6/18
 */
public class CwStdIdxPirClient extends AbstractStdIdxPirClient implements PbcableStdIdxPirClient {


    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * constant weight PIR params
     */
    private final CwStdIdxPirParams params;
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
     * public key
     */
    private byte[] publicKey;
    /**
     * private key
     */
    private byte[] secretKey;

    public CwStdIdxPirClient(Rpc clientRpc, Party serverParty, CwStdIdxPirConfig config) {
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
            params.setQueryParams(n);
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
    public void query(int x) {
        int indexOfPlaintext = x / elementSizeOfPlaintext;
        // encode PT indices
        int[] encodedIndex = switch (params.getEqualityType()) {
            case FOLKLORE -> CwStdIdxPirUtils.getFolkloreCodeword(
                indexOfPlaintext, params.getCodewordsBitLength()
            );
            case CONSTANT_WEIGHT -> CwStdIdxPirUtils.getPerfectConstantWeightCodeword(
                indexOfPlaintext, params.getCodewordsBitLength(), params.getHammingWeight()
            );
        };
        List<byte[]> queryPayload = CwStdIdxPirNativeUtils.generateQuery(
            params.getEncryptionParams(),
            publicKey,
            secretKey,
            encodedIndex,
            params.getUsedSlotsPerPlain(),
            params.getNumInputCiphers()
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
            long[] coeffs = CwStdIdxPirNativeUtils.decryptReply(
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

    @Override
    public Pair<List<byte[]>, List<byte[]>> keyGen() {
        List<byte[]> keyPair = CwStdIdxPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 4);
        List<byte[]> clientKeys = new ArrayList<>();
        clientKeys.add(keyPair.get(0));
        clientKeys.add(keyPair.get(1));
        List<byte[]> serverKeys = new ArrayList<>();
        // add Galois keys and Relin keys
        serverKeys.add(keyPair.get(2));
        serverKeys.add(keyPair.get(3));
        return Pair.of(clientKeys, serverKeys);
    }
}