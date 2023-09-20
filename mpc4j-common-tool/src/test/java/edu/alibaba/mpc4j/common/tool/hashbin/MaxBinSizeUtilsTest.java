package edu.alibaba.mpc4j.common.tool.hashbin;

import org.junit.Assert;
import org.junit.Test;

/**
 * max bin size utilities test.
 *
 * @author Weiran Liu
 * @date 2021/12/16
 */
public class MaxBinSizeUtilsTest {
    /**
     * min(log_2(n))
     */
    private static final int MIN_LOG_N = 0;
    /**
     * max(log_2(n))
     */
    private static final int MAX_LOG_N = 10;
    /**
     * min(log_2(b))
     */
    private static final int MIN_LOG_B = 0;
    /**
     * max(log_2(b))
     */
    private static final int MAX_LOG_B = 10;
    /**
     * n in {2^0, 2^1, ..., 2^10}, b in {2^0, 2^1, ..., 2^9}, we use C/C++ version to compute the correct results.
     * Note that for most result, Java version = C/C++ version + 1, this is because Java use more accurate computation.
     */
    private static final int[][] EXACT_K_ARRAY = new int[][]{
        // n = 2^0
        new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,},
        // n = 2^1
        new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,},
        // n = 2^2,
        new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,},
        // n = 2^3
        new int[]{8, 8, 8, 8, 8, 8, 8, 8, 7, 6, 6,},
        // n = 2^4
        new int[]{16, 16, 16, 16, 14, 12, 10, 9, 8, 7, 7,},
        // n = 2^5
        new int[]{32, 32, 28, 23, 18, 15, 13, 11, 10, 9, 8,},
        // n = 2^6
        new int[]{64, 59, 45, 33, 25, 20, 16, 13, 11, 10, 9,},
        // n = 2^7
        new int[]{128, 104, 71, 50, 36, 27, 21, 17, 14, 12, 10,},
        // n = 2^8
        new int[]{256, 185, 118, 77, 52, 37, 27, 21, 17, 14, 12,},
        // n = 2^9
        new int[]{512, 337, 204, 126, 81, 54, 38, 28, 22, 17, 14,},
        // n = 2^10
        new int[]{1024, 627, 361, 213, 130, 83, 55, 39, 29, 22, 18,},
    };

    @Test
    public void testExactMaxBinSize() {
        for (int logN = MIN_LOG_N; logN <= MAX_LOG_N; logN++) {
            for (int logB = MIN_LOG_B; logB <= MAX_LOG_B; logB++) {
                int n = 1 << logN;
                int b = 1 << logB;
                int exactK = MaxBinSizeUtils.exactMaxBinSize(n, b);
                Assert.assertEquals(EXACT_K_ARRAY[logN][logB], exactK);
            }
        }
    }

    /**
     * n in {2^0, 2^1, ..., 2^10}, b in {2^0, 2^1, ..., 2^9}, we use C/C++ version to compute the correct results.
     * Note that for most result, Java version = C/C++ version + 1, this is because Java use more accurate computation.
     */
    private static final int[][] APPROX_K_ARRAY = new int[][]{
        // n = 2^0
        new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,},
        // n = 2^1
        new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,},
        // n = 2^2,
        new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,},
        // n = 2^3
        new int[]{8, 8, 8, 8, 8, 8, 7, 7, 7, 6, 6,},
        // n = 2^4
        new int[]{16, 16, 16, 16, 13, 12, 9, 8, 7, 7, 7,},
        // n = 2^5
        new int[]{32, 32, 27, 23, 17, 14, 13, 11, 9, 8, 7,},
        // n = 2^6
        new int[]{64, 58, 45, 32, 25, 19, 16, 13, 11, 9, 8,},
        // n = 2^7
        new int[]{128, 103, 71, 49, 36, 26, 21, 16, 13, 12, 9,},
        // n = 2^8
        new int[]{256, 184, 118, 76, 52, 36, 26, 21, 16, 13, 12,},
        // n = 2^9
        new int[]{512, 337, 203, 126, 81, 54, 38, 27, 22, 16, 13,},
        // n = 2^10
        new int[]{1024, 627, 360, 213, 129, 83, 54, 38, 28, 22, 17,},
    };

    @Test
    public void testApproxMaxBinSize() {
        for (int logN = MIN_LOG_N; logN <= MAX_LOG_N; logN++) {
            for (int logB = MIN_LOG_B; logB <= MAX_LOG_B; logB++) {
                int n = 1 << logN;
                int b = 1 << logB;
                int approxK = MaxBinSizeUtils.approxMaxBinSize(n, b);
                Assert.assertEquals(APPROX_K_ARRAY[logN][logB], approxK);
            }
        }
    }

    @Test
    public void testExpectMaxBinSize() {
        for (int logN = MIN_LOG_N; logN <= MAX_LOG_N; logN++) {
            for (int logB = MIN_LOG_B; logB <= MAX_LOG_B; logB++) {
                int n = 1 << logN;
                int b = 1 << logB;
                int expectK = MaxBinSizeUtils.expectMaxBinSize(n, b);
                Assert.assertTrue(expectK >= EXACT_K_ARRAY[logN][logB]);
                Assert.assertTrue(expectK >= APPROX_K_ARRAY[logN][logB]);
            }
        }
    }

}
