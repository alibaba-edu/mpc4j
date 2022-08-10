package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * CMG21关键词索引PIR方案参数。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirParams implements KwPirParams {
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 哈希桶数目
     */
    private final int binNum;
    /**
     * 每个哈希桶内分块的最大元素个数
     */
    private final int maxPartitionSizePerBin;
    /**
     * 元素编码后占的卡槽个数
     */
    private final int itemEncodedSlotSize;
    /**
     * Paterson-Stockmeyer方法的低阶值
     */
    private final int psLowDegree;
    /**
     * 查询幂次方
     */
    private final int[] queryPowers;
    /**
     * 明文模数
     */
    private final long plainModulus;
    /**
     * 多项式阶
     */
    private final int polyModulusDegree;
    /**
     * 系数模数的比特值
     */
    private final int[] coeffModulusBits;
    /**
     * 预估服务端数据量
     */
    private final int expectServerSize;
    /**
     * 支持查询的关键词数量
     */
    private final int maxRetrievalSize;

    private Cmg21KwPirParams(CuckooHashBinType cuckooHashBinType, int binNum, int maxPartitionSizePerBin,
                             int itemEncodedSlotSize, int psLowDegree, int[] queryPowers,
                             long plainModulus, int polyModulusDegree, int[] coeffModulusBits,
                             int expectServerSize, int maxRetrievalSize) {
        this.cuckooHashBinType = cuckooHashBinType;
        this.binNum = binNum;
        this.maxPartitionSizePerBin = maxPartitionSizePerBin;
        this.itemEncodedSlotSize = itemEncodedSlotSize;
        this.psLowDegree = psLowDegree;
        this.queryPowers = queryPowers;
        this.plainModulus = plainModulus;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusBits = coeffModulusBits;
        this.expectServerSize = expectServerSize;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    /**
     * 创建关键词PIR协议参数，不检查参数的有效性。
     *
     * @param cuckooHashBinType      布谷鸟哈希类型
     * @param binNum                 哈希桶数目。
     * @param maxPartitionSizePerBin 每个哈希桶内分块的最大元素个数。
     * @param itemEncodedSlotSize    元素编码后占的卡槽个数。
     * @param psLowDegree            Paterson-Stockmeyer方法的低阶值。
     * @param queryPowers            查询幂次方。
     * @param plainModulus           明文模数。
     * @param polyModulusDegree      多项式阶。
     * @param coeffModulusBits       系数模数的比特值。
     * @param expectServerSize       预估服务端数据量。
     * @param maxRetrievalSize       支持查询的关键词数量。
     * @return 关键词PIR协议参数。
     */
    public static Cmg21KwPirParams uncheckCreate(CuckooHashBinType cuckooHashBinType, int binNum, int maxPartitionSizePerBin,
                                                 int itemEncodedSlotSize, int psLowDegree, int[] queryPowers,
                                                 long plainModulus, int polyModulusDegree, int[] coeffModulusBits,
                                                 int expectServerSize, int maxRetrievalSize) {
        return new Cmg21KwPirParams(
            cuckooHashBinType, binNum, maxPartitionSizePerBin,
            itemEncodedSlotSize, psLowDegree, queryPowers,
            plainModulus, polyModulusDegree, coeffModulusBits,
            expectServerSize, maxRetrievalSize);
    }

    /**
     * 创建关键词PIR协议参数，检查参数的有效性。
     *
     * @param cuckooHashBinType      布谷鸟哈希类型
     * @param binNum                 哈希桶数目。
     * @param maxPartitionSizePerBin 每个哈希桶内分块的最大元素个数。
     * @param itemEncodedSlotSize    元素编码后占的卡槽个数。
     * @param psLowDegree            Paterson-Stockmeyer方法的低阶值。
     * @param queryPowers            查询幂次方。
     * @param plainModulus           明文模数。
     * @param polyModulusDegree      多项式阶。
     * @param coeffModulusBits       系数模数的比特值。
     * @param expectServerSize       预估服务端数据量。
     * @param maxRetrievalSize       支持查询的关键词数量。
     * @return 关键词PIR协议参数。
     */
    public static Cmg21KwPirParams create(CuckooHashBinType cuckooHashBinType, int binNum,
                                          int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                                          int[] queryPowers, long plainModulus, int polyModulusDegree,
                                          int[] coeffModulusBits, int expectServerSize, int maxRetrievalSize) {
        Cmg21KwPirParams cmg21KwPirParams = uncheckCreate(
            cuckooHashBinType, binNum, maxPartitionSizePerBin,
            itemEncodedSlotSize, psLowDegree, queryPowers,
            plainModulus, polyModulusDegree, coeffModulusBits,
            expectServerSize, maxRetrievalSize);
        if (Cmg21KwPirParamsChecker.checkValid(cmg21KwPirParams)) {
            return cmg21KwPirParams;
        } else {
            throw new IllegalArgumentException("Invalid SEAL parameters: " + cmg21KwPirParams);
        }
    }

    /**
     * 服务端100W，客户端最大检索数量4096
     */
    public static final Cmg21KwPirParams SERVER_1M_CLIENT_MAX_4096 = Cmg21KwPirParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 6552, 770,
        5,
        26, new int[]{1, 5, 8, 27, 135},
        1785857L, 8192, new int[]{50, 56, 56, 50},
        1000000, 4096
    );

    /**
     * 服务端100W，客户端最大检索数量1
     */
    public static final Cmg21KwPirParams SERVER_1M_CLIENT_MAX_1 = Cmg21KwPirParams.uncheckCreate(
        CuckooHashBinType.NO_STASH_ONE_HASH, 1638, 228,
        5,
        0, new int[]{1, 3, 8, 19, 33, 39, 92, 102},
        65537L, 8192, new int[]{56, 48, 48},
        1000000, 1
    );

    /**
     * 返回布谷鸟哈希类型。
     *
     * @return 布谷鸟哈希类型。
     */
    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    /**
     * 返回布谷鸟哈希桶的哈希数量。
     *
     * @return 布谷鸟哈希桶的哈希数量。
     */
    public int getCuckooHashKeyNum() {
        return CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    /**
     * 返回哈希桶数目。
     *
     * @return 哈希桶数目。
     */
    public int getBinNum() {
        return binNum;
    }

    /**
     * 返回每个哈希桶内分块的最大元素个数。
     *
     * @return 每个哈希桶内分块的最大元素个数。
     */
    public int getMaxPartitionSizePerBin() {
        return maxPartitionSizePerBin;
    }

    /**
     * 返回元素编码后占的卡槽个数。
     *
     * @return 元素编码后占的卡槽个数。
     */
    public int getItemEncodedSlotSize() {
        return itemEncodedSlotSize;
    }

    /**
     * 返回Paterson-Stockmeyer方法的低阶值。
     *
     * @return Paterson-Stockmeyer方法的低阶值。
     */
    public int getPsLowDegree() {
        return psLowDegree;
    }

    /**
     * 返回查询幂次方。
     *
     * @return 查询幂次方。
     */
    public int[] getQueryPowers() {
        return queryPowers;
    }

    /**
     * 返回明文模数。
     *
     * @return 明文模数。
     */
    public long getPlainModulus() {
        return plainModulus;
    }

    /**
     * 返回多项式阶。
     *
     * @return 多项式阶。
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * 返回系数模数的比特值。
     *
     * @return 系数模数的比特值。
     */
    public int[] getCoeffModulusBits() {
        return coeffModulusBits;
    }

    @Override
    public int maxRetrievalSize() {
        return maxRetrievalSize;
    }

    @Override
    public int expectServerSize() {
        return expectServerSize;
    }

    @Override
    public String toString() {
        return "Parameters chosen:" + "\n" +
            "  - hash_bin_params: {" + "\n" +
            "     - cuckoo_hash_bin_type : " + cuckooHashBinType + "\n" +
            "     - bin_num : " + binNum + "\n" +
            "     - max_items_per_bin : " + maxPartitionSizePerBin + "\n" +
            "  }" + "\n" +
            "  - item_params: {" + "\n" +
            "     - felts_per_item : " + itemEncodedSlotSize + "\n" +
            "  }" + "\n" +
            "  - query_params: {" + "\n" +
            "     - ps_low_degree : " + psLowDegree + "\n" +
            "     - query_powers : " + Arrays.toString(queryPowers) + "\n" +
            "  }" + "\n" +
            "  - seal_params: {" + "\n" +
            "     - plain_modulus : " + plainModulus + "\n" +
            "     - poly_modulus_degree : " + polyModulusDegree + "\n" +
            "     - coeff_modulus_bits : " + Arrays.toString(coeffModulusBits) + "\n" +
            "  }" + "\n";
    }

    /**
     * 返回哈希桶条目中元素对应的编码数组。
     *
     * @param hashBinEntry 哈希桶条目。
     * @param isReceiver   是否为接收方。
     * @param secureRandom 随机状态。
     * @return 哈希桶条目中元素对应的编码数组。
     */
    public long[] getHashBinEntryEncodedArray(HashBinEntry<ByteBuffer> hashBinEntry, boolean isReceiver,
                                              SecureRandom secureRandom) {
        long[] encodedArray = new long[itemEncodedSlotSize];
        int bitLength = (BigInteger.valueOf(plainModulus).bitLength() - 1) * itemEncodedSlotSize;
        assert bitLength >= 80;
        int shiftBits = BigInteger.valueOf(plainModulus).bitLength() - 1;
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(shiftBits).subtract(BigInteger.ONE);
        // 判断是否为空桶
        if (hashBinEntry.getHashIndex() != -1) {
            assert (hashBinEntry.getHashIndex() < 3) : "hash index should be [0, 1, 2]";
            BigInteger input = BigIntegerUtils.byteArrayToNonNegBigInteger(hashBinEntry.getItem().array());
            input = input.mod(BigInteger.ONE.shiftLeft(CommonConstants.BLOCK_BIT_LENGTH));
            for (int i = 0; i < itemEncodedSlotSize; i++) {
                encodedArray[i] = input.and(shiftMask).longValueExact();
                input = input.shiftRight(shiftBits);
            }
        } else {
            IntStream.range(0, itemEncodedSlotSize).forEach(i -> {
                long random = Math.abs(secureRandom.nextLong()) % plainModulus / 4;
                encodedArray[i] = random << 1 | (isReceiver ? 1L : 0L);
            });
        }
        for (int i = 0; i < itemEncodedSlotSize; i++) {
            assert (encodedArray[i] < plainModulus);
        }
        return encodedArray;
    }

    /**
     * 返回标签编码数组。
     *
     * @param labelBytes   标签字节。
     * @param partitionNum 分块数目。
     * @return 标签编码数组。
     */
    public long[][] encodeLabel(byte[] labelBytes, int partitionNum) {
        long[][] encodedArray = new long[partitionNum][itemEncodedSlotSize];
        int shiftBits = (int) Math.ceil(((double) (labelBytes.length * Byte.SIZE)) / (itemEncodedSlotSize * partitionNum));
        BigInteger bigIntLabel = BigIntegerUtils.byteArrayToNonNegBigInteger(labelBytes);
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(shiftBits).subtract(BigInteger.ONE);
        for (int i = 0; i < partitionNum; i++) {
            for (int j = 0; j < itemEncodedSlotSize; j++) {
                encodedArray[i][j] = bigIntLabel.and(shiftMask).longValueExact();
                bigIntLabel = bigIntLabel.shiftRight(shiftBits);
            }
        }
        for (int i = 0; i < partitionNum; i++) {
            for (int j = 0; j < itemEncodedSlotSize; j++) {
                assert (encodedArray[i][j] < plainModulus);
            }
        }
        return encodedArray;
    }
}