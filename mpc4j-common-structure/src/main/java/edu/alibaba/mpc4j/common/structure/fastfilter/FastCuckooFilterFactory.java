package edu.alibaba.mpc4j.common.structure.fastfilter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * fast cuckoo filter factory.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
public class FastCuckooFilterFactory {
    /**
     * private constructor
     */
    private FastCuckooFilterFactory() {
        // empty
    }

    public enum FastCuckooFilterType {
        /**
         * mobile fast vacuum filter
         */
        MOBILE_FAST_VACUUM_FILTER,
        /**
         * naive fast vacuum filter
         */
        NAIVE_FAST_VACUUM_FILTER,
        /**
         * mobile fast cuckoo filter
         */
        MOBILE_FAST_CUCKOO_FILTER,
        /**
         * naive fast cuckoo filter
         */
        NAIVE_FAST_CUCKOO_FILTER,
    }

    /**
     * Gets bucket num.
     *
     * @param type    random cuckoo filter type.
     * @param maxSize max size.
     * @return bucket num.
     */
    public static int getBucketNum(FastCuckooFilterType type, int maxSize) {
        return switch (type) {
            case MOBILE_FAST_VACUUM_FILTER -> MobileFastVacuumFilterPosition.getBucketNum(maxSize);
            case NAIVE_FAST_VACUUM_FILTER -> NaiveFastVacuumFilterPosition.getBucketNum(maxSize);
            case MOBILE_FAST_CUCKOO_FILTER -> MobileFastCuckooFilterPosition.getBucketNum(maxSize);
            case NAIVE_FAST_CUCKOO_FILTER -> NaiveFastCuckooFilterPosition.getBucketNum(maxSize);
        };
    }

    /**
     * Gets entries per bucket.
     *
     * @param type random cuckoo filter type.
     * @return entries per bucket.
     */
    public static int getEntriesPerBucket(FastCuckooFilterType type) {
        return switch (type) {
            case MOBILE_FAST_VACUUM_FILTER -> MobileFastVacuumFilterPosition.ENTRIES_PER_BUCKET;
            case NAIVE_FAST_VACUUM_FILTER -> NaiveFastVacuumFilterPosition.ENTRIES_PER_BUCKET;
            case MOBILE_FAST_CUCKOO_FILTER -> MobileFastCuckooFilterPosition.ENTRIES_PER_BUCKET;
            case NAIVE_FAST_CUCKOO_FILTER -> NaiveFastCuckooFilterPosition.ENTRIES_PER_BUCKET;
        };
    }

    /**
     * Gets fingerprint byte length.
     *
     * @param type random cuckoo filter type.
     * @return fingerprint byte length.
     */
    public static int getFingerprintByteLength(FastCuckooFilterType type) {
        return switch (type) {
            case MOBILE_FAST_VACUUM_FILTER ->
                CommonUtils.getByteLength(MobileFastVacuumFilterPosition.FINGERPRINT_BIT_LENGTH);
            case NAIVE_FAST_VACUUM_FILTER ->
                CommonUtils.getByteLength(NaiveFastVacuumFilterPosition.FINGERPRINT_BIT_LENGTH);
            case MOBILE_FAST_CUCKOO_FILTER ->
                CommonUtils.getByteLength(MobileFastCuckooFilterPosition.FINGERPRINT_BIT_LENGTH);
            case NAIVE_FAST_CUCKOO_FILTER ->
                CommonUtils.getByteLength(NaiveFastCuckooFilterPosition.FINGERPRINT_BIT_LENGTH);
        };
    }

    /**
     * Gets estimate byte size.
     * should note that the real size of MOBILE_FAST_VACUUM_FILTER may less than computed result
     *
     * @param type random cuckoo filter type.
     * @return estimate byte size.
     */
    public static long estimateByteSize(FastCuckooFilterType type, int maxSize) {
        int bucketNum = getBucketNum(type, maxSize);
        int fingerprintByteLength = getFingerprintByteLength(type);
        int entriesPerBucket = getEntriesPerBucket(type);
        switch (type) {
            case NAIVE_FAST_CUCKOO_FILTER, NAIVE_FAST_VACUUM_FILTER -> {
                long storageByteLength = ((long) bucketNum) * entriesPerBucket * fingerprintByteLength;
                // type + hashSeed + maxSize + size
                return Integer.BYTES + Long.BYTES + Integer.BYTES * 2 + storageByteLength;
            }
            case MOBILE_FAST_CUCKOO_FILTER, MOBILE_FAST_VACUUM_FILTER -> {
                int bitRequireForEachBucket = type.equals(FastCuckooFilterType.MOBILE_FAST_CUCKOO_FILTER) ? 2 : 3;
                int bitmapByteNum = CommonUtils.getByteLength(bitRequireForEachBucket * bucketNum);
                long storageByteLength = (long) maxSize * fingerprintByteLength;
                // type + hashSeed + maxSize + size
                return Integer.BYTES + Long.BYTES + Integer.BYTES * 2 + bitmapByteNum + storageByteLength;
            }
            case null, default ->
                throw new IllegalArgumentException("Invalid " + FastCuckooFilterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * save the part information of a filter.
     *
     * @param type random cuckoo filter type.
     * @param saveMsg part save data
     * @return bucket.
     */
    public static long[][] loadPart(FastCuckooFilterType type, List<byte[]> saveMsg){
        return switch (type) {
            case MOBILE_FAST_VACUUM_FILTER -> MobileFastVacuumFilter.loadPart(saveMsg);
            case NAIVE_FAST_VACUUM_FILTER -> NaiveFastVacuumFilter.loadPart(saveMsg);
            case MOBILE_FAST_CUCKOO_FILTER -> MobileFastCuckooFilter.loadPart(saveMsg);
            case NAIVE_FAST_CUCKOO_FILTER -> NaiveFastCuckooFilter.loadPart(saveMsg);
        };
    }

    /**
     * save the part information of a filter.
     *
     * @param type random cuckoo filter type.
     * @param saveMsg part save data
     * @return bucket.
     */
    public static byte[][] loadPartByte(FastCuckooFilterType type, List<byte[]> saveMsg){
        return switch (type) {
            case MOBILE_FAST_VACUUM_FILTER -> MobileFastVacuumFilter.loadPartByte(saveMsg);
            case NAIVE_FAST_VACUUM_FILTER -> NaiveFastVacuumFilter.loadPartByte(saveMsg);
            case MOBILE_FAST_CUCKOO_FILTER -> MobileFastCuckooFilter.loadPartByte(saveMsg);
            case NAIVE_FAST_CUCKOO_FILTER -> NaiveFastCuckooFilter.loadPartByte(saveMsg);
        };
    }

    /**
     * recover part fingerprints of fast cuckoo filter in byte array.
     *
     * @param oneRow the fingerprints represented by {@code byte[]}.
     * @return the fingerprints
     */
    public static long[] recoverFingerprint(FastCuckooFilterType type, byte[] oneRow){
        if(oneRow == null || oneRow.length == 0){
            return null;
        }
        int fingerprintByteLength = getFingerprintByteLength(type);
        assert oneRow.length % fingerprintByteLength == 0;
        int fingerprintNum = oneRow.length / fingerprintByteLength;
        return IntStream.range(0, fingerprintNum)
            .mapToLong(i -> LongUtils.fixedByteArrayToLong(Arrays.copyOfRange(oneRow, i * fingerprintByteLength, (i + 1) * fingerprintByteLength)))
            .toArray();
    }

    /**
     * Creates an empty fast cuckoo filter.
     *
     * @param maxSize  max number of elements.
     * @param hashSeed hash seed.
     * @return an empty fast cuckoo filter.
     */
    public static <X> FastCuckooFilter<X> createCuckooFilter(FastCuckooFilterType type, int maxSize, long hashSeed) {
        return switch (type) {
            case MOBILE_FAST_VACUUM_FILTER -> MobileFastVacuumFilter.create(maxSize, hashSeed);
            case NAIVE_FAST_VACUUM_FILTER -> NaiveFastVacuumFilter.create(maxSize, hashSeed);
            case MOBILE_FAST_CUCKOO_FILTER -> MobileFastCuckooFilter.create(maxSize, hashSeed);
            case NAIVE_FAST_CUCKOO_FILTER -> NaiveFastCuckooFilter.create(maxSize, hashSeed);
        };
    }

    /**
     * Creates a fast cuckoo filter position.
     *
     * @param type     fast cuckoo filter type.
     * @param maxSize  max number of elements.
     * @param hashSeed hash seed.
     * @return a fast cuckoo filter position.
     */
    public static <X> FastCuckooFilterPosition<X> createCuckooFilterPosition(FastCuckooFilterType type, int maxSize, long hashSeed) {
        return switch (type) {
            case MOBILE_FAST_VACUUM_FILTER -> new MobileFastVacuumFilterPosition<>(maxSize, hashSeed);
            case NAIVE_FAST_VACUUM_FILTER -> new NaiveFastVacuumFilterPosition<>(maxSize, hashSeed);
            case MOBILE_FAST_CUCKOO_FILTER -> new MobileFastCuckooFilterPosition<>(maxSize, hashSeed);
            case NAIVE_FAST_CUCKOO_FILTER -> new NaiveFastCuckooFilterPosition<>(maxSize, hashSeed);
        };
    }

    /**
     * Loads the fast cuckoo filter from {@code List<byte[]>}.
     *
     * @param byteArrayList the {@code List<byte[]>}.
     * @return the fast cuckoo filter.
     */
    public static <X> FastCuckooFilter<X> loadCuckooFilter(List<byte[]> byteArrayList) {
        Preconditions.checkArgument(!byteArrayList.isEmpty());
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.get(0));
        FastCuckooFilterType type = FastCuckooFilterType.values()[typeOrdinal];
        return switch (type) {
            case MOBILE_FAST_VACUUM_FILTER -> MobileFastVacuumFilter.load(byteArrayList);
            case NAIVE_FAST_VACUUM_FILTER -> NaiveFastVacuumFilter.load(byteArrayList);
            case MOBILE_FAST_CUCKOO_FILTER -> MobileFastCuckooFilter.load(byteArrayList);
            case NAIVE_FAST_CUCKOO_FILTER -> NaiveFastCuckooFilter.load(byteArrayList);
        };
    }
}
