package edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerNode;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerUtils;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiParams;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir.Lpzg24BatchIndexPirPtoDesc.*;

/**
 * PSI-PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzg24BatchIndexPirServer extends AbstractBatchIndexPirServer {

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
     * 哈希算法密钥
     */
    private byte[][] hashKeys;
    /**
     * SEAL上下文参数
     */
    private byte[] sealContext;
    /**
     * 重线性化密钥
     */
    private byte[] relinKeys;
    /**
     * PRF密钥
     */
    private BigInteger alpha;
    /**
     * 序列化的多项式
     */
    private List<List<byte[]>> dbPlaintexts;
    /**
     * 最大分桶内元素数目
     */
    private int[] maxBinSize;

    public Lpzg24BatchIndexPirServer(Rpc serverRpc, Party clientParty, Lpzg24BatchIndexPirConfig config) {
        super(Lpzg24BatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        compressEncode = config.getCompressEncode();
    }

    @Override
    public void init(NaiveDatabase database, int maxRetrievalSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        if (database.rows() <= (1 << 22)) {
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
        } else if (database.rows() <= (1 << 26)) {
            if (maxRetrievalSize <= 1024) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_2048;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        }

        setInitInput(database, maxRetrievalSize, 1);
        // 接收公钥
        DataPacketHeader bfvParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> bfvKeyPair = rpc.receive(bfvParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(bfvKeyPair.size() == 2);
        sealContext = bfvKeyPair.remove(0);
        relinKeys = bfvKeyPair.remove(0);

        stopWatch.start();
        // 服务端生成并发送哈希密钥
        hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        alpha = BigIntegerUtils.randomPositive(EccFactory.createInstance(envType).getN(), secureRandom);
        maxBinSize = new int[partitionSize];
        dbPlaintexts = IntStream.range(0, partitionSize)
            .mapToObj(i -> {
                // 计算PRF
                List<ByteBuffer> elementPrf = computeElementPrf(i);
                // 服务端哈希分桶
                List<List<HashBinEntry<ByteBuffer>>> hashBins = generateCompleteHashBin(elementPrf, i);
                // 计算多项式系数
                List<long[][]> coeffs = encodeDatabase(hashBins);
                IntStream intStream = IntStream.range(0, coeffs.size());
                intStream = parallel ? intStream.parallel() : intStream;
                return intStream
                    .mapToObj(j -> Lpzg24BatchIndexPirNativeUtils.processDatabase(
                        sealContext, coeffs.get(j), params.getPsLowDegree()
                    ))
                    .collect(Collectors.toCollection(ArrayList::new));
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, encodeTime, "Server encodes items");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);
        System.out.println(params);

        stopWatch.start();
        // 服务端执行OPRF协议
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();
        List<byte[]> blindPrfPayload = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrfPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Server runs OPRFs");

        // 接收客户端的加密查询信息
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == params.getCiphertextNum() * params.getQueryPowers().length,
            "The size of query is incorrect"
        );

        stopWatch.start();
        // 密文多项式运算
        MathPreconditions.checkNonNegative("ps_low_degree", params.getPsLowDegree());
        int[][] powerDegree = computePowerDegree();
        List<byte[]> ciphertextPowers = computeQueryPowers(queryPayload, powerDegree);
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> response = intStream
            .mapToObj(i -> computeResponse(ciphertextPowers, powerDegree, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader keywordResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keywordResponseHeader, response));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * 返回编码后的数据库。
     *
     * @param hashBins 哈希分桶。
     * @return 编码后的数据库（明文多项式的系数）。
     */
    private List<long[][]> encodeDatabase(List<List<HashBinEntry<ByteBuffer>>> hashBins) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, (long) params.getPlainModulus());
        // we will split the hash table into partitions
        int binSize = hashBins.get(0).size();
        int partitionNum = CommonUtils.getUnitNum(binSize, params.getMaxPartitionSizePerBin());
        int bigPartitionIndex = binSize / params.getMaxPartitionSizePerBin();
        long[][] coeffs = new long[params.getItemEncodedSlotSize() * params.getItemPerCiphertext()][];
        List<long[][]> coeffsPolys = new ArrayList<>();
        long[][] encodedItemArray = new long[params.getBinNum() * params.getItemEncodedSlotSize()][binSize];
        for (int i = 0; i < params.getBinNum(); i++) {
            IntStream intStream = parallel ? IntStream.range(0, binSize).parallel() : IntStream.range(0, binSize);
            int finalI = i;
            intStream.forEach(j -> {
                long[] item = params.getHashBinEntryEncodedArray(hashBins.get(finalI).get(j), false);
                for (int l = 0; l < params.getItemEncodedSlotSize(); l++) {
                    encodedItemArray[finalI * params.getItemEncodedSlotSize() + l][j] = item[l];
                }
            });
        }
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        for (int i = 0; i < params.getCiphertextNum(); i++) {
            for (int partition = 0; partition < partitionNum; partition++) {
                int partitionCount, partitionStart;
                partitionCount = partition < bigPartitionIndex ?
                    params.getMaxPartitionSizePerBin() : binSize % params.getMaxPartitionSizePerBin();
                partitionStart = params.getMaxPartitionSizePerBin() * partition;
                IntStream intStream = IntStream.range(0, params.getItemPerCiphertext() * params.getItemEncodedSlotSize());
                intStream = parallel ? intStream.parallel() : intStream;
                int finalI = i;
                intStream.forEach(j -> {
                    long[] tempVector = new long[partitionCount];
                    System.arraycopy(
                        encodedItemArray[finalI * params.getItemPerCiphertext() * params.getItemEncodedSlotSize() + j],
                        partitionStart,
                        tempVector,
                        0,
                        partitionCount
                    );
                    coeffs[j] = zp64Poly.rootInterpolate(partitionCount, tempVector, 0L);
                });
                // 转换为列编码
                long[][] temp = new long[partitionCount + 1][params.getPolyModulusDegree()];
                for (int j = 0; j < partitionCount + 1; j++) {
                    for (int l = 0; l < params.getItemPerCiphertext() * params.getItemEncodedSlotSize(); l++) {
                        temp[j][l] = coeffs[l][j];
                    }
                    for (int l = params.getItemPerCiphertext() * params.getItemEncodedSlotSize(); l < params.getPolyModulusDegree(); l++) {
                        temp[j][l] = 0;
                    }
                }
                coeffsPolys.add(temp);
            }
        }
        return coeffsPolys;
    }

    /**
     * 服务端计算元素PRF。
     *
     * @return 元素PRF。
     */
    private List<ByteBuffer> computeElementPrf(int partitionIndex) {
        Ecc ecc = EccFactory.createInstance(envType);
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        IntStream intStream = IntStream.range(0, databases[partitionIndex].rows());
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> ecc.hashToCurve(databases[partitionIndex].getBytesData(i)))
            .map(hash -> ecc.multiply(hash, alpha))
            .map(prf -> ecc.encode(prf, false))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 生成完全哈希分桶。
     *
     * @param elementList    元素集合。
     * @param partitionIndex 分块索引。
     * @return 完全哈希分桶。
     */
    private List<List<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(List<ByteBuffer> elementList,
                                                                         int partitionIndex) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(
            envType, params.getBinNum(), num, hashKeys
        );
        completeHash.insertItems(elementList);
        maxBinSize[partitionIndex] = completeHash.binSize(0);
        for (int i = 1; i < completeHash.binNum(); i++) {
            if (completeHash.binSize(i) > maxBinSize[partitionIndex]) {
                maxBinSize[partitionIndex] = completeHash.binSize(i);
            }
        }
        List<List<HashBinEntry<ByteBuffer>>> completeHashBins = new ArrayList<>();
        byte[] randomBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(ByteBuffer.wrap(randomBytes));
        for (int i = 0; i < completeHash.binNum(); i++) {
            List<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize[partitionIndex] - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            completeHashBins.add(binItems);
        }
        return completeHashBins;
    }

    /**
     * 处理盲化元素。
     *
     * @param blindElements 盲化元素。
     * @return 盲化元素PRF。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private List<byte[]> handleBlindPayload(List<byte[]> blindElements) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindElements.size() > 0);
        Ecc ecc = EccFactory.createInstance(envType);
        Stream<byte[]> blindStream = blindElements.stream();
        blindStream = parallel ? blindStream.parallel() : blindStream;
        return blindStream
            // 解码H(m_c)^β
            .map(ecc::decode)
            // 计算H(m_c)^βα
            .map(element -> ecc.multiply(element, alpha))
            // 编码
            .map(element -> ecc.encode(element, compressEncode))
            .collect(Collectors.toList());
    }

    /**
     * 计算幂次方指数。
     *
     * @return 幂次方指数。
     */
    private int[][] computePowerDegree() {
        int[][] powerDegree;
        if (params.getPsLowDegree() > 0) {
            Set<Integer> innerPowersSet = new HashSet<>();
            Set<Integer> outerPowersSet = new HashSet<>();
            IntStream.range(0, params.getQueryPowers().length).forEach(i -> {
                if (params.getQueryPowers()[i] <= params.getPsLowDegree()) {
                    innerPowersSet.add(params.getQueryPowers()[i]);
                } else {
                    outerPowersSet.add(params.getQueryPowers()[i] / (params.getPsLowDegree() + 1));
                }
            });
            PowerNode[] innerPowerNodes = PowerUtils.computePowers(innerPowersSet, params.getPsLowDegree());
            PowerNode[] outerPowerNodes = PowerUtils.computePowers(
                outerPowersSet, params.getMaxPartitionSizePerBin() / (params.getPsLowDegree() + 1)
            );
            powerDegree = new int[innerPowerNodes.length + outerPowerNodes.length][2];
            int[][] innerPowerNodesDegree = Arrays.stream(innerPowerNodes)
                .map(PowerNode::toIntArray)
                .toArray(int[][]::new);
            int[][] outerPowerNodesDegree = Arrays.stream(outerPowerNodes)
                .map(PowerNode::toIntArray)
                .toArray(int[][]::new);
            System.arraycopy(innerPowerNodesDegree, 0, powerDegree, 0, innerPowerNodesDegree.length);
            System.arraycopy(
                outerPowerNodesDegree, 0, powerDegree, innerPowerNodesDegree.length, outerPowerNodesDegree.length
            );
        } else {
            Set<Integer> sourcePowersSet = Arrays.stream(params.getQueryPowers())
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
            PowerNode[] powerNodes = PowerUtils.computePowers(sourcePowersSet, params.getMaxPartitionSizePerBin());
            powerDegree = Arrays.stream(powerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
        }
        return powerDegree;
    }

    /**
     * 服务端计算密文的次方。
     *
     * @param query 密文多项式。
     * @return 密文的次方。
     */
    private List<byte[]> computeQueryPowers(List<byte[]> query, int[][] powerDegree) {
        // 计算所有的密文次方
        IntStream intStream = IntStream.range(0, params.getCiphertextNum());
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Lpzg24BatchIndexPirNativeUtils.computeEncryptedPowers(
                sealContext,
                relinKeys,
                query.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree())
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 服务端计算密文匹配结果。
     *
     * @param clientQuery 客户端的查询信息。
     * @return 密文匹配结果。
     */
    private List<byte[]> computeResponse(List<byte[]> clientQuery, int[][] powerDegree, int partitionIndex) {
        int binSize = CommonUtils.getUnitNum(maxBinSize[partitionIndex], params.getMaxPartitionSizePerBin());
        int partitionCount = dbPlaintexts.size() / partitionSize;
        IntStream intStream = IntStream.range(0, params.getCiphertextNum());
        if (params.getPsLowDegree() > 0) {
            return intStream
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, binSize).parallel() : IntStream.range(0, binSize))
                        .mapToObj(j -> Lpzg24BatchIndexPirNativeUtils.optComputeMatches(
                            sealContext,
                            relinKeys,
                            dbPlaintexts.get(i * binSize + j + partitionIndex * partitionCount),
                            clientQuery.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            params.getPsLowDegree()
                            ))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            return intStream
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, binSize).parallel() : IntStream.range(0, binSize))
                        .mapToObj(j -> Lpzg24BatchIndexPirNativeUtils.naiveComputeMatches(
                            sealContext,
                            dbPlaintexts.get(i * binSize + j + partitionIndex * partitionCount),
                            clientQuery.subList(i * powerDegree.length, (i + 1) * powerDegree.length)
                            ))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        }
    }

    @Override
    protected void setInitInput(NaiveDatabase database, int maxRetrievalSize, int partitionBitLength) {
        MathPreconditions.checkPositive("serverElementSize", database.rows());
        num = database.rows();
        MathPreconditions.checkPositive("maxRetrievalSize", maxRetrievalSize);
        this.maxRetrievalSize = maxRetrievalSize;
        MathPreconditions.checkPositive("elementBitLength", database.getL());
        this.elementBitLength = database.getL();
        MathPreconditions.checkEqual("partitionBitLength", "1", partitionBitLength, 1);
        this.partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        databases = new ZlDatabase[partitionSize];
        int byteLength = CommonUtils.getByteLength(elementBitLength);
        for (int i = 0; i < partitionSize; i++) {
            List<byte[]> temp = new ArrayList<>();
            for (int j = 0; j < num; j++) {
                boolean value = BinaryUtils.getBoolean(database.getBytesData(j), byteLength * Byte.SIZE - 1 - i);
                if (value) {
                    temp.add(IntUtils.intToByteArray(j));
                }
            }
            databases[i] = ZlDatabase.create(Integer.BYTES * Byte.SIZE, temp.toArray(new byte[0][]));
        }
        initState();
    }
}
