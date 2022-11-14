/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.junit.Assert;
import org.junit.Test;

/**
 * FieldElement test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/FieldElementTest.java
 * </p>
 * Test vectors and the tests they are used in are from curve25519-dalek.
 * <p>
 * github.com/dalek-cryptography/curve25519-dalek/blob/4bdccd7b7c394d9f8ffc4b29d5acc23c972f3d7a/src/field.rs#L280-L301
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/7
 */
public class CafeFieldElementTest {
    /**
     * Random element a of GF(2^255-19), from Sage, a =
     * 10703145068883540813293858232352184442332212228051251926706380353716438957572
     */
    private static final byte[] A_BYTES = {
        (byte) 0x04, (byte) 0xfe, (byte) 0xdf, (byte) 0x98, (byte) 0xa7, (byte) 0xfa, (byte) 0x0a, (byte) 0x68,
        (byte) 0x84, (byte) 0x92, (byte) 0xbd, (byte) 0x59, (byte) 0x08, (byte) 0x07, (byte) 0xa7, (byte) 0x03,
        (byte) 0x9e, (byte) 0xd1, (byte) 0xf6, (byte) 0xf2, (byte) 0xe1, (byte) 0xd9, (byte) 0xe2, (byte) 0xa4,
        (byte) 0xa4, (byte) 0x51, (byte) 0x47, (byte) 0x36, (byte) 0xf3, (byte) 0xc3, (byte) 0xa9, (byte) 0x17,
    };
    /**
     * Byte representation of a^2
     */
    static final byte[] A_SQUARE_BYTES = {
        (byte) 0x75, (byte) 0x97, (byte) 0x24, (byte) 0x9e, (byte) 0xe6, (byte) 0x06, (byte) 0xfe, (byte) 0xab,
        (byte) 0x24, (byte) 0x04, (byte) 0x56, (byte) 0x68, (byte) 0x07, (byte) 0x91, (byte) 0x2d, (byte) 0x5d,
        (byte) 0x0b, (byte) 0x0f, (byte) 0x3f, (byte) 0x1c, (byte) 0xb2, (byte) 0x6e, (byte) 0xf2, (byte) 0xe2,
        (byte) 0x63, (byte) 0x9c, (byte) 0x12, (byte) 0xba, (byte) 0x73, (byte) 0x0b, (byte) 0xe3, (byte) 0x62,
    };
    /**
     * Byte representation of 1/a
     */
    static final byte[] A_INVERSE_BYTES = {
        (byte) 0x96, (byte) 0x1b, (byte) 0xcd, (byte) 0x8d, (byte) 0x4d, (byte) 0x5e, (byte) 0xa2, (byte) 0x3a,
        (byte) 0xe9, (byte) 0x36, (byte) 0x37, (byte) 0x93, (byte) 0xdb, (byte) 0x7b, (byte) 0x4d, (byte) 0x70,
        (byte) 0xb8, (byte) 0x0d, (byte) 0xc0, (byte) 0x55, (byte) 0xd0, (byte) 0x4c, (byte) 0x1d, (byte) 0x7b,
        (byte) 0x90, (byte) 0x71, (byte) 0xd8, (byte) 0xe9, (byte) 0xb6, (byte) 0x18, (byte) 0xe6, (byte) 0x30,
    };
    /**
     * Byte representation of a^((p-5)/8)
     */
    static final byte[] A_POWER_P_MINUS_5_DIV_8_BYTES = {
        (byte) 0x6a, (byte) 0x4f, (byte) 0x24, (byte) 0x89, (byte) 0x1f, (byte) 0x57, (byte) 0x60, (byte) 0x36,
        (byte) 0xd0, (byte) 0xbe, (byte) 0x12, (byte) 0x3c, (byte) 0x8f, (byte) 0xf5, (byte) 0xb1, (byte) 0x59,
        (byte) 0xe0, (byte) 0xf0, (byte) 0xb8, (byte) 0x1b, (byte) 0x20, (byte) 0xd2, (byte) 0xb5, (byte) 0x1f,
        (byte) 0x15, (byte) 0x21, (byte) 0xf9, (byte) 0xe3, (byte) 0xe1, (byte) 0x61, (byte) 0x21, (byte) 0x55,
    };

    @Test
    public void testMul() {
        final CafeFieldElement a = CafeFieldElement.decode(A_BYTES);
        final CafeFieldElement squareA = CafeFieldElement.decode(A_SQUARE_BYTES);
        Assert.assertEquals(squareA, a.mul(a));
    }

    @Test
    public void testSquare() {
        final CafeFieldElement a = CafeFieldElement.decode(A_BYTES);
        final CafeFieldElement squareA = CafeFieldElement.decode(A_SQUARE_BYTES);
        Assert.assertEquals(squareA, a.sqr());
    }

    @Test
    public void testSqrDbl() {
        final CafeFieldElement a = CafeFieldElement.decode(A_BYTES);
        final CafeFieldElement squareA = CafeFieldElement.decode(A_SQUARE_BYTES);
        Assert.assertEquals(squareA.add(squareA), a.sqrDbl());
    }

    @Test
    public void testInv() {
        final CafeFieldElement a = CafeFieldElement.decode(A_BYTES);
        final CafeFieldElement inverseA = CafeFieldElement.decode(A_INVERSE_BYTES);
        final CafeFieldElement actualInverseA = a.inv();
        Assert.assertEquals(inverseA, actualInverseA);
        Assert.assertEquals(CafeFieldElement.ONE, a.mul(actualInverseA));
    }

    @Test
    public void testSqrtRatioM1() {
        CafeFieldElement zero = CafeFieldElement.ZERO;
        CafeFieldElement one = CafeFieldElement.ONE;
        CafeFieldElement i = CafeConstants.SQRT_M1;
        // 2 is non-square mod p.
        CafeFieldElement two = one.add(one);
        // 4 is square mod p.
        CafeFieldElement four = two.add(two);
        CafeFieldElement.SqrtRatioM1Result sqrt;

        // 0/0 should return (1, 0) since u is 0
        sqrt = CafeFieldElement.sqrtRatioM1(zero, zero);
        Assert.assertEquals(1, sqrt.wasSquare);
        Assert.assertEquals(zero, sqrt.result);
        Assert.assertEquals(0, sqrt.result.isNeg());

        // 1/0 should return (0, 0) since v is 0, u is nonzero
        sqrt = CafeFieldElement.sqrtRatioM1(one, zero);
        Assert.assertEquals(0, sqrt.wasSquare);
        Assert.assertEquals(zero, sqrt.result);
        Assert.assertEquals(0, sqrt.result.isNeg());

        // 2/1 is non-square, so we expect (0, sqrt(i*2))
        sqrt = CafeFieldElement.sqrtRatioM1(two, one);
        Assert.assertEquals(0, sqrt.wasSquare);
        Assert.assertEquals(two.mul(i), sqrt.result.sqr());
        Assert.assertEquals(0, sqrt.result.isNeg());

        // 4/1 is square, so we expect (1, sqrt(4))
        sqrt = CafeFieldElement.sqrtRatioM1(four, one);
        Assert.assertEquals(1, sqrt.wasSquare);
        Assert.assertEquals(four, sqrt.result.sqr());
        Assert.assertEquals(0, sqrt.result.isNeg());

        // 1/4 is square, so we expect (1, 1/sqrt(4))
        sqrt = CafeFieldElement.sqrtRatioM1(one, four);
        Assert.assertEquals(1, sqrt.wasSquare);
        Assert.assertEquals(one, sqrt.result.sqr().mul(four));
        Assert.assertEquals(0, sqrt.result.isNeg());
    }

    @Test
    public void testPowPm5d8() {
        CafeFieldElement a = CafeFieldElement.decode(A_BYTES);
        CafeFieldElement ap58 = CafeFieldElement.decode(A_POWER_P_MINUS_5_DIV_8_BYTES);
        Assert.assertEquals(ap58, a.powPm5d8());
    }

    @Test
    public void testEquality() {
        final CafeFieldElement a = CafeFieldElement.decode(A_BYTES);
        final CafeFieldElement inverseA = CafeFieldElement.decode(A_INVERSE_BYTES);
        Assert.assertEquals(a, a);
        Assert.assertNotEquals(a, inverseA);
    }

    /**
     * Notice that the last element has the high bit set, which should be ignored.
     */
    static final byte[] B_BYTES = {
        (byte) 0x71, (byte) 0xBF, (byte) 0xA9, (byte) 0x8F, (byte) 0x5B, (byte) 0xEA, (byte) 0x79, (byte) 0x0F,
        (byte) 0xF1, (byte) 0x83, (byte) 0xD9, (byte) 0x24, (byte) 0xE6, (byte) 0x65, (byte) 0x5C, (byte) 0xEA,
        (byte) 0x08, (byte) 0xD0, (byte) 0xAA, (byte) 0xFB, (byte) 0x61, (byte) 0x7F, (byte) 0x46, (byte) 0xD2,
        (byte) 0x3A, (byte) 0x17, (byte) 0xA6, (byte) 0x57, (byte) 0xF0, (byte) 0xA9, (byte) 0xB8, (byte) 0xB2,
    };

    @Test
    public void testHighestBitIgnoredDecode() {
        byte[] clearedBytes = B_BYTES;
        clearedBytes[CafeFieldElement.BYTE_SIZE - 1] &= 0x7F;
        CafeFieldElement withHighestBitElement = CafeFieldElement.decode(B_BYTES);
        CafeFieldElement withoutHighestBitElement = CafeFieldElement.decode(clearedBytes);
        Assert.assertEquals(withoutHighestBitElement, withHighestBitElement);
    }

    @Test
    public void encodingIsCanonical() {
        // Encode 1 wrongly as 1 + (2^255 - 19) = 2^255 - 18
        byte[] oneEncodedWronglyBytes = {
            (byte) 0xEE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F,
        };
        // Decode to a field element
        CafeFieldElement one = CafeFieldElement.decode(oneEncodedWronglyBytes);
        // Then check that the encoding is correct
        byte[] oneBytes = one.encode();
        Assert.assertEquals(1, oneBytes[0]);
        for (int i = 1; i < CafeFieldElement.BYTE_SIZE; i++) {
            Assert.assertEquals(0, oneBytes[i]);
        }
    }

    @Test
    public void encodeAndDecodeOnZero() {
        byte[] zero = new byte[CafeFieldElement.BYTE_SIZE];
        final CafeFieldElement a = CafeFieldElement.decode(zero);
        Assert.assertEquals(CafeFieldElement.ZERO, a);
        Assert.assertArrayEquals(zero, a.encode());
    }

    @Test
    public void testCmov() {
        int[] tempA = new int[CafeFieldElement.INT_SIZE];
        int[] tempB = new int[CafeFieldElement.INT_SIZE];
        for (int i = 0; i < CafeFieldElement.INT_SIZE; i++) {
            tempA[i] = i;
            tempB[i] = 10 - i;
        }

        final CafeFieldElement a = new CafeFieldElement(tempA);
        final CafeFieldElement b = new CafeFieldElement(tempB);

        Assert.assertEquals(a, a.cmov(b, 0));
        Assert.assertEquals(b, a.cmov(b, 1));
        Assert.assertEquals(b, b.cmov(a, 0));
        Assert.assertEquals(a, b.cmov(a, 1));
    }
}
