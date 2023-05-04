package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.onionpir.Mcr21IndexPirPtoDesc.PtoStep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OnionPIR server.
 *
 * @author Liqiang Peng
 * @date 2022/11/14
 */
public class Mcr21IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * OnionPIR params
     */
    private Mcr21IndexPirParams params;
    /**
     * Galois Keys
     */
    private byte[] galoisKeys;
    /**
     * public key
     */
    private byte[] publicKey;
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
     * partition byte length
     */
    private int partitionByteLength;
    /**
     * decomposed BFV plaintext
     */
    private List<List<long[]>> encodedDatabase;

    public Mcr21IndexPirServer(Rpc serverRpc, Party clientParty, Mcr21IndexPirConfig config) {
        super(Mcr21IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        assert (indexPirParams instanceof Mcr21IndexPirParams);
        params = (Mcr21IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive Galois keys and encrypted secret key
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        handleClientPublicKeysPayload(publicKeyPayload);
        computePartitionByteLength(database.getByteL());
        setInitInput(database, partitionByteLength);
        serverSetup();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(NaiveDatabase database) throws MpcAbortException {
        params = Mcr21IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive Galois keys and encrypted secret key
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        handleClientPublicKeysPayload(publicKeyPayload);
        computePartitionByteLength(database.getByteL());
        setInitInput(database, partitionByteLength);
        serverSetup();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive query
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();

        stopWatch.start();
        List<byte[]> serverResponse = handleClientQueryPayload(clientQueryPayload);
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponse));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * compute partition byte length.
     *
     * @param totalByteLength element byte length.
     */
    public void computePartitionByteLength(int totalByteLength) {
        int maxPartitionByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        partitionByteLength = Math.min(maxPartitionByteLength, totalByteLength);
    }

    /**
     * server handle client public keys.
     *
     * @param clientPublicKeysPayload public keys.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public void handleClientPublicKeysPayload(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 2 + params.getGswDecompSize() * 2);
        this.publicKey = clientPublicKeysPayload.remove(0);
        this.galoisKeys = clientPublicKeysPayload.remove(0);
        this.encryptedSecretKey = clientPublicKeysPayload;
    }

    /**
     * server setup.
     */
    public void serverSetup() {
        elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
            partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
        );
        plaintextSize = (int) Math.ceil((double) num / this.elementSizeOfPlaintext);
        dimensionSize = PirUtils.computeDimensionLength(
            plaintextSize, params.getFirstDimensionSize(), params.getSubsequentDimensionSize()
        );
        // encode database
        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        encodedDatabase = intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
    }

    /**
     * server handle client query.
     *
     * @param clientQueryPayload client query.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> handleClientQueryPayload(List<byte[]> clientQueryPayload) throws MpcAbortException {
        int expectSize = dimensionSize.length == 1 ? 2 : 3;
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == expectSize);
        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Mcr21IndexPirNativeUtils.generateReply(
                params.getEncryptionParams(),
                publicKey,
                galoisKeys,
                encryptedSecretKey,
                clientQueryPayload,
                encodedDatabase.get(i),
                dimensionSize))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * database preprocess.
     *
     * @param partitionIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private List<long[]> preprocessDatabase(int partitionIndex) {
        byte[] combinedBytes = new byte[num * partitionByteLength];
        IntStream.range(0, num).forEach(rowIndex -> {
            byte[] element = databases[partitionIndex].getBytesData(rowIndex);
            System.arraycopy(element, 0, combinedBytes, rowIndex * partitionByteLength, partitionByteLength);
        });
        // number of FV plaintexts needed to create the d-dimensional matrix
        int prod = Arrays.stream(dimensionSize).reduce(1, (a, b) -> a * b);
        assert (plaintextSize <= prod);
        List<long[]> coeffsList = new ArrayList<>();
        int byteSizeOfPlaintext = elementSizeOfPlaintext * partitionByteLength;
        int totalByteSize = num * partitionByteLength;
        int usedCoeffSize = elementSizeOfPlaintext *
            ((int) Math.ceil(Byte.SIZE * partitionByteLength / (double) params.getPlainModulusBitLength()));
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
        return Mcr21IndexPirNativeUtils.preprocessDatabase(params.getEncryptionParams(), coeffsList);
    }
}
