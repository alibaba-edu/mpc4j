package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.Rings;
import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.Gf2xUtils;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * GF(2^l) finite field manager. For efficiency consideration, here we enumerate all minimal polynomial for GF(2^l)
 * where l ∈ {2, 4, 8, 16, 32, 40, 64, 128}. Each polynomial has minimal weight. The table comes from the following paper:
 * <p>Tom Hansen, Gary L. Mullen. Primitive Polynomials over Finite Fields. Mathematics of Computation, 59(200),
 * pp. 639-643, 1992.</p>
 * <p></p>
 * Here we also list the representation used in the paper. "Only the nonzero terms are represented, so that for example
 * over F_7, the polynomial x^14 + 2x^5 + 3 is represented as 14 : 1, 5 : 2, 0 : 3."
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class Gf2eManager {
    /**
     * l = 2: x^2 + x + 1
     */
    private static final byte[] GF002_MINIMAL_POLYNOMIAL = new byte[]{
        0b00000111,
    };
    /**
     * l = 4: x^4 + x + 1
     */
    private static final byte[] GF004_MINIMAL_POLYNOMIAL = new byte[]{
        0b00010011,
    };
    /**
     * l = 8: x^8 + x^4 + x^3 + x^2 + 1
     */
    private static final byte[] GF008_MINIMAL_POLYNOMIAL = new byte[]{
        0b00000001,
        0b00011101,
    };
    /**
     * l = 16: x^16 + x^5 + x^3 + x^2 + 1
     */
    private static final byte[] GF016_MINIMAL_POLYNOMIAL = new byte[]{
        0b00000001,
        0b00000000, 0b00101101,
    };
    /**
     * l = 32: x^32 + x^7 + x^6 + x^2 + 1
     */
    private static final byte[] GF032_MINIMAL_POLYNOMIAL = new byte[]{
        0b00000001,
        0b00000000, 0b00000000, 0b00000000, (byte) 0b11000101,
    };

    /**
     * l = 40: x^40 + x^5 + x^4 + x^3 + 1
     */
    private static final byte[] GF040_MINIMAL_POLYNOMIAL = new byte[]{
        0b00000001,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00111001,
    };
    /**
     * l = 64: x^64 + x^4 + x^3 + x + 1
     */
    private static final byte[] GF064_MINIMAL_POLYNOMIAL = new byte[]{
        0b00000001,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00011011,
    };
    /**
     * l = 128: x^128 + x^7 + x^2 + x + 1, also used in AES.
     */
    private static final byte[] GF128_MINIMAL_POLYNOMIAL = new byte[]{
        0b00000001,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,
        0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, (byte) 0b10000111,
    };
    /**
     * l -> minimal polynomial represented in <code>byte[]</code>.
     */
    private static final TIntObjectMap<byte[]> GF2X_MINIMAL_POLYNOMIAL_MAP;
    /**
     * l -> minimal polynomial represented in Rings.
     */
    private static final TIntObjectMap<FiniteField<UnivariatePolynomialZp64>> GF2X_FINITE_FIELD_MAP;

    static {
        GF2X_MINIMAL_POLYNOMIAL_MAP = new TIntObjectHashMap<>();
        GF2X_FINITE_FIELD_MAP = new TIntObjectHashMap<>();
        // l = 2
        GF2X_MINIMAL_POLYNOMIAL_MAP.put(2, GF002_MINIMAL_POLYNOMIAL);
        GF2X_FINITE_FIELD_MAP.put(2, Rings.GF(Gf2xUtils.byteArrayToRings(GF002_MINIMAL_POLYNOMIAL)));
        // l = 4
        GF2X_MINIMAL_POLYNOMIAL_MAP.put(4, GF004_MINIMAL_POLYNOMIAL);
        GF2X_FINITE_FIELD_MAP.put(4, Rings.GF(Gf2xUtils.byteArrayToRings(GF004_MINIMAL_POLYNOMIAL)));
        // l = 8
        GF2X_MINIMAL_POLYNOMIAL_MAP.put(8, GF008_MINIMAL_POLYNOMIAL);
        GF2X_FINITE_FIELD_MAP.put(8, Rings.GF(Gf2xUtils.byteArrayToRings(GF008_MINIMAL_POLYNOMIAL)));
        // l = 16
        GF2X_MINIMAL_POLYNOMIAL_MAP.put(16, GF016_MINIMAL_POLYNOMIAL);
        GF2X_FINITE_FIELD_MAP.put(16, Rings.GF(Gf2xUtils.byteArrayToRings(GF016_MINIMAL_POLYNOMIAL)));
        // l = 32
        GF2X_MINIMAL_POLYNOMIAL_MAP.put(32, GF032_MINIMAL_POLYNOMIAL);
        GF2X_FINITE_FIELD_MAP.put(32, Rings.GF(Gf2xUtils.byteArrayToRings(GF032_MINIMAL_POLYNOMIAL)));
        // l = 40
        GF2X_MINIMAL_POLYNOMIAL_MAP.put(40, GF040_MINIMAL_POLYNOMIAL);
        GF2X_FINITE_FIELD_MAP.put(40, Rings.GF(Gf2xUtils.byteArrayToRings(GF040_MINIMAL_POLYNOMIAL)));
        // l = 64
        GF2X_MINIMAL_POLYNOMIAL_MAP.put(64, GF064_MINIMAL_POLYNOMIAL);
        GF2X_FINITE_FIELD_MAP.put(64, Rings.GF(Gf2xUtils.byteArrayToRings(GF064_MINIMAL_POLYNOMIAL)));
        // l = 128
        GF2X_MINIMAL_POLYNOMIAL_MAP.put(128, GF128_MINIMAL_POLYNOMIAL);
        GF2X_FINITE_FIELD_MAP.put(128, Rings.GF(Gf2xUtils.byteArrayToRings(GF128_MINIMAL_POLYNOMIAL)));
    }

    /**
     * private constructor.
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
        if (!GF2X_FINITE_FIELD_MAP.containsKey(l)) {
            createFiniteField(l);
        }
        return GF2X_FINITE_FIELD_MAP.get(l);
    }

    /**
     * Gets GF(2^l) for the given element bit length l.
     *
     * @param l element bit length for the finite field GF(2^l).
     * @return the finite field.
     */
    public static byte[] getMinimalPolynomial(int l) {
        assert l > 0 : "l must be greater than 0: " + l;
        if (!GF2X_MINIMAL_POLYNOMIAL_MAP.containsKey(l)) {
            createFiniteField(l);
        }
        return GF2X_MINIMAL_POLYNOMIAL_MAP.get(l);
    }

    private static void createFiniteField(int l) {
        assert !GF2X_MINIMAL_POLYNOMIAL_MAP.containsKey(l);
        FiniteField<UnivariatePolynomialZp64> finiteField = Rings.GF(2, l);
        GF2X_FINITE_FIELD_MAP.put(l, finiteField);
        int minimalPolynomialByteL = CommonUtils.getByteLength(l + 1);
        GF2X_MINIMAL_POLYNOMIAL_MAP.put(l, Gf2xUtils.ringsToByteArray(finiteField.getMinimalPolynomial(), minimalPolynomialByteL));
    }
}
