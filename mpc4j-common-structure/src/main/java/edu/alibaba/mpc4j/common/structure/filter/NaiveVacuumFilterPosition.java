package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * vacuum filter position.
 *
 * @author Weiran Liu
 * @date 2024/9/24
 */
class NaiveVacuumFilterPosition<T> extends AbstractVacuumFilterPosition<T> {
    /**
     * filter type
     */
    static final FilterType FILTER_TYPE = FilterType.NAIVE_VACUUM_FILTER;
    /**
     * cuckoo filter type
     */
    static final CuckooFilterType CUCKOO_FILTER_TYPE = CuckooFilterType.NAIVE_VACUUM_FILTER;
    /**
     * α, number of elements in buckets / total number of elements. The default value is 95.5%, see Table 2.
     */
    static final double LOAD_FACTOR = 0.955;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = 40, the result is 40 + 3 = 43, we round to 48, see Table 2.
     */
    static final int FINGERPRINT_BYTE_LENGTH = 6;

    /**
     * Gets bucket num.
     *
     * @param maxSize max size.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize) {
        return AbstractVacuumFilter.getBucketNum(maxSize, LOAD_FACTOR);
    }

    public NaiveVacuumFilterPosition(EnvType envType, int maxSize, byte[][] keys) {
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
