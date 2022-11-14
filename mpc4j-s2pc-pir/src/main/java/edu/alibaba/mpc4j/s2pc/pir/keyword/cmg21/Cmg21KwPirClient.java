package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipher;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.createCuckooHashBin;

/**
 * CMG21关键词索引PIR协议客户端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirClient<T> extends AbstractKwPirClient<T> {
    /**
     * 流密码
     */
    private final StreamCipher streamCipher;
    /**
     * 是否使用压缩编码
     */
    private final boolean compressEncode;
    /**
     * 关键词索引PIR方案参数
     */
    private Cmg21KwPirParams params;
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

    public Cmg21KwPirClient(Rpc clientRpc, Party serverParty, Cmg21KwPirConfig config) {
        super(Cmg21KwPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        compressEncode = config.getCompressEncode();
        streamCipher = StreamCipherFactory.createInstance(envType);
    }

    @Override
    public void init(KwPirParams kwPirParams, int labelByteLength) throws MpcAbortException {
        setInitInput(kwPirParams, labelByteLength);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        assert (kwPirParams instanceof Cmg21KwPirParams);
        params = (Cmg21KwPirParams) kwPirParams;
        // 客户端接收服务端哈希密钥
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(hashKeyPayload.size() == params.getCuckooHashKeyNum());
        hashKeys = hashKeyPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashKeyTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Map<T, ByteBuffer> pir(Set<T> retrievalSet) throws MpcAbortException {
        setPtoInput(retrievalSet);

        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
        // 客户端执行OPRF协议
        stopWatch.start();
        List<byte[]> blindPayload = generateBlindPayload();
        DataPacketHeader blindHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindHeader, blindPayload));
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPrfPayload = rpc.receive(blindPrfHeader).getPayload();
        ArrayList<ByteBuffer> keywordPrfs = handleBlindPrf(blindPrfPayload);
        Map<ByteBuffer, ByteBuffer> prfKeywordMap = IntStream.range(0, retrievalSize)
            .boxed()
            .collect(Collectors.toMap(keywordPrfs::get, i -> retrievalArrayList.get(i), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        // 客户端布谷鸟哈希分桶
        stopWatch.start();
        boolean succeed = generateCuckooHashBin(keywordPrfs, params.getBinNum(), hashKeys);
        DataPacketHeader cuckooHashResultHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_RESULT.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashResultPayload = new ArrayList<>();
        cuckooHashResultPayload.add(BigIntegerUtils.bigIntegerToByteArray(succeed ? BigInteger.ONE : BigInteger.ZERO));
        rpc.send(DataPacket.fromByteArrayList(cuckooHashResultHeader, cuckooHashResultPayload));
        MpcAbortPreconditions.checkArgument(succeed, "cuckoo hash failed.");
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashTime);

        stopWatch.start();
        // 客户端生成BFV算法密钥和参数
        List<byte[]> fheParams = Cmg21KwPirNativeUtils.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits()
        );
        DataPacketHeader fheParamsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_FHE_PARAMS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> fheParamsPayload = fheParams.subList(0, 3);
        rpc.send(DataPacket.fromByteArrayList(fheParamsHeader, fheParamsPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyGenTime);

        stopWatch.start();
        // 客户端加密查询信息
        List<long[][]> encodedQueryList = encodeQuery();
        Stream<long[][]> encodedQueryStream = encodedQueryList.stream();
        encodedQueryStream = parallel ? encodedQueryStream.parallel() : encodedQueryStream;
        List<byte[]> encryptedQueryList = encodedQueryStream
            .map(i -> Cmg21KwPirNativeUtils.generateQuery(fheParams.get(0), fheParams.get(2), fheParams.get(3), i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader clientQueryDataPacketHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryDataPacketHeader, encryptedQueryList));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), genQueryTime);

        stopWatch.start();
        int ciphertextNumber = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        DataPacketHeader keywordResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> keywordResponsePayload = rpc.receive(keywordResponseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(keywordResponsePayload.size() % ciphertextNumber == 0);
        DataPacketHeader labelResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> labelResponsePayload = rpc.receive(labelResponseHeader).getPayload();
        MpcAbortPreconditions.checkArgument(labelResponsePayload.size() % ciphertextNumber == 0);
        // 客户端解密服务端回复
        Stream<byte[]> keywordResponseStream = keywordResponsePayload.stream();
        keywordResponseStream = parallel ? keywordResponseStream.parallel() : keywordResponseStream;
        ArrayList<long[]> decryptedKeywordResponse = keywordResponseStream
            .map(i -> Cmg21KwPirNativeUtils.decodeReply(fheParams.get(0), fheParams.get(3), i))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<byte[]> labelResponseStream = labelResponsePayload.stream();
        labelResponseStream = parallel ? labelResponseStream.parallel() : labelResponseStream;
        ArrayList<long[]> decryptedLabelResponse = labelResponseStream
            .map(i -> Cmg21KwPirNativeUtils.decodeReply(fheParams.get(0), fheParams.get(3), i))
            .collect(Collectors.toCollection(ArrayList::new));
        Map<T, ByteBuffer> pirResult = recoverPirResult(decryptedKeywordResponse, decryptedLabelResponse, prfKeywordMap);
        stopWatch.stop();
        long decodeResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), decodeResponseTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return pirResult;
    }

    /**
     * 恢复关键词索引PIR结果。
     *
     * @param decryptedKeywordReply 解密后的服务端回复元素。
     * @param decryptedLabelReply   解密后的服务端回复标签。
     * @param oprfMap               OPRF映射。
     * @return 关键词索引PIR结果
     */
    private Map<T, ByteBuffer> recoverPirResult(ArrayList<long[]> decryptedKeywordReply,
                                                ArrayList<long[]> decryptedLabelReply,
                                                Map<ByteBuffer, ByteBuffer> oprfMap) {
        Map<T, ByteBuffer> resultMap = new HashMap<>(retrievalSize);
        int itemEncodedSlotSize = params.getItemEncodedSlotSize();
        int itemPerCiphertext = params.getPolyModulusDegree() / itemEncodedSlotSize;
        int ciphertextNum = params.getBinNum() / itemPerCiphertext;
        int itemPartitionNum = decryptedKeywordReply.size() / ciphertextNum;
        int labelPartitionNum = (int) Math.ceil(((double) (labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH)
            * Byte.SIZE) / ((LongUtils.ceilLog2(params.getPlainModulus()) - 1) * itemEncodedSlotSize));
        int shiftBits = (int) Math.ceil(((double) (labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * Byte.SIZE) /
            (itemEncodedSlotSize * labelPartitionNum));
        for (int i = 0; i < decryptedKeywordReply.size(); i++) {
            // 找到匹配元素的所在行
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < itemEncodedSlotSize * itemPerCiphertext; j++) {
                if (decryptedKeywordReply.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - itemEncodedSlotSize + 1; j++) {
                if (matchedItem.get(j) % itemEncodedSlotSize == 0) {
                    if (matchedItem.get(j + itemEncodedSlotSize - 1) - matchedItem.get(j) == itemEncodedSlotSize - 1) {
                        int hashBinIndex = matchedItem.get(j) / itemEncodedSlotSize + (i / itemPartitionNum)
                            * itemPerCiphertext;
                        BigInteger label = BigInteger.ZERO;
                        int index = 0;
                        for (int l = 0; l < labelPartitionNum; l++) {
                            for (int k = 0; k < itemEncodedSlotSize; k++) {
                                BigInteger temp = BigInteger.valueOf(decryptedLabelReply.get(i * labelPartitionNum + l)[matchedItem.get(j + k)])
                                    .shiftLeft(shiftBits * index);
                                label = label.add(temp);
                                index++;
                            }
                        }
                        byte[] oprf = cuckooHashBin.getHashBinEntry(hashBinIndex).getItem().array();
                        byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                        System.arraycopy(oprf, 0, keyBytes, 0, CommonConstants.BLOCK_BYTE_LENGTH);
                        byte[] ciphertextLabel = BigIntegerUtils.nonNegBigIntegerToByteArray(
                            label, labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH
                        );
                        byte[] plaintextLabel = streamCipher.ivDecrypt(keyBytes, ciphertextLabel);
                        resultMap.put(
                            byteArrayObjectMap.get(oprfMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem())),
                            ByteBuffer.wrap(plaintextLabel)
                        );
                        j = j + itemEncodedSlotSize - 1;
                    }
                }
            }
        }
        return resultMap;
    }

    /**
     * 生成布谷鸟哈希分桶。
     *
     * @param itemList 元素列表。
     * @param binNum   指定桶数量。
     * @param hashKeys 哈希算法密钥。
     * @return 布谷鸟哈希分桶是否成功。
     */
    private boolean generateCuckooHashBin(ArrayList<ByteBuffer> itemList, int binNum, byte[][] hashKeys) {
        // 初始化布谷鸟哈希
        cuckooHashBin = createCuckooHashBin(envType, params.getCuckooHashBinType(), retrievalSize, binNum, hashKeys);
        boolean success = false;
        // 将客户端消息插入到CuckooHash中
        cuckooHashBin.insertItems(itemList);
        if (cuckooHashBin.itemNumInStash() == 0) {
            success = true;
        }
        // 如果成功，则向布谷鸟哈希的空余位置插入空元素
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        return success;
    }

    /**
     * 返回查询关键词的编码。
     *
     * @return 查询关键词的编码。
     */
    public List<long[][]> encodeQuery() {
        int itemEncodedSlotSize = params.getItemEncodedSlotSize();
        int itemPerCiphertext = params.getPolyModulusDegree() / itemEncodedSlotSize;
        int ciphertextNum = params.getBinNum() / itemPerCiphertext;
        long[][] items = new long[ciphertextNum][params.getPolyModulusDegree()];
        for (int i = 0; i < ciphertextNum; i++) {
            for (int j = 0; j < itemPerCiphertext; j++) {
                long[] item = params.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * itemPerCiphertext + j), true, secureRandom
                );
                System.arraycopy(item, 0, items[i], j * itemEncodedSlotSize, itemEncodedSlotSize);
            }
            for (int j = itemPerCiphertext * itemEncodedSlotSize; j < params.getPolyModulusDegree(); j++) {
                items[i][j] = 0;
            }
        }
        IntStream ciphertextStream = IntStream.range(0, ciphertextNum);
        ciphertextStream = parallel ? ciphertextStream.parallel() : ciphertextStream;
        return ciphertextStream
            .mapToObj(i -> computePowers(items[i], params.getPlainModulus(), params.getQueryPowers()))
            .collect(Collectors.toList());
    }

    /**
     * 生成盲化元素。
     *
     * @return 盲化元素。
     */
    private List<byte[]> generateBlindPayload() {
        Ecc ecc = EccFactory.createInstance(envType);
        BigInteger n = ecc.getN();
        inverseBetas = new BigInteger[retrievalArrayList.size()];
        IntStream retrievalIntStream = IntStream.range(0, retrievalArrayList.size());
        retrievalIntStream = parallel ? retrievalIntStream.parallel() : retrievalIntStream;
        return retrievalIntStream
            .mapToObj(index -> {
                // 生成盲化因子
                BigInteger beta = BigIntegerUtils.randomPositive(n, secureRandom);
                inverseBetas[index] = beta.modInverse(n);
                // hash to point
                ECPoint element = ecc.hashToCurve(retrievalArrayList.get(index).array());
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
    private ArrayList<ByteBuffer> handleBlindPrf(List<byte[]> blindPrf) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindPrf.size() == retrievalArrayList.size());
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
     * 计算幂次方。
     *
     * @param base      底数。
     * @param modulus   模数。
     * @param exponents 指数。
     * @return 幂次方。
     */
    private long[][] computePowers(long[] base, long modulus, int[] exponents) {
        Zp64 zp64 = Zp64Factory.createInstance(envType, modulus);
        long[][] result = new long[exponents.length][];
        assert exponents[0] == 1;
        result[0] = base;
        for (int i = 1; i < exponents.length; i++) {
            long[] temp = new long[base.length];
            for (int j = 0; j < base.length; j++) {
                temp[j] = zp64.mulPow(base[j], exponents[i]);
            }
            result[i] = temp;
        }
        return result;
    }
}