package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.structure.fastfilter.FastCuckooFilterFactory.FastCuckooFilterType;

/**
 * mobile fast cuckoo filter position.
 *
 * @author Weiran Liu
 * @date 2024/11/7
 */
public class MobileFastCuckooFilterPosition<T> extends AbstractFastCuckooFilterPosition<T> {
    /**
     * fast cuckoo filter type
     */
    static final FastCuckooFilterType TYPE = FastCuckooFilterType.MOBILE_FAST_CUCKOO_FILTER;
    /**
     *  b = 3.
     */
    static final int ENTRIES_PER_BUCKET = 3;
    /**
     * v = 32.
     */
    static final int FINGERPRINT_BIT_LENGTH = 32;

    /**
     * Gets the bucket num, must be in format 2^k.
     *
     * @param maxSize number of elements.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize) {
        return AbstractFastCuckooFilterPosition.getBucketNum(maxSize, ENTRIES_PER_BUCKET);
    }

    public MobileFastCuckooFilterPosition(int maxSize, long hashSeed) {
        super(maxSize, hashSeed, ENTRIES_PER_BUCKET, FINGERPRINT_BIT_LENGTH);
    }

    @Override
    public FastCuckooFilterType getType() {
        return TYPE;
    }
}
