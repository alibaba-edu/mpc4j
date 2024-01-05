package edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirPtoDesc.PtoStep;

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
public class Mr23SingleIndexPirServer extends AbstractSingleIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Vectorized PIR params
     */
    private Mr23SingleIndexPirParams params;
    /**
     * BFV plaintext in NTT form
     */
    private List<byte[][]> encodedDatabase;
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

    public Mr23SingleIndexPirServer(Rpc serverRpc, Party clientParty, Mr23SingleIndexPirConfig config) {
        super(Mr23SingleIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        setInitInput(database);
        assert (indexPirParams instanceof Mr23SingleIndexPirParams);
        params = (Mr23SingleIndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive public keys
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

        // receive public keys
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
    public List<byte[]> generateResponse(List<byte[]> clientQueryPayload, List<byte[][]> encodedDatabase) {
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Mr23SingleIndexPirNativeUtils.generateReply(
                params.getEncryptionParams(),
                clientQueryPayload,
                encodedDatabase.get(i),
                publicKey,
                relinKeys,
                galoisKeys,
                params.firstTwoDimensionSize)
            )
            .collect(toCollection(ArrayList::new));
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQuery) throws MpcAbortException {
        return generateResponse(clientQuery, encodedDatabase);
    }

    @Override
    public int getQuerySize() {
        return params.getDimension();
    }

    /**
     * database preprocess.
     *
     * @param partitionIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private byte[][] preprocessDatabase(int partitionIndex) {
        long[] items = IntStream.range(0, num)
            .mapToLong(i -> IntUtils.fixedByteArrayToNonNegInt(databases[partitionIndex].getBytesData(i)))
            .toArray();
        int roundedNum = (int) (Math.pow(params.firstTwoDimensionSize, 2) * params.thirdDimensionSize);
        int size = CommonUtils.getUnitNum(roundedNum, params.firstTwoDimensionSize);
        long[][] coeffs = new long[size][params.getPolyModulusDegree()];
        for (int i = 0; i < roundedNum; i++) {
            int plaintextIndex = i / params.firstTwoDimensionSize;
            int slot = (i * params.gap) % params.rowSize;
            if (i < num) {
                coeffs[plaintextIndex][slot] = items[i];
            } else {
                coeffs[plaintextIndex][slot] = secureRandom.nextInt(1 << params.getPlainModulusBitLength());
            }
        }
        for (int i = 0; i < size; i += params.firstTwoDimensionSize) {
            for (int j = 0; j < params.firstTwoDimensionSize; j++) {
                coeffs[i + j] = PirUtils.plaintextRotate(coeffs[i + j], j * params.gap);
            }
        }
        return Mr23SingleIndexPirNativeUtils.preprocessDatabase(params.getEncryptionParams(), coeffs)
            .toArray(new byte[0][]);
    }

    @Override
    public List<byte[][]> serverSetup(NaiveDatabase database) {
        params.calculateDimensions(database.rows());
        int maxPartitionBitLength = params.getPlainModulusBitLength() - 1;
        partitionBitLength = Math.min(maxPartitionBitLength, database.getL());
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        databases = database.partitionZl(partitionBitLength);
        partitionSize = databases.length;
        assert params.getDimension() == 3;
        int product = (int) (Math.pow(params.firstTwoDimensionSize, 2) * params.thirdDimensionSize);
        assert product >= num;
        // encode database
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
    }

    @Override
    public void setPublicKey(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 3);
        publicKey = clientPublicKeysPayload.remove(0);
        relinKeys = clientPublicKeysPayload.remove(0);
        galoisKeys = clientPublicKeysPayload.remove(0);
    }

    @Override
    public void setDefaultParams() {
        params = Mr23SingleIndexPirParams.DEFAULT_PARAMS;
    }
}