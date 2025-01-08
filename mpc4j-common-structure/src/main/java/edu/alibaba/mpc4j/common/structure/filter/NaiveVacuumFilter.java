package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Vacuum Filter. The scheme is described in the following paper:
 * <p>
 * Wang M, Zhou M, Shi S, et al. Vacuum filters: more space-efficient and faster replacement for bloom and cuckoo
 * filters[J]. Proceedings of the VLDB Endowment, 2019, 13(2): 197-210.
 * </p>
 * As shown in Section 1 of the paper, the vacuum filter has unique advantages:
 * <ul>
 * <li>its memory cost is the smallest among existing methods (including Morton filter);</li>
 * <li>its query throughput is higher than most other solutions, only slightly lower than that of cuckoo in very few cases;</li>
 * <li>it supports practical dynamics using the memory hierarchy in practice.</li>
 * </ul>
 * Based on the description, we can find that Vacuum Filter can be seen as a special Cuckoo Filter.
 *
 * @author Li Peng, Ziyuan Liang
 * @date 2020/10/21
 */
class NaiveVacuumFilter<T> extends AbstractVacuumFilter<T> {
    /**
     * filter type
     */
    private static final FilterType FILTER_TYPE = NaiveVacuumFilterPosition.FILTER_TYPE;
    /**
     * cuckoo filter type
     */
    private static final CuckooFilterType CUCKOO_FILTER_TYPE = NaiveVacuumFilterPosition.CUCKOO_FILTER_TYPE;
    /**
     * α, number of elements in buckets / total number of elements. The default value is 95.5%, see Table 2.
     */
    private static final double LOAD_FACTOR = NaiveVacuumFilterPosition.LOAD_FACTOR;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 40, the result is 40 + 3 = 43, we round to 48, see Table 2.
     */
    static final int FINGERPRINT_BYTE_LENGTH = NaiveVacuumFilterPosition.FINGERPRINT_BYTE_LENGTH;

    static <X> NaiveVacuumFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        return new NaiveVacuumFilter<>(envType, maxSize, keys);
    }

    static <X> NaiveVacuumFilter<X> load(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 3);
        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, FILTER_TYPE.ordinal());
        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // read max size
        int maxSize = headerByteBuffer.getInt();
        // read keys
        assert CuckooFilter.getHashKeyNum() == 2;
        byte[][] keys = new byte[2][CommonConstants.BLOCK_BYTE_LENGTH];
        headerByteBuffer.get(keys[0]);
        headerByteBuffer.get(keys[1]);
        NaiveVacuumFilter<X> naiveVacuumFilter = new NaiveVacuumFilter<>(envType, maxSize, keys);
        // read size
        naiveVacuumFilter.size = headerByteBuffer.getInt();
        // read item byte length
        naiveVacuumFilter.itemByteLength = headerByteBuffer.getInt();
        // read elements
        byte[] compressed = byteArrayList.remove(0);
        MathPreconditions.checkEqual(
            "expected length", "actual length",
            naiveVacuumFilter.bucketNum * ENTRIES_PER_BUCKET * FINGERPRINT_BYTE_LENGTH, compressed.length
        );
        ByteBuffer[] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, FINGERPRINT_BYTE_LENGTH)
            .stream()
            .map(ByteBuffer::wrap).toArray(ByteBuffer[]::new);
        for (int bucketIndex = 0; bucketIndex < naiveVacuumFilter.bucketNum; bucketIndex++) {
            ArrayList<ByteBuffer> bucket = naiveVacuumFilter.buckets.get(bucketIndex);
            for (int entryIndex = 0; entryIndex < ENTRIES_PER_BUCKET; entryIndex++) {
                // add non-zero fingerprint
                ByteBuffer fingerprint = bucketFlattenedElements[bucketIndex * ENTRIES_PER_BUCKET + entryIndex];
                if (!fingerprint.equals(naiveVacuumFilter.emptyFingerprint)) {
                    bucket.add(fingerprint);
                }
            }
        }

        return naiveVacuumFilter;
    }

    private NaiveVacuumFilter(EnvType envType, int maxSize, byte[][] keys) {
        super(envType, maxSize, keys, FINGERPRINT_BYTE_LENGTH, LOAD_FACTOR);
    }

    @Override
    public List<byte[]> save() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // write type
        byteArrayList.add(IntUtils.intToByteArray(FILTER_TYPE.ordinal()));
        // write header
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(Integer.BYTES * 3 + CommonConstants.BLOCK_BYTE_LENGTH * 2);
        // max size
        headerByteBuffer.putInt(maxSize);
        // fingerprint hash key
        headerByteBuffer.put(fingerprintHash.getKey());
        // bucket hash key
        headerByteBuffer.put(bucketHashKey);
        // size
        headerByteBuffer.putInt(size);
        // item byte length
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
    public CuckooFilterType getCuckooFilterType() {
        return CUCKOO_FILTER_TYPE;
    }

    @Override
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }
}
