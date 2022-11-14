/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.junit.Assert;
import org.junit.Test;

/**
 * Ristretto compressed point test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/CompressedRistrettoTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class CafeRistrettoCompressedPointTest {

    @Test
    public void testValid() {
        new CafeRistrettoCompressedPoint(new byte[CafeRistrettoCompressedPoint.BYTE_SIZE]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidShort() {
        new CafeRistrettoCompressedPoint(new byte[CafeRistrettoCompressedPoint.BYTE_SIZE - 1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tsetInvalidLong() {
        new CafeRistrettoCompressedPoint(new byte[CafeRistrettoCompressedPoint.BYTE_SIZE + 1]);
    }

    @Test
    public void testEncode() {
        byte[] s = new byte[CafeRistrettoCompressedPoint.BYTE_SIZE];
        s[0] = 0x1f;
        Assert.assertArrayEquals(s, new CafeRistrettoCompressedPoint(s).encode());
    }
}
