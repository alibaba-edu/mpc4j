package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.CuckooFilterFactory.CuckooFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * naive cuckoo filter position.
 *
 * @author Weiran Liu
 * @date 2024/9/24
 */
class NaiveCuckooFilterPosition<T> extends AbstractCuckooFilterPosition<T> {
    /**
     * cuckoo filter type
     */
    static final FilterType FILTER_TYPE = FilterType.NAIVE_CUCKOO_FILTER;
    /**
     * cuckoo filter type
     */
    static final CuckooFilterType CUCKOO_FILTER_TYPE = CuckooFilterType.NAIVE_CUCKOO_FILTER;
    /**
     * number of entries in each bucket. The default value is 4.
     */
    static final int ENTRIES_PER_BUCKET = 4;
    /**
     * byte length for each fingerprint, computed as (log_2(1/ε) + log_2(2 * ENTRIES_PER_BUCKET)).
     * Since ENTRIES_PER_BUCKET = 4, log_2(1/ε) = σ = 40, the result is 40 + 3 = 43, we round to 48, see Table 2.
     */
    static final int FINGERPRINT_BYTE_LENGTH = 48 / Byte.SIZE;
    /**
     * α, number of elements in buckets / total number of elements. The default value is 95.5%, see Table 2.
     */
    static final double LOAD_FACTOR = 0.955;

    /**
     * Gets the bucket num, must be in format 2^k.
     *
     * @param maxSize number of elements.
     * @return bucket num.
     */
    static int getBucketNum(int maxSize) {
        return AbstractCuckooFilterPosition.getBucketNum(maxSize, LOAD_FACTOR, ENTRIES_PER_BUCKET);
    }

    public NaiveCuckooFilterPosition(EnvType envType, int maxSize, byte[][] keys) {
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
