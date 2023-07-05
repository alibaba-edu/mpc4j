package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.security.SecureRandom;

/**
 * garbled cuckoo table with 2 hash functions. The non-doubly construction is from the following paper:
 * <p>
 * Garimella G, Pinkas B, Rosulek M, et al. Oblivious Key-Value Stores and Amplification for Private Set Intersection.
 * CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
 * </p>
 * The doubly-obliviousness construction is form the following paper:
 * <p>
 * Zhang, Cong, Yu Chen, Weiran Liu, Min Zhang, and Dongdai Lin. Linear Private Set Union from Multi-Query Reverse
 * Private Membership Test. USENIX Security 2023.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
class H3SingletonGctGfe2Dokvs<T> extends AbstractH3GctGf2eDokvs<T> {
    /**
     * left ε, i.e., ε_l.
     */
    private static final double LEFT_EPSILON = 1.3;
    /**
     * right ε, i.e., ε_r.
     */
    private static final double RIGHT_EPSILON = 0.5;

    /**
     * Gets left m. The result is shown in Section 5.4 of the paper.
     *
     * @param n number of key-value pairs.
     * @return lm = ε_l * n, with lm % Byte.SIZE == 0.
     */
    static int getLm(int n) {
        MathPreconditions.checkPositive("n", n);
        return CommonUtils.getByteLength((int) Math.ceil(LEFT_EPSILON * n)) * Byte.SIZE;
    }

    /**
     * Gets right m. The result is shown in Section 5.4 of the paper.
     *
     * @param n number of key-value pairs.
     * @return rm = (1 + ε_r) * log(n) + λ, with with rm % Byte.SIZE == 0.
     */
    static int getRm(int n) {
        // when n is very small, we have very high collision probabilities. Wo do the test and find that
        // n = 2^8: 186, n = 2^9: 328, n = 2^10: 561, n = 2^11: 907. When n > 2^12, use formula to compute rm.
        int r = CommonUtils.getByteLength(
            (int) Math.ceil(RIGHT_EPSILON * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
        if (n <= (1 << 8)) {
            return CommonUtils.getByteLength(Math.max(r, n)) * Byte.SIZE;
        } else if (n <= (1 << 9)) {
            // 256 < n <= 512
            return CommonUtils.getByteLength(Math.min(n, 328)) * Byte.SIZE;
        } else if (n <= (1 << 10)) {
            // 512 < n <= 1024
            return CommonUtils.getByteLength(Math.min(n, 561)) * Byte.SIZE;
        } else if (n <= (1 << 11)) {
            // 1024 < n <= 2048
            return CommonUtils.getByteLength(907) * Byte.SIZE;
        } else {
            // n > 2048
            return r;
        }
    }

    H3SingletonGctGfe2Dokvs(EnvType envType, int n, int l, byte[][] keys) {
        this(envType, n, l, keys, new SecureRandom());
    }

    H3SingletonGctGfe2Dokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
        super(envType, n, getLm(n), getRm(n), l, keys, secureRandom);
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.H3_SINGLETON_GCT;
    }
}
