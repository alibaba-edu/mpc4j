package edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiParams;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.createCuckooHashBin;
import static edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir.Lpzl24BatchIndexPirPtoDesc.*;

/**
 * PSI-PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzl24BatchIndexPirClient extends AbstractBatchIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * UPSI client
     */
    private final Cmg21UpsiClient<ByteBuffer> upsiClient;
    /**
     * cuckoo hash bin
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * β^{-1}
     */
    private BigInteger[] inverseBetas;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * index list
     */
    protected List<ByteBuffer> indicesByteBuffer;

    public Lpzl24BatchIndexPirClient(Rpc clientRpc, Party serverParty, Lpzl24BatchIndexPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        upsiClient = new Cmg21UpsiClient<>(clientRpc, serverParty, (Cmg21UpsiConfig) config.getUpsiConfig());
        addSubPtos(upsiClient);
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // UPSI params
        Cmg21UpsiParams params = null;
        if (maxRetrievalSize <= 256) {
            params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_256;
        } else if (maxRetrievalSize <= 512) {
            params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_CMP;
        } else if (maxRetrievalSize <= 1024) {
            params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_CMP;
        } else if (maxRetrievalSize <= 2048) {
            params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_CMP;
        } else if (maxRetrievalSize <= 4096) {
            params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_CMP;
        } else {
            MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
        }
        assert params != null;
        upsiClient.init(params);
        setInitInput(serverElementSize, elementBitLength, params.maxClientElementSize());
        partitionBitLength = 1;
        partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        List<byte[]> keyPair = Lpzl24BatchIndexPirNativeUtils.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits()
        );
        List<byte[]> publicKeysPayload = upsiClient.generateKeyPairPayload(keyPair);
        DataPacketHeader bfvParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(bfvParamsHeader, publicKeysPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getCuckooHashNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<Integer, byte[]> pir(List<Integer> indexList) throws MpcAbortException {
        setPtoInput(indexList);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // MP-OPRF
        stopWatch.start();
        indicesByteBuffer = IntStream.range(0, retrievalSize)
            .mapToObj(i -> ByteBuffer.wrap(IntUtils.intToByteArray(indexList.get(i))))
            .collect(Collectors.toCollection(ArrayList::new));
        List<byte[]> blindPayload = generateBlindPayload();
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindHeader, blindPayload));
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
        List<ByteBuffer> blindPrf = handleBlindPrf(blindPrfPayload);
        Map<ByteBuffer, ByteBuffer> blindPrfMap = IntStream.range(0, retrievalSize)
            .boxed()
            .collect(Collectors.toMap(blindPrf::get, indicesByteBuffer::get, (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, oprfTime, "Client runs OPRF");

        stopWatch.start();
        // generate cuckoo hash bin
        generateCuckooHashBin(blindPrf);
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, cuckooHashKeyTime, "Client generates cuckoo hash bin");

        stopWatch.start();
        // generate query
        List<long[][]> encodedQuery = upsiClient.encodeQuery(cuckooHashBin);
        Stream<long[][]> encodeStream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        List<byte[]> queryPayload = encodeStream
            .map(i -> Lpzl24BatchIndexPirNativeUtils.generateQuery(
                upsiClient.encryptionParams, upsiClient.publicKey, upsiClient.secretKey, i
                ))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader clientQueryDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryDataPacketHeader, queryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, genQueryTime, "Client generates query");

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        stopWatch.start();
        // decode reply
        Map<Integer, byte[]> pirResult = handleServerResponse(responsePayload, blindPrfMap);
        stopWatch.stop();
        long decodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, decodeTime, "Client decodes response");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * handle server response.
     *
     * @param serverResponse server response.
     * @param oprfMap        OPRF map.
     * @return retrieval result map.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private Map<Integer, byte[]> handleServerResponse(List<byte[]> serverResponse, Map<ByteBuffer, ByteBuffer> oprfMap)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(
            serverResponse.size() % (upsiClient.params.getCiphertextNum() * partitionSize) == 0
        );
        int size = serverResponse.size() / partitionSize;
        Stream<byte[]> responseStream = parallel ? serverResponse.stream().parallel() : serverResponse.stream();
        List<long[]> coeffs = responseStream
            .map(i -> Lpzl24BatchIndexPirNativeUtils.decodeReply(upsiClient.encryptionParams, upsiClient.secretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        int byteLength = CommonUtils.getByteLength(elementBitLength);
        byte[][] pirResult = new byte[retrievalSize][byteLength];
        for (int i = 0; i < partitionSize; i++) {
            Set<ByteBuffer> intersectionSet = upsiClient.recoverPsiResult(
                coeffs.subList(i*size, (i+1)*size), oprfMap, cuckooHashBin
            );
            for (int j = 0; j < retrievalSize; j++) {
                boolean temp = intersectionSet.contains(indicesByteBuffer.get(j));
                BinaryUtils.setBoolean(pirResult[j], byteLength * Byte.SIZE - 1 - i, temp);
            }
        }
        return IntStream.range(0, retrievalSize)
            .boxed()
            .collect(Collectors.toMap(
                i -> indicesByteBuffer.get(i).getInt(), i -> pirResult[i], (a, b) -> b,
                () -> new HashMap<>(retrievalSize)
                ));
    }

    /**
     * generate blind element list.
     *
     * @return blind element list.
     */
    private List<byte[]> generateBlindPayload() {
        Ecc ecc = EccFactory.createInstance(envType);
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[retrievalSize];
        IntStream retrievalIntStream = IntStream.range(0, retrievalSize);
        retrievalIntStream = parallel ? retrievalIntStream.parallel() : retrievalIntStream;
        return retrievalIntStream
            .mapToObj(index -> {
                BigInteger beta = BigIntegerUtils.randomPositive(n, secureRandom);
                inverseBetas[index] = beta.modInverse(n);
                // hash to point
                ECPoint element = ecc.hashToCurve(indicesByteBuffer.get(index).array());
                return ecc.multiply(element, beta);
            })
            .map(element -> ecc.encode(element, true))
            .collect(Collectors.toList());
    }

    /**
     * handle blind prf element.
     *
     * @param blindPrf blind prf element.
     * @return prf element.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<ByteBuffer> handleBlindPrf(List<byte[]> blindPrf) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPrf.size() == retrievalSize);
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        byte[][] blindPrfArray = blindPrf.toArray(new byte[0][]);
        Ecc ecc = EccFactory.createInstance(envType);
        IntStream batchIntStream = IntStream.range(0, retrievalSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(index -> {
                ECPoint element = ecc.decode(blindPrfArray[index]);
                return ecc.multiply(element, inverseBetas[index]);
            })
            .map(element -> ecc.encode(element, false))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * client generates no stash cuckoo hash bin.
     *
     * @param itemList item list.
     */
    private void generateCuckooHashBin(List<ByteBuffer> itemList) throws MpcAbortException {
        cuckooHashBin = createCuckooHashBin(
            envType, upsiClient.params.getCuckooHashBinType(), retrievalSize, upsiClient.params.getBinNum(), hashKeys
        );
        boolean success = false;
        cuckooHashBin.insertItems(itemList);
        if (cuckooHashBin.itemNumInStash() == 0) {
            success = true;
        }
        MpcAbortPreconditions.checkArgument(success, "cuckoo hash failed.");
        byte[] randomBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        cuckooHashBin.insertPaddingItems(ByteBuffer.wrap(randomBytes));
    }
}