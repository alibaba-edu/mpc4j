package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.util.List;

/**
 * Cuckoo Filter Factory.
 *
 * @author Weiran Liu
 * @date 2024/9/19
 */
public class CuckooFilterFactory {
    /**
     * private constructor.
     */
    private CuckooFilterFactory() {
        // empty
    }

    /**
     * Cuckoo Filter type.
     */
    public enum CuckooFilterType {
        /**
         * Naive Cuckoo Filter
         */
        NAIVE_CUCKOO_FILTER,
        /**
         * Mobile Cuckoo Filter
         */
        MOBILE_CUCKOO_FILTER,
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
     * @param cuckooFilterType Cuckoo Filter type.
     * @param maxSize          max size.
     * @return bucket num.
     */
    public static int getBucketNum(CuckooFilterType cuckooFilterType, int maxSize) {
        return switch (cuckooFilterType) {
            case NAIVE_CUCKOO_FILTER -> NaiveCuckooFilterPosition.getBucketNum(maxSize);
            case MOBILE_CUCKOO_FILTER -> MobileCuckooFilterPosition.getBucketNum(maxSize);
            case NAIVE_VACUUM_FILTER -> NaiveVacuumFilterPosition.getBucketNum(maxSize);
            case MOBILE_VACUUM_FILTER -> MobileVacuumFilterPosition.getBucketNum(maxSize);
        };
    }

    /**
     * Gets entries per bucket.
     *
     * @param cuckooFilterType Cuckoo Filter type.
     * @return entries per bucket.
     */
    public static int getEntriesPerBucket(CuckooFilterType cuckooFilterType) {
        return switch (cuckooFilterType) {
            case NAIVE_CUCKOO_FILTER -> NaiveCuckooFilterPosition.ENTRIES_PER_BUCKET;
            case MOBILE_CUCKOO_FILTER -> MobileCuckooFilterPosition.ENTRIES_PER_BUCKET;
            case NAIVE_VACUUM_FILTER, MOBILE_VACUUM_FILTER -> AbstractVacuumFilterPosition.ENTRIES_PER_BUCKET;
        };
    }

    /**
     * Gets fingerprint byte length.
     *
     * @param cuckooFilterType Cuckoo Filter type.
     * @return fingerprint byte length.
     */
    public static int getFingerprintByteLength(CuckooFilterType cuckooFilterType) {
        return switch (cuckooFilterType) {
            case NAIVE_CUCKOO_FILTER -> NaiveCuckooFilterPosition.FINGERPRINT_BYTE_LENGTH;
            case MOBILE_CUCKOO_FILTER -> MobileCuckooFilterPosition.FINGERPRINT_BYTE_LENGTH;
            case NAIVE_VACUUM_FILTER -> NaiveVacuumFilter.FINGERPRINT_BYTE_LENGTH;
            case MOBILE_VACUUM_FILTER -> MobileVacuumFilter.FINGERPRINT_BYTE_LENGTH;
        };
    }

    /**
     * Gets estimate byte size.
     *
     * @param cuckooFilterType Cuckoo Filter type.
     * @return estimate byte size.
     */
    public static long estimateByteSize(CuckooFilterType cuckooFilterType, int maxSize) {
        int bucketNum = getBucketNum(cuckooFilterType, maxSize);
        int fingerprintByteLength = getFingerprintByteLength(cuckooFilterType);
        int entriesPerBucket = getEntriesPerBucket(cuckooFilterType);
        switch (cuckooFilterType) {
            case NAIVE_CUCKOO_FILTER, NAIVE_VACUUM_FILTER -> {
                long storageByteLength = ((long) bucketNum) * entriesPerBucket * fingerprintByteLength;
                return Integer.BYTES * 4 + CommonConstants.BLOCK_BYTE_LENGTH * 2 + storageByteLength;
            }
            case MOBILE_CUCKOO_FILTER, MOBILE_VACUUM_FILTER -> {
                int bitmapByteNum = CommonUtils.getByteLength(entriesPerBucket * bucketNum);
                long storageByteLength = (long) maxSize * fingerprintByteLength;
                return Integer.BYTES * 4 + CommonConstants.BLOCK_BYTE_LENGTH * 2 + bitmapByteNum + storageByteLength;
            }
            case null, default ->
                throw new IllegalArgumentException("Invalid " + CuckooFilterType.class.getSimpleName() + ": " + cuckooFilterType);
        }
    }

    /**
     * Creates an empty cuckoo filter.
     *
     * @param envType          environment.
     * @param cuckooFilterType cuckoo filter type.
     * @param maxSize          max number of elements.
     * @param keys             keys.
     * @param <X>              type.
     * @return an empty cuckoo filter.
     */
    public static <X> CuckooFilter<X> createCuckooFilter(EnvType envType, CuckooFilterType cuckooFilterType,
                                                         int maxSize, byte[][] keys) {
        return switch (cuckooFilterType) {
            case NAIVE_CUCKOO_FILTER -> NaiveCuckooFilter.create(envType, maxSize, keys);
            case MOBILE_CUCKOO_FILTER -> MobileCuckooFilter.create(envType, maxSize, keys);
            case NAIVE_VACUUM_FILTER -> NaiveVacuumFilter.create(envType, maxSize, keys);
            case MOBILE_VACUUM_FILTER -> MobileVacuumFilter.create(envType, maxSize, keys);
        };
    }

    /**
     * Creates a cuckoo filter position.
     *
     * @param envType          environment.
     * @param cuckooFilterType cuckoo filter type.
     * @param maxSize          max number of elements.
     * @param keys             keys.
     * @param <X>              type.
     * @return a cuckoo filter position.
     */
    public static <X> CuckooFilterPosition<X> createCuckooFilterPosition(EnvType envType, CuckooFilterType cuckooFilterType,
                                                                         int maxSize, byte[][] keys) {
        return switch (cuckooFilterType) {
            case NAIVE_CUCKOO_FILTER -> new NaiveCuckooFilterPosition<>(envType, maxSize, keys);
            case MOBILE_CUCKOO_FILTER -> new MobileCuckooFilterPosition<>(envType, maxSize, keys);
            case NAIVE_VACUUM_FILTER -> new NaiveVacuumFilterPosition<>(envType, maxSize, keys);
            case MOBILE_VACUUM_FILTER -> new MobileVacuumFilterPosition<>(envType, maxSize, keys);
        };
    }

    /**
     * Loads the cuckoo filter from {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the {@code List<byte[]>}.
     * @return the filter.
     */
    public static <X> CuckooFilter<X> loadCuckooFilter(EnvType envType, List<byte[]> byteArrayList) {
        Preconditions.checkArgument(!byteArrayList.isEmpty());
        int filterTypeOrdinal = IntUtils.byteArrayToInt(byteArrayList.get(0));
        FilterType filterType = FilterType.values()[filterTypeOrdinal];
        return switch (filterType) {
            case NAIVE_CUCKOO_FILTER -> NaiveCuckooFilter.load(envType, byteArrayList);
            case MOBILE_CUCKOO_FILTER -> MobileCuckooFilter.load(envType, byteArrayList);
            case NAIVE_VACUUM_FILTER -> NaiveVacuumFilter.load(envType, byteArrayList);
            case MOBILE_VACUUM_FILTER -> MobileVacuumFilter.load(envType, byteArrayList);
            default ->
                throw new IllegalArgumentException("Invalid " + FilterType.class.getSimpleName() + ": " + filterType);
        };
    }
}
