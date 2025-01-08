package edu.alibaba.mpc4j.common.structure.rcfilter;

/**
 * hash utilities.
 *
 * @author Weiran Liu
 * @date 2024/11/6
 */
class RandomCuckooFilterHashUtils {
    /**
     * private constructor.
     */
    private RandomCuckooFilterHashUtils() {
        // empty
    }

    static long murmurHash64(long h)
    {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
