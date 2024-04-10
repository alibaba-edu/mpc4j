package edu.alibaba.mpc4j.crypto.fhe.zq;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.stream.IntStream;

/**
 * Some common arithmetic computation.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/common.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/9
 */
public class Common {
    /**
     * private constructor.
     */
    private Common() {
        // empty
    }

    /**
     * a long (uint64) value is 8-byte
     */
    public static final int BYTES_PER_UINT64 = Long.BYTES;
    /**
     * a hex value is 4-bit
     */
    public static final int BITS_PER_NIBBLE = 4;
    /**
     * a long (uint64) value is 64-bit
     */
    public static final int BITS_PER_UINT64 = Long.SIZE;
    /**
     * a byte value is 2-hex
     */
    public static final int NIBBLES_PER_BYTE = 2;
    /**
     * a byte value is 8-bit
     */
    public static final int BITS_PER_BYTE = Byte.SIZE;
    /**
     * a long (uint64) value is 16-hex
     */
    public static final int NIBBLES_PER_UINT64 = BYTES_PER_UINT64 * NIBBLES_PER_BYTE;

    /**
     * Converts an uint64 array to a byte array, each uint64 will extend to 8 bytes.
     *
     * @param uint64Array an uint64 array.
     * @param uint64Count number of uint64 in the uint64 array.
     * @return a byte array.
     */
    public static byte[] uint64ArrayToByteArray(long[] uint64Array, int uint64Count) {
        // See https://stackoverflow.com/questions/61844613/java-native-method-to-convert-long-array-to-byte-array
        // However, further tests show that the following method is faster
        ByteBuffer byteBuffer = ByteBuffer.allocate(uint64Count * Common.BYTES_PER_UINT64);
        IntStream.range(0, uint64Count).forEach(index -> byteBuffer.putLong(uint64Array[index]));
        return byteBuffer.array();
    }

    /**
     * Converts a byte array to an uint64 array, each 8 bytes will compress in an uint64.
     *
     * @param byteArray the byte array.
     * @param byteCount number of bytes in the byte array.
     * @return an uint64 array.
     */
    public static long[] byteArrayToUint64Array(byte[] byteArray, int byteCount) {
        assert byteCount % Common.BYTES_PER_UINT64 == 0;
        // we cannot write ByteBuffer.warp(byteArray).asLongBuffer().array(), since here LongBuffer is readOnly
        // See https://stackoverflow.com/questions/19003231/how-to-convert-a-bytebuffer-to-long-in-java for the solution.
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray, 0, byteCount);
        LongBuffer longBuffer = byteBuffer.asLongBuffer();
        long[] uint64Array = new long[longBuffer.capacity()];
        longBuffer.get(uint64Array);
        return uint64Array;
    }

    /**
     * Coverts a hex to a char (upper hex).
     *
     * @param nibble a hex.
     * @return a char (upper hex).
     */
    public static char nibbleToUpperHex(int nibble) {
        // nibble can only be [0, 16)
        assert (nibble >= 0 && nibble < 16);
        if (nibble < 10) {
            return (char) ((char) nibble + '0');
        }
        return (char) ((char) nibble + 'A' - 10);
    }

    /**
     * Determines whether the given character is a hex char.
     *
     * @param hex a char
     * @return whether the given char is a hex char.
     */
    public static boolean isHexChar(char hex) {
        // hex can be {'0',...,'9','A',...,'F','a',...,'f'
        if (hex >= '0' && hex <= '9') {
            return true;
        }
        if (hex >= 'A' && hex <= 'F') {
            return true;
        }
        return hex >= 'a' && hex <= 'f';
    }

    /**
     * Converts a hex char to corresponding decimal value.
     *
     * @param hex a hex char.
     * @return decimal corresponding to the hex char.
     */
    public static int hexToNibble(char hex) {
        if (hex >= '0' && hex <= '9') {
            return hex - '0';
        }
        if (hex >= 'A' && hex <= 'F') {
            return hex - 'A' + 10;
        }
        if (hex >= 'a' && hex <= 'f') {
            return hex - 'a' + 10;
        }
        assert isHexChar(hex);

        return -1;
    }

    /**
     * Gets bit_count of the hex string.
     *
     * @param hexString the hex string.
     * @param charCount the number of chars.
     * @return bit_count of the hex string.
     */
    public static int getHexStringBitCount(String hexString, int charCount) {
        // when hexString is null, we allow charCount <= 0
        assert !(hexString == null && charCount > 0);
        // when hexString is not null, we need charCount >= 0
        assert charCount >= 0;

        for (int i = 0; i < charCount; i++) {
            char hex = hexString.charAt(i);
            int nibble = hexToNibble(hex);
            // find the first non-zero hex char
            if (nibble != 0) {
                // bit_count for the first non-zero hex char
                int nibbleBits = UintCore.getSignificantBitCount(nibble);
                // bit_count for the remaining nibbles
                int remainingNibbles = (charCount - i - 1) * BITS_PER_NIBBLE;

                return nibbleBits + remainingNibbles;
            }
        }
        return 0;
    }

    /**
     * Gets bit_count of the hex string.
     *
     * @param hexString  the hex string.
     * @param startIndex the start index.
     * @param charCount  the number of chars.
     * @return bit_count of the hex string.
     */
    public static int getHexStringBitCount(String hexString, int startIndex, int charCount) {
        // when hexString is null, we allow charCount <= 0
        assert !(hexString == null && charCount > 0);
        // when hexString is not null, we need charCount >= 0
        assert charCount >= 0;

        for (int i = 0; i < charCount; i++) {
            char hex = hexString.charAt(i + startIndex);
            int nibble = hexToNibble(hex);
            // find the first non-zero hex char
            if (nibble != 0) {
                // bit_count for the first non-zero hex char
                int nibbleBits = UintCore.getSignificantBitCount(nibble);
                // bit_count for the remaining nibbles
                int remainingNibbles = (charCount - i - 1) * BITS_PER_NIBBLE;

                return nibbleBits + remainingNibbles;
            }
        }
        return 0;
    }

    /**
     * Computes value / divisor and round up the result.
     *
     * @param value value.
     * @param divisor divisor.
     * @return value / divisor and round up.
     */
    public static int divideRoundUp(int value, int divisor) {
        if (value < 0) {
            throw new IllegalArgumentException("value");
        }
        if (divisor <= 0) {
            throw new IllegalArgumentException("divisor");
        }
        return (addSafe(value, divisor - 1, false)) / divisor;
    }

    /**
     * Gets the hamming weight of the byte value.
     *
     * @param value the byte value.
     * @return the hamming weight of the byte value.
     */
    public static int hammingWeight(byte value) {
        int t = value;
        t -= (t >> 1) & 0x55;
        t = (t & 0x33) + ((t >> 2) & 0x33);
        return (t + (t >> 4)) & 0x0F;
    }

    /**
     * Returns if the two double values are close in floating-point view, i.e., |v1 - v2| < max(v1, v2) * Math.ulp(1.0).
     *
     * @param v1 the double value v1.
     * @param v2 the double value v2.
     * @return true if the two double values are close.
     */
    public static boolean areClose(double v1, double v2) {
        double scaleFactor = Math.max(Math.max(Math.abs(v1), Math.abs(v2)), 1.0);
        return Math.abs(v1 - v2) < scaleFactor * Math.ulp(1.0);
    }

    /**
     * Returns if (uint64) in1 < (uint64) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint64) in1 < (uint64) in2.
     */
    public static boolean unsignedLt(long in1, long in2) {
        return Long.compareUnsigned(in1, in2) < 0;
    }

    /**
     * Returns if (uint64) in1 <= (uint64) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint64) in1 <= (uint64) in2.
     */
    public static boolean unsignedLeq(long in1, long in2) {
        return Long.compareUnsigned(in1, in2) <= 0;
    }

    /**
     * Returns if (uint64) in1 > (uint64) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint64) in1 > (uint64) in2.
     */
    public static boolean unsignedGt(long in1, long in2) {
        return Long.compareUnsigned(in1, in2) > 0;
    }

    /**
     * Returns if (uint64) in1 >= (uint64) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint64) in1 >= (uint64) in2.
     */
    public static boolean unsignedGeq(long in1, long in2) {
        return Long.compareUnsigned(in1, in2) >= 0;
    }

    /**
     * Returns if (uint64) in1 == (uint64) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint64) in1 == (uint64) in2.
     */
    public static boolean unsignedEq(long in1, long in2) {
        return Long.compareUnsigned(in1, in2) == 0;
    }

    /**
     * Returns if (uint64) in1 != (uint64) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint64) in1 != (uint64) in2.
     */
    public static boolean unsignedNeq(long in1, long in2) {
        return Long.compareUnsigned(in1, in2) != 0;
    }

    /**
     * Returns if (uint32) in1 < (uint32) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint32) in1 < (uint32) in2.
     */
    public static boolean unsignedLt(int in1, int in2) {
        return Integer.compareUnsigned(in1, in2) < 0;
    }

    /**
     * Returns if (uint32) in1 <= (uint32) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint32) in1 <= (uint32) in2.
     */
    public static boolean unsignedLeq(int in1, int in2) {
        return Integer.compareUnsigned(in1, in2) <= 0;
    }

    /**
     * Returns if (uint32) in1 > (uint32) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint32) in1 > (uint32) in2.
     */
    public static boolean unsignedGt(int in1, int in2) {
        return Integer.compareUnsigned(in1, in2) > 0;
    }

    /**
     * Returns if (uint32) in1 >= (uint32) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint32) in1 >= (uint32) in2.
     */
    public static boolean unsignedGeq(int in1, int in2) {
        return Integer.compareUnsigned(in1, in2) >= 0;
    }

    /**
     * Returns if (uint32) in1 == (uint32) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint32) in1 == (uint32) in2.
     */
    public static boolean unsignedEq(int in1, int in2) {
        return Integer.compareUnsigned(in1, in2) == 0;
    }

    /**
     * Returns if (uint32) in1 != (uint32) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint32) in1 != (uint32) in2.
     */
    public static boolean unsignedNeq(int in1, int in2) {
        return Integer.compareUnsigned(in1, in2) != 0;
    }

    /**
     * Reverses bits in operand.
     *
     * @param operand  operand.
     * @param bitCount number of remaining reversed bits.
     * @return reversed bits.
     */
    public static long reverseBits(long operand, int bitCount) {
        assert bitCount >= 0;
        assert bitCount <= 64;

        if (bitCount == 0) {
            return 0;
        }
        // take low bitCount bits
        return reverseBits(operand) >>> (64 - bitCount);
    }

    /**
     * Returns the value obtained by reversing the order of the bits in the two's complement binary representation of
     * the specified long value.
     *
     * @param operand the value to be reversed.
     * @return the value obtained by reversing order of the bits in the specified long value.
     */
    public static long reverseBits(long operand) {
        return Long.reverse(operand);
    }

    /**
     * Reverses bits in operand.
     *
     * @param operand  operand.
     * @param bitCount number of remaining reversed bits.
     * @return reversed bits.
     */
    public static int reverseBits(int operand, int bitCount) {
        assert bitCount >= 0;
        assert bitCount <= 32;

        if (bitCount == 0) {
            return 0;
        }
        // take low bitCount bits
        return reverseBits(operand) >>> (32 - bitCount);
    }

    /**
     * Returns the value obtained by reversing the order of the bits in the two's complement binary representation of
     * the specified int value.
     *
     * @param operand the value to be reversed.
     * @return the value obtained by reversing order of the bits in the specified int value.
     */
    public static int reverseBits(int operand) {
        return Integer.reverse(operand);
    }

    /**
     * Gets the most significant bit (msb) index of the value. For example:
     * <li>the msb of 1 is the 0-th bit.</li>
     * <li>the msb of 2 is the 1-th bit.</li>
     *
     * @param value the value.
     * @return the most significant bit (msb) index of the value.
     */
    public static int getMsbIndex(long value) {
        return 63 - Long.numberOfLeadingZeros(value);
    }

    /**
     * Computes a * b * Π_{i = 0}^{n - 1} number[i] safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @param numbers  other numbers to multiply.
     * @return a * b * Π_{i = 0}^{n - 1} number[i].
     * @throws ArithmeticException if overflow occurs.
     */
    public static long mulSafe(long a, long b, boolean unsigned, long... numbers) {
        long prod = mulSafe(a, b, unsigned);
        for (long n : numbers) {
            prod = mulSafe(prod, n, unsigned);
        }
        return prod;
    }

    /**
     * Computes a * b safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @return a * b.
     * @throws ArithmeticException if overflow occurs.
     */
    public static long mulSafe(long a, long b, boolean unsigned) {
        if (unsigned) {
            // neg (64-bit) * neg (64-bit), must overflow since 64-bit * 64-bit > 64-bit
            if (a < 0 && b < 0) {
                throw new ArithmeticException("unsigned overflow");
            }
            // neg (64-bit) * pos, must overflow when pos > 1, since 64-bit * pos (> 1) > 64-bit
            if ((a < 0 && b > 1) || (a > 1 && b < 0)) {
                throw new ArithmeticException("unsigned overflow");
            }
            // pos * pos, if (2^64 / a) > b, then there is no overflow.
            if (a > 1 && b > 1) {
                long tmp = Long.divideUnsigned(0xFFFFFFFFFFFFFFFFL, a);
                if (b > tmp) {
                    throw new ArithmeticException("unsigned overflow");
                }
            }
        } else {
            if ((a > 0) && (b > 0) && (b > Long.MAX_VALUE / a)) {
                // a * b > 0, overflow when b > Long.MAX_VALUE / a
                throw new ArithmeticException("signed overflow");
            } else if ((a < 0) && (b < 0) && ((-b) > Long.MAX_VALUE / (-a))) {
                // a * b > 0, overflow when b > Long.MAX_VALUE / a
                throw new ArithmeticException("signed overflow");
            } else if ((a < 0) && (b > 0) && (b > Long.MAX_VALUE / (-a))) {
                // a * b < 0, overflow when b > Long.MAX_VALUE / (-a)
                throw new ArithmeticException("unsigned overflow");
            } else if ((a > 0) && (b < 0) && (b < (Long.MIN_VALUE / a))) {
                // a * b < 0, overflow when b > Long.MAX_VALUE / (-a)
                throw new ArithmeticException("unsigned overflow");
            }
        }
        return a * b;
    }

    /**
     * Computes a * b * Π_{i = 0}^{n - 1} number[i] safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @param numbers  other numbers to multiply.
     * @return a * b * Π_{i = 0}^{n - 1} number[i].
     * @throws ArithmeticException if overflow occurs.
     */
    public static int mulSafe(int a, int b, boolean unsigned, int... numbers) {
        int prod = mulSafe(a, b, unsigned);
        for (int n : numbers) {
            prod = mulSafe(prod, n, unsigned);
        }
        return prod;
    }

    /**
     * Computes a * b safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @return a * b.
     * @throws ArithmeticException if overflow occurs.
     */
    public static int mulSafe(int a, int b, boolean unsigned) {
        if (unsigned) {
            // neg (32-bit) * neg (32-bit), must overflow since 32-bit * 32-bit > 32-bit
            if (a < 0 && b < 0) {
                throw new ArithmeticException("unsigned overflow");
            }
            // neg (32-bit) * pos, must overflow when pos > 1, since 32-bit * pos (> 1) > 32-bit
            if ((a < 0 && b > 1) || (a > 1 && b < 0)) {
                throw new ArithmeticException("unsigned overflow");
            }
            // pos * pos, if (2^32 / a) > b, then there is no overflow.
            if (a > 1 && b > 1) {
                int tmp = Integer.divideUnsigned(0xFFFFFFFF, a);
                if (b > tmp) {
                    throw new ArithmeticException("unsigned overflow");
                }
            }
        } else {
            if ((a > 0) && (b > 0) && (b > Integer.MAX_VALUE / a)) {
                // a * b > 0, overflow when b > Integer.MAX_VALUE / a
                throw new ArithmeticException("signed overflow");
            } else if ((a < 0) && (b < 0) && ((-b) > Integer.MAX_VALUE / (-a))) {
                // a * b > 0, overflow when b > Integer.MAX_VALUE / a
                throw new ArithmeticException("signed overflow");
            } else if ((a < 0) && (b > 0) && (b > Integer.MAX_VALUE / (-a))) {
                // a * b < 0, overflow when b > Integer.MAX_VALUE / (-a)
                throw new ArithmeticException("unsigned overflow");
            } else if ((a > 0) && (b < 0) && (b < (Integer.MIN_VALUE / a))) {
                // a * b < 0, overflow when b > Integer.MAX_VALUE / (-a)
                throw new ArithmeticException("unsigned overflow");
            }
        }
        return a * b;
    }

    /**
     * Checks if no overflow occurs when putting the result of Π_{i = 0}^{n - 1} number[i] in int.
     *
     * @param unsigned if treating the values as unsigned.
     * @param numbers  numbers to multiply.
     * @return true if no overflow occurs when putting the result of Π_{i = 0}^{n - 1} number[i] in int.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean productFitsIn(boolean unsigned, int... numbers) {
        try {
            mulSafe(1, 1, unsigned, numbers);
        } catch (ArithmeticException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks if no overflow occurs when putting the result of in1 * Π_{i = 0}^{n - 1} number[i] in long.
     *
     * @param unsigned if treating the values as unsigned.
     * @param in1      an input.
     * @param numbers  other numbers to multiply.
     * @return true if no overflow occurs when putting the result of in1 * Π_{i = 0}^{n - 1} number[i] in long.
     */
    public static boolean productFitsIn(boolean unsigned, long in1, long... numbers) {
        try {
            mulSafe(in1, 1L, unsigned, numbers);
        } catch (ArithmeticException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks if no overflow occurs when putting the result of Π_{i = 0}^{n - 1} number[i] in long.
     *
     * @param unsigned if treating the values as unsigned.
     * @param numbers  numbers to multiply.
     * @return true if no overflow occurs when putting the result of Π_{i = 0}^{n - 1} number[i] in long.
     */
    public static boolean productFitsIn(boolean unsigned, long... numbers) {
        try {
            mulSafe(1L, 1L, unsigned, numbers);
        } catch (ArithmeticException e) {
            return false;
        }
        return true;
    }

    /**
     * Computes a - b safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @return a - b.
     * @throws ArithmeticException if overflow occurs.
     */
    public static int subSafe(int a, int b, boolean unsigned) {
        if (unsigned) {
            // the core is judge (a + b)'s bit string  larger than 0xFFFFFFFF...
            // the logic here is same as the borrow computation in subUint64, 0 is the smallest, borrow = 1
            if (a == 0 && b != 0) {
                throw new ArithmeticException("unsigned underflow");
            }
            if (a > 0 && b > 0 && a < b) {
                throw new ArithmeticException("unsigned underflow");
            }
            if (a < 0 && b < 0) {
                if (a < b) {
                    throw new ArithmeticException("unsigned underflow");
                }
            }
            if (a > 0 && b < 0) {
                throw new ArithmeticException("unsigned underflow");
            }
        } else {
            if (a < 0 && (b > Integer.MAX_VALUE + a)) {
                throw new ArithmeticException("signed overflow");
            } else if (a > 0 && (b < Integer.MIN_VALUE + a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        return a - b;
    }

    /**
     * Computes a - b safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @return a - b.
     * @throws ArithmeticException if overflow occurs.
     */
    public static long subSafe(long a, long b, boolean unsigned) {
        if (unsigned) {
            // the core is judge (a + b)'s bit string  larger than 0xFFFFFFFF...
            // the logic here is same as the borrow  computation in subUint64, 0 is the smallest, borrow = 1
            if (a == 0 && b != 0) {
                throw new ArithmeticException("unsigned underflow");
            }
            if (a > 0 && b > 0 && a < b) {
                throw new ArithmeticException("unsigned underflow");
            }
            if (a < 0 && b < 0) {
                if (a < b) {
                    throw new ArithmeticException("unsigned underflow");
                }
            }
            if (a > 0 && b < 0) {
                throw new ArithmeticException("unsigned underflow");
            }
        } else {
            if (a < 0 && (b > Long.MAX_VALUE + a)) {
                throw new ArithmeticException("signed overflow");
            } else if (a > 0 && (b < Long.MIN_VALUE + a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        return a - b;
    }

    /**
     * Computes a + b + Σ_{i = 0}^{n - 1} number[i] safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @param numbers  other numbers to multiply.
     * @return a + b + Σ_{i = 0}^{n - 1} number[i].
     * @throws ArithmeticException if overflow occurs.
     */
    public static long addSafe(long a, long b, boolean unsigned, long... numbers) {
        long sum = addSafe(a, b, unsigned);
        for (long n : numbers) {
            sum = addSafe(sum, n, unsigned);
        }
        return sum;
    }

    /**
     * Computes a + b safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @return a + b.
     * @throws ArithmeticException if overflow occurs.
     */
    public static long addSafe(long a, long b, boolean unsigned) {
        if (unsigned) {
            // the core is judge (a + b)'s bit string larger than 0xFFFFFFFF...
            // the logic here is same as the carry's computation in addUint64
            if (a < 0 && b < 0) {
                throw new ArithmeticException("unsigned overflow");
            }
            if ((a < 0 && b > 0) || (a > 0 && b < 0)) {
                if (a + b >= 0) {
                    throw new ArithmeticException("unsigned overflow");
                }
            }
        } else {
            if (a > 0 && (b > Long.MAX_VALUE - a)) {
                throw new ArithmeticException("signed overflow");
            } else if (a < 0 && (b < Long.MIN_VALUE - a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        return a + b;
    }

    /**
     * Computes a + b + Σ_{i = 0}^{n - 1} number[i] safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @param numbers  other numbers to multiply.
     * @return a + b + Σ_{i = 0}^{n - 1} number[i].
     * @throws ArithmeticException if overflow occurs.
     */
    public static int addSafe(int a, int b, boolean unsigned, int... numbers) {
        int sum = addSafe(a, b, unsigned);
        for (int n : numbers) {
            sum = addSafe(sum, n, unsigned);
        }
        return sum;
    }

    /**
     * Computes a + b safely (checking overflow).
     *
     * @param a        a.
     * @param b        b.
     * @param unsigned if treating the values as unsigned.
     * @return a + b.
     * @throws ArithmeticException if overflow occurs.
     */
    public static int addSafe(int a, int b, boolean unsigned) {
        if (unsigned) {
            // the core is judge (in1 + in2)'s bit string larger than 0xFFFFFFFF...
            // the logic here is same as the carry's computation in addUint64
            if (a < 0 && b < 0) {
                throw new ArithmeticException("unsigned overflow");
            }
            if ((a < 0 && b > 0) || (a > 0 && b < 0)) {
                if (a + b >= 0) {
                    throw new ArithmeticException("unsigned overflow");
                }
            }
        } else {
            if (a > 0 && (b > Integer.MAX_VALUE - a)) {
                throw new ArithmeticException("signed overflow");
            } else if (a < 0 && (b < Integer.MIN_VALUE - a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        return a + b;
    }
}
