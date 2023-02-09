package edu.alibaba.mpc4j.dp.service.fo.rappor;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * RAPPOR Frequency Oracle LDP utilities.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class RapporFoLdpUtils {

    private RapporFoLdpUtils() {
        // empty
    }

    /**
     * Gets the size of the bloom filter.
     *
     * @param d the domain size.
     * @param hashNum the number of hashes.
     * @return the size of the bloom filter.
     */
    public static int getM(int d, int hashNum) {
        MathPreconditions.checkGreater("# of hashes", hashNum, 1);
        MathPreconditions.checkGreater("|Ω|", d, 1);
        // m = d · k / ln(2)
        return (int)Math.ceil(d * hashNum / Math.log(2));
    }
}
