package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.fhe.Evaluator;
import edu.alibaba.mpc4j.crypto.fhe.GaloisKeys;
import edu.alibaba.mpc4j.crypto.fhe.Plaintext;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.Seal4jStdIdxPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.Seal4jStdIdxPirPtoDesc.getInstance;

/**
 * SEAL PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Seal4jStdIdxPirServer extends AbstractStdIdxPirServer implements PbcableStdIdxPirServer {

    /**
     * SEAL PIR params
     */
    private final Seal4jStdIdxPirParams params;
    /**
     * Galois Keys
     */
    private GaloisKeys galoisKeys;
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
     * BFV plaintext size
     */
    private int plaintextSize;
    /**
     * dimension size
     */
    private int[] dimensionSize;
    /**
     * BFV plaintext in NTT form
     */
    private List<Plaintext[]> encodedDatabase;
    /**
     * query payload size
     */
    private int queryPayloadSize;

    private final SealContext context;

    private final EncryptionParameters encryptionParameters;

    private Evaluator evaluator;


    public Seal4jStdIdxPirServer(Rpc serverRpc, Party clientParty, Seal4jStdIdxPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        params = config.getStdIdxPirParams();
        encryptionParameters = params.getEncryptionParameters();
        context = new SealContext(encryptionParameters);
    }

    @Override
    public void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        List<byte[]> serverKeysPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal());
        init(serverKeysPayload, database, maxBatchNum);
    }

    @Override
    public void init(List<byte[]> serverKeys, NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        if (serverKeys == null) {
            init(database, maxBatchNum);
        } else {
            setInitInput(database, maxBatchNum);
            logPhaseInfo(PtoState.INIT_BEGIN);


            evaluator = new Evaluator(context);

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(serverKeys.size() == 1);
            galoisKeys = new GaloisKeys();
            try {
                galoisKeys.load(context, serverKeys.getFirst());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
            partitionBitLength = Math.min(maxPartitionBitLength, database.getL());
            partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
            ZlDatabase[] databases = database.partitionZl(partitionBitLength);
            partitionSize = databases.length;
            elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
                partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
            );
            plaintextSize = CommonUtils.getUnitNum(database.rows(), elementSizeOfPlaintext);
            dimensionSize = PirUtils.computeDimensionLength(plaintextSize, params.getDimension());
            // encode database
            IntStream intStream = parallel ? IntStream.range(0, partitionSize).parallel() : IntStream.range(0, partitionSize);
            encodedDatabase = intStream
                .mapToObj(partitionIndex -> preprocessDatabase(databases, partitionIndex))
                .collect(Collectors.toList());
            queryPayloadSize = params.getDimension();
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

            logPhaseInfo(PtoState.INIT_END);
        }
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
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * database preprocess.
     *
     * @param partitionIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private Plaintext[] preprocessDatabase(ZlDatabase[] databases, int partitionIndex) {
        byte[] combinedBytes = new byte[databases[partitionIndex].rows() * partitionByteLength];
        IntStream.range(0, databases[partitionIndex].rows()).forEach(rowIndex -> {
            byte[] element = databases[partitionIndex].getBytesData(rowIndex);
            System.arraycopy(element, 0, combinedBytes, rowIndex * partitionByteLength, partitionByteLength);
        });
        // number of FV plaintexts needed to create the d-dimensional matrix
        int prod = Arrays.stream(dimensionSize).reduce(1, (a, b) -> a * b);
        assert (plaintextSize <= prod);
        List<long[]> coeffsList = new ArrayList<>();
        int byteSizeOfPlaintext = elementSizeOfPlaintext * partitionByteLength;
        int totalByteSize = databases[partitionIndex].rows() * partitionByteLength;
        int usedCoeffSize = elementSizeOfPlaintext *
            CommonUtils.getUnitNum(Byte.SIZE * partitionByteLength, params.getPlainModulusBitLength());
        assert (usedCoeffSize <= params.getPolyModulusDegree())
            : "coefficient num must be less than or equal to polynomial degree";
        int offset = 0;
        for (int i = 0; i < plaintextSize; i++) {
            int processByteSize;
            if (totalByteSize <= offset) {
                break;
            } else if (totalByteSize < offset + byteSizeOfPlaintext) {
                processByteSize = totalByteSize - offset;
            } else {
                processByteSize = byteSizeOfPlaintext;
            }
            assert (processByteSize % partitionByteLength == 0);
            // Get the coefficients of the elements that will be packed in plaintext i
            long[] coeffs = PirUtils.convertBytesToCoeffs(
                params.getPlainModulusBitLength(), offset, processByteSize, combinedBytes
            );
            assert (coeffs.length <= usedCoeffSize);
            offset += processByteSize;
            long[] paddingCoeffsArray = new long[params.getPolyModulusDegree()];
            System.arraycopy(coeffs, 0, paddingCoeffsArray, 0, coeffs.length);
            // Pad the rest with 1s
            IntStream.range(coeffs.length, params.getPolyModulusDegree()).forEach(j -> paddingCoeffsArray[j] = 1L);
            coeffsList.add(paddingCoeffsArray);
        }
        // Add padding plaintext to make database a matrix
        int currentPlaintextSize = coeffsList.size();
        assert (currentPlaintextSize <= plaintextSize);
        IntStream.range(0, (prod - currentPlaintextSize))
            .mapToObj(i -> IntStream.range(0, params.getPolyModulusDegree()).mapToLong(i1 -> 1L).toArray())
            .forEach(coeffsList::add);
        return Seal4jStdIdxPirUtils.nttTransform(coeffsList, context, evaluator).toArray(new Plaintext[0]);
    }

    @Override
    public void answer() throws MpcAbortException {
        List<byte[]> queryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(queryPayload.size() == queryPayloadSize);
        IntStream intStream = parallel ? IntStream.range(0, partitionSize).parallel() : IntStream.range(0, partitionSize);
        List<byte[]> serverResponsePayload = intStream
            .mapToObj(i -> Seal4jStdIdxPirUtils.generateReply(
                encryptionParameters,
                queryPayload,
                encodedDatabase.get(i),
                dimensionSize,
                context,
                evaluator,
                galoisKeys))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), serverResponsePayload);
    }
}