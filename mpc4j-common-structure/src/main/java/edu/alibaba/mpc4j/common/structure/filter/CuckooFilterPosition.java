package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;

import java.nio.ByteBuffer;

/**
 * Cuckoo filter positions.
 *
 * @author Weiran Liu
 * @date 2024/9/24
 */
public interface CuckooFilterPosition<T> {
    /**
     * Gets filter type.
     *
     * @return filter type.
     */
    FilterType getFilterType();
    /**
     * Gets cuckoo filter type.
     *
     * @return cuckoo filter type.
     */
    CuckooFilterType getCuckooFilterType();

    /**
     * Gets positions for the input data. If data is in the Cuckoo filter, then it must be in the returned position.
     *
     * @param data input data.
     * @return positions.
     */
    int[] positions(T data);

    /**
     * Gets fingerprint for the input data.
     *
     * @param data input data.
     * @return fingerprint.
     */
    ByteBuffer fingerprint(T data);

    /**
     * Gets max size.
     *
     * @return max size.
     */
    int maxSize();

    /**
     * Gets number of entries per bucket.
     *
     * @return number of entries per bucket.
     */
    int getEntriesPerBucket();

    /**
     * Gets byte length of fingerprint.
     *
     * @return byte length of fingerprint.
     */
    int getFingerprintByteLength();

    /**
     * Gets number of buckets.
     *
     * @return number of buckets.
     */
    int getBucketNum();
}
