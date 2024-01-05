package edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirPtoDesc.*;

/**
 * Constant-weight PIR server
 *
 * @author Qixian Zhou
 * @date 2023/6/18
 */
public class Mk22SingleIndexPirServer extends AbstractSingleIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * constant weight PIR params
     */
    private Mk22SingleIndexPirParams params;
    /**
     * Galois Keys
     */
    private byte[] galoisKeys;
    /**
     * Relin keys
     */
    private byte[] relinKeys;
    /**
     * element size per BFV plaintext
     */
    private int elementSizeOfPlaintext;
    /**
     * BFV plaintext size
     */
    private int plaintextSize;
    /**
     * plaintext index codeword
     */
    private List<int[]> plaintextIndexCodewords;
    /**
     * BFV plaintext in NTT form
     */
    private List<byte[][]> encodedDatabase;

    public Mk22SingleIndexPirServer(Rpc serverRpc, Party clientParty, Mk22SingleIndexPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        setInitInput(database);
        assert (indexPirParams instanceof Mk22SingleIndexPirParams);
        params = (Mk22SingleIndexPirParams) indexPirParams;
        params.setQueryParams(database.rows());
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive Galois keys and Relin keys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        setPublicKey(publicKeyPayload);
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
        params.setQueryParams(database.rows());
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive Galois keys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        setPublicKey(publicKeyPayload);
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
    public void setPublicKey(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 2);
        this.galoisKeys = clientPublicKeysPayload.remove(0);
        this.relinKeys = clientPublicKeysPayload.remove(0);
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
        plaintextSize = (int) Math.ceil((double) database.rows() / this.elementSizeOfPlaintext);
        plaintextIndexCodewords = getPlaintextIndexCodeword();
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
        int eqType;
        switch (params.getEqualityType()) {
            case FOLKLORE:
                eqType = 0;
                break;
            case CONSTANT_WEIGHT:
                eqType = 1;
                break;
            default:
                throw new IllegalStateException("Invalid Equality Operator Type");
        }
        return intStream
            .mapToObj(i ->
                Mk22SingleIndexPirNativeUtils.generateReply(
                    params.getEncryptionParams(),
                    galoisKeys,
                    relinKeys,
                    clientQueryPayload,
                    encodedDatabase.get(i),
                    plaintextIndexCodewords,
                    params.getNumInputCiphers(),
                    params.getCodewordsBitLength(),
                    params.getHammingWeight(),
                    eqType
                ))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQuery) throws MpcAbortException {
        return generateResponse(clientQuery, encodedDatabase);
    }

    @Override
    public void setDefaultParams() {
        params = Mk22SingleIndexPirParams.DEFAULT_PARAMS;
    }

    @Override
    public int getQuerySize() {
        return params.getNumInputCiphers();
    }

    /**
     * database preprocess.
     *
     * @param partitionSingleIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private byte[][] preprocessDatabase(int partitionSingleIndex) {
        byte[] combinedBytes = new byte[databases[partitionSingleIndex].rows() * partitionByteLength];
        IntStream.range(0, databases[partitionSingleIndex].rows()).forEach(rowSingleIndex -> {
            byte[] element = databases[partitionSingleIndex].getBytesData(rowSingleIndex);
            System.arraycopy(element, 0, combinedBytes, rowSingleIndex * partitionByteLength, partitionByteLength);
        });
        // in Constant-weight PIR, dimension is always 1.
        List<long[]> coeffsList = new ArrayList<>();
        int byteSizeOfPlaintext = elementSizeOfPlaintext * partitionByteLength;
        int totalByteSize = databases[partitionSingleIndex].rows() * partitionByteLength;
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
        IntStream.range(0, (plaintextSize - currentPlaintextSize))
            .mapToObj(i -> IntStream.range(0, params.getPolyModulusDegree()).mapToLong(i1 -> 1L).toArray())
            .forEach(coeffsList::add);
        return
            Mk22SingleIndexPirNativeUtils.nttTransform(params.getEncryptionParams(), coeffsList).toArray(new byte[0][]);
    }

    /**
     * generate the codeword of index in range [0, plaintextSize -1]
     *
     * @return codewords of indices.
     */
    private List<int[]> getPlaintextIndexCodeword() {
        IntStream intStream = IntStream.range(0, plaintextSize);
        intStream = parallel ? intStream.parallel() : intStream;
        List<int[]> codewords;
        switch (params.getEqualityType()) {
            case FOLKLORE:
                codewords = intStream
                    .mapToObj(i -> Mk22SingleIndexPirUtils.getFolkloreCodeword(i, params.getCodewordsBitLength()))
                    .collect(Collectors.toList());
                break;
            case CONSTANT_WEIGHT:
                codewords = intStream
                    .mapToObj(i -> Mk22SingleIndexPirUtils.getPerfectConstantWeightCodeword(
                        i, params.getCodewordsBitLength(), params.getHammingWeight()
                        ))
                    .collect(Collectors.toList());
                break;
            default:
                throw new IllegalStateException("Invalid Equality Operator Type");
        }
        return codewords;
    }
}

