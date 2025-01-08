package edu.alibaba.mpc4j.common.structure.rcfilter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.util.List;

/**
 * random cuckoo filter factory.
 *
 * @author Weiran Liu
 * @date 2024/11/6
 */
public class RandomCuckooFilterFactory {
    /**
     * random cuckoo filter type.
     */
    public enum RandomCuckooFilterType {
        /**
         * Naive Vacuum Filter
         */
        NAIVE_VACUUM_FILTER,
        /**
         * Mobile Vacuum Filter
         */
        MOBILE_VACUUM_FILTER,
    }

    /**
     * Gets bucket num.
     *
     * @param type    random cuckoo filter type.
     * @param maxSize max size.
     * @return bucket num.
     */
    public static int getBucketNum(RandomCuckooFilterType type, int maxSize) {
        return switch (type) {
            case NAIVE_VACUUM_FILTER -> NaiveRandomVacuumFilter.getBucketNum(maxSize);
            case MOBILE_VACUUM_FILTER -> MobileRandomVacuumFilter.getBucketNum(maxSize);
        };
    }

    /**
     * Gets entries per bucket.
     *
     * @param type random cuckoo filter type.
     * @return entries per bucket.
     */
    public static int getEntriesPerBucket(RandomCuckooFilterType type) {
        return switch (type) {
            case NAIVE_VACUUM_FILTER -> NaiveRandomVacuumFilter.ENTRIES_PER_BUCKET;
            case MOBILE_VACUUM_FILTER -> MobileRandomVacuumFilter.ENTRIES_PER_BUCKET;
        };
    }

    /**
     * Gets fingerprint byte length.
     *
     * @param type random cuckoo filter type.
     * @return fingerprint byte length.
     */
    public static int getFingerprintByteLength(RandomCuckooFilterType type) {
        return switch (type) {
            case NAIVE_VACUUM_FILTER -> NaiveRandomVacuumFilter.FINGERPRINT_BYTE_LENGTH;
            case MOBILE_VACUUM_FILTER -> MobileRandomVacuumFilter.FINGERPRINT_BYTE_LENGTH;
        };
    }

    /**
     * Gets estimate byte size.
     *
     * @param type random cuckoo filter type.
     * @return estimate byte size.
     */
    public static long estimateByteSize(RandomCuckooFilterType type, int maxSize) {
        int bucketNum = getBucketNum(type, maxSize);
        int fingerprintByteLength = getFingerprintByteLength(type);
        int entriesPerBucket = getEntriesPerBucket(type);
        switch (type) {
            case NAIVE_VACUUM_FILTER -> {
                long storageByteLength = ((long) bucketNum) * entriesPerBucket * fingerprintByteLength;
                return Integer.BYTES * 3 + storageByteLength;
            }
            case MOBILE_VACUUM_FILTER -> {
                int bitmapByteNum = CommonUtils.getByteLength(entriesPerBucket * bucketNum);
                long storageByteLength = (long) maxSize * fingerprintByteLength;
                return Integer.BYTES * 3 + bitmapByteNum + storageByteLength;
            }
            case null, default ->
                throw new IllegalArgumentException("Invalid " + RandomCuckooFilterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates an empty random cuckoo filter.
     *
     * @param maxSize max number of elements.
     * @return an empty random cuckoo filter.
     */
    public static RandomCuckooFilter createCuckooFilter(RandomCuckooFilterType type, int maxSize) {
        return switch (type) {
            case NAIVE_VACUUM_FILTER -> NaiveRandomVacuumFilter.create(maxSize);
            case MOBILE_VACUUM_FILTER -> MobileRandomVacuumFilter.create(maxSize);
        };
    }

    /**
     * Creates a random cuckoo filter position.
     *
     * @param type    random cuckoo filter type.
     * @param maxSize max number of elements.
     * @return a random cuckoo filter position.
     */
    public static RandomCuckooFilterPosition createCuckooFilterPosition(RandomCuckooFilterType type, int maxSize) {
        return switch (type) {
            case NAIVE_VACUUM_FILTER -> new NaiveRandomVacuumFilterPosition(maxSize);
            case MOBILE_VACUUM_FILTER -> new MobileRandomVacuumFilterPosition(maxSize);
        };
    }

    /**
     * Loads the random cuckoo filter from {@code List<byte[]>}.
     *
     * @param byteArrayList the {@code List<byte[]>}.
     * @return the random filter.
     */
    public static RandomCuckooFilter loadCuckooFilter(List<byte[]> byteArrayList) {
        Preconditions.checkArgument(!byteArrayList.isEmpty());
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.get(0));
        RandomCuckooFilterType type = RandomCuckooFilterType.values()[typeOrdinal];
        return switch (type) {
            case NAIVE_VACUUM_FILTER -> NaiveRandomVacuumFilter.load(byteArrayList);
            case MOBILE_VACUUM_FILTER -> MobileRandomVacuumFilter.load(byteArrayList);
        };
    }
}
