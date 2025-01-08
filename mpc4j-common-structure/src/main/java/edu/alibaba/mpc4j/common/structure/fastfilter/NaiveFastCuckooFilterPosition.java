package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.structure.fastfilter.FastCuckooFilterFactory.FastCuckooFilterType;

/**
 * naive fast cuckoo filter position.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
class NaiveFastCuckooFilterPosition<T> extends AbstractFastCuckooFilterPosition<T> {
    /**
     * fast cuckoo filter type.
     */
    static final FastCuckooFilterType TYPE = FastCuckooFilterType.NAIVE_FAST_CUCKOO_FILTER;
    /**
     * b = 4
     */
    static final int ENTRIES_PER_BUCKET = 4;
    /**
     * tag size v = 42
     */
    static final int FINGERPRINT_BIT_LENGTH = 42;

    static int getBucketNum(int maxSize) {
        return AbstractFastCuckooFilter.getBucketNum(maxSize, ENTRIES_PER_BUCKET);
    }

    public NaiveFastCuckooFilterPosition(int maxSize, long hashSeed) {
        super(maxSize, hashSeed, ENTRIES_PER_BUCKET, FINGERPRINT_BIT_LENGTH);
    }

    @Override
    public FastCuckooFilterType getType() {
        return TYPE;
    }
}
