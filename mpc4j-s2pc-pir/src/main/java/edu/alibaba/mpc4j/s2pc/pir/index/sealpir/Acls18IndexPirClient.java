package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

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
import edu.alibaba.mpc4j.s2pc.pir.index.sealpir.Acls18IndexPirPtoDesc.PtoStep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * SEAL PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Acls18IndexPirClient extends AbstractIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * SEAL PIR params
     */
    private Acls18IndexPirParams params;
    /**
     * element size per BFV plaintext
     */
    private int elementSizeOfPlaintext;
    /**
     * dimension size
     */
    private int[] dimensionSize;
    /**
     * partition byte length
     */
    private int partitionByteLength;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * private key
     */
    private byte[] secretKey;
    /**
     * partition size
     */
    private int partitionSize;

    public Acls18IndexPirClient(Rpc clientRpc, Party serverParty, Acls18IndexPirConfig config) {
        super(Acls18IndexPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(IndexPirParams indexPirParams, int serverElementSize, int elementByteLength) {
        assert (indexPirParams instanceof Acls18IndexPirParams);
        params = (Acls18IndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        setInitInput(serverElementSize, elementByteLength);
        List<byte[]> publicKeysPayload = clientSetup();
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
        params = Acls18IndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        setInitInput(serverElementSize, elementByteLength);
        List<byte[]> publicKeysPayload = clientSetup();
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

        // receive response
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
        int maxPartitionByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        partitionByteLength = Math.min(maxPartitionByteLength, elementByteLength);
        partitionSize = CommonUtils.getUnitNum(elementByteLength, partitionByteLength);
        elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
            partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
        );
        int plaintextSize = (int) Math.ceil((double) this.num / elementSizeOfPlaintext);
        dimensionSize = PirUtils.computeDimensionLength(plaintextSize, params.getDimension());
        return generateKeyPair();
    }

    /**
     * client generate query.
     *
     * @return client query.
     */
    public List<byte[]> generateQuery() {
        int indexOfPlaintext = index / elementSizeOfPlaintext;
        // compute indices for each dimension
        int[] indices = PirUtils.computeIndices(indexOfPlaintext, dimensionSize);
        return Acls18IndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(), publicKey, secretKey, indices, dimensionSize
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
        int partitionResponseSize = IntStream.range(0, params.getDimension() - 1)
            .map(i -> params.getExpansionRatio())
            .reduce(1, (a, b) -> a * b);
        MpcAbortPreconditions.checkArgument(response.size() == partitionResponseSize * partitionSize);
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(partitionIndex -> {
            long[] coeffs = Acls18IndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(),
                secretKey,
                response.subList(partitionIndex * partitionResponseSize, (partitionIndex + 1) * partitionResponseSize),
                params.getDimension()
            );
            byte[] bytes = PirUtils.convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
            int offset = index % elementSizeOfPlaintext;
            byte[] partitionBytes = new byte[partitionByteLength];
            System.arraycopy(bytes, offset * partitionByteLength, partitionBytes, 0, partitionByteLength);
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
        List<byte[]> keyPair = Acls18IndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 3);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        // add Galois keys
        List<byte[]> publicKeys = new ArrayList<>();
        publicKeys.add(keyPair.remove(0));
        return publicKeys;
    }
}
