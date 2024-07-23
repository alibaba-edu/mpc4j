package edu.alibaba.mpc4j.common.tool.utils;

import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.bouncycastle.util.Arrays;

/**
 * GF2X utilities.
 * <p></p>
 * A GF2X polynomial is a polynomial where each coefficient is in {0, 1}. Therefore, we need to use  <code>byte[]</code>
 * to represent a GF2X polynomial in a compressed way. However, different libraries use different representations for a
 * GF2X polynomial. We list these representations with the following GF2X polynomial as an example.
 * <p>x^8 + x^7 + x^6 + x^5 + x^4 + x^3 + x^2 + x</p>
 * This utility class provide methods to do conversions between these representations.
 * <li>mpc4j: big-endian, the last bit in the last byte of <code>byte[]</code> is x^0.
 * For example, x^8 + x^7 + x^6 + x^5 + x^4 + x^3 + x^2 + x is <code>0b00000001, 0b11111110</code></li>
 * <li>NTL: big-endian in <code>byte[]</code>, the last bit in the first byte of <code>byte[]</code> is x^0.
 * For example: x^8 + x^7 + x^6 + x^5 + x^4 + x^3 + x^2 + x is <code>0b11111110, 0b00000001</code></li>
 * <li>AES-NI (use F(x) = x^128 + x^7 + x^2 + x + 1): little-endian, the first bit in the first byte of
 * <code>byte[]</code> is x^0. For example: x^8 + x^7 + x^6 + x^5 + x^4 + x^3 + x^2 + x is <code>0b01111111, 0b10000000</li>
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class Gf2xUtils {
    /**
     * private constructor.
     */
    private Gf2xUtils() {
        // empty
    }

    /**
     * Converts a GF2X polynomial represented in Rings to a GF2X polynomial represented in <code>byte[]</code>.
     *
     * @param value a GF2X polynomial represented in Rings.
     * @param byteL byte length used for the representation.
     * @return a GF2X polynomial represented in <code>byte[]</code>.
     */
    public static byte[] ringsToByteArray(UnivariatePolynomialZp64 value, int byteL) {
        // We need to consider value == null in OKVS
        if (value == null) {
            return null;
        }
        int l = byteL * Byte.SIZE;
        assert value.modulus() == 2;
        // There are at most d + 1 coefficients in a degree-d GF2X polynomial. Therefore, l >= degree + 1.
        assert value.degree() < l;
        byte[] byteArray = new byte[byteL];
        for (int i = 0; i <= value.degree(); i++) {
            boolean coefficient = value.get(i) != 0L;
            // when i = 0, the coefficient for x^0 should be placed in the last bit of the last byte, which is l - 1.
            BinaryUtils.setBoolean(byteArray, l - 1 - i, coefficient);
        }
        return byteArray;
    }

    /**
     * Converts a GF2X polynomial represented in <code>byte[]</code> to a polynomial represented in Rings.
     *
     * @param value a GF2X polynomial represented in <code>byte[]</code>.
     * @return a GF2X polynomial represented in Rings.
     */
    public static UnivariatePolynomialZp64 byteArrayToRings(byte[] value) {
        int l = value.length * Byte.SIZE;
        long[] longArray = new long[l];
        for (int i = 0; i < l; i++) {
            // when i = 0, the coefficient for x^0 should be placed in the last bit of the last byte, which is l - 1.
            long coefficient = BinaryUtils.getBoolean(value, l - 1 - i) ? 1L : 0L;
            longArray[i] = coefficient;
        }
        return UnivariatePolynomialZp64.create(2L, longArray);
    }

    /**
     * Converts a GF2X polynomial represented in AES-NI to a polynomial represented in <code>byte[]</code>.
     *
     * @param value a GF2X polynomial represented in AES-NI.
     * @return a GF2X polynomial represented in <code>byte[]</code>.
     */
    public static byte[] aesNiToByteArray(byte[] value) {
        // in AES-NI, the polynomial must be an element in GF(2^128), so that the byte length must be 16.
        assert value.length == CommonConstants.BLOCK_BYTE_LENGTH;
        return BytesUtils.reverseBitArray(value);
    }

    /**
     * Converts a GF2X polynomial represented in AES-NI to a polynomial represented in <code>byte[]</code> in-place.
     *
     * @param value a GF2X polynomial represented in AES-NI.
     */
    public static void innerAesNiToByteArray(byte[] value) {
        // in AES-NI, the polynomial must be an element in GF(2^128), so that the byte length must be 16.
        assert value.length == CommonConstants.BLOCK_BYTE_LENGTH;
        BytesUtils.innerReverseBitArray(value);
    }

    /**
     * Converts a GF2X polynomial represented in <code>byte[]</code> to a GF2X polynomial represented in AES-NI.
     *
     * @param value a GF2X polynomial represented in <code>byte[]</code>.
     * @return a GF2X polynomial represented in AES-NI.
     */
    public static byte[] byteArrayToAesNi(byte[] value) {
        // in AES-NI, the polynomial must be an element in GF(2^128), so that the byte length must be 16.
        assert value.length == CommonConstants.BLOCK_BYTE_LENGTH;
        return BytesUtils.reverseBitArray(value);
    }

    /**
     * Converts a GF2X polynomial represented in <code>byte[]</code> to a GF2X polynomial represented in AES-NI.
     *
     * @param value a GF2X polynomial represented in <code>byte[]</code>.
     */
    public static void innerByteArrayToAesNi(byte[] value) {
        // in AES-NI, the polynomial must be an element in GF(2^128), so that the byte length must be 16.
        assert value.length == CommonConstants.BLOCK_BYTE_LENGTH;
        BytesUtils.innerReverseBitArray(value);
    }

    /**
     * Converts a GF2X polynomial represented in NTL to a polynomial represented in <code>byte[]</code>.
     *
     * @param value a GF2X polynomial represented in NTL.
     * @return a GF2X polynomial represented in <code>byte[]</code>.
     */
    public static byte[] ntlToByteArray(byte[] value) {
        return Arrays.reverse(value);
    }

    /**
     * Converts a GF2X polynomial represented in <code>byte[]</code> to a polynomial represented in NTL.
     *
     * @param value a GF2X polynomial represented in <code>byte[]</code>.
     * @return a GF2X polynomial represented in NTL.
     */
    public static byte[] byteArrayToNtl(byte[] value) {
        return Arrays.reverse(value);
    }
}
