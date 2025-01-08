package edu.alibaba.mpc4j.common.tool.galoisfield;

import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * Z3 utilities.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public class Z3Utils {
    /**
     * private constructor.
     */
    private Z3Utils() {
        // empty
    }

    /**
     * compressed byte position
     */
    private static final int[] COMPRESS_BYTE_POSITIONS = new int[]{6, 4, 2, 0};
    /**
     * compressed long position
     */
    private static final int[] COMPRESS_LONG_POSITIONS = new int[]{
        62, 60, 58, 56, 54, 52, 50, 48, 46, 44, 42, 40, 38, 36, 34, 32,
        30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 8, 6, 4, 2, 0
    };

    /**
     * Compresses an Z_3 element array into compressed form. If the given data length cannot divide Byte.SIZE / 2, then
     * we try to put data in leading positions.
     *
     * @param data a byte array, in which each byte is an Z_3 element.
     * @return a compressed byte array, in which each byte stores 4 elements in Z_3.
     */
    public static byte[] compressToByteArray(byte[] data) {
        if (data.length == 0) {
            return new byte[0];
        }
        int compressedByteLength = CommonUtils.getUnitNum(data.length, Byte.SIZE / 2);
        byte[] compressedData = new byte[compressedByteLength];
        for (int i = 0, j = 0; i < compressedByteLength && j < data.length; i++) {
            byte temp = 0;
            for (int k : COMPRESS_BYTE_POSITIONS) {
                assert data[j] >= 0 && data[j] < 0b00000011 : "data[" + j + "] is not an Z3 element: " + data[j];
                temp ^= (byte) (data[j++] << k);
                if (j >= data.length) {
                    break;
                }
            }
            compressedData[i] = temp;
        }
        return compressedData;
    }

    /**
     * Decompresses the compressed Z_3 element byte array.
     *
     * @param compressedData a compressed byte array, in which each byte stores 4 elements in Z_3.
     * @param length         decompressed length.
     * @return a byte array with the given length, in which each byte is an Z_3 element.
     */
    public static byte[] decompressFromByteArray(byte[] compressedData, int length) {
        assert length >= 0 && length <= compressedData.length * Byte.SIZE / 2;
        if (length == 0) {
            return new byte[0];
        }
        byte[] data = new byte[length];
        for (int i = 0, j = 0; i < compressedData.length && j < data.length; i++) {
            for (int k : COMPRESS_BYTE_POSITIONS) {
                data[j] = (byte) ((compressedData[i] >> k) & 0b00000011);
                assert data[j] != 0b00000011 : "decompressed value is not a Z3 element";
                j++;
                if (j >= data.length) {
                    break;
                }
            }
        }
        return data;
    }

    /**
     * Compresses an Z_3 element array into compressed form. If the given data length cannot divide Byte.SIZE / 2, then
     * we try to put data in leading positions.
     *
     * @param data a byte array, in which each byte is an Z_3 element.
     * @return a compressed long array, in which each byte stores 32 elements in Z_3.
     */
    public static long[] compressToLongArray(byte[] data) {
        if (data.length == 0) {
            return new long[0];
        }
        int compressedLongLength = CommonUtils.getUnitNum(data.length, Long.SIZE / 2);
        long[] compressedData = new long[compressedLongLength];
        for (int i = 0, j = 0; i < compressedLongLength && j < data.length; i++) {
            long temp = 0;
            for (int k : COMPRESS_LONG_POSITIONS) {
                assert data[j] >= 0 && data[j] < 0b00000011 : "data[" + j + "] is not an Z3 element: " + data[j];
                temp ^= (((long) data[j++]) << k);
                if (j >= data.length) {
                    break;
                }
            }
            compressedData[i] = temp;
        }
        return compressedData;
    }

    /**
     * Decompresses the compressed Z_3 element long array.
     *
     * @param compressedData a compressed long array, in which each long stores 32 elements in Z_3.
     * @param length         decompressed length.
     * @return a byte array with the given length, in which each byte is an Z_3 element.
     */
    public static byte[] decompressFromLongArray(long[] compressedData, int length) {
        assert length >= 0 && length <= compressedData.length * Long.SIZE / 2;
        if (length == 0) {
            return new byte[0];
        }
        byte[] data = new byte[length];
        for (int i = 0, j = 0; i < compressedData.length && j < data.length; i++) {
            for (int k : COMPRESS_LONG_POSITIONS) {
                data[j] = (byte) ((compressedData[i] >> k) & 0b00000011);
                assert data[j] != 0b00000011 : "decompressed value is not a Z3 element";
                j++;
                if (j >= data.length) {
                    break;
                }
            }
        }
        return data;
    }

    /**
     * int mask used to retain the 1st bit for Z_3 elements
     */
    private static final int INT_RETAIN_1ST_BIT_MASK = 0b10101010;
    /**
     * int mask used to retain the 2nd bit for Z_3 elements
     */
    private static final int INT_RETAIN_2ND_BIT_MASK = 0b01010101;

    /**
     * long mask used to retain the 1st bit for Z_3 elements
     */
    private static final long LONG_RETAIN_1ST_BIT_MASK = 0b10101010_10101010_10101010_10101010_10101010_10101010_10101010_10101010L;
    /**
     * long mask used to retain the 2nd bit for Z_3 elements
     */
    private static final long LONG_RETAIN_2ND_BIT_MASK = 0b010101010_10101010_10101010_10101010_10101010_10101010_10101010_1010101L;

    /**
     * Uncheck Z_3 addition in compressed long form.
     *
     * @param a a.
     * @param b b.
     * @return a + b mod 3.
     */
    public static long uncheckCompressLongAdd(long a, long b) {
        long a1a0Xor = ((a & LONG_RETAIN_1ST_BIT_MASK) >>> 1) ^ a;
        long b1b0Xor = ((b & LONG_RETAIN_1ST_BIT_MASK) >>> 1) ^ b;
        // whether a_1a_0 != 00 and b_1b_0 != 00, we need to deal with carry in this case.
        long carry = a1a0Xor & b1b0Xor & LONG_RETAIN_2ND_BIT_MASK;
        long f = (carry << 1) ^ carry;
        return f ^ (a & b) ^ (a ^ b);
    }

    /**
     * Uncheck inplace Z3 addition in compressed long form.
     *
     * @param a a.
     * @param b b.
     */
    public static void uncheckCompressLongAddi(long[] a, long[] b) {
        assert a.length == b.length;
        for (int i = 0; i < a.length; i++) {
            a[i] = uncheckCompressLongAdd(a[i], b[i]);
        }
    }

    /**
     * Uncheck Z_3 negation in compressed long form.
     *
     * @param a a.
     * @return -a mod 3.
     */
    public static long uncheckCompressLongNeg(long a) {
        // a = a_1a_0
        long a1a0Or = ((a >>> 1) | a) & LONG_RETAIN_2ND_BIT_MASK;
        long atLeastOne = ((a1a0Or << 1) ^ a1a0Or);
        // if a = 01 or 10, atLeastOne = 11 and return ~a; otherwise, return 00s.
        return (atLeastOne & (~a));
    }

    /**
     * Uncheck inplace Z3 negation in compressed long form.
     *
     * @param a a.
     */
    public static void uncheckCompressLongNegi(long[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = uncheckCompressLongNeg(a[i]);
        }
    }

    /**
     * Uncheck Z_3 addition in compressed byte form, used to construct lookup table.
     *
     * @param a a.
     * @param b b.
     * @return a + b mod 3.
     */
    private static byte innerUncheckCompressByteAdd(byte a, byte b) {
        // a = a_1a_0, b = b_1b_0
        int a1a0Xor = ((a & INT_RETAIN_1ST_BIT_MASK) >>> 1) ^ a;
        int b1b0Xor = ((b & INT_RETAIN_1ST_BIT_MASK) >>> 1) ^ b;
        // whether a_1a_0 != 00 and b_1b_0 != 00, we need to deal with carry in this case.
        int carry = a1a0Xor & b1b0Xor & INT_RETAIN_2ND_BIT_MASK;
        int f = (carry << 1) ^ carry;
        return (byte) (f ^ (a & b) ^ (a ^ b));
    }

    /**
     * lookup table for 4 elements addition in one byte. We enumerate all addition results for all 2^8 * 2^8 cases.
     */
    private static final byte[] ADD_LOOKUP_TABLE = new byte[(1 << Byte.SIZE) * (1 << Byte.SIZE)];

    static {
        // construct the lookup table
        for (int a = 0; a < (1 << Byte.SIZE); a++) {
            for (int b = 0; b < (1 << Byte.SIZE); b++) {
                ADD_LOOKUP_TABLE[(a << Byte.SIZE) + b] = innerUncheckCompressByteAdd((byte) a, (byte) b);
            }
        }
    }

    /**
     * Uncheck Z_3 addition in compressed byte form using lookup table.
     *
     * @param a a.
     * @param b b.
     * @return a + b mod 3.
     */
    public static byte uncheckCompressByteAdd(byte a, byte b) {
        return ADD_LOOKUP_TABLE[((a & 0xFF) << Byte.SIZE) + (b & 0xFF)];
    }

    /**
     * Uncheck inplace Z3 addition in compressed byte form.
     *
     * @param a a.
     * @param b b.
     */
    public static void uncheckCompressByteAddi(byte[] a, byte[] b) {
        assert a.length == b.length;
        for (int i = 0; i < a.length; i++) {
            a[i] = uncheckCompressByteAdd(a[i], b[i]);
        }
    }

    /**
     * Uncheck Z_3 Negative in compressed byte form, used to construct lookup table.
     *
     * @param a a.
     * @return -a mod 3.
     */
    private static byte innerUncheckCompressByteNeg(byte a) {
        // a = a_1a_0
        int a1a0Or = ((a >>> 1) | a) & INT_RETAIN_2ND_BIT_MASK;
        int atLeastOne = ((a1a0Or << 1) ^ a1a0Or);
        // if a = 01 or 10, atLeastOne = 11 and return ~a; otherwise, return 00s.
        return (byte) (atLeastOne & (~a));
    }

    /**
     * lookup table for 4 elements negation in one byte. We enumerate all addition results for all 2^8 cases.
     */
    private static final byte[] NEG_LOOKUP_TABLE = new byte[1 << Byte.SIZE];

    static {
        // construct the lookup table
        for (int a = 0; a < (1 << Byte.SIZE); a++) {
            NEG_LOOKUP_TABLE[a] = innerUncheckCompressByteNeg((byte) a);
        }
    }

    /**
     * Uncheck Z_3 negation in compressed byte form using lookup table.
     *
     * @param a a.
     * @return -a mod 3.
     */
    public static byte uncheckCompressByteNeg(byte a) {
        return NEG_LOOKUP_TABLE[a & 0xFF];
    }

    /**
     * Uncheck inplace Z3 negation in compressed byte form.
     *
     * @param a a.
     */
    public static void uncheckCompressByteNegi(byte[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = uncheckCompressByteNeg(a[i]);
        }
    }
}
