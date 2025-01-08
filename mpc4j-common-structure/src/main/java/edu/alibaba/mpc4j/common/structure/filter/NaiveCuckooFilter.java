package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Naive Cuckoo Filter. This is the original Cuckoo Filter described in the following paper:
 * <p>
 * Fan B, Andersen D G, Kaminsky M, et al. Cuckoo filter: Practically better than bloom. CoNET 2014, pp. 75-88.
 * </p>
 * Naive Cuckoo Filter sets the number of elements in each bucket b = 4, tage size v = 42, and load factor = 0.955
 * to reach an FPP of ε_{max} = 2^{-40}.
 *
 * @author Li Peng, Weiran Liu
 * @date 2020/08/29
 */
public class NaiveCuckooFilter<T> extends AbstractCuckooFilter<T> {
    /**
     * filter type
     */
    private static final FilterType FILTER_TYPE = NaiveCuckooFilterPosition.FILTER_TYPE;
    /**
     * number of entries in each bucket. The default value is 4.
     */
    private static final int ENTRIES_PER_BUCKET = NaiveCuckooFilterPosition.ENTRIES_PER_BUCKET;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = σ = 40, the result is 40 + 3 = 43, we round to 48, see Table 2.
     */
    private static final int FINGERPRINT_BYTE_LENGTH = NaiveCuckooFilterPosition.FINGERPRINT_BYTE_LENGTH;
    /**
     * α, number of elements in buckets / total number of elements. The default value is 95.5%, see Table 2.
     */
    private static final double LOAD_FACTOR = NaiveCuckooFilterPosition.LOAD_FACTOR;

    /**
     * Creates an empty cuckoo filter.
     *
     * @param envType environment.
     * @param maxSize max number of inserted elements.
     * @param keys    hash keys.
     * @return an empty filter.
     */
    static <X> NaiveCuckooFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        return new NaiveCuckooFilter<>(envType, maxSize, keys);
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> NaiveCuckooFilter<X> load(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 3);
        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, FILTER_TYPE.ordinal());
        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // read keys
        assert CuckooFilter.getHashKeyNum() == 2;
        byte[][] keys = new byte[2][CommonConstants.BLOCK_BYTE_LENGTH];
        // fingerprint hash key
        headerByteBuffer.get(keys[0]);
        // bucket hash key
        headerByteBuffer.get(keys[1]);
        // read max size
        int maxSize = headerByteBuffer.getInt();
        NaiveCuckooFilter<X> naiveCuckooFilter = new NaiveCuckooFilter<>(envType, maxSize, keys);
        // read size
        naiveCuckooFilter.size = headerByteBuffer.getInt();
        // read item byte length
        naiveCuckooFilter.itemByteLength = headerByteBuffer.getInt();
        // read elements
        byte[] compressed = byteArrayList.remove(0);
        MathPreconditions.checkEqual(
            "expected length", "actual length",
            naiveCuckooFilter.bucketNum * ENTRIES_PER_BUCKET * FINGERPRINT_BYTE_LENGTH, compressed.length
        );
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, FINGERPRINT_BYTE_LENGTH)
            .toArray(new byte[0][]);
        for (int bucketIndex = 0; bucketIndex < naiveCuckooFilter.bucketNum; bucketIndex++) {
            ArrayList<ByteBuffer> bucket = naiveCuckooFilter.buckets.get(bucketIndex);
            for (int entryIndex = 0; entryIndex < ENTRIES_PER_BUCKET; entryIndex++) {
                ByteBuffer fingerprint = ByteBuffer.wrap(bucketFlattenedElements[bucketIndex * ENTRIES_PER_BUCKET + entryIndex]);
                // only add non-zero fingerprint
                if (!fingerprint.equals(naiveCuckooFilter.emptyFingerprint)) {
                    bucket.add(fingerprint);
                }
            }
        }

        return naiveCuckooFilter;
    }

    private NaiveCuckooFilter(EnvType envType, int maxSize, byte[][] keys) {
        super(envType, maxSize, keys, ENTRIES_PER_BUCKET, FINGERPRINT_BYTE_LENGTH, LOAD_FACTOR);
    }

    @Override
    public List<byte[]> save() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // write type
        byteArrayList.add(IntUtils.intToByteArray(FILTER_TYPE.ordinal()));
        // write header
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(Integer.BYTES * 3 + CommonConstants.BLOCK_BYTE_LENGTH * 2);
        // write fingerprint hash and bucket hash
        headerByteBuffer.put(fingerprintHash.getKey());
        headerByteBuffer.put(bucketHashKey);
        // write max size
        headerByteBuffer.putInt(maxSize);
        // write size
        headerByteBuffer.putInt(size);
        // write item byte length
        headerByteBuffer.putInt(itemByteLength);
        byteArrayList.add(headerByteBuffer.array());
        // write elements
        List<byte[]> cuckooFilterList = new LinkedList<>();
        IntStream.range(0, bucketNum).forEach(bucketIndex -> {
            List<ByteBuffer> bucket = buckets.get(bucketIndex);
            int remainSize = ENTRIES_PER_BUCKET - bucket.size();
            for (ByteBuffer fingerprint : bucket) {
                cuckooFilterList.add(BytesUtils.clone(fingerprint.array()));
            }
            while (remainSize > 0) {
                cuckooFilterList.add(new byte[FINGERPRINT_BYTE_LENGTH]);
                remainSize--;
            }
        });
        byteArrayList.add(SerializeUtils.compressEqual(cuckooFilterList, FINGERPRINT_BYTE_LENGTH));

        return byteArrayList;
    }

    @Override
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public CuckooFilterType getCuckooFilterType() {
        return CuckooFilterType.NAIVE_CUCKOO_FILTER;
    }
}
