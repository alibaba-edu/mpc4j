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
 * Scalar test. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/ScalarTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/6
 */
public class CafeScalarTest {
    /**
     * x = 2238329342913194256032495932344128051776374960164957527413114840482143558222
     */
    private static final CafeScalar X = new CafeScalar(
        Hex.decode("4e5ab4345d4708845913b4641bc27d5252a585101bcc4244d449f4a879d9f204")
    );
    /**
     * 1/x = 6859937278830797291664592131120606308688036382723378951768035303146619657244
     */
    private static final CafeScalar INV_X = new CafeScalar(
        Hex.decode("1cdc17fce0e9a5bbd9247e56bb016347bbba31edd5a9bb96d50bcd7a3f962a0f")
    );
    /**
     * y = 2592331292931086675770238855846338635550719849568364935475441891787804997264
     */
    private static final CafeScalar Y = new CafeScalar(
        Hex.decode("907633fe1c4b66a4a28d2dd7678386c353d0de5455d4fc9de8ef7ac31f35bb05")
    );
    /**
     * x * y = 5690045403673944803228348699031245560686958845067437804563560795922180092780
     */
    private static final CafeScalar X_MUL_Y = new CafeScalar(
        Hex.decode("6c3374a1894f62210aaa2fe186a6f92ce0aa75c2779581c295fc08179a73940c")
    );
    /**
     * sage: l = 2^252 + 27742317777372353535851937790883648493
     * sage: big = 2^256 - 1
     * sage: repr((big % l).digits(256))
     */
    private static final CafeScalar CANONICAL_2_256_MINUS_1 = new CafeScalar(
        Hex.decode("1c95988d7431ecd670cf7d73f45befc6feffffffffffffffffffffffffffff0f")
    );
    /**
     * A in Scalar
     */
    private static final CafeScalar A_SCALAR = new CafeScalar(
        Hex.decode("1a0e978a90f6622d3747023f8ad8264da758aa1b88e040d1589e7b7f2376ef09")
    );
    /**
     * A in NAF form
     */
    private static final byte[] A_NAF = new byte[]{
        0, 13, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, -9, 0, 0, 0, 0, -11, 0, 0, 0, 0, 3, 0, 0, 0, 0, 1,
        0, 0, 0, 0, 9, 0, 0, 0, 0, -5, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 11, 0, 0, 0, 0, 11, 0, 0, 0, 0, 0,
        -9, 0, 0, 0, 0, 0, -3, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 9, 0,
        0, 0, 0, -15, 0, 0, 0, 0, -7, 0, 0, 0, 0, -9, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, -3, 0,
        0, 0, 0, -11, 0, 0, 0, 0, -7, 0, 0, 0, 0, -13, 0, 0, 0, 0, 11, 0, 0, 0, 0, -9, 0, 0, 0, 0, 0, 1, 0, 0,
        0, 0, 0, -15, 0, 0, 0, 0, 1, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 13, 0, 0, 0,
        0, 0, 0, 11, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, -9, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 7,
        0, 0, 0, 0, 0, -15, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
    };

    @Test
    public void testValid() {
        byte[] s = new byte[CafeScalar.BYTE_SIZE];
        s[CafeScalar.BYTE_SIZE - 1] = 0x7f;
        new CafeScalar(s);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidHighestBit() {
        byte[] s = new byte[CafeScalar.BYTE_SIZE];
        s[31] = (byte) 0x80;
        new CafeScalar(s);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidShort() {
        new CafeScalar(new byte[CafeScalar.BYTE_SIZE - 1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLong() {
        new CafeScalar(new byte[CafeScalar.BYTE_SIZE + 1]);
    }

    @Test
    public void testDecodeImmutable() {
        // Create byte array representing a zero scalar
        byte[] bytes = new byte[CafeScalar.BYTE_SIZE];
        // Create a scalar from bytes
        CafeScalar s = new CafeScalar(bytes);
        Assert.assertEquals(s, CafeScalar.ZERO);
        // Modify the bytes
        bytes[0] = 1;
        // The scalar should be unaltered
        Assert.assertEquals(s, CafeScalar.ZERO);
    }

    @Test
    public void testEncodeImmutable() {
        // Create a zero scalar
        CafeScalar s = new CafeScalar(new byte[CafeScalar.BYTE_SIZE]);
        Assert.assertEquals(s, CafeScalar.ZERO);
        // Grab the scalar as bytes
        byte[] bytes = s.encode();
        // Modify the bytes
        bytes[0] = 1;
        // The scalar should be unaltered
        Assert.assertEquals(s, CafeScalar.ZERO);
    }

    @Test
    public void testReduce() {
        CafeScalar biggest = CafeScalar.fromBytesModOrder(Hex.decode(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
        ));
        Assert.assertEquals(CANONICAL_2_256_MINUS_1, biggest);
    }

    @Test
    public void testReduceWide() {
        CafeScalar biggest = CafeScalar.fromBytesModOrderWide(Hex.decode(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" +
                "0000000000000000000000000000000000000000000000000000000000000000"
        ));
        Assert.assertEquals(CANONICAL_2_256_MINUS_1, biggest);
    }

    @Test
    public void testCanonicalDecoding() {
        // Canonical encoding of 1667457891
        byte[] canonicalBytes = Hex.decode("6363636300000000000000000000000000000000000000000000000000000000");
        CafeScalar.fromCanonicalBytes(canonicalBytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonCanonicalDecodingUnreduced() {
        /*
         * Encoding of 7265385991361016183439748078976496179028704920197054998554201349516117938192
         * = 28380414028753969466561515933501938171588560817147392552250411230663687203 (mod l)
         * Non-canonical because unreduced mod l
         */
        final byte[] nonCanonicalBytesBecauseUnreduced = new byte[CafeScalar.BYTE_SIZE];
        for (int i = 0; i < CafeScalar.BYTE_SIZE; i++) {
            nonCanonicalBytesBecauseUnreduced[i] = 16;
        }
        CafeScalar.fromCanonicalBytes(nonCanonicalBytesBecauseUnreduced);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonCanonicalDecodingHighestBit() {
        // Encoding with high bit set, to check that the parser isn't pre-masking the high bit
        final byte[] nonCanonicalBytesBecauseHighestBit = new byte[CafeScalar.BYTE_SIZE];
        nonCanonicalBytesBecauseHighestBit[CafeScalar.BYTE_SIZE - 1] = (byte) 0x80;
        CafeScalar.fromCanonicalBytes(nonCanonicalBytesBecauseHighestBit);
    }

    @Test
    public void testFromBytesClearHighestBit() {
        CafeScalar exact = CafeScalar.fromBytes(Hex.decode(
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
        ));
        Assert.assertArrayEquals(
            exact.encode(),
            Hex.decode("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7F")
        );
    }

    @Test
    public void testAddDoesNotReduceNonCanonical() {
        CafeScalar largestEd25519CafeScalar = CafeScalar.fromBytes(Hex.decode(
            "f8ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f"
        ));
        CafeScalar result = CafeScalar.fromCanonicalBytes(Hex.decode(
            "7e344775474a7f9723b63a8be92ae76dffffffffffffffffffffffffffffff0f"
        ));
        Assert.assertNotEquals(result, largestEd25519CafeScalar.add(CafeScalar.ONE));
        Assert.assertEquals(result, largestEd25519CafeScalar.add(CafeScalar.ONE).reduce());
    }

    @Test
    public void testSubDoesNotReduceNonCanonical() {
        CafeScalar largestEd25519CafeScalar = CafeScalar.fromBytes(Hex.decode(
            "f8ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f"
        ));
        CafeScalar result = CafeScalar.fromCanonicalBytes(Hex.decode(
            "7c344775474a7f9723b63a8be92ae76dffffffffffffffffffffffffffffff0f"
        ));
        Assert.assertNotEquals(result, largestEd25519CafeScalar.sub(CafeScalar.ONE));
        Assert.assertEquals(result, largestEd25519CafeScalar.sub(CafeScalar.ONE).reduce());
    }

    @Test
    public void testMul() {
        Assert.assertEquals(X_MUL_Y, X.mul(Y));
        Assert.assertEquals(Y, X_MUL_Y.mul(INV_X));
    }

    @Test
    public void testNonAdjacentForm() {
        byte[] naf = A_SCALAR.nonAdjacentForm();
        Assert.assertArrayEquals(A_NAF, naf);
    }

    /**
     * Example from RFC 8032 test case 1
     */
    static final byte[] TV1_R_INPUT = Hex.decode(
        "b6b19cd8e0426f5983fa112d89a143aa97dab8bc5deb8d5b6253c928b65272f4" +
            "044098c2a990039cde5b6a4818df0bfb6e40dc5dee54248032962323e701352d"
    );
    /**
     * r
     */
    static final byte[] TV1_R = Hex.decode("f38907308c893deaf244787db4af53682249107418afc2edc58f75ac58a07404");
    /**
     * h
     */
    static final byte[] TV1_H = Hex.decode("86eabc8e4c96193d290504e7c600df6cf8d8256131ec2c138a3e7e162e525404");
    /**
     * a
     */
    static final byte[] TV1_A = Hex.decode("307c83864f2833cb427a2ef1c00a013cfdff2768d980c0a3a520f006904de94f");
    /**
     * s = h * a + r
     */
    static final byte[] TV1_S = Hex.decode("5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b");

    @Test
    public void testFromBytesModOrderWide() {
        Assert.assertEquals(new CafeScalar(TV1_R), CafeScalar.fromBytesModOrderWide(TV1_R_INPUT));
    }

    @Test
    public void testMulAndAdd() {
        CafeScalar h = new CafeScalar(TV1_H);
        CafeScalar a = new CafeScalar(TV1_A);
        CafeScalar r = new CafeScalar(TV1_R);
        CafeScalar s = new CafeScalar(TV1_S);
        Assert.assertEquals(s, h.mulAndAdd(a, r));
        Assert.assertEquals(s, h.mul(a).add(r));
        Assert.assertEquals(s.sub(r), h.mul(a));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBytesInvalidShortModOrderWide() {
        CafeScalar.fromBytesModOrderWide(new byte[CafeScalar.BYTE_SIZE * 2 - 1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBytesInvalidLongModOrderWide() {
        CafeScalar.fromBytesModOrderWide(new byte[CafeScalar.BYTE_SIZE * 2 + 1]);
    }

    /**
     * 42
     */
    private static final CafeScalar SCALAR_42 = new CafeScalar(Hex.decode(
        "2A00000000000000000000000000000000000000000000000000000000000000"
    ));
    /**
     * 1234567890
     */
    private static final CafeScalar SCALAR_1234567890 = new CafeScalar(Hex.decode(
        "D202964900000000000000000000000000000000000000000000000000000000"
    ));
    /**
     * 用[-8, 8)表示的0
     */
    private static final byte[] RADIX16_ZERO = Hex.decode(
        "0000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000"
    );
    /**
     * 用[-8, 8)表示的1
     */
    private static final byte[] RADIX16_ONE = Hex.decode(
        "0100000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000"
    );
    /**
     * 用[-8, 8)表示的42
     */
    private static final byte[] RADIX16_42 = Hex.decode(
        "FA03000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000000000000000000000000000"
    );

    @Test
    public void testToRadix16() {
        Assert.assertArrayEquals(RADIX16_ZERO, CafeScalar.ZERO.toRadix16());
        Assert.assertArrayEquals(RADIX16_ONE, CafeScalar.ONE.toRadix16());
        Assert.assertArrayEquals(RADIX16_42, SCALAR_42.toRadix16());

        byte[] radix1234567890 = SCALAR_1234567890.toRadix16();
        int total = 0;
        for (int i = 0; i < radix1234567890.length; i++) {
            Assert.assertTrue(radix1234567890[i] >= (byte) -8);
            Assert.assertTrue(radix1234567890[i] <= (byte) 7);
            total += radix1234567890[i] * Math.pow(16, i);
        }
        Assert.assertEquals(1234567890, total);

        byte[] tv1hRadix16 = (new CafeScalar(TV1_H)).toRadix16();
        for (byte radix16 : tv1hRadix16) {
            Assert.assertTrue(radix16 >= (byte) -8);
            Assert.assertTrue(radix16 <= (byte) 7);
        }
    }
}
