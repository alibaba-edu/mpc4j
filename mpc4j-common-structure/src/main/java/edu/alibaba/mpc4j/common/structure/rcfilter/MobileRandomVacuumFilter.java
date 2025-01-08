package edu.alibaba.mpc4j.common.structure.rcfilter;

import edu.alibaba.mpc4j.common.structure.rcfilter.RandomCuckooFilterFactory.RandomCuckooFilterType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;
import gnu.trove.list.array.TLongArrayList;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * Mobile Random Vacuum Filter. This is inspired by the optimizations introduced in Cuckoo Filter shown in the paper:
 * <p>Daniel Kales, Christian Rechberger, Thomas Schneider, Matthias Senker, and Christian Weinert. Mobile private
 * contact discovery at scale. In 28th USENIX Security Symposium (USENIX Security 19), pp. 1447-1464. 2019.</p>
 * The difference is that Vacuum Filter must set ENTRIES_PER_BUCKET = 4. We can only modify LOAD_FACTOR = 0.66 and
 * FINGERPRINT_BYTE_LENGTH = 32 to reduce size.
 *
 * @author Weiran Liu
 * @date 2024/11/6
 */
class MobileRandomVacuumFilter extends AbstractRandomVacuumFilter {
    /**
     * type
     */
    private static final RandomCuckooFilterType TYPE = RandomCuckooFilterType.MOBILE_VACUUM_FILTER;
    /**
     * α, number of elements in buckets / total number of elements. Experiments show that we cannot set α = 0.66 since
     * there will be non-negligible failure probabilities. We have to set α = 0.955.
     */
    private static final double LOAD_FACTOR = 0.955;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 29, the result is 29 + 3 = 32.
     */
    static final int FINGERPRINT_BYTE_LENGTH = 32 / Byte.SIZE;

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

    static MobileRandomVacuumFilter create(int maxSize) {
        return new MobileRandomVacuumFilter(maxSize);
    }

    static MobileRandomVacuumFilter load(List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 4);
        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, TYPE.ordinal());
        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // read max size
        int maxSize = headerByteBuffer.getInt();
        MobileRandomVacuumFilter mobileVacuumFilter = new MobileRandomVacuumFilter(maxSize);
        // read size
        mobileVacuumFilter.size = headerByteBuffer.getInt();
        // read elements
        BitVector bitmap = BitVectorFactory.create(ENTRIES_PER_BUCKET * mobileVacuumFilter.bucketNum, byteArrayList.remove(0));
        int bitmapCount = bitmap.bitCount();
        byte[] compressed = byteArrayList.remove(0);
        MathPreconditions.checkEqual(
            "expected length", "actual length",
            bitmapCount * FINGERPRINT_BYTE_LENGTH, compressed.length
        );
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, FINGERPRINT_BYTE_LENGTH)
            .toArray(new byte[0][]);
        // create buckets and then add elements.
        int flattenedIndex = 0;
        for (int bucketIndex = 0; bucketIndex < mobileVacuumFilter.bucketNum; bucketIndex++) {
            int offset = bucketIndex * ENTRIES_PER_BUCKET;
            TLongArrayList bucket =  mobileVacuumFilter.buckets.get(bucketIndex);
            for (int entryIndex = 0; entryIndex < ENTRIES_PER_BUCKET; entryIndex++) {
                if (bitmap.get(offset + entryIndex)) {
                    bucket.add(LongUtils.fixedByteArrayToLong(bucketFlattenedElements[flattenedIndex]));
                    flattenedIndex++;
                } else {
                    break;
                }
            }
        }

        return mobileVacuumFilter;
    }

    private MobileRandomVacuumFilter(int maxSize) {
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
        // Optimizations for mobile filter. See USENIX 2019 paper, Section 4.1. For each entry of a Cuckoo filter, an
        // additional bit is transmitted that indicates whether this entry is empty or holds a tag. The entry itself is
        // only transmitted if it is not empty.
        List<byte[]> cuckooFilterList = new LinkedList<>();
        // we concurrently encode bitmap and entries.
        int bitmapSize = ENTRIES_PER_BUCKET * bucketNum;
        BitVector bitmap = BitVectorFactory.createZeros(bitmapSize);
        for (int bucketIndex = 0; bucketIndex < bucketNum; bucketIndex++) {
            int offset = bucketIndex * ENTRIES_PER_BUCKET;
            TLongArrayList bucket = buckets.get(bucketIndex);
            assert bucket.size() <= ENTRIES_PER_BUCKET;
            for (int entryIndex = 0; entryIndex < bucket.size(); entryIndex++) {
                bitmap.set(offset + entryIndex, true);
                byte[] fingerprint = LongUtils.longToFixedByteArray(bucket.get(entryIndex), fingerprintByteLength);
                cuckooFilterList.add(fingerprint);
            }
        }
        byteArrayList.add(bitmap.getBytes());
        byteArrayList.add(SerializeUtils.compressEqual(cuckooFilterList, FINGERPRINT_BYTE_LENGTH));

        return byteArrayList;
    }

    @Override
    public RandomCuckooFilterType getType() {
        return TYPE;
    }
}
