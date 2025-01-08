package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.structure.fastfilter.FastCuckooFilterFactory.FastCuckooFilterType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.SerializeUtils;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * naive fast cuckoo filter. This is the original Cuckoo Filter described in the following paper:
 * <p>
 * Fan B, Andersen D G, Kaminsky M, et al. Cuckoo filter: Practically better than bloom. CoNET 2014, pp. 75-88.
 * </p>
 * Naive Cuckoo Filter sets the number of elements in each bucket b = 4, tag size v = 42, and load factor = 0.955
 * to reach an FPP of ε_{max} = 2^{-40}.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
class NaiveFastCuckooFilter<T> extends AbstractFastCuckooFilter<T> {
    /**
     * fast cuckoo filter type.
     */
    static final FastCuckooFilterType TYPE = FastCuckooFilterType.NAIVE_FAST_CUCKOO_FILTER;
    /**
     * b = 4
     */
    private static final int ENTRIES_PER_BUCKET = NaiveFastCuckooFilterPosition.ENTRIES_PER_BUCKET;
    /**
     * tag size v = 42
     */
    private static final int FINGERPRINT_BIT_LENGTH = NaiveFastCuckooFilterPosition.FINGERPRINT_BIT_LENGTH;

    /**
     * Creates an empty fast cuckoo filter.
     *
     * @param maxSize  max number of inserted elements.
     * @param hashSeed hash seed.
     * @return an empty fast cuckoo filter.
     */
    static <X> NaiveFastCuckooFilter<X> create(int maxSize, long hashSeed) {
        return new NaiveFastCuckooFilter<>(maxSize, hashSeed);
    }

    /**
     * Creates the fast cuckoo filter based on {@code List<byte[]>}.
     *
     * @param byteArrayList the fast cuckoo filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the fast cuckoo filter.
     */
    static <X> NaiveFastCuckooFilter<X> load(List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 3);
        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, TYPE.ordinal());
        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // read seed
        long hashSeed = headerByteBuffer.getLong();
        // read max size
        int maxSize = headerByteBuffer.getInt();
        NaiveFastCuckooFilter<X> filter = new NaiveFastCuckooFilter<>(maxSize, hashSeed);
        // read size
        filter.size = headerByteBuffer.getInt();
        // read elements
        byte[] compressed = byteArrayList.remove(0);
        MathPreconditions.checkEqual(
            "expected length", "actual length",
            filter.bucketNum * ENTRIES_PER_BUCKET * filter.fingerprintByteLength, compressed.length
        );
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, filter.fingerprintByteLength)
            .toArray(new byte[0][]);
        for (int i = 0; i < filter.bucketNum; i++) {
            for (int j = 0; j < ENTRIES_PER_BUCKET; j++) {
                long fingerprint = LongUtils.fixedByteArrayToLong(bucketFlattenedElements[i * ENTRIES_PER_BUCKET + j]);
                if (fingerprint != 0) {
                    filter.table.insertIndexToBucket(i, j, fingerprint);
                } else {
                    break;
                }
            }
        }

        return filter;
    }

    private NaiveFastCuckooFilter(int maxNum, long hashSeed) {
        super(maxNum, hashSeed, ENTRIES_PER_BUCKET, FINGERPRINT_BIT_LENGTH);
    }

    @Override
    public List<byte[]> save() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // write type
        byteArrayList.add(IntUtils.intToByteArray(TYPE.ordinal()));
        // write header
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(Integer.BYTES * 2 + Long.BYTES);
        // seed
        headerByteBuffer.putLong(hashSeed);
        // max size
        headerByteBuffer.putInt(maxSize);
        // size
        headerByteBuffer.putInt(size);
        byteArrayList.add(headerByteBuffer.array());
        // write elements
        List<byte[]> cuckooFilterList = new LinkedList<>();
        IntStream.range(0, bucketNum).forEach(i -> {
            for (int j = 0; j < ENTRIES_PER_BUCKET; j++) {
                long fingerprint = table.readTag(i, j);
                cuckooFilterList.add(LongUtils.longToFixedByteArray(fingerprint, fingerprintByteLength));
            }
        });
        byteArrayList.add(SerializeUtils.compressEqual(cuckooFilterList, fingerprintByteLength));

        return byteArrayList;
    }

    /**
     * Load part data of fast cuckoo filter based on {@code List<byte[]>}.
     *
     * @param partMsg the filter represented by {@code List<byte[]>}.
     * @return the fingerprints
     */
    static long[][] loadPart(List<byte[]> partMsg){
        MathPreconditions.checkEqual("actual list size", "expect list size", partMsg.size(), 2);
        int bucketNum = IntUtils.byteArrayToInt(partMsg.remove(0));
        long[][] fingerprints = new long[bucketNum][ENTRIES_PER_BUCKET];
        // read elements
        byte[] compressed = partMsg.remove(0);
        int fingerprintByteLength = CommonUtils.getByteLength(FINGERPRINT_BIT_LENGTH);
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, fingerprintByteLength)
            .toArray(new byte[0][]);
        // create buckets and then add elements.
        for (int i = 0; i < bucketNum; i++) {
            for (int entryIndex = 0; entryIndex < ENTRIES_PER_BUCKET; entryIndex++) {
                // add non-zero fingerprint
                fingerprints[i][entryIndex] = LongUtils.fixedByteArrayToLong(bucketFlattenedElements[i * ENTRIES_PER_BUCKET + entryIndex]);
            }
        }
        return fingerprints;
    }

    /**
     * Load part data of fast cuckoo filter in byte array form based on {@code List<byte[]>}.
     *
     * @param partMsg the filter represented by {@code List<byte[]>}.
     * @return the fingerprints
     */
    static byte[][] loadPartByte(List<byte[]> partMsg){
        MathPreconditions.checkEqual("actual list size", "expect list size", partMsg.size(), 2);
        int bucketNum = IntUtils.byteArrayToInt(partMsg.remove(0));
        // read elements
        byte[] compressed = partMsg.remove(0);
        int fingerprintByteLength = CommonUtils.getByteLength(FINGERPRINT_BIT_LENGTH);
        byte[][] fingerprints = new byte[bucketNum][fingerprintByteLength * ENTRIES_PER_BUCKET];
        byte[][] bucketFlattenedElements = SerializeUtils.decompressEqual(compressed, fingerprintByteLength)
            .toArray(new byte[0][]);
        // create buckets and then add elements.
        for (int i = 0; i < bucketNum; i++) {
            for (int entryIndex = 0; entryIndex < ENTRIES_PER_BUCKET; entryIndex++) {
                System.arraycopy(bucketFlattenedElements[i * ENTRIES_PER_BUCKET + entryIndex], 0, fingerprints[i], entryIndex * fingerprintByteLength, fingerprintByteLength);
            }
        }
        return fingerprints;
    }

    @Override
    public List<byte[]> savePart(int fromBucketIndex, int toBucketIndex) {
        assert fromBucketIndex >= 0 && toBucketIndex <= bucketNum && fromBucketIndex < toBucketIndex;
        int saveBucketNum = toBucketIndex - fromBucketIndex;
        List<byte[]> byteArrayList = new LinkedList<>();
        List<byte[]> filterList = new LinkedList<>();
        for (int i = fromBucketIndex; i < toBucketIndex; i++) {
            for (int j = 0; j < ENTRIES_PER_BUCKET; j++) {
                long fingerprint = table.readTag(i, j);
                filterList.add(LongUtils.longToFixedByteArray(fingerprint, fingerprintByteLength));
            }
        }
        byteArrayList.add(IntUtils.intToByteArray(saveBucketNum));
        byteArrayList.add(SerializeUtils.compressEqual(filterList, fingerprintByteLength));
        return byteArrayList;
    }

    @Override
    public FastCuckooFilterType getType() {
        return TYPE;
    }
}
