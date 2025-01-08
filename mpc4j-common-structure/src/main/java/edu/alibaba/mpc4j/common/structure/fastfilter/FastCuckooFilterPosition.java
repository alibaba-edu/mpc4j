package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.structure.fastfilter.FastCuckooFilterFactory.FastCuckooFilterType;

/**
 * fast cuckoo filter position.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
public interface FastCuckooFilterPosition<T> {
    /**
     * Gets fast cuckoo filter type.
     *
     * @return filter type.
     */
    FastCuckooFilterType getType();

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
    long fingerprint(T data);

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
     * Gets bit length of fingerprint.
     *
     * @return bit length of fingerprint.
     */
    int getFingerprintBitLength();

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
