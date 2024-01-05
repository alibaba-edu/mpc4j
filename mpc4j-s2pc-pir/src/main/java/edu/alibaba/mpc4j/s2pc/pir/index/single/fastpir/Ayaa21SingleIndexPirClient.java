package edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirPtoDesc.PtoStep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * FastPIR client.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21SingleIndexPirClient extends AbstractSingleIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Fast PIR params
     */
    private Ayaa21SingleIndexPirParams params;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * secret key
     */
    private byte[] secretKey;
    /**
     * is padding
     */
    private boolean isPadding;
    /**
     * element column size
     */
    private int elementColumnLength;
    /**
     * query ciphertext size
     */
    private int querySize;

    public Ayaa21SingleIndexPirClient(Rpc clientRpc, Party serverParty, Ayaa21SingleIndexPirConfig config) {
        super(Ayaa21SingleIndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, int serverElementSize, int elementBitLength) {
        setInitInput(serverElementSize, elementBitLength);
        assert (indexPirParams instanceof Ayaa21SingleIndexPirParams);
        params = (Ayaa21SingleIndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> publicKeysPayload = clientSetup(serverElementSize, elementBitLength);
        // client sends Galois keys
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
        // client sends Galois keys
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
    public List<byte[]> clientSetup(int serverElementSize, int elementBitLength) {
        int elementByteLength = CommonUtils.getByteLength(elementBitLength);
        isPadding = elementByteLength % 2 == 1;
        int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
        int paddingElementBitLength = isPadding ? elementBitLength + Byte.SIZE : elementBitLength;
        partitionBitLength = Math.min(maxPartitionBitLength, paddingElementBitLength);
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        partitionSize = CommonUtils.getUnitNum(paddingElementBitLength, partitionBitLength);
        querySize = CommonUtils.getUnitNum(serverElementSize, params.getPolyModulusDegree() / 2);
        elementColumnLength = CommonUtils.getUnitNum(
            (partitionByteLength / 2) * Byte.SIZE, params.getPlainModulusBitLength()
        );
        return generateKeyPair();
    }

    @Override
    public List<byte[]> generateQuery(int index) {
        return Ayaa21SingleIndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, index, querySize
        );
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
            long[] coeffs = Ayaa21SingleIndexPirNativeUtils.decodeResponse(
                params.getEncryptionParams(), secretKey, response.get(partitionIndex)
            );
            int rowCount = coeffs.length / 2;
            long[][] rotatedCoeffs = new long[2][rowCount];
            IntStream.range(0, rowCount).forEach(i -> {
                rotatedCoeffs[0][i] = coeffs[(index + i) % rowCount];
                rotatedCoeffs[1][i] = coeffs[rowCount + ((index + i) % rowCount)];
            });
            byte[] upperBytes = PirUtils.convertCoeffsToBytes(rotatedCoeffs[0], params.getPlainModulusBitLength());
            byte[] lowerBytes = PirUtils.convertCoeffsToBytes(rotatedCoeffs[1], params.getPlainModulusBitLength());
            byte[] partitionBytes = new byte[partitionByteLength];
            System.arraycopy(upperBytes, 0, partitionBytes, 0, partitionByteLength / 2);
            System.arraycopy(lowerBytes, 0, partitionBytes, partitionByteLength / 2, partitionByteLength / 2);
            databases[partitionIndex] = ZlDatabase.create(partitionByteLength * Byte.SIZE, new byte[][]{partitionBytes});
        });
        int paddingElementBitLength = isPadding ? elementBitLength + Byte.SIZE : elementBitLength;
        byte[] temp = NaiveDatabase.createFromZl(paddingElementBitLength, databases).getBytesData(0);
        byte[] element;
        if (isPadding) {
            element = new byte[CommonUtils.getByteLength(paddingElementBitLength) - 1];
            System.arraycopy(temp, 1, element, 0, CommonUtils.getByteLength(paddingElementBitLength) - 1);
        } else {
            element = temp;
        }
        return element;
    }

    @Override
    public void setDefaultParams() {
        params = Ayaa21SingleIndexPirParams.DEFAULT_PARAMS;
    }

    /**
     * client generates key pair.
     *
     * @return public keys.
     */
    private List<byte[]> generateKeyPair() {
        List<Integer> steps = new ArrayList<>();
        for (int i = 1; i < elementColumnLength; i <<= 1) {
            steps.add(-i);
        }
        List<byte[]> keyPair = Ayaa21SingleIndexPirNativeUtils.keyGen(
            params.getEncryptionParams(), steps.stream().mapToInt(step -> step).toArray()
        );
        assert (keyPair.size() == 3);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        // add Galois keys
        List<byte[]> publicKeys = new ArrayList<>();
        publicKeys.add(keyPair.remove(0));
        return publicKeys;
    }
}