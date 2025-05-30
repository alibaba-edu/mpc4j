package edu.alibaba.mpc4j.common.tool.hashbin;

import edu.alibaba.mpc4j.common.tool.utils.BigDecimalUtils;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * 哈希桶工具类。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
public class MaxBinSizeUtils {
    /**
     * private constructor.
     */
    private MaxBinSizeUtils() {
        // empty
    }

    /**
     * log2(bin size) of mapping n balls to m bins, for λ = 40 bit security. Row i has n=2^i bins, column j has m=2^j
     * balls.
     *
     * <p> This table comes from SimpleIndex.cpp. We can use for loop with logBin ∈ [0, 32], logBall ∈ [0, 30] to
     * compute <code>log2(get_bin_size(1L << logBin, 1L << logBall))</code> to get the table.
     *
     * <p> Here we correct table[5][20]. The correct value is 15.05816088 but the pre-computed value is 15.06149777.
     */
    private static final double[][] LOG_BIN_SIZE_TABLE = new double[][]{
        // 2^00 bins
        new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30},
        // 2^01 bins
        new double[]{0, 1, 2, 3, 4, 5, 5.857980995, 6.686500527, 7.523561956, 8.392317423, 9.290018847, 10.21067134, 11.15228484, 12.10950422, 13.07831762, 14.05579081, 15.03969019, 16.02818667, 17.01999243, 18.01416217, 19.01002812, 20.00709846, 21.00502277, 22.00355291, 23.00251306, 24.00177721, 25.00125665, 26.00088843, 27.00062802, 28.00044386, 29.00031363},
        // 2^02 bins
        new double[]{0, 1, 2, 3, 4, 4.754887502, 5.459431619, 6.129283017, 6.87036472, 7.665335917, 8.491853096, 9.361943774, 10.2632692, 11.18982456, 12.13602985, 13.09737377, 14.06936585, 15.04929552, 16.03499235, 17.02481502, 18.01757508, 19.01244722, 20.00880883, 21.00623293, 22.00440908, 23.00311846, 24.00220528, 25.00155925, 26.00110233, 27.00077917, 28.00055063},
        // 2^03 bins
        new double[]{0, 1, 2, 3, 3.906890596, 4.459431619, 5, 5.614709844, 6.247927513, 6.965784285, 7.727920455, 8.539158811, 9.394462695, 10.28771238, 11.20762447, 12.14879432, 13.10639936, 14.07581338, 15.05392588, 16.03827601, 17.02713927, 18.01923236, 19.0136171, 20.00963729, 21.00681914, 22.00482395, 23.00341171, 24.00241262, 25.00170588, 26.00120598, 27.00085241},
        // 2^04 bins
        new double[]{0, 1, 2, 3, 3.700439718, 4.087462841, 4.584962501, 5.129283017, 5.672425342, 6.321928095, 7.011227255, 7.761551232, 8.566054038, 9.413627929, 10.30149619, 11.21735191, 12.15608308, 13.11146174, 14.07948478, 15.05651071, 16.04011845, 17.0284457, 18.02015526, 19.01427116, 20.0101019, 21.00714706, 22.00505602, 23.0035759, 24.00252877, 25.00178798, 26.00126401},
        // 2^05 bins
        new double[]{0, 1, 2, 3, 3.459431619, 3.807354922, 4.247927513, 4.700439718, 5.169925001, 5.727920455, 6.357552005, 7.033423002, 7.781359714, 8.581200582, 9.426264755, 10.30947635, 11.22339841, 12.16050175, 13.11471839, 14.08173308, 15.05816088, 16.04127413, 17.02926566, 18.02074126, 19.01468524, 20.01039425, 21.00735446, 22.00520271, 23.00367969, 24.00260216, 25.0018399},
        // 2^06 bins
        new double[]{0, 1, 2, 2.807354922, 3.169925001, 3.584962501, 3.906890596, 4.321928095, 4.700439718, 5.209453366, 5.754887502, 6.375039431, 7.055282436, 7.794415866, 8.592457037, 9.434628228, 10.31514956, 11.22821744, 12.16364968, 13.11699368, 14.08339622, 15.05930221, 16.04210821, 17.02985876, 18.0211535, 19.01497939, 20.01060323, 21.00750229, 22.00530724, 23.00375379, 24.00265452},
        // 2^07 bins
        new double[]{0, 1, 2, 2.807354922, 3, 3.321928095, 3.584962501, 4, 4.321928095, 4.754887502, 5.247927513, 5.781359714, 6.392317423, 7.06608919, 7.807354922, 8.599912842, 9.440869168, 10.32080055, 11.23122118, 12.16616308, 13.11877889, 14.08472536, 15.06023151, 16.04277086, 17.03032228, 18.02148428, 19.0152163, 20.01076984, 21.00762, 22.00539086, 23.0038128},
        // 2^08 bins
        new double[]{0, 1, 2, 2.584962501, 2.807354922, 3.169925001, 3.321928095, 3.700439718, 4, 4.392317423, 4.807354922, 5.247927513, 5.807354922, 6.409390936, 7.076815597, 7.813781191, 8.607330314, 9.445014846, 10.32418055, 11.23421868, 12.16835873, 13.1203999, 14.08580438, 15.0610336, 16.04332639, 17.03073179, 18.02177162, 19.01541777, 20.01091459, 21.00772264, 22.00546316},
        // 2^09 bins
        new double[]{0, 1, 2, 2.321928095, 2.584962501, 3, 3.169925001, 3.459431619, 3.700439718, 4, 4.392317423, 4.807354922, 5.285402219, 5.807354922, 6.426264755, 7.087462841, 7.826548487, 8.614709844, 9.451211112, 10.32755264, 11.23720996, 12.17023805, 13.12185725, 14.08679969, 15.06175089, 16.04386035, 17.03109809, 18.02203723, 19.01560561, 20.01104704, 21.00781707},
        // 2^10 bins
        new double[]{0, 1, 2, 2.321928095, 2.584962501, 2.807354922, 3, 3.169925001, 3.459431619, 3.700439718, 4.087462841, 4.392317423, 4.807354922, 5.285402219, 5.832890014, 6.426264755, 7.098032083, 7.832890014, 8.618385502, 9.45532722, 10.33203655, 11.23959853, 12.17211493, 13.12315143, 14.0877943, 15.06246782, 16.04435143, 17.03145353, 18.02229195, 19.01578526, 20.01117265},
        // 2^11 bins
        new double[]{0, 1, 2, 2.321928095, 2.321928095, 2.584962501, 2.807354922, 3, 3.169925001, 3.459431619, 3.700439718, 4.087462841, 4.459431619, 4.857980995, 5.321928095, 5.832890014, 6.442943496, 7.108524457, 7.839203788, 8.625708843, 9.459431619, 10.33539035, 11.24198315, 12.17398937, 13.12444446, 14.08878824, 15.06314225, 16.04484233, 17.03179811, 18.02253579, 19.01595672},
        // 2^12 bins
        new double[]{0, 1, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.807354922, 3, 3.169925001, 3.459431619, 3.807354922, 4.087462841, 4.459431619, 4.857980995, 5.321928095, 5.857980995, 6.459431619, 7.118941073, 7.845490051, 8.62935662, 9.463524373, 10.33873638, 11.24436384, 12.17586138, 13.12573633, 14.08969874, 15.06381637, 16.04531174, 17.03213185, 18.02277416},
        // 2^13 bins
        new double[]{0, 1, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.321928095, 3.459431619, 3.807354922, 4.087462841, 4.459431619, 4.857980995, 5.357552005, 5.882643049, 6.459431619, 7.129283017, 7.851749041, 8.636624621, 9.46760555, 10.34207467, 11.2467406, 12.17741954, 13.12702704, 14.09060867, 15.06444807, 16.04575966, 17.0324655},
        // 2^14 bins
        new double[]{0, 1, 1.584962501, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.321928095, 3.584962501, 3.807354922, 4.169925001, 4.459431619, 4.906890596, 5.357552005, 5.882643049, 6.475733431, 7.139551352, 7.857980995, 8.64385619, 9.47370575, 10.34429591, 11.24911345, 12.1792871, 13.1283166, 14.09151803, 15.06507949, 16.04622877},
        // 2^15 bins
        new double[]{0, 1, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.807354922, 2.807354922, 3.169925001, 3.321928095, 3.584962501, 3.807354922, 4.169925001, 4.523561956, 4.906890596, 5.357552005, 5.906890596, 6.491853096, 7.14974712, 7.87036472, 8.647458426, 9.477758266, 10.34762137, 11.25148241, 12.18084157, 13.12944402, 14.09234422, 15.06571064},
        // 2^16 bins
        new double[]{0, 1, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.807354922, 3, 3.169925001, 3.321928095, 3.584962501, 3.906890596, 4.169925001, 4.523561956, 4.906890596, 5.392317423, 5.906890596, 6.491853096, 7.159871337, 7.876516947, 8.654636029, 9.481799432, 10.35093918, 11.25384748, 12.18270471, 13.13073143, 14.09325249},
        // 2^17 bins
        new double[]{0, 1, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.807354922, 3, 3.169925001, 3.321928095, 3.584962501, 3.906890596, 4.169925001, 4.523561956, 4.95419631, 5.392317423, 5.930737338, 6.50779464, 7.159871337, 7.882643049, 8.658211483, 9.485829309, 10.35424938, 11.25620869, 12.1842555, 13.13185696},
        // 2^18 bins
        new double[]{0, 1, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.169925001, 3.321928095, 3.584962501, 3.906890596, 4.169925001, 4.584962501, 4.95419631, 5.426264755, 5.930737338, 6.523561956, 7.169925001, 7.888743249, 8.665335917, 9.48984796, 10.357552, 11.25856603, 12.18580462},
        // 2^19 bins
        new double[]{0, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.169925001, 3.459431619, 3.700439718, 3.906890596, 4.247927513, 4.584962501, 4.95419631, 5.426264755, 5.95419631, 6.523561956, 7.17990909, 7.894817763, 8.668884984, 9.493855449, 10.35974956, 11.26091953},
        // 2^20 bins
        new double[]{0, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.169925001, 3.459431619, 3.700439718, 3.906890596, 4.247927513, 4.584962501, 5, 5.426264755, 5.95419631, 6.539158811, 7.189824559, 7.900866808, 8.675957033, 9.497851837, 10.36303963},
        // 2^21 bins
        new double[]{0, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.169925001, 3.459431619, 3.700439718, 4, 4.247927513, 4.584962501, 5, 5.459431619, 5.977279923, 6.554588852, 7.199672345, 7.906890596, 8.6794801, 9.501837185},
        // 2^22 bins
        new double[]{0, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.321928095, 2.584962501, 2.807354922, 2.807354922, 3, 3.321928095, 3.459431619, 3.700439718, 4, 4.247927513, 4.64385619, 5, 5.459431619, 5.977279923, 6.554588852, 7.209453366, 7.912889336, 8.682994584},
        // 2^23 bins
        new double[]{0, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.321928095, 2.584962501, 2.807354922, 2.807354922, 3, 3.321928095, 3.459431619, 3.700439718, 4, 4.321928095, 4.64385619, 5.044394119, 5.491853096, 6, 6.569855608, 7.209453366, 7.918863237},
        // 2^24 bins
        new double[]{0, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.169925001, 3.321928095, 3.459431619, 3.700439718, 4, 4.321928095, 4.64385619, 5.044394119, 5.491853096, 6, 6.584962501, 7.21916852},
        // 2^25 bins
        new double[]{0, 1, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.169925001, 3.321928095, 3.459431619, 3.807354922, 4, 4.321928095, 4.64385619, 5.044394119, 5.491853096, 6.022367813, 6.584962501},
        // 2^26 bins
        new double[]{0, 1, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.169925001, 3.321928095, 3.584962501, 3.807354922, 4, 4.321928095, 4.700439718, 5.087462841, 5.523561956, 6.022367813},
        // 2^27 bins
        new double[]{0, 1, 1, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.169925001, 3.321928095, 3.584962501, 3.807354922, 4.087462841, 4.392317423, 4.700439718, 5.087462841, 5.523561956},
        // 2^28 bins
        new double[]{0, 1, 1, 1, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3.169925001, 3.321928095, 3.584962501, 3.807354922, 4.087462841, 4.392317423, 4.700439718, 5.087462841},
        // 2^29 bins
        new double[]{0, 1, 1, 1, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2, 2.321928095, 2.321928095, 2.321928095, 2.584962501, 2.807354922, 2.807354922, 3, 3.169925001, 3.321928095, 3.584962501, 3.807354922, 4.087462841, 4.392317423, 4.754887502},
        // 2^30 bins
        new double[]{0, 1, 1, 1, 1, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 2.807354922, 3, 3.169925001, 3.459431619, 3.584962501, 3.807354922, 4.087462841, 4.392317423},
        // 2^31 bins
        new double[]{0, 1, 1, 1, 1, 1, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 2.807354922, 3, 3.169925001, 3.459431619, 3.584962501, 3.906890596, 4.087462841},
        // 2^32 bins
        new double[]{0, 1, 1, 1, 1, 1, 1, 1, 1, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 1.584962501, 2, 2, 2, 2.321928095, 2.321928095, 2.321928095, 2.584962501, 2.584962501, 2.807354922, 3, 3, 3.321928095, 3.459431619, 3.700439718, 3.906890596},
    };

    /**
     * Gets the approximate bin size when putting n balls into b bins. Note that the result may be even smaller than
     * the exact bin size. This is because the lookup table is generated by the C/C++ version, and there are some
     * precision difference between C/C++ and Java.
     *
     * @param n number of balls.
     * @param b number of bins.
     * @return the approximate max bin size.
     */
    public static int approxMaxBinSize(final int n, final int b) {
        assert b > 0 && n > 0;
        if (b == 1) {
            return n;
        }
        int numBinsLow = (int) Math.floor(DoubleUtils.log2(b));
        int numBinsHigh = (int) Math.ceil(DoubleUtils.log2(b));
        int numBallsLow = (int) Math.floor(DoubleUtils.log2(n));
        int numBallsHgh = (int) Math.ceil(DoubleUtils.log2(n));

        double diffBin = DoubleUtils.log2(b) - numBinsLow;
        double diffBall = DoubleUtils.log2(n) - numBallsLow;

        // interpolate a linear 2d spline between the se-rounding points (a surface).
        // Then evaluate the surface at out bin,ball coordinate.
        double a0 = (diffBin) * LOG_BIN_SIZE_TABLE[numBinsLow][numBallsLow]
            + (1 - diffBin) * LOG_BIN_SIZE_TABLE[numBinsLow][numBallsHgh];
        double a1 = (diffBin) * LOG_BIN_SIZE_TABLE[numBinsHigh][numBallsLow]
            + (1 - diffBin) * LOG_BIN_SIZE_TABLE[numBinsHigh][numBallsHgh];

        double b0 = (diffBall) * a0 + (1 - diffBall) * a1;
        return (int) Math.ceil(Math.pow(2, b0));
    }

    /**
     * Gets the expected bin size when putting n balls into b bins. See the following paper for more details:
     * <p>
     * Pinkas B, Schneider T, Zohner M. Scalable private set intersection based on OT extension. ACM Transactions on
     * Privacy and Security (TOPS), 2018, 21(2): 1-35.
     * </p>
     *
     * @param n number of balls.
     * @param b number of bins.
     * @return the excepted bin size.
     */
    public static int expectMaxBinSize(final int n, final int b) {
        assert b > 0 && n > 0;
        if (b == 1) {
            return n;
        }
        // 先将桶中元素的最大数量设置为1或者n/b的最大值
        int k = Math.max(1, n / b + 1);
        // 先计算一轮溢出概率，如果上来满足要求，则直接返回k
        double probability = expectProbability(n, b, k);
        if (probability <= DoubleUtils.STATS_NEG_PROBABILITY) {
            return k;
        }
        int step = 1;
        // 应用二分查找算法找到最接近给定统计安全常数的桶大小
        boolean doubling = true;
        while (probability > DoubleUtils.STATS_NEG_PROBABILITY || step > 1) {
            if (probability > DoubleUtils.STATS_NEG_PROBABILITY) {
                // 如果当前溢出概率大于要求溢出概率，意味着桶的大小设置得太小，需要增加
                if (doubling) {
                    step = Math.max(1, step * 2);
                } else {
                    step = Math.max(1, step / 2);
                }
                k += step;
            } else {
                // 桶的大小设置得太大，需要减小。减小的时候要一点一点降低
                doubling = false;
                step = Math.max(1, step / 2);
                k -= step;
            }
            probability = expectProbability(n, b, k);
        }
        return k;
    }

    /**
     * 计算将n个元素随机放置在b个桶中，包含元素最多的桶中至少包含k个元素的估计概率。
     *
     * @param b 桶的个数。
     * @param n 元素的个数。
     * @param k 最多的桶中至少包含k个元素。
     * @return n个元素随机放置在b个桶中，包含元素最多的桶中至少包含k个元素的估计概率。
     */
    private static double expectProbability(int n, int b, int k) {
        if (n <= k) {
            // 如果元素的个数小于每个桶至少包含的元素的个数，则概率为0，因此回复的结果是double的最大值
            return 0.0;
        }
        // Pr <= b * (en / bk)^k
        return b * Math.pow(Math.E * n / (b * k), k);
    }

    /**
     * Gets the exact bin size when putting n balls into b bins. See the following paper for more details:
     * <p>
     * Garimella, Gayathri, et al. Oblivious key-value stores and amplification for private set intersection.
     * CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
     * </p>
     * We note that the result may be slightly different with the C/C++ version because Java use more precise computation.
     *
     * @param n number of balls.
     * @param b number of bins.
     * @return the exact bin size.
     */
    public static int exactMaxBinSize(final int n, final int b) {
        assert b > 0 && n > 0;
        if (b == 1) {
            return n;
        }
        // 先将桶中元素的最大数量设置为1或者n/b的最大值，这样迭代速度可以快一些
        int k = Math.max(1, n / b);
        // 先计算一轮溢出概率，如果上来满足要求，则直接返回k
        BigDecimal probability = exactProbability(n, b, k);
        if (probability.compareTo(BigDecimalUtils.STATS_NEG_PROG) <= 0) {
            return k;
        }
        int step = 1;
        // 应用二分查找算法找到最接近给定统计安全常数的桶大小
        boolean doubling = true;
        while (probability.compareTo(BigDecimalUtils.STATS_NEG_PROG) > 0 || step > 1) {
            if (probability.compareTo(BigDecimalUtils.STATS_NEG_PROG) > 0) {
                // 如果当前溢出概率大于要求溢出概率，意味着桶的大小设置得太小，需要增加
                if (doubling) {
                    step = Math.max(1, step * 2);
                } else {
                    step = Math.max(1, step / 2);
                }
                k += step;
            } else {
                // 桶的大小设置得太大，需要减小。减小的时候要一点一点降低
                doubling = false;
                step = Math.max(1, step / 2);
                k -= step;
            }
            probability = exactProbability(n, b, k);
        }
        return k;
    }

    /**
     * 计算将n个元素随机放置在b个桶中，包含元素最多的桶中至少包含k个元素的准确概率。
     *
     * @param b 桶的个数。
     * @param n 元素的个数。
     * @param k 最多的桶中至少包含k个元素。
     * @return n个元素随机放置在b个桶中，包含元素最多的桶中至少包含k个元素的概率。
     */
    private static BigDecimal exactProbability(int n, int b, int k) {
        if (n <= k) {
            // 如果元素的个数小于每个桶至少包含的元素的个数，则概率为0
            return BigDecimal.ZERO;
        }
        // q
        BigDecimal binBigDecimal = BigDecimal.valueOf(b);
        // 1 / q
        BigDecimal binInverseBigDecimal = BigDecimal.ONE.setScale(BigDecimalUtils.PRECISION, RoundingMode.HALF_DOWN)
            .divide(binBigDecimal, RoundingMode.HALF_UP);
        // 1 - 1 / q
        BigDecimal oneMinusBinInverseBigDecimal = BigDecimal.ONE.subtract(binInverseBigDecimal);
        BigDecimal probability = BigDecimal.ZERO;
        for (int i = k; i <= n; i++) {
            BigInteger combinatorial = BigIntegerUtils.binomial(n, i);
            probability = probability.add(
                new BigDecimal(combinatorial)
                    .multiply(binInverseBigDecimal.pow(i))
                    .multiply(oneMinusBinInverseBigDecimal.pow(n - i))
            );
        }
        return binBigDecimal.multiply(probability);
    }
}
