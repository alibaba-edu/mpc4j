package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion.OnionStdIdxPirPtoDesc.PtoStep;

/**
 * OnionPIR server.
 *
 * @author Liqiang Peng
 * @date 2022/11/14
 */
public class OnionStdIdxPirServer extends AbstractStdIdxPirServer implements PbcableStdIdxPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * OnionPIR params
     */
    private final OnionStdIdxPirParams params;
    /**
     * Galois Keys
     */
    private byte[] galoisKeys;
    /**
     * public key
     */
    private byte[] publicKey;
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
     * encrypted secret key
     */
    private List<byte[]> encryptedSecretKey;
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
     * decomposed BFV plaintext
     */
    private List<List<long[]>> encodedDatabase;
    /**
     * query payload size
     */
    private int queryPayloadSize;

    public OnionStdIdxPirServer(Rpc serverRpc, Party clientParty, OnionStdIdxPirConfig config) {
        super(OnionStdIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        params = config.getStdIdxPirParams();
    }

    @Override
    public void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        // receive public key, Galois keys and encrypted secret key
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

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(serverKeys.size() == 2 + params.getGswDecompSize() * 2);
            this.publicKey = serverKeys.get(0);
            this.galoisKeys = serverKeys.get(1);
            this.encryptedSecretKey = serverKeys.subList(2, serverKeys.size());
            int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
            partitionBitLength = Math.min(maxPartitionBitLength, database.getL());
            partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
            ZlDatabase[] databases = database.partitionZl(partitionBitLength);
            partitionSize = databases.length;
            elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
                partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
            );
            plaintextSize = CommonUtils.getUnitNum(database.rows(), elementSizeOfPlaintext);
            dimensionSize = PirUtils.computeDimensionLength(
                plaintextSize, params.getFirstDimensionSize(), params.getSubsequentDimensionSize()
            );
            // encode database
            IntStream intStream = parallel ? IntStream.range(0, partitionSize).parallel() : IntStream.range(0, partitionSize);
            encodedDatabase = intStream
                .mapToObj(partitionIndex -> preprocessDatabase(databases, partitionIndex))
                .collect(Collectors.toList());
            queryPayloadSize = dimensionSize.length == 1 ? 2 : 3;
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

    @Override
    public void answer() throws MpcAbortException {
        List<byte[]> queryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(queryPayload.size() == queryPayloadSize);
        IntStream intStream = parallel ? IntStream.range(0, partitionSize).parallel() : IntStream.range(0, partitionSize);
        List<byte[]> serverResponsePayload = intStream
            .mapToObj(i -> OnionStdIdxPirNativeUtils.generateReply(
                params.getEncryptionParams(),
                publicKey,
                galoisKeys,
                encryptedSecretKey,
                queryPayload,
                encodedDatabase.get(i),
                dimensionSize))
            .collect(Collectors.toCollection(ArrayList::new));
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), serverResponsePayload);
    }

    /**
     * database preprocess.
     *
     * @param partitionIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private List<long[]> preprocessDatabase(ZlDatabase[] databases, int partitionIndex) {
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
        return OnionStdIdxPirNativeUtils.preprocessDatabase(params.getEncryptionParams(), coeffsList);
    }
}