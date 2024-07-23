package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import cc.redberry.rings.Rings;
import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eManager;
import edu.alibaba.mpc4j.common.tool.utils.Gf2xUtils;

/**
 * Subfield GF2K manager.
 *
 * @author Weiran Liu
 * @date 2024/6/2
 */
public class Sgf2kManager {
    /**
     * private constructor.
     */
    private Sgf2kManager() {
        // empty
    }

    /**
     * field L
     */
    private static final int FIELD_L = CommonConstants.BLOCK_BIT_LENGTH;

    /**
     * t = 2, F_{2^t} = GF(2^2), r = 64, f(X) =
     * <p>
     * (  x)X^0  + (1  )X^1  + (1  )X^2  + (   )X^3  +
     * (   )X^4  + (  x)X^5  + (   )X^6    (  x)X^7  +
     * (1  )X^8  + (1+x)X^9  + (1+x)X^10 + (1+x)X^11 +
     * (1  )X^12 + (   )X^13 + (   )X^14 + (1  )X^15 +
     * (1+x)X^16 + (1+x)X^17 + (   )X^18 + (1  )X^19 +
     * (   )X^20 + (   )X^21 + (1  )X^22 + (   )X^23 +
     * (   )X^24 + (1  )X^25 + (   )X^26 + (   )X^27 +
     * (1+x)X^28 + (   )X^29 + (   )X^30 + (  x)X^31 +
     * (   )X^32 + (   )X^33 + (  x)X^34 + (   )X^35 +
     * (  x)X^36 + (1  )X^37 + (1+x)X^38 + (1  )X^39 +
     * (  x)X^40 + (  x)X^41 + (  x)X^42 + (   )X^43 +
     * (1  )X^44 + (1  )X^45 + (1+x)X^46 + (   )X^47 +
     * (1  )X^48 + (  x)X^49 + (   )X^50 + (   )X^51 +
     * (   )X^52 + (1+x)X^53 + (   )X^54 + (   )X^55 +
     * (1  )X^56 + (   )X^57 + (   )X^58 + (   )X^59 +
     * (   )X^60 + (1+x)X^61 + (   )X^62 + (   )X^63 +
     * (1  )X^64
     * </p>
     */
    private static final byte[][] SGF2K_002_FIELD_MINIMAL_POLYNOMIAL = new byte[][]{
        // (  x)X^0  + (1  )X^1  + (1  )X^2  + (   )X^3  +
        new byte[]{0b00000010}, new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000000},
        // (   )X^4  + (  x)X^5  + (   )X^6  + (  x)X^7  +
        new byte[]{0b00000000}, new byte[]{0b00000010}, new byte[]{0b00000000}, new byte[]{0b00000010},
        // (1  )X^8  + (1+x)X^9  + (1+x)X^10 + (1+x)X^11 +
        new byte[]{0b00000001}, new byte[]{0b00000011}, new byte[]{0b00000011}, new byte[]{0b00000011},
        // (1  )X^12 + (   )X^13 + (   )X^14 + (1  )X^15
        new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001},
        // (1+x)X^16 + (1+x)X^17 + (   )X^18 + (1  )X^19
        new byte[]{0b00000011}, new byte[]{0b00000011}, new byte[]{0b00000000}, new byte[]{0b00000001},
        // (   )X^20 + (   )X^21 + (1  )X^22 + (   )X^23
        new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000000},
        // (   )X^24 + (1  )X^25 + (   )X^26 + (   )X^27
        new byte[]{0b00000000}, new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000},
        // (1+x)X^28 + (   )X^29 + (   )X^30 + (  x)X^31 +
        new byte[]{0b00000011}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000010},
        // (   )X^32 + (   )X^33 + (  x)X^34 + (   )X^35 +
        new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000010}, new byte[]{0b00000000},
        // (  x)X^36 + (1  )X^37 + (1+x)X^38 + (1  )X^39 +
        new byte[]{0b00000010}, new byte[]{0b00000001}, new byte[]{0b00000011}, new byte[]{0b00000001},
        // (  x)X^40 + (  x)X^41 + (  x)X^42 + (   )X^43 +
        new byte[]{0b00000010}, new byte[]{0b00000010}, new byte[]{0b00000010}, new byte[]{0b00000000},
        // (1  )X^44 + (1  )X^45 + (1+x)X^46 + (   )X^47 +
        new byte[]{0b00000001}, new byte[]{0b00000001}, new byte[]{0b00000011}, new byte[]{0b00000000},
        // (1  )X^48 + (  x)X^49 + (   )X^50 + (   )X^51 +
        new byte[]{0b00000001}, new byte[]{0b00000010}, new byte[]{0b00000000}, new byte[]{0b00000000},
        // (   )X^52 + (1+x)X^53 + (   )X^54 + (   )X^55 +
        new byte[]{0b00000000}, new byte[]{0b00000011}, new byte[]{0b00000000}, new byte[]{0b00000000},
        // (1  )X^56 + (   )X^57 + (   )X^58 + (   )X^59 +
        new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
        // (   )X^60 + (1+x)X^61 + (   )X^62 + (   )X^63 +
        new byte[]{0b00000000}, new byte[]{0b00000011}, new byte[]{0b00000000}, new byte[]{0b00000000},
        // (1  )X^64
        new byte[]{0b00000001},
    };

    /**
     * t = 2, F_{2^t} = GF(2^2), r = 64, field finite field
     */
    private static final FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> SGF2K_002_FIELD_FINITE_FIELD;

    static {
        SGF2K_002_FIELD_FINITE_FIELD = setFieldFiniteField(SGF2K_002_FIELD_MINIMAL_POLYNOMIAL);
    }

    /**
     * t = 4, F_{2^t} = GF(2^4), r = 32, f(X) =
     * <p>
     * (  x+x^2+x^3)X^0  + (        x^3)X^1  + (  x+x^2    )X^2  + (1+      x^3)X^3  +
     * (  x        )X^4  + (           )X^5  + (  x+x^2    )X^6  + (  x        )X^7  +
     * (1          )X^8  + (           )X^9  + (  x        )X^10 + (           )X^11 +
     * (1+x+x^2    )X^12 + (1+x        )X^13 + (1+  x^2    )X^14 + (  x+x^2    )X^15 +
     * (1+x+x^2    )X^16 + (           )X^17 + (           )X^18 + (           )X^19 +
     * (1+x+x^2+x^3)X^20 + (  x+x^2    )X^21 + (  x        )X^22 + (1          )X^23 +
     * (1  +x^2+x^3)X^24 + (           )X^25 + (  x+    x^3)X^26 + (  x+x^2+x^3)X^27 +
     * (    x^2    )X^28 + (1+x        )X^29 + (  x        )X^30 + (1+x+x^2    )X^31 +
     * (1          )X^32
     * </p>
     */
    private static final byte[][] SGF2K_004_FIELD_MINIMAL_POLYNOMIAL = new byte[][]{
        // (  x+x^2+x^3)X^0  + (        x^3)X^1  + (  x+x^2    )X^2  + (1+      x^3)X^3  +
        new byte[]{0b00001110}, new byte[]{0b00001000}, new byte[]{0b00000110}, new byte[]{0b00001001},
        // (  x        )X^4  + (           )X^5  + (  x+x^2    )X^6  + (  x        )X^7  +
        new byte[]{0b00000010}, new byte[]{0b00000000}, new byte[]{0b00000110}, new byte[]{0b00000010},
        // (1          )X^8  + (           )X^9  + (  x        )X^10 + (           )X^11 +
        new byte[]{0b00000001}, new byte[]{0b00000000}, new byte[]{0b00000010}, new byte[]{0b00000000},
        // (1+x+x^2    )X^12 + (1+x        )X^13 + (1+  x^2    )X^14 + (  x+x^2    )X^15 +
        new byte[]{0b00000111}, new byte[]{0b00000011}, new byte[]{0b00000101}, new byte[]{0b00000110},
        // (1+x+x^2    )X^16 + (           )X^17 + (           )X^18 + (           )X^19 +
        new byte[]{0b00000111}, new byte[]{0b00000000}, new byte[]{0b00000000}, new byte[]{0b00000000},
        // (1+x+x^2+x^3)X^20 + (  x+x^2    )X^21 + (  x        )X^22 + (1          )X^23 +
        new byte[]{0b00001111}, new byte[]{0b00000110}, new byte[]{0b00000010}, new byte[]{0b00000001},
        // (1  +x^2+x^3)X^24 + (           )X^25 + (  x+    x^3)X^26 + (  x+x^2+x^3)X^27 +
        new byte[]{0b00001101}, new byte[]{0b00000000}, new byte[]{0b00001010}, new byte[]{0b00001110},
        // (    x^2    )X^28 + (1+x        )X^29 + (  x        )X^30 + (1+x+x^2    )X^31 +
        new byte[]{0b00000100}, new byte[]{0b00000011}, new byte[]{0b00000010}, new byte[]{0b00000111},
        // (1          )X^32
        new byte[]{0b00000001},
    };

    /**
     * t = 4, F_{2^t} = GF(2^4), r = 32, field finite field
     */
    private static final FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> SGF2K_004_FIELD_FINITE_FIELD;

    static {
        SGF2K_004_FIELD_FINITE_FIELD = setFieldFiniteField(SGF2K_004_FIELD_MINIMAL_POLYNOMIAL);
    }

    /**
     * t = 8, F_{2^t} = GF(2^8), r = 16, f(X) =
     * <p>
     * (1+x+x^2+        x^5+x^6    )X^0  + (                           )X^1  +
     * (  x+x^2+x^3+x^4+    x^6    )X^2  + (1+  x^2+    x^4+x^5+x^6+x^7)X^3  +
     * (                           )X^4  + (                           )X^5  +
     * (        x^3+    x^5        )X^6  + (  x+        x^4+        x^7)X^7  +
     * (    x^2+x^3+x^4+x^5+    x^7)X^8  + (                           )X^9  +
     * (                           )X^10 + (1+x+x^2+        x^5+x^6    )X^11 +
     * (    x^2+x^3+        x^6+x^7)X^12 + (                           )X^13 +
     * (  x+        x^4+x^5+x^6    )X^14 + (                           )X^15 +
     * (1                          )X^16
     * </p>
     */
    private static final byte[][] SGF2K_008_FIELD_MINIMAL_POLYNOMIAL = new byte[][]{
        // (1+x+x^2+        x^5+x^6    )X^0  + (                           )X^1  +
        new byte[]{(byte) 0b01100111}, new byte[]{(byte) 0b00000000},
        // (  x+x^2+x^3+x^4+    x^6    )X^2  + (1+  x^2+    x^4+x^5+x^6+x^7)X^3  +
        new byte[]{(byte) 0b01011110}, new byte[]{(byte) 0b11110101},
        // (                           )X^4  + (                           )X^5  +
        new byte[]{(byte) 0b00000000}, new byte[]{(byte) 0b00000000},
        // (        x^3+    x^5        )X^6  + (  x+        x^4+        x^7)X^7  +
        new byte[]{(byte) 0b00101000}, new byte[]{(byte) 0b10010010},
        // (    x^2+x^3+x^4+x^5+    x^7)X^8  + (                           )X^9  +
        new byte[]{(byte) 0b10111100}, new byte[]{(byte) 0b00000000},
        // (                           )X^10 + (1+x+x^2+        x^5+x^6    )X^11 +
        new byte[]{(byte) 0b10111100}, new byte[]{(byte) 0b01100111},
        // (    x^2+x^3+        x^6+x^7)X^12 + (                           )X^13 +
        new byte[]{(byte) 0b11001100}, new byte[]{(byte) 0b00000000},
        // (  x+        x^4+x^5+x^6    )X^14 + (                           )X^15 +
        new byte[]{(byte) 0b01110010}, new byte[]{(byte) 0b00000000},
        // (1                          )X^16
        new byte[]{(byte) 0b00000001},
    };

    /**
     * t = 8, F_{2^t} = GF(2^8), r = 16, field finite field
     */
    private static final FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> SGF2K_008_FIELD_FINITE_FIELD;

    static {
        SGF2K_008_FIELD_FINITE_FIELD = setFieldFiniteField(SGF2K_008_FIELD_MINIMAL_POLYNOMIAL);
    }

    /**
     * t = 16, F_{2^t} = GF(2^16), r = 8, f(X) =
     * <p>
     * (1+      x^3+    x^5+x^6+            x^10+x^11+     x^13+     x^15)X^0 +
     * (                                                                 )X^1 +
     * (        x^3+    x^5+    x^7+        x^10+               x^14     )X^2 +
     * (                                                                 )X^3 +
     * (1+x+x^2+        x^5+x^6+    x^8+x^9+x^10+          x^13+     x^15)X^4 +
     * (            x^4+                    x^10+     x^12+     x^14     )X^5 +
     * (                                                                 )X^6 +
     * (  x+        x^4+x^5+        x^8+x^9+x^10+x^11+x^12+x^13+x^14     )X^7 +
     * (1                                                                )X^8
     * </p>
     */
    private static final byte[][] SGF2K_016_FIELD_MINIMAL_POLYNOMIAL = new byte[][]{
        // (1+      x^3+    x^5+x^6+            x^10+x^11+     x^13+     x^15)X^0 +
        new byte[]{(byte) 0b10101100, (byte) 0b01101001},
        // (                                                                 )X^1 +
        new byte[]{(byte) 0b00000000, (byte) 0b00000000},
        // (        x^3+    x^5+    x^7+        x^10+               x^14     )X^2 +
        new byte[]{(byte) 0b01000100, (byte) 0b10101000},
        // (                                                                 )X^3 +
        new byte[]{(byte) 0b00000000, (byte) 0b00000000},
        // (1+x+x^2+        x^5+x^6+    x^8+x^9+x^10+          x^13+     x^15)X^4 +
        new byte[]{(byte) 0b10100111, (byte) 0b01100111},
        // (            x^4+                    x^10+     x^12+     x^14     )X^5 +
        new byte[]{(byte) 0b01010100, (byte) 0b00010000},
        // (                                                                 )X^6 +
        new byte[]{(byte) 0b00000000, (byte) 0b00000000},
        // (  x+        x^4+x^5+        x^8+x^9+x^10+x^11+x^12+x^13+x^14     )X^7 +
        new byte[]{(byte) 0b01111111, (byte) 0b00110010},
        // (1                                                                )X^8
        new byte[]{(byte) 0b00000000, (byte) 0b00000001},
    };

    /**
     * t = 16, F_{2^t} = GF(2^16), r = 8, field finite field
     */
    private static final FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> SGF2K_016_FIELD_FINITE_FIELD;

    static {
        SGF2K_016_FIELD_FINITE_FIELD = setFieldFiniteField(SGF2K_016_FIELD_MINIMAL_POLYNOMIAL);
    }

    /**
     * t = 32, F_{2^t} = GF(2^32), r = 4, f(X) =
     * <p>
     * (                    x^4 +     x^6+      x^8 +x^9 +     x^11+
     * x^17+x^18+                                   x^26+x^27                    )X^0 +
     * (1+   x+   x^2 +     x^4 +     x^6+ x^7+ x^8+ x^9+ x^10+                    x^15+
     * x^18+     x^20+     x^22+          x^25+x^26+     x^28+x^29+x^30+x^31)X^1 +
     * (                    x^4+ x^5+                x^9+      x^11+x^12+x^13+x^14+
     * x^19+          x^22+x^23+x^24+x^25+          x^28+x^29          )X^2 +
     * (1+   x+   x^2+      x^4+      x^6+ x^7+      x^9+
     * x^16+               x^20+     x^22+x^23+                    x^28+     x^30+x^31)X^3 +
     * (1
     * )X^4
     * </p>
     */
    private static final byte[][] SGF2K_032_FIELD_MINIMAL_POLYNOMIAL = new byte[][]{
        // (                    x^4 +     x^6+      x^8 +x^9 +     x^11+
        //       x^17+x^18+                                   x^26+x^27                    )X^0 +
        new byte[]{
            (byte) 0b00001100, (byte) 0b00000110,
            (byte) 0b00001011, (byte) 0b01010000,
        },
        // (1+   x+   x^2 +     x^4 +     x^6+ x^7+ x^8+ x^9+ x^10+                    x^15+
        //            x^18+     x^20+     x^22+          x^25+x^26+     x^28+x^29+x^30+x^31)X^1 +
        new byte[]{
            (byte) 0b11110110, (byte) 0b01010100,
            (byte) 0b10000111, (byte) 0b11010111,
        },
        // (                    x^4+ x^5+                x^9+      x^11+x^12+x^13+x^14+
        //                 x^19+          x^22+x^23+x^24+x^25+          x^28+x^29          )X^2 +
        new byte[]{
            (byte) 0b00110011, (byte) 0b11001000,
            (byte) 0b01111010, (byte) 0b00110000,
        },
        // (1+   x+   x^2+      x^4+      x^6+ x^7+      x^9+
        //  x^16+               x^20+     x^22+x^23+                    x^28+     x^30+x^31)X^3 +
        new byte[]{
            (byte) 0b11010000, (byte) 0b11010001,
            (byte) 0b00000010, (byte) 0b11010111,
        },
        // (1
        //                                                                                 )X^4
        new byte[]{
            (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b00000000, (byte) 0b00000001,
        },
    };

    /**
     * t = 32, F_{2^t} = GF(2^32), r = 4, field finite field
     */
    private static final FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> SGF2K_032_FIELD_FINITE_FIELD;

    static {
        SGF2K_032_FIELD_FINITE_FIELD = setFieldFiniteField(SGF2K_032_FIELD_MINIMAL_POLYNOMIAL);
    }

    /**
     * t = 64, F_{2^t} = GF(2^64), r = 2, f(X) =
     * <p>
     * (1+                       x^5+                x^9+                x^13+
     * x^17+               x^21+               x^25+               x^29+
     * x^33+               x^37+               x^41+               x^45+
     * x^49+               x^53+               x^57+               x^61          )X^0 +
     * (1+        x^2+                x^6+ x^7+      x^9 +     x^11+x^12+x^13+x^14+x^15+
     * x^16+x^17+x^18+                    x^22+x^23+          x^26+     x^28+x^29+
     * x^33+     x^35+x^36+          x^39+     x^41+                    x^46+x^47+
     * x^51+x^52+x^53+     x^55+          x^58+x^59+x^60+     x^62+x^63)X^1 +
     * (1+
     * <p>
     * <p>
     * )X^2
     * </p>
     */
    private static final byte[][] SGF2K_064_FIELD_MINIMAL_POLYNOMIAL = new byte[][]{
        // (1+                       x^5+                x^9+                x^13+
        //       x^17+               x^21+               x^25+               x^29+
        //       x^33+               x^37+               x^41+               x^45+
        //       x^49+               x^53+               x^57+               x^61          )X^0 +
        new byte[]{
            (byte) 0b00100010, (byte) 0b00100010, (byte) 0b00100010, (byte) 0b00100010,
            (byte) 0b00100010, (byte) 0b00100010, (byte) 0b00100010, (byte) 0b00100010,
        },
        // (1+        x^2+                x^6+ x^7+      x^9 +     x^11+x^12+x^13+x^14+x^15+
        //  x^16+x^17+x^18+                    x^22+x^23+          x^26+     x^28+x^29+
        //       x^33+     x^35+x^36+          x^39+     x^41+                    x^46+x^47+
        //                 x^51+x^52+x^53+     x^55+          x^58+x^59+x^60+     x^62+x^63)X^1 +
        new byte[]{
            (byte) 0b11011100, (byte) 0b10111000, (byte) 0b11000010, (byte) 0b10011010,
            (byte) 0b00110100, (byte) 0b11000111, (byte) 0b11111010, (byte) 0b11000101,
        },
        // (1+
        //
        //
        //                                                                                 )X^2
        new byte[]{
            (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000001,
        }
    };

    /**
     * t = 64, F_{2^t} = GF(2^64), r = 2, field finite field
     */
    private static final FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> SGF2K_064_FIELD_FINITE_FIELD;

    static {
        SGF2K_064_FIELD_FINITE_FIELD = setFieldFiniteField(SGF2K_064_FIELD_MINIMAL_POLYNOMIAL);
    }

    private static FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> setFieldFiniteField(byte[][] fieldMinimalPolynomial) {
        int subfieldL = FIELD_L / (fieldMinimalPolynomial.length - 1);
        FiniteField<UnivariatePolynomialZp64> subfieldFiniteField = Gf2eManager.getFiniteField(subfieldL);
        UnivariatePolynomial<UnivariatePolynomialZp64> polyMinPolynomial = UnivariatePolynomial.zero(subfieldFiniteField);
        for (int i = 0; i < fieldMinimalPolynomial.length; i++) {
            polyMinPolynomial.set(i, Gf2xUtils.byteArrayToRings(fieldMinimalPolynomial[i]));
        }
        return Rings.GF(polyMinPolynomial);
    }

    /**
     * Gets field minimal field.
     *
     * @param subfieldL subfield L.
     * @return field minimal field.
     */
    public static byte[][] getFieldMinimalPolynomial(int subfieldL) {
        switch (subfieldL) {
            case 2:
                return SGF2K_002_FIELD_MINIMAL_POLYNOMIAL;
            case 4:
                return SGF2K_004_FIELD_MINIMAL_POLYNOMIAL;
            case 8:
                return SGF2K_008_FIELD_MINIMAL_POLYNOMIAL;
            case 16:
                return SGF2K_016_FIELD_MINIMAL_POLYNOMIAL;
            case 32:
                return SGF2K_032_FIELD_MINIMAL_POLYNOMIAL;
            case 64:
                return SGF2K_064_FIELD_MINIMAL_POLYNOMIAL;
            default:
                throw new IllegalArgumentException("Invalid subfield L, must be in {2, 4, 8, 16, 32, 64}: " + subfieldL);
        }
    }

    /**
     * Gets field finite field.
     *
     * @param subfieldL subfield L.
     * @return field finite field.
     */
    public static FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> getFieldFiniteField(int subfieldL) {
        switch (subfieldL) {
            case 2:
                return SGF2K_002_FIELD_FINITE_FIELD;
            case 4:
                return SGF2K_004_FIELD_FINITE_FIELD;
            case 8:
                return SGF2K_008_FIELD_FINITE_FIELD;
            case 16:
                return SGF2K_016_FIELD_FINITE_FIELD;
            case 32:
                return SGF2K_032_FIELD_FINITE_FIELD;
            case 64:
                return SGF2K_064_FIELD_FINITE_FIELD;
            default:
                throw new IllegalArgumentException("Invalid subfield L, must be in {2, 4, 8, 16, 32, 64}: " + subfieldL);
        }
    }
}
