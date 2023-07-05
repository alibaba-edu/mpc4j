package edu.alibaba.mpc4j.common.tool.filter;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.util.Arrays;
import java.util.List;

/**
 * naive Bloom Filter. We refer to the following implementation:
 * <p>
 * https://github.com/google/guava/blob/master/guava/src/com/google/common/hash/BloomFilter.java
 * </p>
 * The theory about Bloom Filter is shown in the following paper:
 * <p>
 * Dong C, Chen L, Wen Z. When private set intersection meets big data: an efficient and scalable protocol.
 * CCS 2013, pp. 789-800.
 * </p>
 *
 * @author Weiran Liu
 * @date 2020/06/30
 */
public class NaiveBloomFilter<T> extends AbstractBloomFilter<T> {
    /**
     * When m = n log_2(e) * log_2(1/p), HASH_NUM = log_2(1/p)
     */
    static final int HASH_NUM = CommonConstants.STATS_BIT_LENGTH;

    /**
     * Gets m for the given n.
     *
     * @param maxSize number of elements.
     * @return m.
     */
    public static int bitSize(int maxSize) {
        MathPreconditions.checkPositive("n", maxSize);
        // m = n / ln(2) * σ, flooring so that m % Byte.SIZE = 0.
        int bitLength = (int) Math.ceil(maxSize * CommonConstants.STATS_BIT_LENGTH / Math.log(2));
        return CommonUtils.getByteLength(bitLength) * Byte.SIZE;
    }

    /**
     * Creates an empty filter.
     *
     * @param envType environment.
     * @param maxSize max number of inserted elements.
     * @param keys    hash keys.
     * @return an empty filter.
     */
    public static <X> NaiveBloomFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, HASH_NUM);
        int m = NaiveBloomFilter.bitSize(maxSize);
        byte[] storage = new byte[CommonUtils.getByteLength(m)];
        // all positions are initiated as 0
        Arrays.fill(storage, (byte) 0x00);
        return new NaiveBloomFilter<>(envType, maxSize, m, keys, 0, storage, 0);
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> NaiveBloomFilter<X> fromByteArrayList(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("byteArrayList.size", "desired size", byteArrayList.size(), 5 + HASH_NUM);
        // type
        byteArrayList.remove(0);
        // max size
        int maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        int m = NaiveBloomFilter.bitSize(maxSize);
        // size
        int size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // item byte length
        int itemByteLength = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // storage
        byte[] storage = byteArrayList.remove(0);
        // keys
        byte[][] keys = byteArrayList.toArray(new byte[0][]);
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, HASH_NUM);

        return new NaiveBloomFilter<>(envType, maxSize, m, keys, size, storage, itemByteLength);
    }

    NaiveBloomFilter(EnvType envType, int maxSize, int m, byte[][] keys, int size, byte[] storage, int itemByteLength) {
        super(FilterType.NAIVE_BLOOM_FILTER, envType, maxSize, m, keys, size, storage, itemByteLength);
    }

    @Override
    public int[] hashIndexes(T data) {
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        return Arrays.stream(hashes)
            .mapToInt(hash -> hash.getInteger(dataBytes, m))
            .distinct()
            .toArray();
    }
}
