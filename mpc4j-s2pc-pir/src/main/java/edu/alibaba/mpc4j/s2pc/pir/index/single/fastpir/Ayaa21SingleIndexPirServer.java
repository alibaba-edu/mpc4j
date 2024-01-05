package edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirPtoDesc.PtoStep;

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
public class Ayaa21SingleIndexPirServer extends AbstractSingleIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Fast PIR params
     */
    private Ayaa21SingleIndexPirParams params;
    /**
     * Galois keys
     */
    private byte[] galoisKeys;
    /**
     * BFV plaintext in NTT form
     */
    private List<byte[][]> encodedDatabase;
    /**
     * query ciphertext size
     */
    private int querySize;
    /**
     * element column size
     */
    private int elementColumnLength;

    public Ayaa21SingleIndexPirServer(Rpc serverRpc, Party clientParty, Ayaa21SingleIndexPirConfig config) {
        super(Ayaa21SingleIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        setInitInput(database);
        assert (indexPirParams instanceof Ayaa21SingleIndexPirParams);
        params = (Ayaa21SingleIndexPirParams) indexPirParams;
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
    public void init(NaiveDatabase database) throws MpcAbortException {
        setInitInput(database);
        setDefaultParams();
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
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();

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
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 1);
        galoisKeys = clientPublicKeysPayload.remove(0);
    }

    @Override
    public List<byte[][]> serverSetup(NaiveDatabase database) {
        int byteLength = database.getByteL();
        if (byteLength % 2 == 1) {
            byteLength++;
        }
        int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
        partitionBitLength = Math.min(maxPartitionBitLength, byteLength * Byte.SIZE);
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        databases = database.partitionZl(partitionBitLength);
        partitionSize = databases.length;
        querySize = CommonUtils.getUnitNum(database.rows(), params.getPolyModulusDegree() / 2);
        elementColumnLength = CommonUtils.getUnitNum(
            (partitionByteLength / 2) * Byte.SIZE, params.getPlainModulusBitLength()
        );
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
    }

    /**
     * database preprocess.
     *
     * @param partitionIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private byte[][] preprocessDatabase(int partitionIndex) {
        int coeffCount = params.getPolyModulusDegree();
        long[][] encodedDatabase = new long[querySize * elementColumnLength][coeffCount];
        int rowSize = params.getPolyModulusDegree() / 2;
        for (int i = 0; i < databases[partitionIndex].rows(); i++) {
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
        return Ayaa21SingleIndexPirNativeUtils.nttTransform(params.getEncryptionParams(), encodedDatabase)
            .toArray(new byte[0][]);
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQueryPayload, List<byte[][]> encodedDatabase) {
        List<byte[]> clientQuery = IntStream.range(0, querySize)
            .mapToObj(clientQueryPayload::get)
            .collect(Collectors.toCollection(ArrayList::new));
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Ayaa21SingleIndexPirNativeUtils.generateResponse(
                params.getEncryptionParams(),
                galoisKeys,
                clientQuery,
                encodedDatabase.get(i),
                elementColumnLength))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQuery) throws MpcAbortException {
        return generateResponse(clientQuery, encodedDatabase);
    }

    @Override
    public void setDefaultParams() {
        params = Ayaa21SingleIndexPirParams.DEFAULT_PARAMS;
    }

    @Override
    public int getQuerySize() {
        return querySize;
    }
}
