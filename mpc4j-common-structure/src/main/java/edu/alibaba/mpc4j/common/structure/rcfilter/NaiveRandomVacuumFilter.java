package edu.alibaba.mpc4j.common.structure.rcfilter;

import edu.alibaba.mpc4j.common.structure.rcfilter.RandomCuckooFilterFactory.RandomCuckooFilterType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import gnu.trove.list.array.TLongArrayList;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Random Vacuum Filter. The scheme is described in the following paper:
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
 * @author Weiran Liu
 * @date 2024/11/6
 */
public class NaiveRandomVacuumFilter extends AbstractRandomVacuumFilter {
    /**
     * filter type
     */
    private static final RandomCuckooFilterType TYPE = RandomCuckooFilterType.NAIVE_VACUUM_FILTER;
    /**
     * α, number of elements in buckets / total number of elements. The default value is 95.5%, see Table 2.
     */
    private static final double LOAD_FACTOR = 0.955;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 40, the result is 40 + 3 = 43, we round to 48, see Table 2.
     */
    static final int FINGERPRINT_BYTE_LENGTH = 6;

    /**
     * Gets bucket num.
     *
     * @param maxSize max size.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize) {
        return AbstractRandomVacuumFilter.getBucketNum(maxSize, LOAD_FACTOR);
    }

    /**
     * Gets bucket num.
     *
     * @param maxSize        max size.
     * @param alternateRange pre-computed alternate range.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize, int[] alternateRange) {
        return AbstractRandomVacuumFilter.getBucketNum(maxSize, LOAD_FACTOR, alternateRange);
    }

    /**
     * Sets alternate range.
     *
     * @param maxSize        max size.
     * @param alternateRange initial alternate range.
     */
    static void setAlternateRange(int maxSize, int[] alternateRange) {
        AbstractRandomVacuumFilter.setAlternateRange(maxSize, LOAD_FACTOR, alternateRange);
    }

    static NaiveRandomVacuumFilter create(int maxSize) {
        return new NaiveRandomVacuumFilter(maxSize);
    }

    static NaiveRandomVacuumFilter load(List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 3);
        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, TYPE.ordinal());
        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // read max size
        int maxSize = headerByteBuffer.getInt();
        NaiveRandomVacuumFilter naiveVacuumFilter = new NaiveRandomVacuumFilter(maxSize);
        // read size
        naiveVacuumFilter.size = headerByteBuffer.getInt();
        // read elements
        byte[] compressed = byteArrayList.remove(0);
        MathPreconditions.checkEqual(
            "expected length", "actual length",
            naiveVacuumFilter.bucketNum * ENTRIES_PER_BUCKET * FINGERPRINT_BYTE_LENGTH, compressed.length
        );
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, FINGERPRINT_BYTE_LENGTH)
            .toArray(byte[][]::new);
        for (int bucketIndex = 0; bucketIndex < naiveVacuumFilter.bucketNum; bucketIndex++) {
            TLongArrayList bucket = naiveVacuumFilter.buckets.get(bucketIndex);
            for (int entryIndex = 0; entryIndex < ENTRIES_PER_BUCKET; entryIndex++) {
                // add non-zero fingerprint
                long fingerprint = LongUtils.fixedByteArrayToLong(bucketFlattenedElements[bucketIndex * ENTRIES_PER_BUCKET + entryIndex]);
                if (fingerprint != naiveVacuumFilter.emptyFingerprint) {
                    bucket.add(fingerprint);
                }
            }
        }

        return naiveVacuumFilter;
    }

    private NaiveRandomVacuumFilter(int maxSize) {
        super(maxSize, FINGERPRINT_BYTE_LENGTH, LOAD_FACTOR);
    }

    @Override
    public List<byte[]> save() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // write type
        byteArrayList.add(IntUtils.intToByteArray(TYPE.ordinal()));
        // write header
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(Integer.BYTES * 2);
        // max size
        headerByteBuffer.putInt(maxSize);
        // size
        headerByteBuffer.putInt(size);
        byteArrayList.add(headerByteBuffer.array());
        // write elements
        List<byte[]> cuckooFilterList = new LinkedList<>();
        IntStream.range(0, bucketNum).forEach(bucketIndex -> {
            TLongArrayList bucket = buckets.get(bucketIndex);
            int remainSize = ENTRIES_PER_BUCKET - bucket.size();
            for (long fingerprint : bucket.toArray()) {
                cuckooFilterList.add(LongUtils.longToFixedByteArray(fingerprint, fingerprintByteLength));
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
    public RandomCuckooFilterType getType() {
        return TYPE;
    }
}
