package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirClient;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.Seal4jStdIdxPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.Seal4jStdIdxPirPtoDesc.getInstance;

/**
 * SEAL PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Seal4jStdIdxPirClient extends AbstractStdIdxPirClient implements PbcableStdIdxPirClient {

    /**
     * SEAL PIR params
     */
    private final Seal4jStdIdxPirParams params;
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
    private PublicKey publicKey;
    /**
     * private key
     */
    private SecretKey secretKey;

    private final SealContext context;

    private final EncryptionParameters encryptionParameters;

    private Evaluator evaluator;

    private Encryptor encryptor;

    private Decryptor decryptor;

    public Seal4jStdIdxPirClient(Rpc clientRpc, Party serverParty, Seal4jStdIdxPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        params = config.getStdIdxPirParams();
        encryptionParameters = params.getEncryptionParameters();
        context = new SealContext(encryptionParameters);
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

            evaluator = new Evaluator(context);

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(clientKeys.size() == 2);
            this.publicKey = new PublicKey();
            this.secretKey = new SecretKey();
            try {
                publicKey.load(context, clientKeys.get(0));
                secretKey.load(context, clientKeys.get(1));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            encryptor = new Encryptor(context, publicKey, secretKey);
            decryptor = new Decryptor(context, secretKey);

            int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
            partitionBitLength = Math.min(maxPartitionBitLength, l);
            partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
            partitionSize = CommonUtils.getUnitNum(l, partitionBitLength);
            elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
                partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
            );
            int plaintextSize = CommonUtils.getUnitNum(n, elementSizeOfPlaintext);
            dimensionSize = PirUtils.computeDimensionLength(plaintextSize, params.getDimension());
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
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    @Override
    public Pair<List<byte[]>, List<byte[]>> keyGen() {
        KeyGenerator keyGenerator = new KeyGenerator(context);
        List<byte[]> keyPair = Seal4jStdIdxPirUtils.keyGen(params.getEncryptionParameters(), keyGenerator);
        assert (keyPair.size() == 3);
        List<byte[]> clientKeys = new ArrayList<>();
        clientKeys.add(keyPair.get(0));
        clientKeys.add(keyPair.get(1));
        // client sends Galois keys
        List<byte[]> serverKeys = new ArrayList<>();
        serverKeys.add(keyPair.get(2));
        return Pair.of(clientKeys, serverKeys);
    }

    @Override
    public void query(int x) {
        int indexOfPlaintext = x / elementSizeOfPlaintext;
        // compute indices for each dimension
        int[] indices = PirUtils.decomposeIndex(indexOfPlaintext, dimensionSize);
        List<byte[]> queryPayload = Seal4jStdIdxPirUtils.generateQuery(
            params.getEncryptionParameters(),
            indices,
            dimensionSize,
            encryptor
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
            long[] coeffs = Seal4jStdIdxPirUtils.decryptReply(
                responsePayload.subList(partitionIndex * partitionResponseSize, (partitionIndex + 1) * partitionResponseSize),
                params.getDimension(),
                context,
                decryptor
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
