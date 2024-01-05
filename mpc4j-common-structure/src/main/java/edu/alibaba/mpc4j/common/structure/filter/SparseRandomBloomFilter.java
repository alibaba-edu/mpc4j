package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.*;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * sparse random Bloom Filter, which have larger m than naive Bloom Filter. This Bloom Filter is used in the paper:
 * <p></p>
 * Rindal P, Rosulek M. Improved private set intersection against malicious adversaries. EUROCRYPT 2017. Springer, Cham,
 * 2017: 235-259.
 *
 * @author Ziyuan Liang, Weiran Liu
 * @date 2020/09/30
 */
public class SparseRandomBloomFilter<T> extends AbstractBloomFilter<T> {
    /**
     * hash key num = 1
     */
    static final int HASH_KEY_NUM = 1;
    /**
     * max number of supported elements (in log size)
     */
    private static final int MAX_LOG_N = 23;
    /**
     * type
     */
    private static final FilterFactory.FilterType FILTER_TYPE = FilterFactory.FilterType.SPARSE_RANDOM_BLOOM_FILTER;
    /**
     * n -> m
     */
    private static final TIntIntMap SBF_BIT_LENGTH_INIT_MATRIX = new TIntIntHashMap();

    static {
        SBF_BIT_LENGTH_INIT_MATRIX.put(2, 7196);
        SBF_BIT_LENGTH_INIT_MATRIX.put(3, 9296);
        SBF_BIT_LENGTH_INIT_MATRIX.put(4, 12896);
        SBF_BIT_LENGTH_INIT_MATRIX.put(5, 19172);
        SBF_BIT_LENGTH_INIT_MATRIX.put(6, 30350);
        SBF_BIT_LENGTH_INIT_MATRIX.put(7, 50726);
        SBF_BIT_LENGTH_INIT_MATRIX.put(8, 88627);
        SBF_BIT_LENGTH_INIT_MATRIX.put(9, 160506);
        SBF_BIT_LENGTH_INIT_MATRIX.put(10, 302436);
        SBF_BIT_LENGTH_INIT_MATRIX.put(11, 578306);
        SBF_BIT_LENGTH_INIT_MATRIX.put(12, 1121959);
        SBF_BIT_LENGTH_INIT_MATRIX.put(13, 2182857);
        SBF_BIT_LENGTH_INIT_MATRIX.put(14, 4270964);
        SBF_BIT_LENGTH_INIT_MATRIX.put(15, 8402960);
        SBF_BIT_LENGTH_INIT_MATRIX.put(16, 16579297);
        SBF_BIT_LENGTH_INIT_MATRIX.put(17, 32836550);
        SBF_BIT_LENGTH_INIT_MATRIX.put(18, 65163755);
        SBF_BIT_LENGTH_INIT_MATRIX.put(19, 129392705);
        SBF_BIT_LENGTH_INIT_MATRIX.put(20, 257635123);
        SBF_BIT_LENGTH_INIT_MATRIX.put(21, 513277951);
        SBF_BIT_LENGTH_INIT_MATRIX.put(22, 1023879938);
        SBF_BIT_LENGTH_INIT_MATRIX.put(23, 2042206617);
    }

    /**
     * Gets m for the given n.
     *
     * @param maxSize number of elements.
     * @return m.
     */
    public static int bitSize(int maxSize) {
        MathPreconditions.checkPositive("n", maxSize);
        int nLogValue = Math.max(2, LongUtils.ceilLog2(maxSize));
        if (nLogValue > MAX_LOG_N) {
            throw new IllegalArgumentException("n is greater than the max supported n = " + (1 << 23) + ": " + maxSize);
        }
        return CommonUtils.getByteLength(SBF_BIT_LENGTH_INIT_MATRIX.get(nLogValue)) * Byte.SIZE;
    }

    /**
     * n -> hashNum
     */
    private static final TIntIntMap HASH_NUM_INIT_MATRIX = new TIntIntHashMap();

    static {
        HASH_NUM_INIT_MATRIX.put(2, 94);
        HASH_NUM_INIT_MATRIX.put(3, 94);
        HASH_NUM_INIT_MATRIX.put(4, 94);
        HASH_NUM_INIT_MATRIX.put(5, 94);
        HASH_NUM_INIT_MATRIX.put(6, 94);
        HASH_NUM_INIT_MATRIX.put(7, 94);
        HASH_NUM_INIT_MATRIX.put(8, 94);
        HASH_NUM_INIT_MATRIX.put(9, 94);
        HASH_NUM_INIT_MATRIX.put(10, 94);
        HASH_NUM_INIT_MATRIX.put(11, 94);
        HASH_NUM_INIT_MATRIX.put(12, 94);
        HASH_NUM_INIT_MATRIX.put(13, 92);
        HASH_NUM_INIT_MATRIX.put(14, 93);
        HASH_NUM_INIT_MATRIX.put(15, 91);
        HASH_NUM_INIT_MATRIX.put(16, 91);
        HASH_NUM_INIT_MATRIX.put(17, 91);
        HASH_NUM_INIT_MATRIX.put(18, 90);
        HASH_NUM_INIT_MATRIX.put(19, 89);
        HASH_NUM_INIT_MATRIX.put(20, 90);
        HASH_NUM_INIT_MATRIX.put(21, 89);
        HASH_NUM_INIT_MATRIX.put(22, 88);
        HASH_NUM_INIT_MATRIX.put(23, 90);
    }

    /**
     * Gets hash num, from Table 5 of the paper.
     *
     * @param maxSize number of elements.
     * @return hash num.
     */
    public static int getHashNum(int maxSize) {
        MathPreconditions.checkPositive("n", maxSize);
        int nLogValue = Math.max(2, LongUtils.ceilLog2(maxSize));
        if (nLogValue > MAX_LOG_N) {
            throw new IllegalArgumentException("n is greater than the max supported n = " + (1 << 23) + ": " + maxSize);
        }
        return HASH_NUM_INIT_MATRIX.get(nLogValue);
    }

    /**
     * Creates an empty filter.
     *
     * @param envType environment.
     * @param maxSize max number of inserted elements.
     * @param key    hash key.
     * @return an empty filter.
     */
    public static <X> SparseRandomBloomFilter<X> create(EnvType envType, int maxSize, byte[] key) {
        int m = SparseRandomBloomFilter.bitSize(maxSize);
        byte[] storage = new byte[CommonUtils.getByteLength(m)];
        // all positions are initiated as 0
        Arrays.fill(storage, (byte) 0x00);

        return new SparseRandomBloomFilter<>(envType, maxSize, m, key, 0, storage, 0);
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> SparseRandomBloomFilter<X> load(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 3);

        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, FILTER_TYPE.ordinal());

        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // max size
        int maxSize = headerByteBuffer.getInt();
        int m = SparseRandomBloomFilter.bitSize(maxSize);
        // size
        int size = headerByteBuffer.getInt();
        // item byte length
        int itemByteLength = headerByteBuffer.getInt();
        // key
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        headerByteBuffer.get(key);

        // read storage
        byte[] storage = byteArrayList.remove(0);

        return new SparseRandomBloomFilter<>(envType, maxSize, m, key, size, storage, itemByteLength);
    }

    SparseRandomBloomFilter(EnvType envType, int maxSize, int m, byte[] key, int size, byte[] storage, int itemByteLength) {
        super(FILTER_TYPE, envType, maxSize, m,
            SparseRandomBloomFilter.getHashNum(maxSize),
            key, size, storage, itemByteLength);
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
