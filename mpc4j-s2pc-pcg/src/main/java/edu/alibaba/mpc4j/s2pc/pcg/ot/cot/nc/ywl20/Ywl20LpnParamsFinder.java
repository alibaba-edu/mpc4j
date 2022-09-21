package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20;

import java.util.HashMap;
import java.util.Map;

import edu.alibaba.mpc4j.common.tool.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.lpn.LpnParamsChecker;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;

/**
 * YWL20-NC-COT协议LPN参数查找器。参数查找方法的思路来自于下述论文第7节：
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 * 论文原文描述为：
 * <p>
 * First, we determine the LPN parameter k. This is because the LPN encoding process can get the maximum speed if all k
 * values can fit in the CPU cache. With k determined, we pick n and t such that n − t * log(n / t) − k is close to
 * 10^7 and that (n, t, k) is secure against all known attacks of complexity 2^{128} steps. This by done by enumerating
 * the set of possible t and find the smallest parameter set.
 * </p>
 * 通过计算YWL20的参数可以发现：
 * <p>
 * One-Time Step阶段，n / k约为16；Main Iteration阶段，n / k约为18，且t最大不会超过1400。
 * </p>
 * 为此：
 * <p>
 * - 给定需要计算得到的n，令k的最小值为离n / 32向上取整，且形式为2^x格式的值。
 * - 从t = 1到t = 1536进行二分查找，找到满足最优解的最小t，作为返回结果。
 * </p>
 * 按照此方法查找，可以得到k取得最小值的(n, k, t)，但t的取值会比k略大。
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/01/26
 */
public class Ywl20LpnParamsFinder {
    /**
     * 单位K，根据论文推断应为2^6，但是由于参数查找速度比较慢，这里选择128比特
     */
    private static final int UNIT_K = 128;
    /**
     * 迭代最小log(n)
     */
    private static final int ITERATION_MIN_LOG_N = 12;
    /**
     * 迭代最大log(n)
     */
    private static final int ITERATION_MAX_LOG_N = 24;

    /**
     * 搜索N时的放大倍数，通过实验得到
     */
    private static final Map<Integer, Double> ITERATION_UPPER_N_FACTOR_MAP = new HashMap<>();

    static {
        ITERATION_UPPER_N_FACTOR_MAP.put(12, 3.7);
        ITERATION_UPPER_N_FACTOR_MAP.put(13, 2.4);
        ITERATION_UPPER_N_FACTOR_MAP.put(14, 1.8);
        ITERATION_UPPER_N_FACTOR_MAP.put(15, 1.5);
        ITERATION_UPPER_N_FACTOR_MAP.put(16, 1.4);
        ITERATION_UPPER_N_FACTOR_MAP.put(17, 1.3);
        ITERATION_UPPER_N_FACTOR_MAP.put(18, 1.2);
        ITERATION_UPPER_N_FACTOR_MAP.put(19, 1.2);
        ITERATION_UPPER_N_FACTOR_MAP.put(20, 1.1);
        ITERATION_UPPER_N_FACTOR_MAP.put(21, 1.1);
        ITERATION_UPPER_N_FACTOR_MAP.put(22, 1.1);
        ITERATION_UPPER_N_FACTOR_MAP.put(23, 1.1);
        ITERATION_UPPER_N_FACTOR_MAP.put(24, 1.1);
    }
    /**
     * 私有构造函数
     */
    private Ywl20LpnParamsFinder() {
        // empty
    }

    /**
     * 查找启动（Setup）阶段的LPN参数。
     *
     * @param config 配置项。
     * @param iterationLpnParams 迭代阶段的LPN参数。
     * @return 启动阶段的LPN参数。
     */
    public static LpnParams findSetupLpnParams(MspCotConfig config, LpnParams iterationLpnParams) {
        // 初始化阶段需要k = k个COT，以及MSPCOT所需的预计算COT
        int minSetupN = iterationLpnParams.getK()
            + MspCotFactory.getPrecomputeNum(config, iterationLpnParams.getT(), iterationLpnParams.getN());
        int k = UNIT_K;
        LpnParams optimalLpnParams = null;
        int optimalCotNum = -1;
        while (k < minSetupN) {
            LpnParams lpnParams = findSetupMinT(k, minSetupN);
            if (lpnParams != null) {
                // 计算LPN参数所需COT数量：init阶段的k个COT（如果是恶意安全，则为k + λ） + MSPCOT协议消耗的COT（与n和t相关）
                int cotNum = MspCotFactory.getPrecomputeNum(config, lpnParams.getT(), lpnParams.getN())
                    + lpnParams.getK();
                // 如果此LPN参数所需的COT数量更少，则将此LPN参数设置为最优参数
                if (optimalLpnParams == null || cotNum < optimalCotNum) {
                    optimalLpnParams = lpnParams;
                    optimalCotNum = cotNum;
                }
            }
            // 放大k
            k += UNIT_K;
        }
        if (optimalLpnParams != null) {
            return optimalLpnParams;
        }
        throw new IllegalArgumentException("Cannot find valid setupN > " + minSetupN + " even when k = " + k);
    }

    /**
     * 查找启动（Setup）阶段满足安全性要求，且t取得最小值的LPN参数。
     * 1 <= t <= min(minSetupN, 2048)中的每一个t都对应一个最大的n，在所有满足条件的t中选择最小的t。
     *
     * @param k         k的取值。
     * @param minSetupN 启动阶段n的最小值。
     * @return 满足安全性要求，且t取得最小值的LPN参数。
     */
    private static LpnParams findSetupMinT(int k, int minSetupN) {
        // 从t在[1, 1536]且t <= n的范围中二分查找
        int lowerT = 1;
        int upperT = Math.min(minSetupN, 1536);
        LpnParams lpnParams = null;
        int currentT;
        while (lowerT <= upperT) {
            currentT = (lowerT + upperT) / 2;
            int n = findSetupMaxN(k, currentT, minSetupN);
            if (n >= 0) {
                // 如果能找到满足要求的n，则尝试让t更小
                lpnParams = LpnParams.uncheckCreate(n, k, currentT);
                upperT = currentT - 1;
            } else {
                // 如果无法找到满足要求的n，则让t更大
                lowerT = currentT + 1;
            }
        }
        return lpnParams;
    }

    /**
     * 查找启动（Setup）阶段满足安全性要求的n最大值。
     * 给定k和t后，安全参数随着n的增大而降低。只要(k, t, minSetupN)满足安全性要求，就可以找到满足安全性要求的最大n。
     *
     * @param k         k的取值。
     * @param t         t的取值。
     * @param minSetupN 启动阶段n的最小值。
     * @return 满足安全性要求的最大n。
     */
    private static int findSetupMaxN(int k, int t, int minSetupN) {
        if (!LpnParamsChecker.validLpnParams(minSetupN, k, t)) {
            // 如果最小值都不满足要求，则直接返回-1
            return -1;
        }
        int logMinSetupN = LongUtils.ceilLog2(minSetupN);
        // 将目标n的最大值设为给定值向上取2^x格式的2倍，超过这个范围已经没有意义了
        int lowerN = minSetupN;
        int upperN = (int)(minSetupN * ITERATION_UPPER_N_FACTOR_MAP.get(logMinSetupN));
        // 二分查找
        int maxN = -1;
        int currentN;
        while (lowerN <= upperN) {
            currentN = (lowerN + upperN) / 2;
            if (LpnParamsChecker.validLpnParams(currentN, k, t)) {
                maxN = currentN;
                // 当前n满足要求，n可以再增大
                lowerN = currentN + 1;
            } else {
                // 当前n不满足要求，n需要减小
                upperN = currentN - 1;
            }
        }
        return maxN;
    }

    /**
     * 查找迭代（Iteration）阶段的LPN参数。
     *
     * @param config 配置项。
     * @return 启动阶段的LPN参数。
     */
    public static LpnParams findIterationLpnParams(MspCotConfig config, int minN) {
        assert minN >= (1 << ITERATION_MIN_LOG_N) && minN <= (1 << ITERATION_MAX_LOG_N);
        int k = UNIT_K;
        while (k < minN) {
            LpnParams lpnParams = findIterationMinT(config, k, minN);
            if (lpnParams != null) {
                return lpnParams;
            }
            // 放大k
            k += UNIT_K;
        }
        throw new IllegalArgumentException("Cannot find valid n > " + minN + " even when k = " + k);
    }

    /**
     * 查找迭代（Iteration）阶段满足安全性要求，且t取得最小值的LPN参数。
     * 1 <= t <= min(minN, 2048)中的每一个t都对应一个最大的n，在所有满足条件的t中选择最小的t。
     *
     * @param config MSPCOT协议配置项。
     * @param k      k的取值。
     * @param minN   单轮密钥数量n的最小值。
     * @return 满足安全性要求，且t取得最小值的LPN参数。
     */
    private static LpnParams findIterationMinT(MspCotConfig config, int k, int minN) {
        // 从t在[1, 1536]且t <= n的范围中二分查找
        int lowerT = 1;
        int upperT = Math.min(minN, 1536);
        LpnParams lpnParams = null;
        int currentT;
        while (lowerT <= upperT) {
            currentT = (lowerT + upperT) / 2;
            int suitableN = findIterationSuitableN(config, k, currentT, minN);
            if (suitableN >= 0) {
                // 如果能找到满足要求的n，则尝试让t更小
                lpnParams = LpnParams.uncheckCreate(suitableN, k, currentT);
                upperT = currentT - 1;
            } else {
                // 如果无法找到满足要求的n，则让t更大
                lowerT = currentT + 1;
            }
        }
        return lpnParams;
    }

    /**
     * 查找迭代（Iteration）阶段满足安全性要求的n最大值。
     * 给定k和t后，安全参数随着n的增大而降低。要求(k, t, minN)满足安全性要求，且n - k大于MSPCOT消耗的COT数量，查到满足安全性要求的最小n。
     *
     * @param config MSPCOT协议配置项。
     * @param k      k的取值。
     * @param t      t的取值。
     * @param minN   单轮密钥数量n的最小值。
     * @return 满足安全性要求的最大n。
     */
    private static int findIterationSuitableN(MspCotConfig config, int k, int t, int minN) {
        if (!LpnParamsChecker.validLpnParams(minN, k, t)) {
            // 如果最小值都不满足要求，则直接返回-1
            return -1;
        }
        // 将目标n的最大值设为给定值的某个倍数
        int logMinN = LongUtils.ceilLog2(minN);
        int lowerN = minN;
        int upperN = (int)(minN * ITERATION_UPPER_N_FACTOR_MAP.get(logMinN));
        // 二分查找
        int maxN = -1;
        int currentN;
        while (lowerN <= upperN) {
            currentN = (lowerN + upperN) / 2;
            if (LpnParamsChecker.validLpnParams(currentN, k, t)) {
                maxN = currentN;
                // 当前n满足要求，n可以再增大
                lowerN = maxN + 1;
            } else {
                // 当前n不满足要求，n需要减小
                upperN = maxN - 1;
            }
        }
        if (maxN < 0) {
            return -1;
        }
        // 如果maxN都不能满足要求，则找不到合适的参数
        int maxNecessaryCotNum = MspCotFactory.getPrecomputeNum(config, t, maxN) + k;
        if (maxN - minN < maxNecessaryCotNum) {
            return -1;
        }
        // 如果maxN满足要求，再进行二分查找，寻找最合适的n
        lowerN = minN;
        upperN = maxN;
        int suitableN = maxN;
        while (lowerN <= upperN) {
            currentN = (lowerN + upperN) / 2;
            int necessaryCotNum = MspCotFactory.getPrecomputeNum(config, t, currentN) + k;
            if (currentN - minN >= necessaryCotNum) {
                suitableN = currentN;
                // 当前n满足要求，n可以减小
                upperN = currentN - 1;
            } else {
                // 当前n不满足要求，n需要增大
                lowerN = currentN + 1;
            }
        }
        return suitableN;
    }

    /**
     * 计算迭代（Iteration）阶段输出的密钥对数量。
     *
     * @param config MSPCOT协议配置项。
     * @param iterationLpnParams 迭代LPN参数。
     * @return 迭代（Iteration）阶段输出的密钥对数量。
     */
    public static int getIterationOutputSize(MspCotConfig config, LpnParams iterationLpnParams) {
        int n = iterationLpnParams.getN();
        int k = iterationLpnParams.getK();
        int t = iterationLpnParams.getT();
        // 迭代过程需要留出MSPCOT的预计算COT和k个预计算COT
        return n - MspCotFactory.getPrecomputeNum(config, t, n) - k;
    }
}
