package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.Rings;
import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * GF(2^l) finite field manager.
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class Gf2eManager {
    /**
     * moduli polynomial for GF(2^128): f(x) = x^128 + x^7 + x^2 + x + 1
     */
    private static final UnivariatePolynomialZp64 GF2K_POLYNOMIAL = UnivariatePolynomialZp64.create(2,
        new long[]{
            1L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            1L,
        }
    );

    /**
     * moduli polynomial for GF(2^64): f(x) = x^64 + x^4 + x^3 + x + 1
     */
    private static final UnivariatePolynomialZp64 GF64_POLYNOMIAL = UnivariatePolynomialZp64.create(
        2, new long[]{
            1L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
            1L,
        }
    );
    /**
     * l -> GF(2^l) map
     */
    private static final TIntObjectMap<FiniteField<UnivariatePolynomialZp64>> GF2X_MAP = new TIntObjectHashMap<>();

    static {
        GF2X_MAP.put(CommonConstants.BLOCK_BIT_LENGTH, Rings.GF(GF2K_POLYNOMIAL));
        GF2X_MAP.put(64, Rings.GF(GF64_POLYNOMIAL));
    }

    /**
     * private constructor
     */
    private Gf2eManager() {
        // empty
    }

    /**
     * Gets GF(2^l) for the given element bit length l.
     *
     * @param l element bit length for the finite field GF(2^l).
     * @return the finite field.
     */
    public static FiniteField<UnivariatePolynomialZp64> getFiniteField(int l) {
        assert l > 0 : "l must be greater than 0: " + l;
        if (GF2X_MAP.containsKey(l)) {
            return GF2X_MAP.get(l);
        } else {
            FiniteField<UnivariatePolynomialZp64> finiteField = Rings.GF(2, l);
            GF2X_MAP.put(l, finiteField);

            return finiteField;
        }
    }
}
