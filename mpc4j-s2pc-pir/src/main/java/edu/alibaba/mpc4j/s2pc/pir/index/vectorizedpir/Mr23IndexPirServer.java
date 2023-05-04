package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir.Mr23IndexPirPtoDesc.PtoStep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toCollection;

/**
 * Vectorized PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23IndexPirServer extends AbstractIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Vectorized PIR params
     */
    private Mr23IndexPirParams params;
    /**
     * BFV plaintext in NTT form
     */
    private List<List<byte[]>> encodedDatabase;
    /**
     * partition byte length
     */
    private int partitionByteLength;
    /**
     * dimension size
     */
    private int[] dimensionSize;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * relin keys
     */
    private byte[] relinKeys;
    /**
     * Galois keys
     */
    private byte[] galoisKeys;

    public Mr23IndexPirServer(Rpc serverRpc, Party clientParty, Mr23IndexPirConfig config) {
        super(Mr23IndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        assert (indexPirParams instanceof Mr23IndexPirParams);
        params = (Mr23IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive public keys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        handleClientPublicKeysPayload(publicKeyPayload);
        int maxPartitionByteLength = params.getPlainModulusBitLength()/ Byte.SIZE;
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
        params = Mr23IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive public keys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        handleClientPublicKeysPayload(publicKeyPayload);
        int maxPartitionByteLength = params.getPlainModulusBitLength()/ Byte.SIZE;
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
     * server handle client query.
     *
     * @param clientQueryPayload client query.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> handleClientQueryPayload(List<byte[]> clientQueryPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == params.getDimension());
        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Mr23IndexPirNativeUtils.generateReply(
                params.getEncryptionParams(),
                clientQueryPayload,
                encodedDatabase.get(i),
                publicKey,
                relinKeys,
                galoisKeys,
                params.getFirstTwoDimensionSize(),
                params.getThirdDimensionSize())
            )
            .collect(toCollection(ArrayList::new));
    }

    /**
     * database preprocess.
     *
     * @param partitionIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private List<byte[]> preprocessDatabase(int partitionIndex) {
        long[] coeffs = new long[num];
        IntStream.range(0, num).forEach(i -> {
            long[] temp = PirUtils.convertBytesToCoeffs(
                params.getPlainModulusBitLength(), 0, partitionByteLength, databases[partitionIndex].getBytesData(i)
            );
            assert temp.length == 1;
            coeffs[i] = temp[0];
        });
        int totalSize = params.getFirstTwoDimensionSize() * params.getThirdDimensionSize();
        return Mr23IndexPirNativeUtils.preprocessDatabase(
            params.getEncryptionParams(), coeffs, dimensionSize, totalSize
        );
    }

    /**
     * server setup.
     *
     * @return encoded database.
     */
    public List<List<byte[]>> serverSetup() {
        assert params.getDimension() == 3;
        int product = params.getFirstTwoDimensionSize() * params.getFirstTwoDimensionSize() * params.getThirdDimensionSize();
        assert product >= num;
        this.dimensionSize = new int[] {
            params.getThirdDimensionSize(), params.getFirstTwoDimensionSize(), params.getFirstTwoDimensionSize()
        };
        // encode database
        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
    }

    /**
     * server handle client public keys.
     *
     * @param clientPublicKeysPayload public keys.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public void handleClientPublicKeysPayload(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 3);
        this.publicKey = clientPublicKeysPayload.remove(0);
        this.relinKeys = clientPublicKeysPayload.remove(0);
        this.galoisKeys = clientPublicKeysPayload.remove(0);
    }
}