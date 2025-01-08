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
 * naive fast vacuum filter.
 *
 * @author Weiran Liu
 * @date 2024/11/8
 */
public class NaiveFastVacuumFilter<T> extends AbstractFastVacuumFilter<T> {
    /**
     * fast cuckoo filter type
     */
    private static final FastCuckooFilterType TYPE = NaiveFastVacuumFilterPosition.TYPE;
    /**
     * α, number of elements in buckets / total number of elements. The default value is 95.5%, see Table 2.
     */
    private static final double LOAD_FACTOR = NaiveFastVacuumFilterPosition.LOAD_FACTOR;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 40, the result is 40 + 3 = 43, we round to 48, see Table 2.
     */
    static final int FINGERPRINT_BIT_LENGTH = NaiveFastVacuumFilterPosition.FINGERPRINT_BIT_LENGTH;

    static <X> NaiveFastVacuumFilter<X> create(int maxSize, long hashSeed) {
        return new NaiveFastVacuumFilter<>(maxSize, hashSeed);
    }

    static <X> NaiveFastVacuumFilter<X> load(List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 3);
        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, TYPE.ordinal());
        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // read hash seed
        long hashSeed = headerByteBuffer.getLong();
        // read max size
        int maxSize = headerByteBuffer.getInt();
        NaiveFastVacuumFilter<X> filter = new NaiveFastVacuumFilter<>(maxSize, hashSeed);
        // read size
        filter.size = headerByteBuffer.getInt();
        // read elements
        byte[] compressed = byteArrayList.remove(0);
        MathPreconditions.checkEqual(
            "expected length", "actual length",
            filter.bucketNum * ENTRIES_PER_BUCKET * filter.fingerprintByteLength, compressed.length
        );
        byte[][] bucketFlattenedElements = SerializeUtils
            .decompressEqual(compressed, filter.fingerprintByteLength)
            .toArray(byte[][]::new);
        for (int bucketIndex = 0; bucketIndex < filter.bucketNum; bucketIndex++) {
            for (int entryIndex = 0; entryIndex < ENTRIES_PER_BUCKET; entryIndex++) {
                // add non-zero fingerprint
                long fingerprint = LongUtils.fixedByteArrayToLong(bucketFlattenedElements[bucketIndex * ENTRIES_PER_BUCKET + entryIndex]);
                if (fingerprint != 0L) {
                    filter.table.insertIndexToBucket(bucketIndex, entryIndex, fingerprint);
                }
            }
        }

        return filter;
    }

    private NaiveFastVacuumFilter(int maxSize, long hashSeed) {
        super(maxSize, hashSeed, FINGERPRINT_BIT_LENGTH, LOAD_FACTOR);
    }

    @Override
    public List<byte[]> save() {
        List<byte[]> byteArrayList = new LinkedList<>();
        // write type
        byteArrayList.add(IntUtils.intToByteArray(TYPE.ordinal()));
        // write header
        ByteBuffer headerByteBuffer = ByteBuffer.allocate(Integer.BYTES * 2 + Long.BYTES);
        // hash seed
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
