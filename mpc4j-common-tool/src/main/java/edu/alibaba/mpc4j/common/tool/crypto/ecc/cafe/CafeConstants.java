/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.Curve25519FieldUtils;

/**
 * Various constants and useful parameters. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/Constants.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public final class CafeConstants {
    /**
     * The order of the Ed25519 base point, $\ell = 2^{252} + 27742317777372353535851937790883648493$.
     */
    static final CafeScalar BASE_POINT_ORDER = new CafeScalar(new byte[]{
        (byte) 0xed, (byte) 0xd3, (byte) 0xf5, (byte) 0x5c, (byte) 0x1a, (byte) 0x63, (byte) 0x12, (byte) 0x58,
        (byte) 0xd6, (byte) 0x9c, (byte) 0xf7, (byte) 0xa2, (byte) 0xde, (byte) 0xf9, (byte) 0xde, (byte) 0x14,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10,
    });

    /**
     * The unpacked form of the Ed25519 base point order $\ell$.
     */
    static final CafeUnpackedScalar L = CafeUnpackedScalar.decode(BASE_POINT_ORDER.encode());

    /**
     * $\ell * \text{LFACTOR} = -1 \bmod 2^{29}$
     */
    static final int L_FACTOR = 0x12547e1b;

    /**
     * $= R \bmod \ell$ where $R = 2^{261}$
     */
    static final CafeUnpackedScalar R = new CafeUnpackedScalar(new int[]{
        0x114df9ed, 0x1a617303, 0x0f7c098c, 0x16793167, 0x1ffd656e, 0x1fffffff, 0x1fffffff, 0x1fffffff, 0x000fffff,
    });

    /**
     * $= R^2 \bmod \ell$ where $R = 2^{261}$
     */
    static final CafeUnpackedScalar RR = new CafeUnpackedScalar(new int[]{
        0x0b5f9d12, 0x1e141b17, 0x158d7f3d, 0x143f3757, 0x1972d781, 0x042feb7c, 0x1ceec73d, 0x1e184d1e, 0x0005046d,
    });
    /**
     * Edwards $d$ value, equal to $-121665/121666 \bmod p$.
     */
    static final CafeFieldElement EDWARDS_D = new CafeFieldElement(Curve25519FieldUtils.EDWARDS_D_INTS);
    /**
     * Edwards $-d$ value, equal to $121665/121666 \bmod p$.
     */
    static final CafeFieldElement NEG_EDWARDS_D = EDWARDS_D.neg();
    /**
     * Edwards $2*d$ value, equal to $2*(-121665/121666) \bmod p$.
     */
    static final CafeFieldElement EDWARDS_2D = new CafeFieldElement(new int[]{
        -21827239, -5839606, -30745221, 13898782, 229458, 15978800, -12551817, -6495438, 29715968, 9444199,
    });
    /**
     * $= 1 - d^2$, where $d$ is the Edwards curve parameter.
     */
    static final CafeFieldElement ONE_MINUS_D_SQ = CafeFieldElement.ONE.sub(EDWARDS_D.sqr());
    /**
     * $= (d - 1)^2$, where $d$ is the Edwards curve parameter.
     */
    static final CafeFieldElement D_MINUS_ONE_SQ = EDWARDS_D.sub(CafeFieldElement.ONE).sqr();
    /**
     * $= \sqrt{a*d - 1}$, where $a = -1 \bmod p$, $d$ are the Edwards curve parameters.
     */
    static final CafeFieldElement SQRT_AD_MINUS_ONE = new CafeFieldElement(new int[]{
        24849947, -153582, -23613485, 6347715, -21072328, -667138, -25271143, -15367704, -870347, 14525639,
    });
    /**
     * $= 1/\sqrt{a-d}$, where $a = -1 \bmod p$, $d$ are the Edwards curve parameters.
     */
    static final CafeFieldElement INVSQRT_A_MINUS_D = new CafeFieldElement(new int[]{
        6111485, 4156064, -27798727, 12243468, -25904040, 120897, 20826367, -7060776, 6093568, -1986012,
    });
    /**
     * Precomputed value of one of the square roots of -1 (mod p).
     */
    static final CafeFieldElement SQRT_M1 = new CafeFieldElement(Curve25519FieldUtils.SQRT_M1_INTS);
    /**
     * The Ed25519 basepoint, as an EdwardsPoint.
     */
    public static final CafeEdwardsPoint ED25519_BASE_POINT = new CafeEdwardsPoint(
        new CafeFieldElement(new int[]{
            -14297830, -7645148, 16144683, -16471763, 27570974, -2696100, -26142465, 8378389, 20764389, 8758491,
        }),
        new CafeFieldElement(new int[]{
            -26843541, -6710886, 13421773, -13421773, 26843546, 6710886, -13421773, 13421773, -26843546, -6710886,
        }),
        new CafeFieldElement(new int[]{
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        }),
        new CafeFieldElement(new int[]{
            28827062, -6116119, -27349572, 244363, 8635006, 11264893, 19351346, 13413597, 16611511, -6414980,
        })
    );

    /**
     * Table containing pre-computed multiples of the Ed25519 base point.
     */
    static final CafeEdwardsPrecomputeTable ED25519_BASE_POINT_TABLE = new CafeEdwardsPrecomputeTable(ED25519_BASE_POINT);

    /**
     * Odd multiples of the Ed25519 base point.
     */
    static final CafeAffineNielsPoint.NafLookupTable AFFINE_ODD_MULTIPLES_OF_BASE_POINT
        = CafeAffineNielsPoint.buildNafLookupTable(ED25519_BASE_POINT);

    /**
     * The ristretto255 generator, as a RistrettoElement.
     */
    static final CafeRistrettoPoint RISTRETTO_GENERATOR = new CafeRistrettoPoint(ED25519_BASE_POINT);

    /**
     * Table containing pre-computed multiples of the ristretto255 generator.
     */
    static final CafeRistrettoGeneratorTable RISTRETTO_GENERATOR_TABLE = new CafeRistrettoGeneratorTable(RISTRETTO_GENERATOR);
}
