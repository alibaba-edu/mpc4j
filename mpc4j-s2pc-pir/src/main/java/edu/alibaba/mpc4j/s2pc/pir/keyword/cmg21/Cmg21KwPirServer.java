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
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerNode;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerUtils;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.AbstractKwPirServer;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CMG21关键词索引PIR协议服务端。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirServer<T> extends AbstractKwPirServer<T> {
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
     * 哈希分桶
     */
    private ArrayList<ArrayList<HashBinEntry<ByteBuffer>>> hashBins;
    /**
     * 服务端元素编码
     */
    private long[][][] serverKeywordEncode;
    /**
     * 服务端标签编码
     */
    private long[][][] serverLabelEncode;
    /**
     * PRF密钥
     */
    private BigInteger alpha;
    /**
     * 关键词回复
     */
    private ArrayList<byte[]> keywordResponsePayload;
    /**
     * 标签回复
     */
    private ArrayList<byte[]> labelResponsePayload;
    /**
     * 哈希算法密钥
     */
    private byte[][] hashKeys;

    public Cmg21KwPirServer(Rpc serverRpc, Party clientParty, Cmg21KwPirConfig config) {
        super(Cmg21KwPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        compressEncode = config.getCompressEncode();
        streamCipher = StreamCipherFactory.createInstance(envType);
    }

    @Override
    public void init(KwPirParams kwPirParams, Map<T, ByteBuffer> serverKeywordLabelMap, int labelByteLength) {
        setInitInput(serverKeywordLabelMap, labelByteLength);
        info("{}{} Server Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        assert (kwPirParams instanceof Cmg21KwPirParams);
        params = (Cmg21KwPirParams) kwPirParams;
        // 服务端生成并发送哈希密钥
        hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashKeyNum(), secureRandom);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 1/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashKeyTime);

        stopWatch.start();
        // 计算PRF
        ArrayList<ByteBuffer> keywordPrfs = computeKeywordPrf();
        Map<ByteBuffer, ByteBuffer> prfLabelMap = IntStream.range(0, keywordSize)
            .boxed()
            .collect(Collectors.toMap(
                keywordPrfs::get,
                i -> serverKeywordLabelMap.get(byteArrayObjectMap.get(keywordArrayList.get(i))),
                (a, b) -> b)
            );
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 2/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // 计算完全哈希分桶
        hashBins = generateCompleteHashBin(keywordPrfs, params.getBinNum());
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 3/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), hashTime);

        stopWatch.start();
        // 计算多项式系数
        encodeDatabase(prfLabelMap);
        stopWatch.stop();
        long encodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Init Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), encodeTime);

        initialized = true;
        info("{}{} Server Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        info("{}{} Server begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 服务端执行OPRF协议
        DataPacketHeader blindHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();
        List<byte[]> blindPrfPayload = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            taskId, ptoDesc.getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrfPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 1/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), oprfTime);

        stopWatch.start();
        // 接收客户端布谷鸟哈希分桶结果
        DataPacketHeader cuckooHashResultHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_RESULT.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> cuckooHashResultPayload = rpc.receive(cuckooHashResultHeader).getPayload();
        handleCuckooHashResultPayload(cuckooHashResultPayload);
        stopWatch.stop();
        long cuckooHashResultTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 2/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashResultTime);

        stopWatch.start();
        // 接收客户端的加密密钥
        DataPacketHeader fheParamsHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_FHE_PARAMS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> fheParamsPayload = rpc.receive(fheParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            fheParamsPayload.size() == 3, "Failed to receive BFV encryption parameters"
        );
        stopWatch.stop();
        long fheParamsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 3/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), fheParamsTime);

        stopWatch.start();
        // 接收客户端的加密查询信息
        DataPacketHeader queryHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        ArrayList<byte[]> queryPayload = new ArrayList<>(rpc.receive(queryHeader).getPayload());
        int ciphertextNumber = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == ciphertextNumber * params.getQueryPowers().length,
            "The size of query is incorrect"
        );
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 4/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), queryTime);

        stopWatch.start();
        // 密文多项式运算
        computeResponse(queryPayload, fheParamsPayload);
        DataPacketHeader keywordResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ITEM_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keywordResponseHeader, keywordResponsePayload));
        DataPacketHeader labelResponseHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_LABEL_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(labelResponseHeader, labelResponsePayload));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Server Step 5/5 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), replyTime);

        info("{}{} Server end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    /**
     * 哈希桶内元素的排序。
     *
     * @param binItems 哈希桶内的元素。
     * @return 排序后的哈希桶。
     */
    private ArrayList<HashBinEntry<ByteBuffer>> sortedHashBinEntries(ArrayList<HashBinEntry<ByteBuffer>> binItems) {
        ArrayList<ArrayList<HashBinEntry<ByteBuffer>>> partitions = new ArrayList<>(binItems.size());
        ArrayList<ArrayList<Set<Long>>> partElementSets = new ArrayList<>();
        for (int i = 0; i < binItems.size(); i++) {
            partElementSets.add(i, new ArrayList<>());
            for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                partElementSets.get(i).add(j, new HashSet<>());
            }
        }
        for (int i = 0; i < binItems.size(); i++) {
            partitions.add(new ArrayList<>());
        }
        BigInteger blockMask = BigInteger.ONE.shiftLeft(CommonConstants.BLOCK_BIT_LENGTH).subtract(BigInteger.ONE);
        int shiftBits = BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1;
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(shiftBits).subtract(BigInteger.ONE);
        for (HashBinEntry<ByteBuffer> binItem : binItems) {
            long[] itemParts = new long[params.getItemEncodedSlotSize()];
            BigInteger item = BigIntegerUtils.byteArrayToBigInteger(binItem.getItem().array());
            item = item.and(blockMask);
            for (int i = 0; i < params.getItemEncodedSlotSize(); i++) {
                itemParts[i] = item.and(shiftMask).longValueExact();
                item = item.shiftRight(shiftBits);
            }
            for (int i = 0; i < partitions.size(); i++) {
                ArrayList<HashBinEntry<ByteBuffer>> partition = partitions.get(i);
                if (partition.size() == 0) {
                    partition.add(binItem);
                    for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                        partElementSets.get(i).get(j).add(itemParts[j]);
                    }
                    break;
                } else {
                    if (partition.size() != params.getMaxPartitionSizePerBin()) {
                        int l;
                        for (l = 0; l < partition.size(); l++) {
                            if (checkRepeatedItemPart(partElementSets.get(i), itemParts)) {
                                break;
                            }
                        }
                        if (l == partition.size()) {
                            partition.add(binItem);
                            for (int j = 0; j < params.getItemEncodedSlotSize(); j++) {
                                partElementSets.get(i).get(j).add(itemParts[j]);
                            }
                            break;
                        }
                    }
                }
            }
        }
        Stream<ArrayList<HashBinEntry<ByteBuffer>>> partitionStream = partitions.stream();
        partitionStream = parallel ? partitionStream.parallel() : partitionStream;
        return partitionStream
            .filter(partition -> partition.size() != 0)
            .peek(partition -> {
                for (int index = partition.size(); index < params.getMaxPartitionSizePerBin(); index++) {
                    partition.add(HashBinEntry.fromEmptyItem(botElementByteBuffer));
                }
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 生成完全哈希分桶。
     *
     * @param itemList 元素集合。
     * @param binNum   哈希桶数量。
     * @return 完全哈希分桶。
     */
    private ArrayList<ArrayList<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(ArrayList<ByteBuffer> itemList,
                                                                                   int binNum) {
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, keywordSize, hashKeys);
        completeHash.insertItems(itemList);
        IntStream binIndexIntStream = IntStream.range(0, completeHash.binNum());
        binIndexIntStream = parallel ? binIndexIntStream.parallel() : binIndexIntStream;
        ArrayList<ArrayList<HashBinEntry<ByteBuffer>>> hashBinList = binIndexIntStream
            .mapToObj(binIndex -> {
                ArrayList<HashBinEntry<ByteBuffer>> binArrayList = new ArrayList<>(completeHash.getBin(binIndex));
                return sortedHashBinEntries(binArrayList);
            })
            .collect(Collectors.toCollection(ArrayList::new));
        int maxBinSize = hashBinList.stream().mapToInt(ArrayList::size).max().orElse(0);
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(botElementByteBuffer);
        hashBinList.forEach(bin -> {
            int paddingNum = maxBinSize - bin.size();
            for (int index = 0; index < paddingNum; index++) {
                bin.add(paddingEntry);
            }
        });
        return hashBinList;
    }

    /**
     * 检查元素分块后，是否与已有的分块重复。
     *
     * @param existingItemParts 已有的元素分块。
     * @param itemParts         元素分块。
     * @return 是否与已有的分块重复。
     */
    private boolean checkRepeatedItemPart(List<Set<Long>> existingItemParts, long[] itemParts) {
        assert existingItemParts.size() == itemParts.length;
        return IntStream.range(0, itemParts.length).anyMatch(i -> existingItemParts.get(i).contains(itemParts[i]));
    }

    /**
     * 预计算数据库编码后的多项式系数。
     *
     * @param prfMap prf映射。
     */
    private void encodeDatabase(Map<ByteBuffer, ByteBuffer> prfMap) {
        long plainModulus = params.getPlainModulus();
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, plainModulus);
        int itemEncodedSlotSize = params.getItemEncodedSlotSize();
        int itemPerCiphertext = params.getPolyModulusDegree() / itemEncodedSlotSize;
        int ciphertextNum = params.getBinNum() / itemPerCiphertext;
        int binSize = hashBins.get(0).size();
        int partitionCount = (binSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        int bigPartitionCount = binSize / params.getMaxPartitionSizePerBin();
        int labelPartitionCount = (int) Math.ceil(((double) (labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * Byte.SIZE) /
            ((LongUtils.ceilLog2(plainModulus) - 1) * itemEncodedSlotSize));
        serverKeywordEncode = new long[partitionCount * ciphertextNum][][];
        serverLabelEncode = new long[partitionCount * ciphertextNum * labelPartitionCount][][];
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        // and coeffs of g(x), which has the property g(y) = label(y) for each y in bucket.
        // ciphertext num is small, therefore we need to do parallel computation inside the loop
        for (int i = 0; i < ciphertextNum; i++) {
            // 在循环内部构建final的index，否则无法并发
            int finalIndex = i;
            for (int partition = 0; partition < partitionCount; partition++) {
                // 元素的多项式系数
                long[][] fCoeffs = new long[itemPerCiphertext * itemEncodedSlotSize][];
                // 标签的多项式系数
                long[][][] gCoeffs = new long[labelPartitionCount][itemPerCiphertext * itemEncodedSlotSize][];
                // 对分块内的元素和标签编码
                int partitionSize, partitionStart;
                partitionSize = partition < bigPartitionCount ?
                    params.getMaxPartitionSizePerBin() : binSize % params.getMaxPartitionSizePerBin();
                partitionStart = params.getMaxPartitionSizePerBin() * partition;
                IntStream itemIndexStream = IntStream.range(0, itemPerCiphertext);
                itemIndexStream = parallel ? itemIndexStream.parallel() : itemIndexStream;
                itemIndexStream.forEach(j -> {
                    // 存储每块的元素编码
                    long[][] currentBucketElement = new long[itemEncodedSlotSize][partitionSize];
                    // 存储每块的标签编码
                    long[][][] currentBucketLabels = new long[labelPartitionCount][itemEncodedSlotSize][partitionSize];
                    for (int l = 0; l < partitionSize; l++) {
                        HashBinEntry<ByteBuffer> entry = hashBins.get(finalIndex * itemPerCiphertext + j).get(partitionStart + l);
                        long[] temp = params.getHashBinEntryEncodedArray(entry, false, secureRandom);
                        for (int k = 0; k < itemEncodedSlotSize; k++) {
                            currentBucketElement[k][l] = temp[k];
                        }
                    }
                    for (int l = 0; l < itemEncodedSlotSize; l++) {
                        fCoeffs[j * itemEncodedSlotSize + l] = zp64Poly.rootInterpolate(partitionSize, currentBucketElement[l], 0L);
                    }
                    int nonEmptyBuckets = 0;
                    for (int l = 0; l < partitionSize; l++) {
                        HashBinEntry<ByteBuffer> entry = hashBins.get(finalIndex * itemPerCiphertext + j).get(partitionStart + l);
                        if (entry.getHashIndex() != HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                            for (int k = 0; k < itemEncodedSlotSize; k++) {
                                currentBucketElement[k][nonEmptyBuckets] = currentBucketElement[k][l];
                            }
                            byte[] oprf = entry.getItem().array();
                            // 取oprf的前128比特
                            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            System.arraycopy(oprf, 0, keyBytes, 0, CommonConstants.BLOCK_BYTE_LENGTH);
                            byte[] iv = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                            secureRandom.nextBytes(iv);
                            byte[] plaintextLabel = prfMap.get(entry.getItem()).array();
                            byte[] ciphertextLabel = streamCipher.ivEncrypt(keyBytes, iv, plaintextLabel);
                            long[][] temp = params.encodeLabel(ciphertextLabel, labelPartitionCount);
                            for (int k = 0; k < labelPartitionCount; k++) {
                                for (int h = 0; h < itemEncodedSlotSize; h++) {
                                    currentBucketLabels[k][h][nonEmptyBuckets] = temp[k][h];
                                }
                            }
                            nonEmptyBuckets++;
                        }
                    }
                    for (int l = 0; l < itemEncodedSlotSize; l++) {
                        for (int k = 0; k < labelPartitionCount; k++) {
                            long[] xArray = new long[nonEmptyBuckets];
                            long[] yArray = new long[nonEmptyBuckets];
                            for (int index = 0; index < nonEmptyBuckets; index++) {
                                xArray[index] = currentBucketElement[l][index];
                                yArray[index] = currentBucketLabels[k][l][index];
                            }
                            if (nonEmptyBuckets > 0) {
                                gCoeffs[k][j * itemEncodedSlotSize + l] = zp64Poly.interpolate(
                                    nonEmptyBuckets, xArray, yArray
                                );
                            } else {
                                gCoeffs[k][j * itemEncodedSlotSize + l] = new long[0];
                            }
                        }
                    }
                });
                long[][] encodeElementVector = new long[partitionSize + 1][params.getPolyModulusDegree()];
                int labelSize = IntStream.range(1, gCoeffs[0].length)
                    .map(j -> gCoeffs[0][j].length).filter(j -> j >= 1)
                    .max()
                    .orElse(1);
                long[][][] encodeLabelVector = new long[labelPartitionCount][labelSize][params.getPolyModulusDegree()];
                for (int j = 0; j < partitionSize + 1; j++) {
                    // encode the jth coefficients of all polynomials into a vector
                    for (int l = 0; l < itemEncodedSlotSize * itemPerCiphertext; l++) {
                        encodeElementVector[j][l] = fCoeffs[l][j];
                    }
                    for (int l = itemEncodedSlotSize * itemPerCiphertext; l < params.getPolyModulusDegree(); l++) {
                        encodeElementVector[j][l] = 0;
                    }
                }
                for (int j = 0; j < labelSize; j++) {
                    for (int k = 0; k < labelPartitionCount; k++) {
                        for (int l = 0; l < itemEncodedSlotSize * itemPerCiphertext; l++) {
                            if (gCoeffs[k][l].length == 0) {
                                encodeLabelVector[k][j][l] = Math.abs(secureRandom.nextLong()) % plainModulus;
                            } else {
                                encodeLabelVector[k][j][l] = (j < gCoeffs[k][l].length) ? gCoeffs[k][l][j] : 0;
                            }
                        }
                        for (int l = itemEncodedSlotSize * itemPerCiphertext; l < params.getPolyModulusDegree(); l++) {
                            encodeLabelVector[k][j][l] = 0;
                        }
                    }
                }
                serverKeywordEncode[partition + i * partitionCount] = encodeElementVector;
                for (int j = 0; j < labelPartitionCount; j++) {
                    serverLabelEncode[j + partition * labelPartitionCount + i * partitionCount * labelPartitionCount] =
                        encodeLabelVector[j];
                }
            }
        }
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

    private void handleCuckooHashResultPayload(List<byte[]> cuckooHashResultPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashResultPayload.size() == 1);
        BigInteger cuckooHashResult = BigIntegerUtils.byteArrayToBigInteger(cuckooHashResultPayload.get(0));
        MpcAbortPreconditions.checkArgument(cuckooHashResult.equals(BigInteger.ONE), "cuckoo hash failed.");
    }

    /**
     * 服务端计算密文多项式结果。
     *
     * @param encryptedQueryList   加密查询列表。
     * @param encryptionParamsList 加密方案参数列表。
     * @throws MpcAbortException 如果协议异常中止。
     */
    private void computeResponse(ArrayList<byte[]> encryptedQueryList, List<byte[]> encryptionParamsList)
        throws MpcAbortException {
        int binSize = hashBins.get(0).size();
        int partitionCount = (binSize + params.getMaxPartitionSizePerBin() - 1) / params.getMaxPartitionSizePerBin();
        // 计算所有的密文次方
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
                outerPowersSet, params.getMaxPartitionSizePerBin() / (params.getPsLowDegree() + 1));
            powerDegree = new int[innerPowerNodes.length + outerPowerNodes.length][2];
            int[][] innerPowerNodesDegree = Arrays.stream(innerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            int[][] outerPowerNodesDegree = Arrays.stream(outerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            System.arraycopy(innerPowerNodesDegree, 0, powerDegree, 0, innerPowerNodesDegree.length);
            System.arraycopy(outerPowerNodesDegree, 0, powerDegree, innerPowerNodesDegree.length, outerPowerNodesDegree.length);
        } else {
            Set<Integer> sourcePowersSet = Arrays.stream(params.getQueryPowers())
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
            PowerNode[] powerNodes = PowerUtils.computePowers(sourcePowersSet, params.getMaxPartitionSizePerBin());
            powerDegree = Arrays.stream(powerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
        }
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int labelPartitionCount = (int) Math.ceil((labelByteLength + CommonConstants.BLOCK_BYTE_LENGTH) * 8.0 /
            ((BigInteger.valueOf(params.getPlainModulus()).bitLength() - 1) * params.getItemEncodedSlotSize()));
        IntStream queryIntStream = parallel ?
            IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum);
        ArrayList<byte[]> queryPowers = queryIntStream
            .mapToObj(i -> Cmg21KwPirNativeUtils.computeEncryptedPowers(
                encryptionParamsList.get(0),
                encryptionParamsList.get(1),
                encryptedQueryList.subList(i * params.getQueryPowers().length, (i + 1) * params.getQueryPowers().length),
                powerDegree,
                params.getQueryPowers(),
                params.getPsLowDegree()))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.getPsLowDegree() > 0) {
            keywordResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.optComputeMatches(
                                encryptionParamsList.get(0),
                                encryptionParamsList.get(2),
                                encryptionParamsList.get(1),
                                serverKeywordEncode[i * partitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            labelResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.optComputeMatches(
                                encryptionParamsList.get(0),
                                encryptionParamsList.get(2),
                                encryptionParamsList.get(1),
                                serverLabelEncode[i * partitionCount * labelPartitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                params.getPsLowDegree()))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        } else if (params.getPsLowDegree() == 0) {
            keywordResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount).parallel() : IntStream.range(0, partitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.naiveComputeMatches(
                                encryptionParamsList.get(0),
                                encryptionParamsList.get(2),
                                serverKeywordEncode[i * partitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
            labelResponsePayload = (parallel ? IntStream.range(0, ciphertextNum).parallel() : IntStream.range(0, ciphertextNum))
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, partitionCount * labelPartitionCount).parallel() :
                        IntStream.range(0, partitionCount * labelPartitionCount))
                        .mapToObj(j ->
                            Cmg21KwPirNativeUtils.naiveComputeMatches(
                                encryptionParamsList.get(0),
                                encryptionParamsList.get(2),
                                serverLabelEncode[i * partitionCount + j],
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length)))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        } else {
            throw new MpcAbortException("ps_low_degree参数设置不正确");
        }
    }

    /**
     * 服务端计算关键词PRF。
     *
     * @return 关键词PRF。
     */
    private ArrayList<ByteBuffer> computeKeywordPrf() {
        Ecc ecc = EccFactory.createInstance(envType);
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        Stream<ByteBuffer> keywordStream = keywordArrayList.stream();
        keywordStream = parallel ? keywordStream.parallel() : keywordStream;
        return keywordStream
            .map(keyword -> ecc.hashToCurve(keyword.array()))
            .map(hash -> ecc.multiply(hash, alpha))
            .map(prf -> ecc.encode(prf, false))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}