package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.structure.fastfilter.FastCuckooFilterFactory.FastCuckooFilterType;

/**
 * mobile fast vacuum filter position.
 *
 * @author Weiran Liu
 * @date 2024/11/8
 */
public class MobileFastVacuumFilterPosition<T> extends AbstractFastVacuumFilterPosition<T> {
    /**
     * fast cuckoo filter type
     */
    static final FastCuckooFilterType TYPE = FastCuckooFilterType.MOBILE_FAST_VACUUM_FILTER;
    /**
     * α, number of elements in buckets / total number of elements. Experiments show that we cannot set α = 0.66 since
     * there will be non-negligible failure probabilities. We have to set α = 0.955.
     */
    static final double LOAD_FACTOR = 0.667;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 29, the result is 29 + 3 = 32.
     */
    static final int FINGERPRINT_BIT_LENGTH = 32;

    /**
     * Gets bucket num.
     *
     * @param maxSize max size.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize) {
        return AbstractFastVacuumFilterPosition.getBucketNum(maxSize, LOAD_FACTOR);
    }

    public MobileFastVacuumFilterPosition(int maxSize, long hashSeed) {
        super(maxSize, hashSeed, FINGERPRINT_BIT_LENGTH, LOAD_FACTOR);
    }

    @Override
    public FastCuckooFilterType getType() {
        return TYPE;
    }
}
