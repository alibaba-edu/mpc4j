package edu.alibaba.mpc4j.common.structure.okve.dokvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

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
        // check the output byte length of the hash function
        assert hash.getOutputByteLength() == SPARSE_HASH_NUM * Integer.BYTES;
        assert SPARSE_HASH_NUM <= m;
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] hashes = IntUtils.byteArrayToIntArray(hash.getBytes(keyBytes));
        // we now use the method provided in VOLE-PSI to get distinct hash indexes
        for (int j = 0; j < SPARSE_HASH_NUM; j++) {
            // hj = r % (m - j)
            int modulus = m - j;
            int hj = Math.abs(hashes[j] % modulus);
            int i = 0;
            int end = j;
            // for each previous hi <= hj, we set hj = hj + 1.
            while (i != end) {
                if (hashes[i] <= hj) {
                    hj++;
                } else {
                    break;
                }
                i++;
            }
            // now we now that all hi > hj, we place the value
            while (i != end) {
                hashes[end] = hashes[end - 1];
                end--;
            }
            hashes[i] = hj;
        }
        return hashes;
    }
}
