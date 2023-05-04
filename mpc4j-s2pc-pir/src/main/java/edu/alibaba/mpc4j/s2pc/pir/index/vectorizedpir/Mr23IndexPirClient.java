package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

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
import edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir.Mr23IndexPirPtoDesc.PtoStep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Vectorized PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Vectorized PIR params
     */
    private Mr23IndexPirParams params;
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
    /**
     * dimension size
     */
    private int[] dimensionSize;
    /**
     * partition size
     */
    private int partitionSize;
    /**
     * partition byte length
     */
    private int partitionByteLength;

    public Mr23IndexPirClient(Rpc clientRpc, Party serverParty, Mr23IndexPirConfig config) {
        super(Mr23IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        assert (indexPirParams instanceof Mr23IndexPirParams);
        params = (Mr23IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        setInitInput(serverElementSize, elementByteLength);
        List<byte[]> publicKeysPayload = clientSetup();
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
    public void init(int serverElementSize, int elementByteLength) {
        params = Mr23IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        setInitInput(serverElementSize, elementByteLength);
        List<byte[]> publicKeysPayload = clientSetup();
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

        // 客户端接收并解密回复
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
     * client generate query.
     *
     * @return client query.
     */
    public List<byte[]> generateQuery() {
        int[] temp = PirUtils.computeIndices(index, dimensionSize);
        int[] permutedIndices = IntStream.range(0, params.getDimension())
            .map(i -> temp[params.getDimension() - 1 - i])
            .toArray();
        int[] indices = new int[params.getDimension()];
        for (int i = 0; i < params.getDimension(); i++) {
            indices[i] = permutedIndices[i];
            for (int j = 0; j < i; j++) {
                indices[i] = (indices[i] + permutedIndices[j]) % params.getFirstTwoDimensionSize();
            }
        }
        this.offset = indices[params.getDimension() - 1];
        return Mr23IndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, indices, params.getFirstTwoDimensionSize()
        );
    }

    /**
     * client setup.
     *
     * @return public keys.
     */
    public List<byte[]> clientSetup() {
        int maxPartitionByteLength = params.getPlainModulusBitLength()/ Byte.SIZE;
        partitionByteLength = Math.min(maxPartitionByteLength, elementByteLength);
        partitionSize = CommonUtils.getUnitNum(elementByteLength, partitionByteLength);
        assert params.getDimension() == 3;
        int product = params.getFirstTwoDimensionSize() * params.getFirstTwoDimensionSize() * params.getThirdDimensionSize();
        assert product >= num;
        this.dimensionSize = new int[] {
            params.getThirdDimensionSize(), params.getFirstTwoDimensionSize(), params.getFirstTwoDimensionSize()
        };
        return generateKeyPair();
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
            long coeffs = Mr23IndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(),
                secretKey,
                response.get(partitionIndex),
                offset,
                params.getFirstTwoDimensionSize()
            );
            byte[] bytes = PirUtils.convertCoeffsToBytes(new long[]{coeffs}, params.getPlainModulusBitLength());
            byte[] partitionBytes = new byte[partitionByteLength];
            System.arraycopy(bytes, 0, partitionBytes, 0, partitionByteLength);
            databases[partitionIndex] = ZlDatabase.create(partitionByteLength * Byte.SIZE, new byte[][]{partitionBytes});
        });
        return NaiveDatabase.createFromZl(elementByteLength * Byte.SIZE, databases).getBytesData(0);
    }

    /**
     * client generates key pair.
     *
     * @return public keys.
     */
    private List<byte[]> generateKeyPair() {
        List<byte[]> keyPair = Mr23IndexPirNativeUtils.keyGen(
            params.getEncryptionParams(), params.getFirstTwoDimensionSize()
        );
        assert (keyPair.size() == 4);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
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
