package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Distinct Bloom Filter related to distrinct Garbled Bloom Filter, which requires that any inputs have constant distinct
 * positions in the Garbled Bloom Filter. This requirement is used in the following paper:
 * <p></p>
 * Lepoint, Tancrede, Sarvar Patel, Mariana Raykova, Karn Seth, and Ni Trieu. Private join and compute from PIR with
 * default. ASIACRYPT 2021, Part II, pp. 605-634. Cham: Springer International Publishing, 2021.
 *
 * @author Qixian Zhou
 * @date 2023/5/4
 */
public class DistinctBloomFilter<T> extends AbstractBloomFilter<T> {
    /**
     * When m = n log_2(e) * log_2(1/p), HASH_NUM = log_2(1/p)
     */
    private static final int HASH_NUM = CommonConstants.STATS_BIT_LENGTH;
    /**
     * hash key num = 1
     */
    public static final int HASH_KEY_NUM = 1;
    /**
     * type
     */
    private static final FilterType FILTER_TYPE = FilterType.DISTINCT_BLOOM_FILTER;

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
     * @param key     hash key.
     * @return an empty filter.
     */
    public static <X> DistinctBloomFilter<X> create(EnvType envType, int maxSize, byte[] key) {
        int m = DistinctBloomFilter.bitSize(maxSize);
        byte[] storage = new byte[CommonUtils.getByteLength(m)];
        // all positions are initialed as 0
        Arrays.fill(storage, (byte) 0x00);

        return new DistinctBloomFilter<>(envType, maxSize, m, key, 0, storage, 0);
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> DistinctBloomFilter<X> load(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 3);

        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, FILTER_TYPE.ordinal());

        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // max size
        int maxSize = headerByteBuffer.getInt();
        int m = DistinctBloomFilter.bitSize(maxSize);
        // size
        int size = headerByteBuffer.getInt();
        // item byte length
        int itemByteLength = headerByteBuffer.getInt();
        // key
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        headerByteBuffer.get(key);

        // read storage
        byte[] storage = byteArrayList.remove(0);

        return new DistinctBloomFilter<>(envType, maxSize, m, key, size, storage, itemByteLength);
    }


    DistinctBloomFilter(EnvType envType, int maxSize, int m, byte[] key, int size, byte[] storage, int itemByteLength) {
        super(FILTER_TYPE, envType, maxSize, m, HASH_NUM, key, size, storage, itemByteLength);
    }

    @Override
    public int[] hashIndexes(T data) {
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        int[] hashes = IntUtils.byteArrayToIntArray(hash.getBytes(dataBytes));
        // we now use the method provided in VOLE-PSI to get distinct hash indexes
        for (int j = 0; j < HASH_NUM; j++) {
            // hj = r % (m - j)
            int modulus = m - j;
            int hj = Math.abs(hashes[j] % modulus);
            int i = 0;
            int end = j;
            // for each previous hi <= hj, we set hj = hj + 1.
            while (i != end) {
                if (hashes[i] <= hj) {
                    hj++;
                } else {
                    break;
                }
                i++;
            }
            // now we now that all hi > hj, we place the value
            while (i != end) {
                hashes[end] = hashes[end - 1];
                end--;
            }
            hashes[i] = hj;
        }
        return hashes;
    }
}
