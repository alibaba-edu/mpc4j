package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * mobile cuckoo filter position.
 *
 * @author Weiran Liu
 * @date 2024/9/24
 */
class MobileCuckooFilterPosition<T> extends AbstractCuckooFilterPosition<T> {
    /**
     * filter type
     */
    static final FilterType FILTER_TYPE = FilterType.MOBILE_CUCKOO_FILTER;
    /**
     * cuckoo filter type
     */
    static final CuckooFilterType CUCKOO_FILTER_TYPE = CuckooFilterType.MOBILE_CUCKOO_FILTER;
    /**
     * number of entries in each bucket b, mobile cuckoo filter sets b = 3.
     */
    static final int ENTRIES_PER_BUCKET = 3;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since b = 3, log_2(1/ε) = σ = 29, the result is 29 + 3 = 32.
     */
    static final int FINGERPRINT_BYTE_LENGTH = 32 / Byte.SIZE;
    /**
     * α, number of elements in buckets / total number of elements, mobile filter sets α = 0.66.
     */
    static final double LOAD_FACTOR = 0.66;

    /**
     * Gets the bucket num, must be in format 2^k.
     *
     * @param maxSize number of elements.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize) {
        return AbstractCuckooFilterPosition.getBucketNum(maxSize, LOAD_FACTOR, ENTRIES_PER_BUCKET);
    }

    public MobileCuckooFilterPosition(EnvType envType, int maxSize, byte[][] keys) {
        super(envType, maxSize, keys, ENTRIES_PER_BUCKET, FINGERPRINT_BYTE_LENGTH, LOAD_FACTOR);
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
