/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

/**
 * Edwards point test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/EdwardsPointTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/7
 */
public class CafeEdwardsPointTest {
    /**
     * Compressed Edwards Y form of the Ed25519 base point.
     */
    private static final CafeEdwardsCompressedPoint ED25519_BASE_COMPRESSED = new CafeEdwardsCompressedPoint(Hex.decode(
        "5866666666666666666666666666666666666666666666666666666666666666"
    ));
    /**
     * Compressed Edwards Y form of 2 * base point.
     */
    private static final CafeEdwardsCompressedPoint BASE_MUL_2_COMPRESSED = new CafeEdwardsCompressedPoint(Hex.decode(
        "c9a3f86aae465f0e56513864510f3997561fa2c9e85ea21dc2292309f3cd6022"
    ));
    /**
     * Compressed Edwards Y form of 16 * base point.
     */
    private static final CafeEdwardsCompressedPoint BASE_MUL_16_COMPRESSED = new CafeEdwardsCompressedPoint(Hex.decode(
        "eb2767c137ab7ad8279c078eff116ab0786ead3a2e0f989f72c37f82f2969670"
    ));
    /**
     * 4493907448824000747700850167940867464579944529806937181821189941592931634714
     */
    static final CafeScalar A_SCALAR = new CafeScalar(Hex.decode(
        "1a0e978a90f6622d3747023f8ad8264da758aa1b88e040d1589e7b7f2376ef09"
    ));
    /**
     * 2506056684125797857694181776241676200180934651973138769173342316833279714961
     */
    private static final CafeScalar B_SCALAR = new CafeScalar(Hex.decode(
        "91267acf25c2091ba217747b66f0b32e9df2a56741cfdac456a7d4aab8608a05"
    ));
    /**
     * A_SCALAR * basepoint, computed with ed25519.py
     */
    static final CafeEdwardsCompressedPoint A_MUL_BASE = new CafeEdwardsCompressedPoint(Hex.decode(
        "ea27e26053df1b5956f14d5dec3c34c384a269b74cc3803ea8e2e7c9425e40a5"
    ));
    /**
     * A_SCALAR * (A_MUL_BASE) + B_SCALAR * BASE computed with ed25519.py
     */
    private static final CafeEdwardsCompressedPoint DOUBLE_SCALAR_BASE_MUL_RESULT = new CafeEdwardsCompressedPoint(Hex.decode(
        "7dfd6c45af6d6e0eba20371a236459c4c0468343de704b85096ffe354f132b42"
    ));

    /**
     * The 8-torsion subgroup $\mathcal E [8]$.
     * <p>
     * In the case of Curve25519, it is cyclic; the $i$-th element of the array is
     * $[i]P$, where $P$ is a point of order $8$ generating $\mathcal E[8]$.
     * <p>
     * Thus $\mathcal E[8]$ is the points indexed by 0,2,4,6, and $\mathcal E[2]$ is
     * the points indexed by 0,4.
     */
    private static final CafeEdwardsCompressedPoint[] EIGHT_TORSION_COMPRESSED = new CafeEdwardsCompressedPoint[]{
        new CafeEdwardsCompressedPoint(Hex.decode("0100000000000000000000000000000000000000000000000000000000000000")),
        new CafeEdwardsCompressedPoint(Hex.decode("c7176a703d4dd84fba3c0b760d10670f2a2053fa2c39ccc64ec7fd7792ac037a")),
        new CafeEdwardsCompressedPoint(Hex.decode("0000000000000000000000000000000000000000000000000000000000000080")),
        new CafeEdwardsCompressedPoint(Hex.decode("26e8958fc2b227b045c3f489f2ef98f0d5dfac05d3c63339b13802886d53fc05")),
        new CafeEdwardsCompressedPoint(Hex.decode("ecffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f")),
        new CafeEdwardsCompressedPoint(Hex.decode("26e8958fc2b227b045c3f489f2ef98f0d5dfac05d3c63339b13802886d53fc85")),
        new CafeEdwardsCompressedPoint(Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")),
        new CafeEdwardsCompressedPoint(Hex.decode("c7176a703d4dd84fba3c0b760d10670f2a2053fa2c39ccc64ec7fd7792ac03fa")),
    };

    @Test
    public void testDecompress() {
        CafeEdwardsPoint basePoint = ED25519_BASE_COMPRESSED.decompress();
        Assert.assertEquals(ED25519_BASE_COMPRESSED, basePoint.compress());
    }

    @Test
    public void testCompress() {
        // Manually set the high bit of the last byte to flip the sign
        byte[] negBaseBytes = ED25519_BASE_COMPRESSED.encode();
        negBaseBytes[CafeEdwardsCompressedPoint.BYTE_SIZE - 1] |= 1 << 7;
        CafeEdwardsPoint negBasePoint = new CafeEdwardsCompressedPoint(negBaseBytes).decompress();
        // Test projective coordinates exactly since we know they should only differ by a flipped sign.
        Assert.assertEquals(CafeConstants.ED25519_BASE_POINT.x.neg(), negBasePoint.x);
        Assert.assertEquals(CafeConstants.ED25519_BASE_POINT.y, negBasePoint.y);
        Assert.assertEquals(CafeConstants.ED25519_BASE_POINT.z, negBasePoint.z);
        Assert.assertEquals(CafeConstants.ED25519_BASE_POINT.t.neg(), negBasePoint.t);
    }

    @Test
    public void testCmov() {
        Assert.assertEquals(CafeConstants.ED25519_BASE_POINT, CafeConstants.ED25519_BASE_POINT.cmove(CafeEdwardsPoint.IDENTITY, 0));
        Assert.assertEquals(CafeEdwardsPoint.IDENTITY, CafeConstants.ED25519_BASE_POINT.cmove(CafeEdwardsPoint.IDENTITY, 1));
        Assert.assertEquals(CafeEdwardsPoint.IDENTITY, CafeEdwardsPoint.IDENTITY.cmove(CafeConstants.ED25519_BASE_POINT, 0));
        Assert.assertEquals(CafeConstants.ED25519_BASE_POINT, CafeEdwardsPoint.IDENTITY.cmove(CafeConstants.ED25519_BASE_POINT, 1));
    }

    @Test
    public void testAdd() {
        CafeEdwardsPoint baseMul2 = CafeConstants.ED25519_BASE_POINT.add(CafeConstants.ED25519_BASE_POINT);
        Assert.assertEquals(BASE_MUL_2_COMPRESSED, baseMul2.compress());
    }

    @Test
    public void testProjectiveNeilsAdd() {
        CafeEdwardsPoint baseMul2 = CafeConstants.ED25519_BASE_POINT
            .add(CafeConstants.ED25519_BASE_POINT.toProjectiveNiels())
            .toExtended();
        Assert.assertEquals(CafeEdwardsPointTest.BASE_MUL_2_COMPRESSED, baseMul2.compress());
    }

    @Test
    public void testAffineNielsAdd() {
        CafeEdwardsPoint baseMul2 = CafeConstants.ED25519_BASE_POINT
            .add(CafeConstants.ED25519_BASE_POINT.toAffineNiels())
            .toExtended();
        Assert.assertEquals(CafeEdwardsPointTest.BASE_MUL_2_COMPRESSED, baseMul2.compress());
    }

    @Test
    public void testDbl() {
        CafeEdwardsPoint baseMul2 = CafeConstants.ED25519_BASE_POINT.dbl();
        Assert.assertEquals(BASE_MUL_2_COMPRESSED, baseMul2.compress());
    }

    @Test
    public void testSub() {
        CafeEdwardsPoint baseMul2 = CafeConstants.ED25519_BASE_POINT.dbl();
        Assert.assertEquals(CafeConstants.ED25519_BASE_POINT, baseMul2.sub(CafeConstants.ED25519_BASE_POINT));
    }

    @Test
    public void testNeg() {
        Assert.assertEquals(
            CafeConstants.ED25519_BASE_POINT.neg(), CafeEdwardsPoint.IDENTITY.sub(CafeConstants.ED25519_BASE_POINT)
        );
    }

    @Test
    public void testMul() {
        Assert.assertEquals(A_MUL_BASE, CafeConstants.ED25519_BASE_POINT.mul(A_SCALAR).compress());
    }

    @Test
    public void testDoubleScalarBaseMul() {
        // Little-endian
        CafeScalar zero = CafeScalar.ZERO;
        CafeScalar one = CafeScalar.ONE;
        CafeScalar two = new CafeScalar(Hex.decode(
            "0200000000000000000000000000000000000000000000000000000000000000"
        ));
        CafeScalar a = new CafeScalar(Hex.decode(
            "d072f8dd9c07fa7bc8d22a4b325d26301ee9202f6db89aa7c3731529e37e437c"
        ));
        CafeEdwardsPoint pointA = new CafeEdwardsCompressedPoint(Hex.decode(
            "d4cf8595571830644bd14af416954d09ab7159751ad9e0f7a6cbd92379e71a66"
        )).decompress();
        CafeEdwardsPoint basePoint = CafeConstants.ED25519_BASE_POINT;
        CafeEdwardsPoint identityPoint = CafeEdwardsPoint.IDENTITY;

        // 0 * I + 0 * B = I
        Assert.assertEquals(identityPoint, CafeEdwardsPoint.doubleScalarBaseMul(zero, identityPoint, zero));
        // 1 * I + 0 * B = I
        Assert.assertEquals(identityPoint, CafeEdwardsPoint.doubleScalarBaseMul(one, identityPoint, zero));
        // 1 * I + 1 * B = B
        Assert.assertEquals(basePoint, CafeEdwardsPoint.doubleScalarBaseMul(one, identityPoint, one));
        // 1 * B + 1 * B = 2 * B
        Assert.assertEquals(basePoint.dbl(), CafeEdwardsPoint.doubleScalarBaseMul(one, basePoint, one));
        // 1 * B + 2 * B = 3 * B
        Assert.assertEquals(basePoint.dbl().add(basePoint), CafeEdwardsPoint.doubleScalarBaseMul(one, basePoint, two));
        // 2 * B + 2 * B = 4 * B
        Assert.assertEquals(basePoint.dbl().dbl(), CafeEdwardsPoint.doubleScalarBaseMul(two, basePoint, two));

        // 0 * B + a * B = A
        Assert.assertEquals(pointA, CafeEdwardsPoint.doubleScalarBaseMul(zero, basePoint, a));
        // a * B + 0 * B = A
        Assert.assertEquals(pointA, CafeEdwardsPoint.doubleScalarBaseMul(a, basePoint, zero));
        // a * B + a * B = 2 * A
        Assert.assertEquals(pointA.dbl(), CafeEdwardsPoint.doubleScalarBaseMul(a, basePoint, a));
    }

    @Test
    public void testDoubleScalarMulBase() {
        CafeEdwardsPoint pointA = A_MUL_BASE.decompress();
        CafeEdwardsPoint result = CafeEdwardsPoint.doubleScalarBaseMul(A_SCALAR, pointA, B_SCALAR);
        Assert.assertEquals(DOUBLE_SCALAR_BASE_MUL_RESULT, result.compress());
    }

    @Test
    public void testPow2Mul() {
        Assert.assertEquals(BASE_MUL_16_COMPRESSED.decompress(), CafeConstants.ED25519_BASE_POINT.pow2Mul(4));
    }

    @Test
    public void testIsIdentity() {
        Assert.assertTrue(CafeEdwardsPoint.IDENTITY.isIdentity());
        Assert.assertFalse(CafeConstants.ED25519_BASE_POINT.isIdentity());
    }

    @Test
    public void testHashSmallOrder() {
        // The base point has large prime order
        Assert.assertFalse(CafeConstants.ED25519_BASE_POINT.hasSmallOrder());
        // EIGHT_TORSION_COMPRESSED has all points of small order.
        for (CafeEdwardsCompressedPoint torsionCompressedPoint : EIGHT_TORSION_COMPRESSED) {
            Assert.assertTrue(torsionCompressedPoint.decompress().hasSmallOrder());
        }
    }

    @Test
    public void isTorsionFree() {
        // The base point is torsion-free.
        Assert.assertTrue(CafeConstants.ED25519_BASE_POINT.isTorsionFree());

        // Adding the identity leaves it torsion-free.
        Assert.assertTrue(CafeConstants.ED25519_BASE_POINT.add(CafeEdwardsPoint.IDENTITY).isTorsionFree());

        // Adding any of the 8-torsion points to it (except the identity) affects the result.
        Assert.assertEquals(EIGHT_TORSION_COMPRESSED[0], CafeEdwardsPoint.IDENTITY.compress());
        for (int i = 1; i < EIGHT_TORSION_COMPRESSED.length; i++) {
            CafeEdwardsPoint withTorsion = CafeConstants.ED25519_BASE_POINT.add(EIGHT_TORSION_COMPRESSED[i].decompress());
            Assert.assertFalse(withTorsion.isTorsionFree());
        }
    }
}
