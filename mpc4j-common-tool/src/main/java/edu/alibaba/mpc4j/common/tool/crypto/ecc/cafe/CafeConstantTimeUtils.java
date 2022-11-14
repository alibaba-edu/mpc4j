/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * Constant-time functions. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/subtle/ConstantTime.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/6
 */
public final class CafeConstantTimeUtils {

    private CafeConstantTimeUtils() {
        // empty
    }

    /**
     * Constant-time byte comparison.
     *
     * @param b a byte, represented as an int.
     * @param c a byte, represented as an int.
     * @return 1 if b and c are equal, 0 otherwise.
     */
    public static int equal(int b, int c) {
        int result = 0;
        int xor = b ^ c;
        for (int i = 0; i < Byte.SIZE; i++) {
            result |= xor >> i;
        }
        return (result ^ 0x01) & 0x01;
    }

    /**
     * Constant-time byte[] comparison. Fails fast if the lengths differ.
     *
     * @param b a byte[].
     * @param c a byte[].
     * @return 1 if b and c are equal, 0 otherwise.
     */
    public static int equal(byte[] b, byte[] c) {
        // Fail-fast if the lengths differ
        if (b.length != c.length) {
            return 0;
        }

        // Now use a constant-time comparison
        int result = 0;
        for (int i = 0; i < b.length; i++) {
            result |= b[i] ^ c[i];
        }

        return equal(result, 0);
    }

    /**
     * Constant-time determine if byte is negative. For example:
     * <p><ul>
     * <li> 1 is non-negative (return 0) </li>
     * <li> 0 is non-negative (return 0) </li>
     * <li> -1 is negative (return 1) </li>
     * </ul></p>
     *
     * @param b the byte to check, represented as an int.
     * @return 1 if the byte is negative, 0 otherwise.
     */
    public static int isNeg(int b) {
        assert b > -(1 << Byte.SIZE) && b < (1 << Byte.SIZE)
            : "b must be in range (" + -(1 << Byte.SIZE) + ", " + (1 << Byte.SIZE) + "): " + b;
        return (b >> Byte.SIZE) & 1;
    }

    /**
     * Get the i'th bit of a byte array. Big-endian representation, but little-endian representation in each byte.
     * For example:
     * <p><ul>
     * <li> the 0'th bit is a byte array is the least significant bit bit in the 0'th byte. </li>
     * <li> the 7'th bit is a byte array is the most significant bit bit in the 0'th byte. </li>
     * <li> the 8'th bit is a byte array is the least significant bit in the 1'th byte. </li>
     * </ul></p>
     *
     * @param h the byte array.
     * @param i the bit index.
     * @return 0 or 1, the value of the i'th bit in h。
     */
    public static int bit(byte[] h, int i) {
        return (h[i >> 3] >> (i & 7)) & 1;
    }
}
