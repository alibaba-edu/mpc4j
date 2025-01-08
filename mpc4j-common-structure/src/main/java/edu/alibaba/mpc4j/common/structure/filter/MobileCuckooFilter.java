package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Mobile Cuckoo Filter. This kind of cuckoo filter is used in the following two papers:
 * <ul>
 * <li>Daniel Kales, Christian Rechberger, Thomas Schneider, Matthias Senker, and Christian Weinert. Mobile private
 * contact discovery at scale. In 28th USENIX Security Symposium (USENIX Security 19), pp. 1447-1464. 2019.</li>
 * <li>Laura Hetz, Thomas Schneider, and Christian Weinert. Scaling mobile private contact discovery to billions of
 * users. In European Symposium on Research in Computer Security, pp. 455-476. Cham: Springer Nature Switzerland, 2023.</li>
 * </ul>
 * Mobile cuckoo filter sets b = 3, v = 32, load factor = 0.66 to reach an FPP of ε_{max} = 2^{-29}. Mobile cuckoo filter
 * also introduces a novel Cuckoo Filter Compression techniques.
 *
 * @author Weiran Liu
 * @date 2024/9/24
 */
class MobileCuckooFilter<T> extends AbstractCuckooFilter<T> {
    /**
     * filter type
     */
    private static final FilterType FILTER_TYPE = MobileCuckooFilterPosition.FILTER_TYPE;
    /**
     * cuckoo filter type
     */
    private static final CuckooFilterType CUCKOO_FILTER_TYPE = CuckooFilterType.MOBILE_CUCKOO_FILTER;
    /**
     * number of entries in each bucket b, mobile cuckoo filter sets b = 3.
     */
    private static final int ENTRIES_PER_BUCKET = MobileCuckooFilterPosition.ENTRIES_PER_BUCKET;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since b = 3, log_2(1/ε) = σ = 29, the result is 29 + 3 = 32.
     */
    private static final int FINGERPRINT_BYTE_LENGTH = MobileCuckooFilterPosition.FINGERPRINT_BYTE_LENGTH;
    /**
     * α, number of elements in buckets / total number of elements, mobile filter sets α = 0.66.
     */
    private static final double LOAD_FACTOR = MobileCuckooFilterPosition.LOAD_FACTOR;


    /**
     * Creates an empty cuckoo filter.
     *
     * @param envType environment.
     * @param maxSize max number of inserted elements.
     * @param keys    hash keys.
     * @return an empty filter.
     */
    static <X> MobileCuckooFilter<X> create(EnvType envType, int maxSize, byte[][] keys) {
        return new MobileCuckooFilter<>(envType, maxSize, keys);
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> MobileCuckooFilter<X> load(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 4);
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
        MobileCuckooFilter<X> mobileCuckooFilter = new MobileCuckooFilter<>(envType, maxSize, keys);
        // read size
        mobileCuckooFilter.size = headerByteBuffer.getInt();
        // read item byte length
        mobileCuckooFilter.itemByteLength = headerByteBuffer.getInt();
        // read elements
        BitVector bitmap = BitVectorFactory.create(ENTRIES_PER_BUCKET * mobileCuckooFilter.bucketNum, byteArrayList.remove(0));
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
        for (int bucketIndex = 0; bucketIndex < mobileCuckooFilter.bucketNum; bucketIndex++) {
            int offset = bucketIndex * ENTRIES_PER_BUCKET;
            ArrayList<ByteBuffer> bucket =  mobileCuckooFilter.buckets.get(bucketIndex);
            for (int entryIndex = 0; entryIndex < ENTRIES_PER_BUCKET; entryIndex++) {
                if (bitmap.get(offset + entryIndex)) {
                    bucket.add(ByteBuffer.wrap(bucketFlattenedElements[flattenedIndex]));
                    flattenedIndex++;
                } else {
                    break;
                }
            }
        }
        return mobileCuckooFilter;
    }

    private MobileCuckooFilter(EnvType envType, int maxSize, byte[][] keys) {
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
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public CuckooFilterType getCuckooFilterType() {
        return CUCKOO_FILTER_TYPE;
    }
}
