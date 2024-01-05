package edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirPtoDesc.*;

/**
 * OnionPIR client.
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class Mcr21SingleIndexPirClient extends AbstractSingleIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * OnionPIR params
     */
    private Mcr21SingleIndexPirParams params;
    /**
     * element size per BFV plaintext
     */
    private int elementSizeOfPlaintext;
    /**
     * dimension size
     */
    private int[] dimensionSize;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * private key
     */
    private byte[] secretKey;

    public Mcr21SingleIndexPirClient(Rpc clientRpc, Party serverParty, Mcr21SingleIndexPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, int serverElementSize, int elementBitLength) {
        setInitInput(serverElementSize, elementBitLength);
        assert (indexPirParams instanceof Mcr21SingleIndexPirParams);
        params = (Mcr21SingleIndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> publicKeysPayload = clientSetup(serverElementSize, elementBitLength);
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

        // receive response
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();
        stopWatch.start();
        byte[] result = decodeResponse(serverResponsePayload, index);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Server handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    @Override
    public List<byte[]> clientSetup(int serverElementSize, int elementBitLength) {
        int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
        partitionBitLength = Math.min(maxPartitionBitLength, elementBitLength);
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
            partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
        );
        int plaintextSize = CommonUtils.getUnitNum(serverElementSize, elementSizeOfPlaintext);
        dimensionSize = PirUtils.computeDimensionLength(
            plaintextSize, params.getFirstDimensionSize(), params.SUBSEQUENT_DIMENSION_SIZE
        );
        return generateKeyPair();
    }

    @Override
    public List<byte[]> generateQuery(int index) {
        int indexOfPlaintext = index / elementSizeOfPlaintext;
        // compute indices for each dimension
        int[] indices = PirUtils.computeIndices(indexOfPlaintext, dimensionSize);
        return Mcr21SingleIndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, indices, dimensionSize
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
            long[] coeffs = Mcr21SingleIndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(), secretKey, response.get(partitionIndex)
            );
            byte[] bytes = PirUtils.convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
            int offset = index % elementSizeOfPlaintext;
            byte[] partitionBytes = new byte[partitionByteLength];
            System.arraycopy(bytes, offset * partitionByteLength, partitionBytes, 0, partitionByteLength);
            databases[partitionIndex] = ZlDatabase.create(partitionBitLength, new byte[][]{partitionBytes});
        });
        return NaiveDatabase.createFromZl(elementBitLength, databases).getBytesData(0);
    }

    @Override
    public void setDefaultParams() {
        params = Mcr21SingleIndexPirParams.DEFAULT_PARAMS;
    }

    /**
     * client generates key pair.
     *
     * @return public keys.
     */
    private List<byte[]> generateKeyPair() {
        List<byte[]> keyPair = Mcr21SingleIndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 3);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        // add Galois keys and public key
        List<byte[]> publicKeys = new ArrayList<>();
        publicKeys.add(publicKey);
        publicKeys.add(keyPair.remove(0));
        publicKeys.addAll(
            Mcr21SingleIndexPirNativeUtils.encryptSecretKey(params.getEncryptionParams(), publicKey, secretKey)
        );
        return publicKeys;
    }
}