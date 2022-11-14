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
 * constants test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/ConstantsTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class CafeConstantsTest {

    @Test
    public void testCheckEdwardsD() {
        Assert.assertEquals(
            CafeFieldElement.decode(Hex.decode("a3785913ca4deb75abd841414d0a700098e879777940c78c73fe6f2bee6c0352")),
            CafeConstants.EDWARDS_D
        );
    }

    @Test
    public void testCheckEdwards2D() {
        Assert.assertEquals(
            CafeConstants.EDWARDS_D.mul(CafeFieldElement.ONE.add(CafeFieldElement.ONE)),
            CafeConstants.EDWARDS_2D
        );
    }

    @Test
    public void testCheckSqrtAdSubOne() {
        Assert.assertEquals(
            CafeConstants.SQRT_AD_MINUS_ONE.sqr().add(CafeFieldElement.ONE).neg(),
            CafeConstants.EDWARDS_D
        );
    }

    @Test
    public void checkInvSqrtASubD() {
        Assert.assertEquals(
            CafeConstants.INVSQRT_A_MINUS_D.inv().sqr().add(CafeFieldElement.ONE).neg(),
            CafeConstants.EDWARDS_D
        );
    }

    @Test
    public void testCheckSqrtM1() {
        Assert.assertEquals(
            CafeFieldElement.decode(Hex.decode("b0a00e4a271beec478e42fad0618432fa7d7fb3d99004d2b0bdfc14f8024832b")),
            CafeConstants.SQRT_M1
        );
    }

    @Test
    public void testCheckEd25519BasePoint() {
        CafeEdwardsCompressedPoint encodeBasePoint = new CafeEdwardsCompressedPoint(
                Hex.decode("5866666666666666666666666666666666666666666666666666666666666666")
        );
        CafeEdwardsPoint basePoint = encodeBasePoint.decompress();
        Assert.assertEquals(basePoint.x, CafeConstants.ED25519_BASE_POINT.x);
        Assert.assertEquals(basePoint.y, CafeConstants.ED25519_BASE_POINT.y);
        Assert.assertEquals(basePoint.z, CafeConstants.ED25519_BASE_POINT.z);
        Assert.assertEquals(basePoint.t, CafeConstants.ED25519_BASE_POINT.t);
    }
}
