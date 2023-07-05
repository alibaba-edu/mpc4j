package edu.alibaba.mpc4j.common.tool.filter;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.util.*;

/**
 * LPRST21 Bloom Filter related to LPRST21 Garbled Bloom Filter, which requires that any inputs have constant distinct
 * positions in the Garbled Bloom Filter. This requirement is used in the following paper:
 * <p>
 * Lepoint, Tancrede, Sarvar Patel, Mariana Raykova, Karn Seth, and Ni Trieu. Private join and compute from PIR with
 * default. ASIACRYPT 2021, Part II, pp. 605-634. Cham: Springer International Publishing, 2021.
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/5/4
 */
public class Lprst21BloomFilter<T> extends AbstractBloomFilter<T> {
    /**
     * Bloom Filter needs 40 hashes
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
        return CommonUtils.getByteLength((int) Math.ceil(maxSize * CommonConstants.STATS_BIT_LENGTH / Math.log(2))) * Byte.SIZE;
    }

    /**
     * Creates an empty filter.
     *
     * @param envType environment.
     * @param maxSize max number of inserted elements.
     * @param keys    hash keys.
     * @return an empty filter.
     */
    public static <X> Lprst21BloomFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, HASH_NUM);
        int m = Lprst21BloomFilter.bitSize(maxSize);
        byte[] storage = new byte[CommonUtils.getByteLength(m)];
        // all positions are initialed as 0
        Arrays.fill(storage, (byte) 0x00);

        return new Lprst21BloomFilter<>(envType, maxSize, m, keys, 0, storage, 0);
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> Lprst21BloomFilter<X> fromByteArrayList(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("byteArrayList.size", "desired size", byteArrayList.size(), 5 + HASH_NUM);
        // type
        byteArrayList.remove(0);
        // max size
        int maxSize = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        int m = Lprst21BloomFilter.bitSize(maxSize);
        // size
        int size = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // item byte length
        int itemByteLength = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        // storage
        byte[] storage = byteArrayList.remove(0);
        // keys
        byte[][] keys = byteArrayList.toArray(new byte[0][]);
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, HASH_NUM);

        return new Lprst21BloomFilter<>(envType, maxSize, m, keys, size, storage, itemByteLength);
    }

    Lprst21BloomFilter(EnvType envType, int maxSize, int m, byte[][] keys, int size, byte[] storage, int itemByteLength) {
        super(FilterType.LPRST21_BLOOM_FILTER, envType, maxSize, m, keys, size, storage, itemByteLength);
    }

    @Override
    public int[] hashIndexes(T data) {
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        int[] hashIndexes = new int[HASH_NUM];
        Set<Integer> positionSet = new HashSet<>(HASH_NUM);
        hashIndexes[0] = hashes[0].getInteger(0, dataBytes, m);
        positionSet.add(hashIndexes[0]);
        // generate k distinct positions
        for (int i = 1; i < HASH_NUM; i++) {
            int hiIndex = 0;
            do {
                hashIndexes[i] = hashes[i].getInteger(hiIndex, dataBytes, m);
                hiIndex++;
            } while (positionSet.contains(hashIndexes[i]));
            positionSet.add(hashIndexes[i]);
        }
        return hashIndexes;
    }
}
