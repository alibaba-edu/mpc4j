package edu.alibaba.mpc4j.s2pc.pir.index.single.xpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirPtoDesc.*;

/**
 * XPIR server.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16SingleIndexPirServer extends AbstractSingleIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * XPIR params
     */
    private Mbfk16SingleIndexPirParams params;
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
    private List<byte[][]> encodedDatabase;

    public Mbfk16SingleIndexPirServer(Rpc serverRpc, Party clientParty, Mbfk16SingleIndexPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        setInitInput(database);
        assert (indexPirParams instanceof Mbfk16SingleIndexPirParams);
        params = (Mbfk16SingleIndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        encodedDatabase = serverSetup(database);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(NaiveDatabase database) throws MpcAbortException {
        setInitInput(database);
        setDefaultParams();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        encodedDatabase = serverSetup(database);
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
        List<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());

        stopWatch.start();
        List<byte[]> serverResponsePayload = generateResponse(clientQueryPayload, encodedDatabase);
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

    @Override
    public List<byte[][]> serverSetup(NaiveDatabase database) {
        int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
        partitionBitLength = Math.min(maxPartitionBitLength, database.getL());
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        databases = database.partitionZl(partitionBitLength);
        partitionSize = databases.length;
        elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
            partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
        );
        plaintextSize = CommonUtils.getUnitNum(database.rows(), elementSizeOfPlaintext);
        dimensionSize = PirUtils.computeDimensionLength(plaintextSize, params.getDimension());
        // encode database
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQueryPayload, List<byte[][]> encodedDatabase)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == getQuerySize());
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Mbfk16SingleIndexPirNativeUtils.generateReply(
                params.getEncryptionParams(), clientQueryPayload, encodedDatabase.get(i), dimensionSize)
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQuery) throws MpcAbortException {
        return generateResponse(clientQuery, encodedDatabase);
    }

    @Override
    public int getQuerySize() {
        return Arrays.stream(dimensionSize).sum();
    }

    /**
     * database preprocess.
     *
     * @param partitionIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private byte[][] preprocessDatabase(int partitionIndex) {
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
        return Mbfk16SingleIndexPirNativeUtils.nttTransform(params.getEncryptionParams(), coeffsList)
            .toArray(new byte[0][]);
    }

    @Override
    public void setPublicKey(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        // empty;
    }

    @Override
    public void setDefaultParams() {
        params = Mbfk16SingleIndexPirParams.DEFAULT_PARAMS;
    }
}