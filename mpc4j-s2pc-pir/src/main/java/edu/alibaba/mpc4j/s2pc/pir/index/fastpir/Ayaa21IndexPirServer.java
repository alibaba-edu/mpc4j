package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirPtoDesc.PtoStep;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * FastPIR server.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Fast PIR params
     */
    private Ayaa21IndexPirParams params;
    /**
     * Galois keys
     */
    private byte[] galoisKeys;
    /**
     * BFV plaintext in NTT form
     */
    private List<List<byte[]>> encodedDatabase;
    /**
     * query ciphertext size
     */
    private int querySize;
    /**
     * partition byte length
     */
    private int partitionByteLength;
    /**
     * element column size
     */
    private int elementColumnLength;

    public Ayaa21IndexPirServer(Rpc serverRpc, Party clientParty, Ayaa21IndexPirConfig config) {
        super(Ayaa21IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        assert (indexPirParams instanceof Ayaa21IndexPirParams);
        params = (Ayaa21IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive Galois keys
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
        params = Ayaa21IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive Galois keys
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

        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();

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
     * server handle client public keys.
     *
     * @param clientPublicKeysPayload public keys.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public void handleClientPublicKeysPayload(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 1);
        this.galoisKeys = clientPublicKeysPayload.remove(0);
    }

    /**
     * compute partition byte length.
     *
     * @param totalByteLength element byte length.
     */
    public void computePartitionByteLength(int totalByteLength) {
        if (totalByteLength % 2 == 1) {
            totalByteLength++;
        }
        int maxPartitionByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        partitionByteLength = Math.min(maxPartitionByteLength, totalByteLength);
    }

    /**
     * server setup.
     */
    public void serverSetup() {
        querySize = (int) Math.ceil(num / (params.getPolyModulusDegree() / 2.0));
        elementColumnLength = (int) Math.ceil((partitionByteLength/2.0) * Byte.SIZE / params.getPlainModulusBitLength());
        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        encodedDatabase = intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
    }

    /**
     * database preprocess.
     *
     * @param partitionIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private List<byte[]> preprocessDatabase(int partitionIndex) {
        int coeffCount = params.getPolyModulusDegree();
        long[][] encodedDatabase = new long[querySize * elementColumnLength][coeffCount];
        int rowSize = params.getPolyModulusDegree() / 2;
        for (int i = 0; i < num; i++) {
            int rowIndex = i / rowSize;
            int colIndex = i % rowSize;
            byte[] element = databases[partitionIndex].getBytesData(i);
            int length = element.length / 2;
            byte[] upperBytes = new byte[length];
            System.arraycopy(element, 0, upperBytes, 0, length);
            byte[] lowerBytes = new byte[length];
            System.arraycopy(element, length, lowerBytes, 0, length);
            long[] upperCoeffs = PirUtils.convertBytesToCoeffs(params.getPlainModulusBitLength(), 0, length, upperBytes);
            long[] lowerCoeffs = PirUtils.convertBytesToCoeffs(params.getPlainModulusBitLength(), 0, length, lowerBytes);
            for (int j = 0; j < elementColumnLength; j++) {
                encodedDatabase[rowIndex][colIndex] = upperCoeffs[j];
                encodedDatabase[rowIndex][colIndex + rowSize] = lowerCoeffs[j];
                rowIndex += querySize;
            }
        }
        return Ayaa21IndexPirNativeUtils.nttTransform(params.getEncryptionParams(), encodedDatabase);
    }

    /**
     * server handle client query.
     *
     * @param clientQueryPayload client query.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> handleClientQueryPayload(List<byte[]> clientQueryPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == querySize);
        List<byte[]> clientQuery = IntStream.range(0, querySize)
            .mapToObj(clientQueryPayload::get)
            .collect(Collectors.toCollection(ArrayList::new));
        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Ayaa21IndexPirNativeUtils.generateResponse(
                params.getEncryptionParams(),
                galoisKeys,
                clientQuery,
                encodedDatabase.get(i),
                elementColumnLength))
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
