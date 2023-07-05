package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.H2CuckooTableTcFinder;

import java.security.SecureRandom;

/**
 * garbled cuckoo table with 2 hash functions. The non-doubly construction is from the following paper:
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020,
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 * The doubly-obliviousness construction is form the following paper:
 * <p>
 * Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: fast OPRF and circuit-PSI from vector-OLE. EUROCRYPT 2021,
 * pp. 901-930. Cham: Springer International Publishing, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
class H2TwoCoreGctGf2eDokvs<T> extends AbstractH2GctGf2eDokvs<T> {
    /**
     * left ε, i.e., ε_l.
     */
    private static final double LEFT_EPSILON = 2.4;
    /**
     * right ε, i.e., ε_r.
     */
    private static final double RIGHT_EPSILON = 1.4;

    /**
     * Gets left m. The result is shown in Table 2 of the paper.
     *
     * @param n number of key-value pairs.
     * @return lm = ε_l * n, with lm % Byte.SIZE == 0.
     */
    static int getLm(int n) {
        MathPreconditions.checkPositive("n", n);
        return CommonUtils.getByteLength((int) Math.ceil(LEFT_EPSILON * n)) * Byte.SIZE;
    }

    /**
     * Gets right m. The result is shown in the full version of the paper page 18.
     *
     * @param n number of key-value pairs.
     * @return rm = ε_r * log(n) + λ, with rm % Byte.SIZE == 0.
     */
    static int getRm(int n) {
        MathPreconditions.checkPositive("n", n);
        return CommonUtils.getByteLength(
            (int) Math.ceil(RIGHT_EPSILON * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
    }

    H2TwoCoreGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys) {
        this(envType, n, l, keys, new SecureRandom());
    }

    H2TwoCoreGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
        super(envType, n, getLm(n), getRm(n), l, keys, new H2CuckooTableTcFinder<>(), secureRandom);
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.H2_TWO_CORE_GCT;
    }
}
