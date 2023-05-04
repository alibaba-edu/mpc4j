package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirPtoDesc.PtoStep;

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
public class Ayaa21IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Fast PIR params
     */
    private Ayaa21IndexPirParams params;
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
    /**
     * partition size
     */
    private int partitionSize;
    /**
     * partition byte length
     */
    private int partitionByteLength;

    public Ayaa21IndexPirClient(Rpc clientRpc, Party serverParty, Ayaa21IndexPirConfig config) {
        super(Ayaa21IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        assert (indexPirParams instanceof Ayaa21IndexPirParams);
        params = (Ayaa21IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> publicKeysPayload;
        if (elementByteLength % 2 == 1) {
            setInitInput(serverElementSize, elementByteLength + 1);
            publicKeysPayload = clientSetup();
            isPadding = true;
        } else {
            setInitInput(serverElementSize, elementByteLength);
            publicKeysPayload = clientSetup();
            isPadding = false;
        }
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
    public void init(int serverElementSize, int elementByteLength) {
        params = Ayaa21IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> publicKeysPayload;
        if (elementByteLength % 2 == 1) {
            setInitInput(serverElementSize, elementByteLength + 1);
            publicKeysPayload = clientSetup();
            isPadding = true;
        } else {
            setInitInput(serverElementSize, elementByteLength);
            publicKeysPayload = clientSetup();
            isPadding = false;
        }
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
        List<byte[]> clientQueryPayload = generateQuery();
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
        byte[] element = handleServerResponsePayload(serverResponsePayload);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return element;
    }

    /**
     * client setup.
     *
     * @return public keys.
     */
    public List<byte[]> clientSetup() {
        querySize = (int) Math.ceil(num / (params.getPolyModulusDegree() / 2.0));
        int maxPartitionByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        partitionByteLength = Math.min(maxPartitionByteLength, elementByteLength);
        partitionSize = CommonUtils.getUnitNum(elementByteLength, partitionByteLength);
        elementColumnLength = (int) Math.ceil((partitionByteLength/2.0) * Byte.SIZE / params.getPlainModulusBitLength());
        return generateKeyPair();
    }

    /**
     * client generate query.
     *
     * @return client query.
     */
    public List<byte[]> generateQuery() {
        return Ayaa21IndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, index, querySize
        );
    }

    /**
     * client decodes server response.
     *
     * @param response server response.
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private byte[] handleServerResponsePayload(List<byte[]> response) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(response.size() == partitionSize);
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(partitionIndex -> {
            long[] coeffs = Ayaa21IndexPirNativeUtils.decodeResponse(
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
        byte[] temp = NaiveDatabase.createFromZl(elementByteLength * Byte.SIZE, databases).getBytesData(0);
        byte[] element;
        if (isPadding) {
            element = new byte[elementByteLength - 1];
            System.arraycopy(temp, 1, element, 0, elementByteLength - 1);
        } else {
            element = temp;
        }
        return element;
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
        List<byte[]> keyPair = Ayaa21IndexPirNativeUtils.keyGen(
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