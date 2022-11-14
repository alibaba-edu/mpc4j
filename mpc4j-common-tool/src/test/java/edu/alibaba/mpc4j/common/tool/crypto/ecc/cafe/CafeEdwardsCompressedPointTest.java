/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.junit.Assert;
import org.junit.Test;

/**
 * Edwards compressed point test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/CompressedEdwardsYTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/9
 */
public class CafeEdwardsCompressedPointTest {

    @Test
    public void testValid() {
        new CafeEdwardsCompressedPoint(new byte[CafeEdwardsCompressedPoint.BYTE_SIZE]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidShort() {
        new CafeEdwardsCompressedPoint(new byte[CafeEdwardsCompressedPoint.BYTE_SIZE - 1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLong() {
        new CafeEdwardsCompressedPoint(new byte[CafeEdwardsCompressedPoint.BYTE_SIZE + 1]);
    }

    @Test
    public void testEncode() {
        byte[] s = new byte[CafeEdwardsCompressedPoint.BYTE_SIZE];
        s[0] = 0x1f;
        Assert.assertEquals(s, new CafeEdwardsCompressedPoint(s).encode());
    }
}
