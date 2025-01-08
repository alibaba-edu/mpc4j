package edu.alibaba.mpc4j.common.structure.rcfilter;

import edu.alibaba.mpc4j.common.structure.rcfilter.RandomCuckooFilterFactory.RandomCuckooFilterType;

/**
 * Random Cuckoo filter positions.
 *
 * @author Weiran Liu
 * @date 2024/11/6
 */
public interface RandomCuckooFilterPosition {
    /**
     * Gets random cuckoo filter type.
     *
     * @return random cuckoo filter type.
     */
    RandomCuckooFilterType getType();

    /**
     * Gets positions for the input data. If data is in the Cuckoo filter, then it must be in the returned position.
     *
     * @param data input data.
     * @return positions.
     */
    int[] positions(long data);

    /**
     * Gets fingerprint for the input data.
     *
     * @param data input data.
     * @return fingerprint.
     */
    long fingerprint(long data);

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
