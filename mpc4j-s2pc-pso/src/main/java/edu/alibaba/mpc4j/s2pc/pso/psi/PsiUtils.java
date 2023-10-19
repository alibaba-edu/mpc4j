package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * PSI utilities.
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class PsiUtils {
    /**
     * private constructor
     */
    private PsiUtils() {
        // empty
    }

    /**
     * Gets byte length for private equality test for protocols with semi-honest security.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return byte length for private equality test.
     */
    public static int getSemiHonestPeqtByteLength(int serverElementSize, int clientElementSize) {
        MathPreconditions.checkPositive("serverElementSize", serverElementSize);
        MathPreconditions.checkPositive("clientElementSize", clientElementSize);
        // λ + log(m) + log(n)
        return CommonConstants.STATS_BYTE_LENGTH
            + CommonUtils.getByteLength(LongUtils.ceilLog2(serverElementSize, 1))
            + CommonUtils.getByteLength(LongUtils.ceilLog2(clientElementSize, 1));
    }

    /**
     * Gets byte length for private equality test for protocols with malicious security.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return byte length for private equality test.
     */
    public static int getMaliciousPeqtByteLength(int serverElementSize, int clientElementSize) {
        MathPreconditions.checkPositive("serverElementSize", serverElementSize);
        MathPreconditions.checkPositive("clientElementSize", clientElementSize);
        /*
         * PRTY19 paper suggests 2 * κ.
         * CM20 paper suggests log_2(Q_2 * n_2) + σ, where Q_2 is the maximal number of PRF queries for the adversary.
         * CM20 implementation assumes Q_2 = 2^64, i.e., log_2(n) + λ + 64.
         */
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(64 + LongUtils.ceilLog2(clientElementSize));
    }
}
