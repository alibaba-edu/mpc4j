package edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirPtoDesc.*;

/**
 * Vectorized PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23SingleIndexPirClient extends AbstractSingleIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Vectorized PIR params
     */
    private Mr23SingleIndexPirParams params;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * secret key
     */
    private byte[] secretKey;
    /**
     * offset
     */
    private int offset;

    public Mr23SingleIndexPirClient(Rpc clientRpc, Party serverParty, Mr23SingleIndexPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, int serverElementSize, int elementBitLength) {
        setInitInput(serverElementSize, elementBitLength);
        assert (indexPirParams instanceof Mr23SingleIndexPirParams);
        params = (Mr23SingleIndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> publicKeysPayload = clientSetup(serverElementSize, elementBitLength);
        // client sends public keys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int serverElementSize, int elementBitLength) {
        setInitInput(serverElementSize, elementBitLength);
        setDefaultParams();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> publicKeysPayload = clientSetup(serverElementSize, elementBitLength);
        // client sends public keys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // client generates query
        List<byte[]> clientQueryPayload = generateQuery(index);
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();

        stopWatch.start();
        byte[] element = decodeResponse(serverResponsePayload, index);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return element;
    }

    @Override
    public List<byte[]> generateQuery(int index) {
        int[] slotPositions = new int[params.getDimension()];
        slotPositions[2] = (int) (index / Math.pow(params.firstTwoDimensionSize, 2));
        slotPositions[1] = (int)
            ((index - slotPositions[2] * Math.pow(params.firstTwoDimensionSize, 2)) / params.firstTwoDimensionSize);
        slotPositions[0] = index % params.firstTwoDimensionSize;
        int currentSlot = 0;
        for (int i = 0; i < params.getDimension(); i++) {
            int slotPosition = slotPositions[i];
            int rotatedSlot = (currentSlot + slotPosition) % params.firstTwoDimensionSize;
            slotPositions[i] = (rotatedSlot * params.gap) % params.rowSize;
            currentSlot = (currentSlot + slotPosition) % params.firstTwoDimensionSize;
        }
        offset = currentSlot;
        return Mr23SingleIndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, slotPositions
        );
    }

    @Override
    public List<byte[]> clientSetup(int serverElementSize, int elementBitLength) {
        params.calculateDimensions(serverElementSize);
        int maxPartitionBitLength = params.getPlainModulusBitLength() - 1;
        partitionBitLength = Math.min(maxPartitionBitLength, elementBitLength);
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        assert params.getDimension() == 3;
        int product = (int) (Math.pow(params.firstTwoDimensionSize, 2) * params.thirdDimensionSize);
        assert product >= num;
        return generateKeyPair();
    }

    @Override
    public byte[] decodeResponse(List<byte[]> response, int index) throws MpcAbortException {
        return decodeResponse(response, index, elementBitLength);
    }

    @Override
    public byte[] decodeResponse(List<byte[]> response, int index, int elementBitLength) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(response.size() == partitionSize);
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(partitionIndex -> {
            long coeffs = Mr23SingleIndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(), secretKey, response.get(partitionIndex), offset, params.gap
            );
            byte[] bytes = IntUtils.nonNegIntToFixedByteArray(Math.toIntExact(coeffs), partitionByteLength);
            databases[partitionIndex] = ZlDatabase.create(partitionBitLength, new byte[][]{bytes});
        });
        return NaiveDatabase.createFromZl(elementBitLength, databases).getBytesData(0);
    }

    @Override
    public void setDefaultParams() {
        params = Mr23SingleIndexPirParams.DEFAULT_PARAMS;
    }

    /**
     * client generates key pair.
     *
     * @return public keys.
     */
    private List<byte[]> generateKeyPair() {
        List<byte[]> keyPair = Mr23SingleIndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 4);
        publicKey = keyPair.remove(0);
        secretKey = keyPair.remove(0);
        List<byte[]> publicKeys = new ArrayList<>();
        // add public key
        publicKeys.add(publicKey);
        // add Relin keys
        publicKeys.add(keyPair.remove(0));
        // add Galois keys
        publicKeys.add(keyPair.remove(0));
        return publicKeys;
    }
}
