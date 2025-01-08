package edu.alibaba.mpc4j.common.structure.fastfilter;

import edu.alibaba.mpc4j.common.structure.fastfilter.FastCuckooFilterFactory.FastCuckooFilterType;

/**
 * naive fast vacuum filter position.
 *
 * @author Weiran Liu
 * @date 2024/11/8
 */
public class NaiveFastVacuumFilterPosition<T> extends AbstractFastVacuumFilterPosition<T> {
    /**
     * fast cuckoo filter type
     */
    static final FastCuckooFilterType TYPE = FastCuckooFilterType.NAIVE_FAST_VACUUM_FILTER;
    /**
     * α, number of elements in buckets / total number of elements. The default value is 95.5%, see Table 2.
     */
    static final double LOAD_FACTOR = 0.955;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 40, the result is 40 + 3 = 43, we round to 48, see Table 2.
     */
    static final int FINGERPRINT_BIT_LENGTH = 42;

    /**
     * Gets bucket num.
     *
     * @param maxSize max size.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize) {
        return AbstractFastVacuumFilterPosition.getBucketNum(maxSize, LOAD_FACTOR);
    }

    public NaiveFastVacuumFilterPosition(int maxSize, long hashSeed) {
        super(maxSize, hashSeed, FINGERPRINT_BIT_LENGTH, LOAD_FACTOR);
    }

    @Override
    public FastCuckooFilterType getType() {
        return TYPE;
    }
}
