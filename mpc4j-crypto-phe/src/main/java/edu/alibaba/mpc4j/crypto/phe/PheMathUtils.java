package edu.alibaba.mpc4j.crypto.phe;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * Utility class for mathematical operations in PHE. This is created for separating dependencies of other submodules.
 *
 * @author Weiran Liu
 * @date 2025/5/15
 */
public class PheMathUtils {
    /**
     * private constructor
     */
    private PheMathUtils() {
        // empty
    }

    /**
     * precision for floating-point operations.
     */
    public static final double DOUBLE_OPERATION_PRECISION = 1e-7;

    /*
     * The following definitions and methods are related to BigInteger operations.
     */

    /**
     * minimum <code>long</code> value -2<sup>63</sup> represented by <code>BigInteger</code>.
     */
    public static final BigInteger LONG_MIN_BIGINTEGER_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
    /**
     * maximum <code>long</code> value 2<sup>63</sup>-1 represented by <code>BigInteger</code>.
     */
    public static final BigInteger LONG_MAX_BIGINTEGER_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    /**
     * Converts a {@code BigInteger} to a {@code byte[]}.
     *
     * @param bigInteger given {@code BigInteger}.
     * @return converted {@code byte[]}.
     */
    public static byte[] bigIntegerToByteArray(BigInteger bigInteger) {
        return bigInteger.toByteArray();
    }

    /**
     * Converts a {@code byte[]} to a {@code BigInteger}.
     *
     * @param byteArray given {@code byte[]}.
     * @return converted {@code BigInteger}.
     */
    public static BigInteger byteArrayToBigInteger(byte[] byteArray) {
        return new BigInteger(byteArray);
    }

    /**
     * Returns a <code>BigInteger</code> whose value is <code>(base<sup>exponent</sup> mod m)</code>.
     *
     * @param base     base.
     * @param exponent exponent.
     * @param modulus  modulus.
     * @return <code>(base<sup>exponent</sup> mod m)</code>.
     */
    public static BigInteger modPow(BigInteger base, BigInteger exponent, BigInteger modulus) {
        return base.modPow(exponent, modulus);
    }

    /**
     * Returns a BigInteger whose value is <code>(a<sup>-1</sup> mod m)</code>.
     *
     * @param a       a.
     * @param modulus modulus.
     * @return <code>(a<sup>-1</sup> mod m)</code>.
     * @throws ArithmeticException if the inverse does not exist.
     */
    public static BigInteger modInverse(BigInteger a, BigInteger modulus) throws ArithmeticException {
        return a.modInverse(modulus);
    }

    /**
     * Returns if the given <code>BigInteger n</code> is positive (<code>n > 0</code>).
     *
     * @param n given <code>BigInteger n</code>.
     * @return true if the given <code>BigInteger n</code> is positive, false otherwise.
     */
    public static boolean positive(BigInteger n) {
        return n.signum() > 0;
    }

    /**
     * Returns if the given <code>BigInteger n</code> is non-negative <code>n >= 0</code>.
     *
     * @param n given <code>BigInteger n</code>.
     * @return true if the given <code>BigInteger n</code> is non-negative, false otherwise.
     */
    public static boolean nonNegative(BigInteger n) {
        return n.signum() >= 0;
    }

    /**
     * Returns if the given <code>BigInteger n</code> is negative (<code>n < 0</code>).
     *
     * @param n given <code>BigInteger n</code>.
     * @return true if the given <code>BigInteger n</code> is negative, false otherwise.
     */
    public static boolean negative(BigInteger n) {
        return n.signum() < 0;
    }

    /**
     * Returns if the given <code>BigInteger n</code> is non-positive (n <= 0).
     *
     * @param n given <code>BigInteger n</code>.
     * @return true if the given <code>BigInteger n</code> is non-positive, false otherwise.
     */
    public static boolean nonPositive(BigInteger n) {
        return n.signum() <= 0;
    }

    /**
     * Returns if {@code BigInteger a} is greater than {@code BigInteger b} (<code>a > b</code>).
     *
     * @param a <code>BigInteger a</code>.
     * @param b <code>BigInteger b</code>.
     * @return true if {@code BigInteger a} is greater than {@code BigInteger b}, false otherwise.
     */
    public static boolean greater(BigInteger a, BigInteger b) {
        return a.compareTo(b) > 0;
    }

    /**
     * Returns if {@code BigInteger a} is greater than or equal to {@code BigInteger b} (<code>a >= b</code>).
     *
     * @param a <code>BigInteger a</code>.
     * @param b <code>BigInteger b</code>.
     * @return true if {@code BigInteger a} is greater than or equal to {@code BigInteger b}, false otherwise.
     */
    public static boolean greaterOrEqual(BigInteger a, BigInteger b) {
        return a.compareTo(b) >= 0;
    }

    /**
     * Returns if {@code a} is less than {@code b} (<code>a < b</code>).
     *
     * @param a <code>BigInteger a</code>.
     * @param b <code>BigInteger b</code>.
     * @return true if {@code BigInteger a} is less than {@code BigInteger b}, false otherwise.
     */
    public static boolean less(BigInteger a, BigInteger b) {
        return a.compareTo(b) < 0;
    }

    /**
     * Returns if {@code a} is less than or equal to {@code b} (<code>a <= b</code>).
     *
     * @param a <code>BigInteger a</code>.
     * @param b <code>BigInteger b</code>.
     * @return true if {@code BigInteger a} is less than or equal to {@code BigInteger b}, false otherwise.
     */
    public static boolean lessOrEqual(BigInteger a, BigInteger b) {
        return a.compareTo(b) <= 0;
    }

    /**
     * Creates a random {@code BigInteger} in the range [1, n).
     *
     * @param n            upper bound <code>BigInteger n</code>.
     * @param secureRandom random state.
     * @return a random {@code BigInteger} in the range [1, n).
     */
    public static BigInteger randomPositive(final BigInteger n, SecureRandom secureRandom) {
        // n > 1, otherwise there is no number in the range [1, n).
        assert greater(n, BigInteger.ONE);
        int bits = n.bitLength();
        while (true) {
            // r must be in the range [0, 2^k), only need to check 1 <= r < n.
            BigInteger r = new BigInteger(bits, secureRandom);
            if (less(r, BigInteger.ONE) || greaterOrEqual(r, n)) {
                continue;
            }
            return r;
        }
    }

    /**
     * Creates a random {@code BigInteger} in the range [0, n).
     *
     * @param n            upper bound <code>BigInteger n</code>.
     * @param secureRandom random state.
     * @return a random {@code BigInteger} in the range [0, n).
     */
    public static BigInteger randomNonNegative(final BigInteger n, SecureRandom secureRandom) {
        // n > 0, otherwise there is no number in the range [0, n).
        assert positive(n);
        int bits = n.bitLength();
        while (true) {
            // r must be in the range [0, 2^k), only need to check r < n.
            BigInteger r = new BigInteger(bits, secureRandom);
            if (greaterOrEqual(r, n)) {
                continue;
            }
            return r;
        }
    }

    /**
     * Returns the base-2 logarithm of {@code x}.
     * <p>
     * The source code is from Maarten Bodewes:
     * <a href="http://stackoverflow.com/questions/739532/logarithm-of-a-bigdecimal">Logarithm of a BigDecimal</a>.
     *
     * @param x the input x.
     * @return the base-2 logarithm of {@code x}.
     * @throws IllegalArgumentException if {@code x <= 0}.
     */
    public static double log2(BigInteger x) {
        // x > 0, otherwise log_2(x) is undefined.
        assert positive(x);
        if (x.equals(BigInteger.ONE)) {
            return 0.0;
        }
        // Get the minimum number of bits necessary to hold this value.
        int n = x.bitLength();
        /*
         * Calculate the double-precision fraction of this number; as if the binary point was left of the most significant
         * '1' bit. (Get the most significant 53 bits and divide by 2^53).
         * Note that mantissa is 53 bits (including hidden bit).
         */
        long mask = 1L << 52;
        long mantissa = 0;
        int j = 0;
        for (int i = 1; i < 54; i++) {
            j = n - i;
            if (j < 0) {
                break;
            }
            if (x.testBit(j)) {
                mantissa |= mask;
            }
            mask >>>= 1;
        }
        // Round up if next bit is 1.
        if (j > 0 && x.testBit(j - 1)) {
            mantissa++;
        }
        double f = mantissa / (double) (1L << 52);
        /*
         * Add the logarithm to the number of bits, and subtract 1 because the number of bits is always higher than
         * necessary for a number (i.e. log_2(x) < n for every x).
         * Note that magic number converts from base e to base 2 before adding. For other bases, correct the result,
         * NOT this number!
         */
        return (n - 1 + Math.log(f) * 1.44269504088896340735992468100189213742664595415298D);
    }

    /*
     * The following methods are related to int operations.
     */

    /**
     * Converts an <code>int</code> to a <code>byte[]</code> (length = <code>Integer.BYTES</code>) using big-endian format.
     *
     * @param value given <code>int</code>.
     * @return converted <code>byte[]</code> (length = <code>Integer.BYTES</code>).
     */
    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    /**
     * Converts a <code>byte[]</code> (length must be <code>Integer.BYTES</code>) to an <code>int</code> using
     * big-endian format.
     *
     * @param bytes given <code>byte[]</code> (length must be <code>Integer.BYTES</code>).
     * @return converted <code>int</code>.
     */
    public static int byteArrayToInt(byte[] bytes) {
        assert bytes.length == Integer.BYTES : "length must be equal to " + Integer.BYTES + ": " + bytes.length;
        return ByteBuffer.wrap(bytes).getInt();
    }

    /*
     * The following methods are related to double operations.
     */

    /**
     * ln(10)
     */
    public static final double LOG10_DOUBLE_VALUE = Math.log(10.0);
    /**
     * ln(2)
     */
    public static final double LOG2_DOUBLE_VALUE = Math.log(2.0);

    /**
     * Computes log_2(x).
     *
     * @param x x.
     * @return log_2(x).
     */
    public static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    /*
     * The following methods are related to BigDecimal operations.
     */

    /**
     * Computes log_b(x).
     *
     * @param x the value x.
     * @param b the base b.
     * @return log_b(x).
     */
    public static double log(BigDecimal x, int b) {
        return (log2(x.unscaledValue()) * LOG2_DOUBLE_VALUE - x.scale() * LOG10_DOUBLE_VALUE) / Math.log(b);
    }
}
