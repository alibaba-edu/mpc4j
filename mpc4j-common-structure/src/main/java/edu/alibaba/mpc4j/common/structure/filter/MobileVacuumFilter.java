package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Mobile Vacuum Filter. This is inspired by the optimizations introduced in Cuckoo Filter shown in the paper:
 * <p>Daniel Kales, Christian Rechberger, Thomas Schneider, Matthias Senker, and Christian Weinert. Mobile private
 * contact discovery at scale. In 28th USENIX Security Symposium (USENIX Security 19), pp. 1447-1464. 2019.</p>
 * The difference is that Vacuum Filter must set ENTRIES_PER_BUCKET = 4. We can only modify LOAD_FACTOR = 0.66 and
 * FINGERPRINT_BYTE_LENGTH = 32 to reduce size.
 *
 * @author Weiran Liu
 * @date 2024/9/24
 */
class MobileVacuumFilter<T> extends AbstractVacuumFilter<T> {
    /**
     * filter type
     */
    private static final FilterType FILTER_TYPE = MobileVacuumFilterPosition.FILTER_TYPE;
    /**
     * cuckoo filter type
     */
    private static final CuckooFilterType CUCKOO_FILTER_TYPE = MobileVacuumFilterPosition.CUCKOO_FILTER_TYPE;
    /**
     * α, number of elements in buckets / total number of elements. Experiments show that we cannot set α = 0.66 since
     * there will be non-negligible failure probabilities. We have to set α = 0.955.
     */
    private static final double LOAD_FACTOR = MobileVacuumFilterPosition.LOAD_FACTOR;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 29, the result is 29 + 3 = 32.
     */
    static final int FINGERPRINT_BYTE_LENGTH = MobileVacuumFilterPosition.FINGERPRINT_BYTE_LENGTH;

    static <X> MobileVacuumFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        return new MobileVacuumFilter<>(envType, maxSize, keys);
    }

    static <X> MobileVacuumFilter<X> load(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 4);
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
        MobileVacuumFilter<X> mobileVacuumFilter = new MobileVacuumFilter<>(envType, maxSize, keys);
        // read size
        mobileVacuumFilter.size = headerByteBuffer.getInt();
        // read item byte length
        mobileVacuumFilter.itemByteLength = headerByteBuffer.getInt();
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
            ArrayList<ByteBuffer> bucket =  mobileVacuumFilter.buckets.get(bucketIndex);
            for (int entryIndex = 0; entryIndex < ENTRIES_PER_BUCKET; entryIndex++) {
                if (bitmap.get(offset + entryIndex)) {
                    bucket.add(ByteBuffer.wrap(bucketFlattenedElements[flattenedIndex]));
                    flattenedIndex++;
                } else {
                    break;
                }
            }
        }

        return mobileVacuumFilter;
    }

    private MobileVacuumFilter(EnvType envType, int maxSize, byte[][] keys) {
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
        // Optimizations for mobile filter. See USENIX 2019 paper, Section 4.1. For each entry of a Cuckoo filter, an
        // additional bit is transmitted that indicates whether this entry is empty or holds a tag. The entry itself is
        // only transmitted if it is not empty.
        List<byte[]> cuckooFilterList = new LinkedList<>();
        // we concurrently encode bitmap and entries.
        int bitmapSize = ENTRIES_PER_BUCKET * bucketNum;
        BitVector bitmap = BitVectorFactory.createZeros(bitmapSize);
        for (int bucketIndex = 0; bucketIndex < bucketNum; bucketIndex++) {
            int offset = bucketIndex * ENTRIES_PER_BUCKET;
            ArrayList<ByteBuffer> bucket = buckets.get(bucketIndex);
            assert bucket.size() <= ENTRIES_PER_BUCKET;
            for (int entryIndex = 0; entryIndex < bucket.size(); entryIndex++) {
                bitmap.set(offset + entryIndex, true);
                ByteBuffer fingerprint = bucket.get(entryIndex);
                cuckooFilterList.add(BytesUtils.clone(fingerprint.array()));
            }
        }
        byteArrayList.add(bitmap.getBytes());
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
