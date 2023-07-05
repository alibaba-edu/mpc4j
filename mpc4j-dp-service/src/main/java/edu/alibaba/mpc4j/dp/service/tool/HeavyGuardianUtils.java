package edu.alibaba.mpc4j.dp.service.tool;

import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

/**
 * heavy guardian utils.
 *
 * @author Weiran Liu
 * @date 2023/6/13
 */
public class HeavyGuardianUtils {
    /**
     * private constructor.
     */
    private HeavyGuardianUtils() {
        // empty
    }

    /**
     * Gets item bucket.
     *
     * @param intHash int hash function.
     * @param w bucket num.
     * @param item item.
     * @return item bucket.
     */
    public static int getItemBucket(IntHash intHash, int w, String item) {
        if (w == 1) {
            return 0;
        } else {
            return Math.abs(intHash.hash(ObjectUtils.objectToByteArray(item)) % w);
        }
    }
}
