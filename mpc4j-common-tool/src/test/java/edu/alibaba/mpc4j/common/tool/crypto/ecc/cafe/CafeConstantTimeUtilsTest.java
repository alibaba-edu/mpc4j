/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * Constant-time functions test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/subtle/ConstantTimeTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/6
 */
public class CafeConstantTimeUtilsTest {

    @Test
    public void equalOnByte() {
        Assert.assertEquals(1, CafeConstantTimeUtils.equal(0, 0));
        Assert.assertEquals(1, CafeConstantTimeUtils.equal(1, 1));
        Assert.assertEquals(0, CafeConstantTimeUtils.equal(1, 0));
        Assert.assertEquals(0, CafeConstantTimeUtils.equal(1, 127));
        Assert.assertEquals(0, CafeConstantTimeUtils.equal(-127, 127));
        Assert.assertEquals(1, CafeConstantTimeUtils.equal(-42, -42));
        Assert.assertEquals(1, CafeConstantTimeUtils.equal(255, 255));
        Assert.assertEquals(0, CafeConstantTimeUtils.equal(-255, -256));
    }

    @Test
    public void equalOnByteArraysWithSingleDifference() {
        byte[] zero = new byte[32];
        byte[] one = new byte[32];
        one[0] = 1;

        Assert.assertEquals(1, CafeConstantTimeUtils.equal(zero, zero));
        Assert.assertEquals(1, CafeConstantTimeUtils.equal(one, one));
        Assert.assertEquals(0, CafeConstantTimeUtils.equal(one, zero));
        Assert.assertEquals(0, CafeConstantTimeUtils.equal(zero, one));
    }

    @Test
    public void equalOnByteArraysWithDifferentLengths() {
        byte[] zeroNine = new byte[9];
        byte[] zeroTen = new byte[10];

        Assert.assertEquals(1, CafeConstantTimeUtils.equal(zeroNine, zeroNine));
        Assert.assertEquals(1, CafeConstantTimeUtils.equal(zeroTen, zeroTen));
        Assert.assertEquals(0, CafeConstantTimeUtils.equal(zeroNine, zeroTen));
        Assert.assertEquals(0, CafeConstantTimeUtils.equal(zeroTen, zeroNine));
    }

    @Test
    public void equalOnByteArraysWithRandomData() {
        Random random = new Random(758094325);
        for (int i = 1; i < 33; i++) {
            byte[] a = new byte[i];
            byte[] b = new byte[i];
            // randomly choose two byte arrays
            random.nextBytes(a);
            random.nextBytes(b);
            Assert.assertEquals(1, CafeConstantTimeUtils.equal(a, a));
            Assert.assertEquals(1, CafeConstantTimeUtils.equal(b, b));
            Assert.assertEquals(0, CafeConstantTimeUtils.equal(a, b));
            Assert.assertEquals(0, CafeConstantTimeUtils.equal(b, a));
            // Test mutation in MSB
            byte[] aPrime = BytesUtils.clone(a);
            Assert.assertEquals(1, CafeConstantTimeUtils.equal(a, aPrime));
            // mutate the most significant bit
            aPrime[i - 1] += 1;
            Assert.assertEquals(0, CafeConstantTimeUtils.equal(a, aPrime));
        }
    }

    @Test
    public void isNegative() {
        Assert.assertEquals(0, CafeConstantTimeUtils.isNeg(0));
        Assert.assertEquals(0, CafeConstantTimeUtils.isNeg(1));
        Assert.assertEquals(1, CafeConstantTimeUtils.isNeg(-1));
        Assert.assertEquals(0, CafeConstantTimeUtils.isNeg(32));
        Assert.assertEquals(1, CafeConstantTimeUtils.isNeg(-100));
        Assert.assertEquals(0, CafeConstantTimeUtils.isNeg(127));
        Assert.assertEquals(1, CafeConstantTimeUtils.isNeg(-255));
    }

    @Test
    public void bit() {
        Assert.assertEquals(0, CafeConstantTimeUtils.bit(new byte[]{0b00000000}, 0));
        Assert.assertEquals(1, CafeConstantTimeUtils.bit(new byte[]{0b00001000}, 3));
        Assert.assertEquals(1, CafeConstantTimeUtils.bit(new byte[]{0b00000001, 0b00000010, 0b00000011}, 9));
        Assert.assertEquals(0, CafeConstantTimeUtils.bit(new byte[]{0b00000001, 0b00000010, 0b00000011}, 15));
        Assert.assertEquals(1, CafeConstantTimeUtils.bit(new byte[]{0b00000001, 0b00000010, 0b00000011}, 16));
    }
}
