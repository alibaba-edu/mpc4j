package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirPtoDesc.PtoStep;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * XPIR server.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR params
     */
    private Mbfk16IndexPirParams params;
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
     * BFV plaintext in NTT form
     */
    private List<List<byte[]>> encodedDatabase;

    public Mbfk16IndexPirServer(Rpc serverRpc, Party clientParty, Mbfk16IndexPirConfig config) {
        super(Mbfk16IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        assert (indexPirParams instanceof Mbfk16IndexPirParams);
        params = (Mbfk16IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxPartitionByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        partitionByteLength = Math.min(maxPartitionByteLength, database.getByteL());
        setInitInput(database, partitionByteLength);
        encodedDatabase = serverSetup();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(NaiveDatabase database) throws MpcAbortException {
        params = Mbfk16IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxPartitionByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        partitionByteLength = Math.min(maxPartitionByteLength, database.getByteL());
        setInitInput(database, partitionByteLength);
        encodedDatabase = serverSetup();
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

        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());

        // receive query
        stopWatch.start();
        List<byte[]> serverResponsePayload = handleClientQueryPayload(clientQueryPayload);
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * server setup.
     *
     * @return encoded database.
     */
    public List<List<byte[]>> serverSetup() {
        elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
            partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
        );
        plaintextSize = (int) Math.ceil((double) num / this.elementSizeOfPlaintext);
        dimensionSize = PirUtils.computeDimensionLength(plaintextSize, params.getDimension());
        // encode database
        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
    }

    /**
     * server handle client query.
     *
     * @param clientQueryPayload client query.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public List<byte[]> handleClientQueryPayload(List<byte[]> clientQueryPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == Arrays.stream(dimensionSize).sum());
        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Mbfk16IndexPirNativeUtils.generateReply(
                params.getEncryptionParams(), clientQueryPayload, encodedDatabase.get(i), dimensionSize)
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * database preprocess.
     *
     * @param partitionIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    public List<byte[]> preprocessDatabase(int partitionIndex) {
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
        return Mbfk16IndexPirNativeUtils.nttTransform(params.getEncryptionParams(), coeffsList);
    }
}