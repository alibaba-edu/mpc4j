package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParamsChecker;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

/**
 * YWL20-NC-COT LPN parameter finder. The idea comes from Section 7 of the following paper:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 * It says:
 * <p>
 * First, we determine the LPN parameter k. This is because the LPN encoding process can get the maximum speed if all k
 * values can fit in the CPU cache. With k determined, we pick n and t such that n − t * log(n / t) − k is close to
 * 10^7 and that (n, t, k) is secure against all known attacks of complexity 2^{128} steps. This by done by enumerating
 * the set of possible t and find the smallest parameter set.
 * </p>
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/01/26
 */
public class Ywl20NcCotLpnParamsFinder {
    /**
     * unit K, the paper tries K = 2^6, for efficiency reason, we choose a larger unit K.
     */
    private static final int UNIT_K = 128;
    /**
     * min log(n)
     */
    static final int ITERATION_MIN_LOG_N = 12;
    /**
     * max log(n)
     */
    static final int ITERATION_MAX_LOG_N = 24;
    /**
     * factors when searching max(n). The values are selected by experiments.
     */
    private static final TIntDoubleMap ITERATION_UPPER_N_FACTOR_MAP = new TIntDoubleHashMap();

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
     * private constructor.
     */
    private Ywl20NcCotLpnParamsFinder() {
        // empty
    }

    /**
     * Finds LPN parameters in Setup phase.
     *
     * @param config             config.
     * @param iterationLpnParams LPN parameters in Iteration phase.
     * @return LPN parameters in Setup phase.
     */
    public static LpnParams findSetupLpnParams(MspCotConfig config, LpnParams iterationLpnParams) {
        // in Setup phase, we need k = k COT and pre-computed COT used in MSP-COT
        int minSetupN = iterationLpnParams.getK()
            + MspCotFactory.getPrecomputeNum(config, iterationLpnParams.getT(), iterationLpnParams.getN());
        int k = UNIT_K;
        LpnParams optimalLpnParams = null;
        int optimalCotNum = -1;
        while (k < minSetupN) {
            LpnParams lpnParams = findSetupMinT(k, minSetupN);
            if (lpnParams != null) {
                // compute COT num: k in Init phase and ones used in MSP-COT (depends on n and t).
                int cotNum = MspCotFactory.getPrecomputeNum(config, lpnParams.getT(), lpnParams.getN())
                    + lpnParams.getK();
                // choose this param if this requires fewer COT.
                if (optimalLpnParams == null || cotNum < optimalCotNum) {
                    optimalLpnParams = lpnParams;
                    optimalCotNum = cotNum;
                }
            }
            // add k
            k += UNIT_K;
        }
        if (optimalLpnParams != null) {
            return optimalLpnParams;
        }
        throw new IllegalArgumentException("Cannot find valid setupN > " + minSetupN + " even when k = " + k);
    }

    /**
     * Finds min(t) in Setup phase that satisfies security parameter. Each t ∈ [1, min(minSetupN, 1536)] has a max(n),
     * we choose the min(t).
     *
     * @param k         k.
     * @param minSetupN min(n) in Setup phase.
     * @return min(t) in Setup phase that satisfies security parameter.
     */
    private static LpnParams findSetupMinT(int k, int minSetupN) {
        // t ∈ [1, min(minSetupN, 1536)]
        int lowerT = 1;
        int upperT = Math.min(minSetupN, 1536);
        LpnParams lpnParams = null;
        int currentT;
        while (lowerT <= upperT) {
            currentT = (lowerT + upperT) / 2;
            int n = findSetupMaxN(k, currentT, minSetupN);
            if (n >= 0) {
                // if we can find a valid n, then try decrease t.
                lpnParams = LpnParams.uncheckCreate(n, k, currentT);
                upperT = currentT - 1;
            } else {
                // if we cannot find a valid n, then increase t.
                lowerT = currentT + 1;
            }
        }
        return lpnParams;
    }

    /**
     * Finds max(n) in Setup phase that satisfies security parameter. Given k and t, the security parameter
     * decreases with the increase of n. Here we find (k, t, min(n)) that satisfies security parameter and n - k is
     * greater than the required MSP-COT when iterating.
     *
     * @param k         k.
     * @param t         t.
     * @param minSetupN min(n) in Setup phase.
     * @return max(n) in Setup phase that satisfies security parameter.
     */
    private static int findSetupMaxN(int k, int t, int minSetupN) {
        if (!LpnParamsChecker.validLpnParams(minSetupN, k, t)) {
            // if min(n) is invalid, return -1
            return -1;
        }
        // set lowerN and upperN, then do binary search
        int logMinSetupN = LongUtils.ceilLog2(minSetupN);
        int lowerN = minSetupN;
        int upperN = (int) (minSetupN * ITERATION_UPPER_N_FACTOR_MAP.get(logMinSetupN));
        int maxN = -1;
        int currentN;
        while (lowerN <= upperN) {
            currentN = (lowerN + upperN) / 2;
            if (LpnParamsChecker.validLpnParams(currentN, k, t)) {
                maxN = currentN;
                // current n is valid, try to increase.
                lowerN = currentN + 1;
            } else {
                // current n is invalid, decrease.
                upperN = currentN - 1;
            }
        }
        return maxN;
    }

    /**
     * Finds LPN parameters in Iteration phase.
     *
     * @param config     config.
     * @param minOutputN min(n) that needs to output in Iteration phase.
     * @return 启动阶段的LPN参数。
     */
    public static LpnParams findIterationLpnParams(MspCotConfig config, int minOutputN) {
        MathPreconditions.checkInRangeClosed(
            "minOutputN", minOutputN, 1 << ITERATION_MIN_LOG_N, 1 << ITERATION_MAX_LOG_N
        );
        int k = UNIT_K;
        while (k < minOutputN) {
            LpnParams lpnParams = findIterationMinT(config, k, minOutputN);
            if (lpnParams != null) {
                return lpnParams;
            }
            // add k
            k += UNIT_K;
        }
        throw new IllegalArgumentException("Cannot find valid n > " + minOutputN + " even when k = " + k);
    }

    /**
     * Finds min(t) in Iteration phase that satisfies security parameter. Each t ∈ [1, min(minOutputN, 1536)] has a max(n),
     * we choose the min(t).
     *
     * @param config     config.
     * @param k          k.
     * @param minOutputN min(n) that needs to output in Iteration phase.
     * @return min(t) in Iteration phase that satisfies security parameter.
     */
    private static LpnParams findIterationMinT(MspCotConfig config, int k, int minOutputN) {
        // t ∈ [1, min(minOutputN, 1536)]
        int lowerT = 1;
        int upperT = Math.min(minOutputN, 1536);
        LpnParams lpnParams = null;
        int currentT;
        while (lowerT <= upperT) {
            currentT = (lowerT + upperT) / 2;
            int suitableN = findIterationSuitableN(config, k, currentT, minOutputN);
            if (suitableN >= 0) {
                // if we can find a valid n, then try decrease t.
                lpnParams = LpnParams.uncheckCreate(suitableN, k, currentT);
                upperT = currentT - 1;
            } else {
                // if we cannot find a valid n, then increase t.
                lowerT = currentT + 1;
            }
        }
        return lpnParams;
    }

    /**
     * Finds suitable(n) in Iteration phase that satisfies security parameter. Given k and t, the security parameter
     * decreases with the increase of n. Here we find (k, t, suitable(n)) that satisfies security parameter and n - k is
     * greater than the required GF2K-MSP-VOLE when iterating.
     *
     * @param config     config.
     * @param k          k.
     * @param t          t.
     * @param minOutputN min(n) that needs to output in Iteration phase.
     * @return suitable(n) in Iteration phase that satisfies security parameter.
     */
    private static int findIterationSuitableN(MspCotConfig config, int k, int t, int minOutputN) {
        if (!LpnParamsChecker.validLpnParams(minOutputN, k, t)) {
            // if min(n) is invalid, return -1
            return -1;
        }
        // set binary search range
        int logMinN = LongUtils.ceilLog2(minOutputN);
        int lowerN = minOutputN;
        int upperN = (int) (minOutputN * ITERATION_UPPER_N_FACTOR_MAP.get(logMinN));
        int maxN = -1;
        int currentN;
        while (lowerN <= upperN) {
            currentN = (lowerN + upperN) / 2;
            if (LpnParamsChecker.validLpnParams(currentN, k, t)) {
                maxN = currentN;
                // if n is valid, try to increase n.
                lowerN = maxN + 1;
            } else {
                // if n is invalid, try to decrease n.
                upperN = maxN - 1;
            }
        }
        if (maxN < 0) {
            return -1;
        }
        // if max(n) - k is smaller than the required MSP-COT when iterating, it means we cannot find a valid n.
        int maxNecessaryCotNum = MspCotFactory.getPrecomputeNum(config, t, maxN) + k;
        if (maxN - minOutputN < maxNecessaryCotNum) {
            return -1;
        }
        // if max(n) is OK, we can try to decrease n.
        lowerN = minOutputN;
        upperN = maxN;
        int suitableN = maxN;
        while (lowerN <= upperN) {
            currentN = (lowerN + upperN) / 2;
            int necessaryCotNum = MspCotFactory.getPrecomputeNum(config, t, currentN) + k;
            if (currentN - minOutputN >= necessaryCotNum) {
                suitableN = currentN;
                // if n is valid, try to increase n.
                upperN = currentN - 1;
            } else {
                // if n is invalid, try to decrease n.
                lowerN = currentN + 1;
            }
        }
        return suitableN;
    }

    /**
     * Gets the number of output COT in each Iteration.
     *
     * @param config             config.
     * @param iterationLpnParams LPN parameters in Iteration phase.
     * @return the number of output COT in each Iteration.
     */
    public static int getIterationOutputSize(MspCotConfig config, LpnParams iterationLpnParams) {
        int n = iterationLpnParams.getN();
        int k = iterationLpnParams.getK();
        int t = iterationLpnParams.getT();
        // we need to subtract k and the required MSP-COT when iterating.
        return n - MspCotFactory.getPrecomputeNum(config, t, n) - k;
    }
}
