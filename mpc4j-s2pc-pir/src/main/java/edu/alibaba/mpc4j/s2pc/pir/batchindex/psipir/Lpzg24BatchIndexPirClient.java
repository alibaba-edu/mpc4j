package edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir;

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
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.AbstractBatchIndexPirClient;
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
import static edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir.Lpzg24BatchIndexPirPtoDesc.*;

/**
 * PSI-PIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzg24BatchIndexPirClient extends AbstractBatchIndexPirClient {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 是否使用压缩编码
     */
    private final boolean compressEncode;
    /**
     * 非平衡PSI方案参数
     */
    private Cmg21UpsiParams params;
    /**
     * 无贮存区布谷鸟哈希分桶
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * β^{-1}
     */
    private BigInteger[] inverseBetas;
    /**
     * 哈希算法密钥
     */
    private byte[][] hashKeys;
    /**
     * SEAL上下文参数
     */
    private byte[] sealContext;
    /**
     * 公钥
     */
    private byte[] publicKey;
    /**
     * 私钥
     */
    private byte[] secretKey;
    /**
     * 模p有限域
     */
    private Zp64 zp64;

    public Lpzg24BatchIndexPirClient(Rpc clientRpc, Party serverParty, Lpzg24BatchIndexPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        if (serverElementSize <= (1 << 22)) {
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
        } else if (serverElementSize <= (1 << 26)) {
            if (maxRetrievalSize <= 1024) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_2048;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        } else if (serverElementSize <= (1 << 28)) {
            if (maxRetrievalSize <= 1024) {
                params = Cmg21UpsiParams.SERVER_256M_CLIENT_MAX_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Cmg21UpsiParams.SERVER_256M_CLIENT_MAX_2048;
            } else if (maxRetrievalSize <= 4096) {
                params = Cmg21UpsiParams.SERVER_256M_CLIENT_MAX_4096;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        }
        zp64 = Zp64Factory.createInstance(envType, (long) params.getPlainModulus());
        setInitInput(serverElementSize, elementBitLength, params.maxClientElementSize(), 1);

        stopWatch.start();
        // 客户端生成BFV算法密钥和参数
        List<byte[]> bfvKeyPair = generateKeyPair();
        DataPacketHeader bfvParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(bfvParamsHeader, bfvKeyPair));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        // 客户端接收服务端哈希密钥
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getCuckooHashKeyNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<Integer, byte[]> pir(List<Integer> indexList) throws MpcAbortException {
        setPtoInput(indexList);
        logPhaseInfo(PtoState.PTO_BEGIN);
        System.out.println(params);

        // 执行OPRF协议
        stopWatch.start();
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
        // 客户端布谷鸟哈希分桶
        boolean succeed = generateCuckooHashBin(blindPrf);
        MpcAbortPreconditions.checkArgument(succeed, "cuckoo hash failed.");
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, cuckooHashKeyTime, "Client generates cuckoo hash bin");

        stopWatch.start();
        // 客户端加密查询信息
        List<long[][]> query = encodeQuery();
        Stream<long[][]> queryStream = query.stream();
        queryStream = parallel ? queryStream.parallel() : queryStream;
        List<byte[]> encryptedQuery = queryStream
            .map(i -> Lpzg24BatchIndexPirNativeUtils.generateQuery(sealContext, publicKey, secretKey, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader clientQueryDataPacketHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryDataPacketHeader, encryptedQuery));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, genQueryTime, "Client generates query");

        // 客户端接收回复
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        stopWatch.start();
        // 客户端解密密文匹配结果
        Map<Integer, byte[]> pirResult = handleServerResponse(responsePayload, blindPrfMap);
        stopWatch.stop();
        long decodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, decodeTime, "Client decodes response");

        logPhaseInfo(PtoState.PTO_END);
        return pirResult;
    }

    /**
     * 客户端处理服务端回复信息。
     *
     * @param serverResponse 服务端回复信息。
     * @param oprfMap        OPRF映射。
     * @return 检索结果。
     * @throws MpcAbortException 如果协议异常终止。
     */
    private Map<Integer, byte[]> handleServerResponse(List<byte[]> serverResponse, Map<ByteBuffer, ByteBuffer> oprfMap)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverResponse.size() % (params.getCiphertextNum() * partitionSize) == 0);
        int partitionSize = serverResponse.size() / this.partitionSize;
        Stream<byte[]> responseStream = parallel ? serverResponse.stream().parallel() : serverResponse.stream();
        List<long[]> coeffs = responseStream
            .map(i -> Lpzg24BatchIndexPirNativeUtils.decodeReply(sealContext, secretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        int byteLength = CommonUtils.getByteLength(elementBitLength);
        byte[][] pirResult = new byte[retrievalSize][byteLength];
        for (int i = 0; i < this.partitionSize; i++) {
            Set<ByteBuffer> intersectionSet = recoverIntersection(
                coeffs.subList(i * partitionSize, (i + 1) * partitionSize), oprfMap
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
     * 恢复隐私集合交集。
     *
     * @param response 解密后的服务端回复。
     * @param oprfMap  OPRF映射。
     * @return 隐私集合交集。
     */
    private Set<ByteBuffer> recoverIntersection(List<long[]> response, Map<ByteBuffer, ByteBuffer> oprfMap) {
        int binSize = response.size() / params.getCiphertextNum();
        Set<ByteBuffer> intersectionSet = new HashSet<>();
        for (int i = 0; i < response.size(); i++) {
            // 找到匹配元素的所在行
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < params.getItemEncodedSlotSize() * params.getItemPerCiphertext(); j++) {
                if (response.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - params.getItemEncodedSlotSize() + 1; j++) {
                if (matchedItem.get(j) % params.getItemEncodedSlotSize() == 0) {
                    if (matchedItem.get(j + params.getItemEncodedSlotSize() - 1) - matchedItem.get(j)
                        == params.getItemEncodedSlotSize() - 1) {
                        int hashBinIndex = (matchedItem.get(j) / params.getItemEncodedSlotSize())
                            + (i / binSize) * params.getItemPerCiphertext();
                        intersectionSet.add(oprfMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem()));
                        j = j + params.getItemEncodedSlotSize() - 1;
                    }
                }
            }
        }
        return intersectionSet;
    }

    /**
     * 返回查询信息的编码。
     *
     * @return 查询信息的编码。
     */
    public List<long[][]> encodeQuery() {
        long[][] items = new long[params.getCiphertextNum()][params.getPolyModulusDegree()];
        for (int i = 0; i < params.getCiphertextNum(); i++) {
            for (int j = 0; j < params.getItemPerCiphertext(); j++) {
                long[] item = params.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * params.getItemPerCiphertext() + j), true
                );
                System.arraycopy(
                    item, 0, items[i], j * params.getItemEncodedSlotSize(), params.getItemEncodedSlotSize()
                );
            }
            for (int j = params.getItemPerCiphertext() * params.getItemEncodedSlotSize();
                 j < params.getPolyModulusDegree(); j++) {
                items[i][j] = 0;
            }
        }
        return IntStream.range(0, params.getCiphertextNum())
            .mapToObj(i -> computePowers(items[i]))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 计算幂次方。
     *
     * @param base 底数。
     * @return 幂次方。
     */
    private long[][] computePowers(long[] base) {
        int[] exponents = params.getQueryPowers();
        assert exponents[0] == 1;
        long[][] result = new long[exponents.length][base.length];
        result[0] = base;
        IntStream intStream = IntStream.range(1, exponents.length);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i ->
            IntStream.range(0, base.length).forEach(j -> result[i][j] = zp64.pow(base[j], exponents[i]))
        );
        return result;
    }

    /**
     * 返回密钥对。
     *
     * @return 密钥对。
     */
    private List<byte[]> generateKeyPair() {
        List<byte[]> fheParams = Lpzg24BatchIndexPirNativeUtils.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits()
        );
        assert (fheParams.size() == 4);
        this.sealContext = fheParams.get(0);
        this.publicKey = fheParams.get(2);
        this.secretKey = fheParams.get(3);
        return fheParams.subList(0, 2);
    }

    /**
     * 生成盲化元素。
     *
     * @return 盲化元素。
     */
    private List<byte[]> generateBlindPayload() {
        Ecc ecc = EccFactory.createInstance(envType);
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[retrievalSize];
        IntStream retrievalIntStream = IntStream.range(0, retrievalSize);
        retrievalIntStream = parallel ? retrievalIntStream.parallel() : retrievalIntStream;
        return retrievalIntStream
            .mapToObj(index -> {
                // 生成盲化因子
                BigInteger beta = BigIntegerUtils.randomPositive(n, secureRandom);
                inverseBetas[index] = beta.modInverse(n);
                // hash to point
                ECPoint element = ecc.hashToCurve(indicesByteBuffer.get(index).array());
                // 盲化
                return ecc.multiply(element, beta);
            })
            .map(element -> ecc.encode(element, compressEncode))
            .collect(Collectors.toList());
    }

    /**
     * 处理盲化元素PRF。
     *
     * @param blindPrf 盲化元素PRF。
     * @return 元素PRF。
     * @throws MpcAbortException 如果协议异常中止。
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
                // 解码
                ECPoint element = ecc.decode(blindPrfArray[index]);
                // 去盲化
                return ecc.multiply(element, inverseBetas[index]);
            })
            .map(element -> ecc.encode(element, false))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 生成布谷鸟哈希分桶。
     *
     * @param itemList 元素列表。
     * @return 布谷鸟哈希分桶是否成功。
     */
    private boolean generateCuckooHashBin(List<ByteBuffer> itemList) {
        // 初始化布谷鸟哈希
        cuckooHashBin = createCuckooHashBin(
            envType, params.getCuckooHashBinType(), retrievalSize, params.getBinNum(), hashKeys
        );
        boolean success = false;
        // 将客户端消息插入到CuckooHash中
        cuckooHashBin.insertItems(itemList);
        if (cuckooHashBin.itemNumInStash() == 0) {
            success = true;
        }
        // 如果成功，则向布谷鸟哈希的空余位置插入空元素
        byte[] randomBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        cuckooHashBin.insertPaddingItems(ByteBuffer.wrap(randomBytes));
        return success;
    }
}