package edu.alibaba.mpc4j.common.structure.okve.dokvs;

import edu.alibaba.mpc4j.common.structure.okve.OkveHashUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * distinct GBF utilities.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
public class DistinctGbfUtils {
    /**
     * private constructor
     */
    private DistinctGbfUtils() {
        // empty
    }

    /**
     * sparse hash num
     */
    public static int SPARSE_HASH_NUM = CommonConstants.STATS_BIT_LENGTH;
    /**
     * we only need to use one hash key
     */
    public static final int HASH_KEY_NUM = 1;

    /**
     * Gets m for the given n.
     *
     * @param n number of key-value pairs.
     * @return m.
     */
    public static int getM(int n) {
        MathPreconditions.checkPositive("n", n);
        // m = n / ln(2) * σ, flooring so that m % Byte.SIZE = 0.
        return CommonUtils.getByteLength((int) Math.ceil(n * CommonConstants.STATS_BIT_LENGTH / Math.log(2))) * Byte.SIZE;
    }

    /**
     * Computes sparse positions for the given key.
     *
     * @param hash hash function.
     * @param key  key.
     * @param m    sparse position bound.
     * @param <X>  key type.
     * @return sparse positions for the given key.
     */
    public static <X> int[] sparsePositions(Prf hash, X key, int m) {
        return OkveHashUtils.distinctPositions(hash, key, SPARSE_HASH_NUM, m);
    }
}
