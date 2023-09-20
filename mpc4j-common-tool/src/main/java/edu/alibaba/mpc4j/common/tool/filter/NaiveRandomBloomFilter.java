package edu.alibaba.mpc4j.common.tool.filter;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.util.Arrays;
import java.util.List;

/**
 * naive random Bloom Filter. We refer to the following implementation:
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
public class NaiveRandomBloomFilter<T> extends AbstractBloomFilter<T> {
    /**
     * When m = n log_2(e) * log_2(1/p), HASH_NUM = log_2(1/p)
     */
    private static final int HASH_NUM = CommonConstants.STATS_BIT_LENGTH;
    /**
     * hash key num = 1
     */
    static final int HASH_KEY_NUM = 1;
    /**
     * type
     */
    private static final FilterType FILTER_TYPE = FilterType.NAIVE_RANDOM_BLOOM_FILTER;

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
     * @param key     hash key.
     * @return an empty filter.
     */
    public static <X> NaiveRandomBloomFilter<X> create(EnvType envType, int maxSize, byte[] key) {
        int m = NaiveRandomBloomFilter.bitSize(maxSize);
        byte[] storage = new byte[CommonUtils.getByteLength(m)];
        // all positions are initiated as 0
        Arrays.fill(storage, (byte) 0x00);
        return new NaiveRandomBloomFilter<>(envType, maxSize, m, key, 0, storage, 0);
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> NaiveRandomBloomFilter<X> fromByteArrayList(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("byteArrayList.size", "desired size", byteArrayList.size(), 6);
        // type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("", "", typeOrdinal, FILTER_TYPE.ordinal());
        // max size
        int maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        int m = NaiveRandomBloomFilter.bitSize(maxSize);
        // size
        int size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // item byte length
        int itemByteLength = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // storage
        byte[] storage = byteArrayList.remove(0);
        // key
        byte[] key = byteArrayList.remove(0);

        return new NaiveRandomBloomFilter<>(envType, maxSize, m, key, size, storage, itemByteLength);
    }

    NaiveRandomBloomFilter(EnvType envType, int maxSize, int m, byte[] key, int size, byte[] storage, int itemByteLength) {
        super(FILTER_TYPE, envType, maxSize, m, HASH_NUM, key, size, storage, itemByteLength);
    }

    @Override
    public int[] hashIndexes(T data) {
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        byte[] hashes = hash.getBytes(dataBytes);
        return Arrays.stream(IntUtils.byteArrayToIntArray(hashes))
            .map(hi -> Math.abs(hi % m))
            .distinct()
            .toArray();
    }
}
