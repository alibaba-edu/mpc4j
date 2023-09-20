package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * no-stash cuckoo hash bin utilities. We use regression method to obtain the parameters. Although the regression results
 * show that h = 4 and h = 5 can have small bin num, i.e.,
 * <p>h = 3: ε = 23.584n^{-0.613} + 4.0 / n </p>
 * <p>h = 4: ε = 7.4258n^{-0.410} + 4.0 / n </p>
 * <p>h = 5: ε = 4.8187n^{-0.337} + 2.0 / n </p>
 * practical tests show that the estimated bin num is not correct.
 * Therefore, we use the same parameters for all cases.
 *
 * @author Weiran Liu
 * @date 2023/8/2
 */
public class NoStashCuckooHashBinUtils {
    /**
     * private constructor.
     */
    private NoStashCuckooHashBinUtils() {
        // empty
    }

    /**
     * Gets ε for small item size for 3 hashes.
     *
     * @param maxItemSize number of items.
     * @return ε.
     */
    public static double getH3SmallItemSizeEpsilon(int maxItemSize) {
        MathPreconditions.checkPositive("maxItemSize", maxItemSize);
        // h = 3: ε = 23.584n^{-0.613} + 4.0 / n
        return 23.584 * Math.pow(maxItemSize, -0.613) + 4.0 / maxItemSize;
    }

    /**
     * Gets ε for small item size for 4 hashes.
     *
     * @param maxItemSize number of items.
     * @return ε.
     */
    public static double getH4SmallItemSizeEpsilon(int maxItemSize) {
        MathPreconditions.checkPositive("maxItemSize", maxItemSize);
        // h = 4: ε = 7.4258n^{-0.410} + 4.0 / n. In practice, we use parameters from h = 3.
        return 23.584 * Math.pow(maxItemSize, -0.613) + 4.0 / maxItemSize;
    }

    /**
     * Gets ε for small item size for 5 hashes.
     *
     * @param maxItemSize maxItemSize number of items.
     * @return ε.
     */
    public static double getH5SmallItemSizeEpsilon(int maxItemSize) {
        MathPreconditions.checkPositive("maxItemSize", maxItemSize);
        // h = 5: ε = 4.8187n^{-0.337} + 2.0 / n. In practice, we use parameters from h = 3.
        return 23.584 * Math.pow(maxItemSize, -0.613) + 4.0 / maxItemSize;
    }
}
