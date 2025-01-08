package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Mobile Vacuum Filter position.
 *
 * @author Weiran Liu
 * @date 2024/9/24
 */
public class MobileVacuumFilterPosition<T> extends AbstractVacuumFilterPosition<T> {
    /**
     * filter type
     */
    static final FilterType FILTER_TYPE = FilterType.MOBILE_VACUUM_FILTER;
    /**
     * cuckoo filter type
     */
    static final CuckooFilterType CUCKOO_FILTER_TYPE = CuckooFilterType.MOBILE_VACUUM_FILTER;
    /**
     * α, number of elements in buckets / total number of elements. Experiments show that we cannot set α = 0.66 since
     * there will be non-negligible failure probabilities. We have to set α = 0.955.
     */
    static final double LOAD_FACTOR = 0.955;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 29, the result is 29 + 3 = 32.
     */
    static final int FINGERPRINT_BYTE_LENGTH = 32 / Byte.SIZE;

    /**
     * Gets bucket num.
     *
     * @param maxSize max size.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize) {
        return AbstractVacuumFilterPosition.getBucketNum(maxSize, LOAD_FACTOR);
    }

    public MobileVacuumFilterPosition(EnvType envType, int maxSize, byte[][] keys) {
        super(envType, maxSize, keys, FINGERPRINT_BYTE_LENGTH, LOAD_FACTOR);
    }

    @Override
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public CuckooFilterType getCuckooFilterType() {
        return CUCKOO_FILTER_TYPE;
    }
}
