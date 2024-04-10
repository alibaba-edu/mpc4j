package edu.alibaba.mpc4j.common.tool.utils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.nio.LongBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 布尔数组{@code boolean[]}工具类。
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public class BinaryUtils {
    /**
     * 私有构造函数。
     */
    private BinaryUtils() {
        // empty
    }

    /**
     * 字节对应某一位置布尔值的真值查找表
     */
    private static final int[] BYTE_BOOLEAN_TRUE_TABLE = {
        0b10000000,
        0b01000000,
        0b00100000,
        0b00010000,
        0b00001000,
        0b00000100,
        0b00000010,
        0b00000001,
    };

    /**
     * 字节对应某一位置布尔值的假值查找表
     */
    private static final int[] BYTE_BOOLEAN_FALSE_TABLE = {
        0b01111111,
        0b10111111,
        0b11011111,
        0b11101111,
        0b11110111,
        0b11111011,
        0b11111101,
        0b11111110,
    };

    /**
     * 将{@code byte}转换为{@code boolean[]}，大端表示。
     *
     * @param byteValue 待转换的{@code byte}。
     * @return 转换结果。
     */
    public static boolean[] byteToBinary(final byte byteValue) {
        boolean[] binary = new boolean[Byte.SIZE];
        IntStream.range(0, Byte.SIZE).forEach(index ->
            binary[index] = ((byteValue & BYTE_BOOLEAN_TRUE_TABLE[index]) != 0)
        );
        return binary;
    }

    /**
     * 将{@code boolean[]}转换为{@code byte}，大端表示。
     *
     * @param binary 待转换的{@code boolean[]}。
     * @return 转换结果。
     */
    public static byte binaryToByte(final boolean[] binary) {
        assert binary.length == Byte.SIZE : "binary.length must be equal to " + Byte.SIZE + ": " + binary.length;
        byte byteValue = 0;
        for (int index = 0; index < Byte.SIZE; index++) {
            byteValue = binary[index] ? (byte) (byteValue | BYTE_BOOLEAN_TRUE_TABLE[index]) : byteValue;
        }
        return byteValue;
    }

    /**
     * 将{@code byte[]}转换为{@code boolean[]}，大端表示。
     *
     * @param byteArray 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static boolean[] byteArrayToBinary(final byte[] byteArray) {
        if (byteArray.length == 0) {
            return new boolean[0];
        }
        boolean[] binary = new boolean[byteArray.length * Byte.SIZE];
        // 从前到后进行转换
        for (int byteIndex = 0; byteIndex < byteArray.length; byteIndex++) {
            int startBinaryIndex = byteIndex * Byte.SIZE;
            for (int index = 0; index < Byte.SIZE; index++) {
                binary[startBinaryIndex + index] = ((byteArray[byteIndex] & BYTE_BOOLEAN_TRUE_TABLE[index]) != 0);
            }
        }
        return binary;
    }

    /**
     * 将{@code byte[]}转换为{@code boolean[]}，大端表示。
     *
     * @param byteArray 待转换的{@code byte[]}。
     * @param bitLength 转换的比特长度。
     * @return 转换结果。
     */
    public static boolean[] byteArrayToBinary(final byte[] byteArray, int bitLength) {
        assert bitLength >= 0 && bitLength <= byteArray.length * Byte.SIZE
            : "bitLength must be in range [0, " + byteArray.length * Byte.SIZE + "]: " + bitLength;
        if (byteArray.length == 0) {
            // byteArray.length = 0 implies bitLength = 0
            return new boolean[0];
        }
        assert BytesUtils.isReduceByteArray(byteArray, bitLength);
        boolean[] directBinary = byteArrayToBinary(byteArray);
        if (bitLength == byteArray.length * Byte.SIZE) {
            return directBinary;
        } else {
            boolean[] resultBinary = new boolean[bitLength];
            // 如果所要求的字节长度小于实际转换的字节长度，则前面截断
            System.arraycopy(directBinary, directBinary.length - resultBinary.length, resultBinary, 0,
                resultBinary.length);
            return resultBinary;
        }
    }

    /**
     * Converts {@code byte[]} to {@code boolean[]} without checking {@code byte[]} containing at most bitLength bits.
     *
     * @param byteArray given {@code byte[]}.
     * @param bitLength bit length.
     * @return result.
     */
    public static boolean[] uncheckByteArrayToBinary(final byte[] byteArray, int bitLength) {
        assert bitLength >= 0 && bitLength <= byteArray.length * Byte.SIZE
            : "bitLength must be in range [0, " + byteArray.length * Byte.SIZE + "]: " + bitLength;
        if (byteArray.length == 0) {
            // byteArray.length = 0 implies bitLength = 0
            return new boolean[0];
        }
        int targetByteLength = CommonUtils.getByteLength(bitLength);
        // direct conversion
        boolean[] directBinary = new boolean[targetByteLength * Byte.SIZE];
        int offset = byteArray.length - targetByteLength;
        for (int byteIndex = 0; byteIndex < targetByteLength; byteIndex++) {
            int startBinaryIndex = byteIndex * Byte.SIZE;
            for (int index = 0; index < Byte.SIZE; index++) {
                directBinary[startBinaryIndex + index] = ((byteArray[byteIndex + offset] & BYTE_BOOLEAN_TRUE_TABLE[index]) != 0);
            }
        }
        if (bitLength == directBinary.length) {
            // directly return result
            return directBinary;
        } else {
            // return truncated result
            boolean[] resultBinary = new boolean[bitLength];
            System.arraycopy(directBinary, directBinary.length - resultBinary.length, resultBinary, 0, resultBinary.length);
            return resultBinary;
        }
    }

    /**
     * 将{@code boolean[]}转换为{@code byte[]}，大端表示。此转换要求{@code boolean[]}的长度可以被{@code Byte.SIZE}整除。
     *
     * @param binary 待转换的{@code boolean[]}。
     * @return 转换结果。
     */
    @CanIgnoreReturnValue
    public static byte[] binaryToByteArray(final boolean[] binary) {
        assert binary.length % Byte.SIZE == 0 : "binary.length must divides Byte.SIZE: " + binary.length;
        if (binary.length == 0) {
            return new byte[0];
        }
        int byteLength = binary.length >> 3;
        byte[] byteArray = new byte[byteLength];
        for (int byteIndex = 0; byteIndex < byteLength; byteIndex++) {
            int binaryIndexOffset = byteIndex << 3;
            for (int index = 0; index < Byte.SIZE; index++) {
                if (binary[binaryIndexOffset + index]) {
                    byteArray[byteIndex] |= BYTE_BOOLEAN_TRUE_TABLE[index];
                }
            }
        }
        return byteArray;
    }

    /**
     * 将{@code boolean[]}转换为{@code byte[]}，大端表示。
     *
     * @param binary 待转换的{@code boolean[]}。
     * @return 转换结果。
     */
    public static byte[] binaryToRoundByteArray(final boolean[] binary) {
        if (binary.length == 0) {
            return new byte[0];
        }
        int byteLength = CommonUtils.getByteLength(binary.length);
        int offset = byteLength * Byte.SIZE - binary.length;
        byte[] roundByteArray = new byte[byteLength];
        for (int index = 0; index < binary.length; index++) {
            if (binary[index]) {
                BinaryUtils.setBoolean(roundByteArray, offset + index, true);
            }
        }
        return roundByteArray;
    }

    /**
     * 将{@code Boolean[]}转换为{@code boolean[]}。
     *
     * @param objectBinary 待转换的{@code Boolean[]}。
     * @return 转换结果。
     */
    public static boolean[] objectBinaryToBinary(final Boolean[] objectBinary) {
        if (objectBinary == null) {
            return null;
        }
        boolean[] binary = new boolean[objectBinary.length];
        IntStream.range(0, objectBinary.length).forEach(index -> binary[index] = objectBinary[index]);

        return binary;
    }

    /**
     * 将{@code boolean[]}转换为{@code Boolean[]}。
     *
     * @param binary 待转换的{@code boolean[]}。
     * @return 转换结果。
     */
    public static Boolean[] binaryToObjectBinary(final boolean[] binary) {
        if (binary == null) {
            return null;
        }
        Boolean[] objectBinary = new Boolean[binary.length];
        IntStream.range(0, binary.length).forEach(index -> objectBinary[index] = binary[index]);

        return objectBinary;
    }

    /**
     * Get the i'th bit of a byte array, big-endian representation.
     * For example:
     * <p><ul>
     * <li> the 0'th bit is a byte array is the most significant bit bit in the 0'th byte. </li>
     * <li> the 7'th bit is a byte array is the least significant bit bit in the 0'th byte. </li>
     * <li> the 8'th bit is a byte array is the most significant bit in the 1'th byte. </li>
     * </ul></p>
     *
     * @param byteArray the byte array.
     * @param i         the bit index.
     * @return the value of the i'th bit in byteArray.
     */
    public static boolean getBoolean(final byte[] byteArray, final int i) {
        assert i >= 0 && i < byteArray.length * Byte.SIZE
            : "i must be in range [0, " + byteArray.length * Byte.SIZE + "): " + i;
        return (byteArray[i >> 3] & BYTE_BOOLEAN_TRUE_TABLE[i & 0x07]) != 0;
    }

    /**
     * Set the i'th bit of a byte array, big-endian representation.
     * For example:
     * <p><ul>
     * <li> the 0'th bit is a byte array is the most significant bit bit in the 0'th byte. </li>
     * <li> the 7'th bit is a byte array is the least significant bit bit in the 0'th byte. </li>
     * <li> the 8'th bit is a byte array is the most significant bit in the 1'th byte. </li>
     * </ul></p>
     *
     * @param byteArray the byte array.
     * @param i         the bit index.
     * @param value     the set value of the i'th bit in byteArray.
     */
    public static void setBoolean(byte[] byteArray, final int i, final boolean value) {
        assert i >= 0 && i < byteArray.length * Byte.SIZE;
        int byteIndex = i >> 3;
        int binaryIndex = i & 0x07;
        if (value) {
            byteArray[byteIndex] |= BYTE_BOOLEAN_TRUE_TABLE[binaryIndex];
        } else {
            byteArray[byteIndex] &= BYTE_BOOLEAN_FALSE_TABLE[binaryIndex];
        }
    }

    /**
     * Set all i'th bits of a byte array, big-endian representation.
     * For example:
     * <p><ul>
     * <li> the 0'th bit is a byte array is the most significant bit bit in the 0'th byte. </li>
     * <li> the 7'th bit is a byte array is the least significant bit bit in the 0'th byte. </li>
     * <li> the 8'th bit is a byte array is the most significant bit in the 1'th byte. </li>
     * </ul></p>
     *
     * @param byteArray    the byte array.
     * @param i            all bit indices.
     * @param booleanValue the set value of all bit indices in byteArray.
     */
    public static void setBoolean(byte[] byteArray, final int[] i, final boolean booleanValue) {
        // 提前判断设置的数据，这样可以减少很多次判断操作
        if (booleanValue) {
            Arrays.stream(i).forEach(position -> {
                assert position >= 0 && position < byteArray.length * Byte.SIZE;
                int byteIndex = position >> 3;
                int binaryIndex = position & 0x07;
                byteArray[byteIndex] |= BYTE_BOOLEAN_TRUE_TABLE[binaryIndex];
            });
        } else {
            Arrays.stream(i).forEach(position -> {
                assert position >= 0 && position < byteArray.length * Byte.SIZE;
                int byteIndex = position >> 3;
                int binaryIndex = position & 0x07;
                byteArray[byteIndex] &= BYTE_BOOLEAN_FALSE_TABLE[binaryIndex];
            });
        }
    }

    /**
     * 长整数对应某一位置布尔值的真值查找表
     */
    private static final long[] LONG_BOOLEAN_TRUE_TABLE = {
        0b1000000000000000000000000000000000000000000000000000000000000000L,
        0b0100000000000000000000000000000000000000000000000000000000000000L,
        0b0010000000000000000000000000000000000000000000000000000000000000L,
        0b0001000000000000000000000000000000000000000000000000000000000000L,
        0b0000100000000000000000000000000000000000000000000000000000000000L,
        0b0000010000000000000000000000000000000000000000000000000000000000L,
        0b0000001000000000000000000000000000000000000000000000000000000000L,
        0b0000000100000000000000000000000000000000000000000000000000000000L,
        0b0000000010000000000000000000000000000000000000000000000000000000L,
        0b0000000001000000000000000000000000000000000000000000000000000000L,
        0b0000000000100000000000000000000000000000000000000000000000000000L,
        0b0000000000010000000000000000000000000000000000000000000000000000L,
        0b0000000000001000000000000000000000000000000000000000000000000000L,
        0b0000000000000100000000000000000000000000000000000000000000000000L,
        0b0000000000000010000000000000000000000000000000000000000000000000L,
        0b0000000000000001000000000000000000000000000000000000000000000000L,
        0b0000000000000000100000000000000000000000000000000000000000000000L,
        0b0000000000000000010000000000000000000000000000000000000000000000L,
        0b0000000000000000001000000000000000000000000000000000000000000000L,
        0b0000000000000000000100000000000000000000000000000000000000000000L,
        0b0000000000000000000010000000000000000000000000000000000000000000L,
        0b0000000000000000000001000000000000000000000000000000000000000000L,
        0b0000000000000000000000100000000000000000000000000000000000000000L,
        0b0000000000000000000000010000000000000000000000000000000000000000L,
        0b0000000000000000000000001000000000000000000000000000000000000000L,
        0b0000000000000000000000000100000000000000000000000000000000000000L,
        0b0000000000000000000000000010000000000000000000000000000000000000L,
        0b0000000000000000000000000001000000000000000000000000000000000000L,
        0b0000000000000000000000000000100000000000000000000000000000000000L,
        0b0000000000000000000000000000010000000000000000000000000000000000L,
        0b0000000000000000000000000000001000000000000000000000000000000000L,
        0b0000000000000000000000000000000100000000000000000000000000000000L,
        0b0000000000000000000000000000000010000000000000000000000000000000L,
        0b0000000000000000000000000000000001000000000000000000000000000000L,
        0b0000000000000000000000000000000000100000000000000000000000000000L,
        0b0000000000000000000000000000000000010000000000000000000000000000L,
        0b0000000000000000000000000000000000001000000000000000000000000000L,
        0b0000000000000000000000000000000000000100000000000000000000000000L,
        0b0000000000000000000000000000000000000010000000000000000000000000L,
        0b0000000000000000000000000000000000000001000000000000000000000000L,
        0b0000000000000000000000000000000000000000100000000000000000000000L,
        0b0000000000000000000000000000000000000000010000000000000000000000L,
        0b0000000000000000000000000000000000000000001000000000000000000000L,
        0b0000000000000000000000000000000000000000000100000000000000000000L,
        0b0000000000000000000000000000000000000000000010000000000000000000L,
        0b0000000000000000000000000000000000000000000001000000000000000000L,
        0b0000000000000000000000000000000000000000000000100000000000000000L,
        0b0000000000000000000000000000000000000000000000010000000000000000L,
        0b0000000000000000000000000000000000000000000000001000000000000000L,
        0b0000000000000000000000000000000000000000000000000100000000000000L,
        0b0000000000000000000000000000000000000000000000000010000000000000L,
        0b0000000000000000000000000000000000000000000000000001000000000000L,
        0b0000000000000000000000000000000000000000000000000000100000000000L,
        0b0000000000000000000000000000000000000000000000000000010000000000L,
        0b0000000000000000000000000000000000000000000000000000001000000000L,
        0b0000000000000000000000000000000000000000000000000000000100000000L,
        0b0000000000000000000000000000000000000000000000000000000010000000L,
        0b0000000000000000000000000000000000000000000000000000000001000000L,
        0b0000000000000000000000000000000000000000000000000000000000100000L,
        0b0000000000000000000000000000000000000000000000000000000000010000L,
        0b0000000000000000000000000000000000000000000000000000000000001000L,
        0b0000000000000000000000000000000000000000000000000000000000000100L,
        0b0000000000000000000000000000000000000000000000000000000000000010L,
        0b0000000000000000000000000000000000000000000000000000000000000001L,
    };

    /**
     * 长整数对应某一位置布尔值的假值查找表
     */
    private static final long[] LONG_BOOLEAN_FALSE_TABLE = {
        0b0111111111111111111111111111111111111111111111111111111111111111L,
        0b1011111111111111111111111111111111111111111111111111111111111111L,
        0b1101111111111111111111111111111111111111111111111111111111111111L,
        0b1110111111111111111111111111111111111111111111111111111111111111L,
        0b1111011111111111111111111111111111111111111111111111111111111111L,
        0b1111101111111111111111111111111111111111111111111111111111111111L,
        0b1111110111111111111111111111111111111111111111111111111111111111L,
        0b1111111011111111111111111111111111111111111111111111111111111111L,
        0b1111111101111111111111111111111111111111111111111111111111111111L,
        0b1111111110111111111111111111111111111111111111111111111111111111L,
        0b1111111111011111111111111111111111111111111111111111111111111111L,
        0b1111111111101111111111111111111111111111111111111111111111111111L,
        0b1111111111110111111111111111111111111111111111111111111111111111L,
        0b1111111111111011111111111111111111111111111111111111111111111111L,
        0b1111111111111101111111111111111111111111111111111111111111111111L,
        0b1111111111111110111111111111111111111111111111111111111111111111L,
        0b1111111111111111011111111111111111111111111111111111111111111111L,
        0b1111111111111111101111111111111111111111111111111111111111111111L,
        0b1111111111111111110111111111111111111111111111111111111111111111L,
        0b1111111111111111111011111111111111111111111111111111111111111111L,
        0b1111111111111111111101111111111111111111111111111111111111111111L,
        0b1111111111111111111110111111111111111111111111111111111111111111L,
        0b1111111111111111111111011111111111111111111111111111111111111111L,
        0b1111111111111111111111101111111111111111111111111111111111111111L,
        0b1111111111111111111111110111111111111111111111111111111111111111L,
        0b1111111111111111111111111011111111111111111111111111111111111111L,
        0b1111111111111111111111111101111111111111111111111111111111111111L,
        0b1111111111111111111111111110111111111111111111111111111111111111L,
        0b1111111111111111111111111111011111111111111111111111111111111111L,
        0b1111111111111111111111111111101111111111111111111111111111111111L,
        0b1111111111111111111111111111110111111111111111111111111111111111L,
        0b1111111111111111111111111111111011111111111111111111111111111111L,
        0b1111111111111111111111111111111101111111111111111111111111111111L,
        0b1111111111111111111111111111111110111111111111111111111111111111L,
        0b1111111111111111111111111111111111011111111111111111111111111111L,
        0b1111111111111111111111111111111111101111111111111111111111111111L,
        0b1111111111111111111111111111111111110111111111111111111111111111L,
        0b1111111111111111111111111111111111111011111111111111111111111111L,
        0b1111111111111111111111111111111111111101111111111111111111111111L,
        0b1111111111111111111111111111111111111110111111111111111111111111L,
        0b1111111111111111111111111111111111111111011111111111111111111111L,
        0b1111111111111111111111111111111111111111101111111111111111111111L,
        0b1111111111111111111111111111111111111111110111111111111111111111L,
        0b1111111111111111111111111111111111111111111011111111111111111111L,
        0b1111111111111111111111111111111111111111111101111111111111111111L,
        0b1111111111111111111111111111111111111111111110111111111111111111L,
        0b1111111111111111111111111111111111111111111111011111111111111111L,
        0b1111111111111111111111111111111111111111111111101111111111111111L,
        0b1111111111111111111111111111111111111111111111110111111111111111L,
        0b1111111111111111111111111111111111111111111111111011111111111111L,
        0b1111111111111111111111111111111111111111111111111101111111111111L,
        0b1111111111111111111111111111111111111111111111111110111111111111L,
        0b1111111111111111111111111111111111111111111111111111011111111111L,
        0b1111111111111111111111111111111111111111111111111111101111111111L,
        0b1111111111111111111111111111111111111111111111111111110111111111L,
        0b1111111111111111111111111111111111111111111111111111111011111111L,
        0b1111111111111111111111111111111111111111111111111111111101111111L,
        0b1111111111111111111111111111111111111111111111111111111110111111L,
        0b1111111111111111111111111111111111111111111111111111111111011111L,
        0b1111111111111111111111111111111111111111111111111111111111101111L,
        0b1111111111111111111111111111111111111111111111111111111111110111L,
        0b1111111111111111111111111111111111111111111111111111111111111011L,
        0b1111111111111111111111111111111111111111111111111111111111111101L,
        0b1111111111111111111111111111111111111111111111111111111111111110L,
    };

    /**
     * 将{@code long}转换为{@code boolean[]}，大端表示。
     *
     * @param longValue 待转换的{@code long}。
     * @return 转换结果。
     */
    public static boolean[] longToBinary(final long longValue) {
        boolean[] binary = new boolean[Long.SIZE];
        IntStream.range(0, Long.SIZE).forEach(index ->
            binary[index] = ((longValue & LONG_BOOLEAN_TRUE_TABLE[index]) != 0)
        );
        return binary;
    }

    /**
     * 将{@code boolean[]}转换为{@code long}，大端表示。
     *
     * @param binary 待转换的{@code boolean[]}。
     * @return 转换结果。
     */
    public static long binaryToLong(final boolean[] binary) {
        assert binary.length == Long.SIZE;
        long longValue = 0;
        for (int index = 0; index < Long.SIZE; index++) {
            longValue = binary[index] ? (longValue | LONG_BOOLEAN_TRUE_TABLE[index]) : longValue;
        }
        return longValue;
    }

    /**
     * 将{@code long[]}转换为{@code boolean[]}，大端表示。
     *
     * @param longArray 待转换的{@code op0jg[]}。
     * @return 转换结果。
     */
    public static boolean[] longArrayToBinary(final long[] longArray) {
        if (longArray.length == 0) {
            return new boolean[0];
        }
        boolean[] binary = new boolean[longArray.length * Long.SIZE];
        // 从前到后进行转换
        for (int longIndex = 0; longIndex < longArray.length; longIndex++) {
            int startBinaryIndex = longIndex * Long.SIZE;
            for (int index = 0; index < Long.SIZE; index++) {
                binary[startBinaryIndex + index] = ((longArray[longIndex] & LONG_BOOLEAN_TRUE_TABLE[index]) != 0);
            }
        }
        return binary;
    }

    /**
     * 将{@code long[]}转换为{@code boolean[]}，大端表示。
     *
     * @param longArray 待转换的{@code long[]}。
     * @param bitLength 转换的比特长度。
     * @return 转换结果。
     */
    public static boolean[] longArrayToBinary(final long[] longArray, int bitLength) {
        assert bitLength <= longArray.length * Long.SIZE
            : "bitLength must be in range [0, " + longArray.length * Long.SIZE + "]:" + bitLength;
        if (longArray.length == 0) {
            return new boolean[0];
        }
        assert LongUtils.isReduceLongArray(longArray, bitLength);
        boolean[] directBinary = longArrayToBinary(longArray);
        if (bitLength == longArray.length * Long.SIZE) {
            return directBinary;
        } else {
            boolean[] resultBinary = new boolean[bitLength];
            // 如果所要求的字节长度小于实际转换的字节长度，则前面截断
            System.arraycopy(directBinary, directBinary.length - resultBinary.length, resultBinary, 0,
                resultBinary.length);
            return resultBinary;
        }
    }

    /**
     * 将{@code boolean[]}转换为{@code long[]}，大端表示。
     *
     * @param binary 待转换的{@code boolean[]}。
     * @return 转换结果。
     */
    public static long[] binaryToLongArray(final boolean[] binary) {
        assert binary.length % Long.SIZE == 0;
        if (binary.length == 0) {
            return new long[0];
        }
        int longLength = binary.length / Long.SIZE;
        LongBuffer longBuffer = LongBuffer.allocate(longLength);
        for (int longIndex = 0; longIndex < longLength; longIndex++) {
            longBuffer.put(
                binaryToLong(Arrays.copyOfRange(binary, longIndex * Long.SIZE, (longIndex + 1) * Long.SIZE))
            );
        }
        return longBuffer.array();
    }

    /**
     * 将{@code boolean[]}转换为尽可能短的{@code long[]}，大端表示。
     *
     * @param binary 待转换的{@code boolean[]}。
     * @return 转换结果。
     */
    public static long[] binaryToRoundLongArray(final boolean[] binary) {
        if (binary.length == 0) {
            return new long[0];
        }
        int longLength = CommonUtils.getLongLength(binary.length);
        int offset = longLength * Long.SIZE - binary.length;
        long[] roundLongArray = new long[longLength];
        for (int index = 0; index < binary.length; index++) {
            if (binary[index]) {
                BinaryUtils.setBoolean(roundLongArray, offset + index, true);
            }
        }
        return roundLongArray;
    }

    /**
     * 返回指定位置所对应的布尔值，大端表示。
     *
     * @param longArray 长整数数组。
     * @param position  位置。
     * @return 位置对应的布尔值。
     */
    public static boolean getBoolean(final long[] longArray, final int position) {
        assert position >= 0 && position < longArray.length * Long.SIZE;
        int longIndex = position / Long.SIZE;
        int binaryIndex = position % Long.SIZE;
        return (longArray[longIndex] & LONG_BOOLEAN_TRUE_TABLE[binaryIndex]) != 0;
    }

    /**
     * 将指定位置所对应的布尔值设置为给定的布尔值，大端表示。
     *
     * @param longArray    长整数数组。
     * @param position     位置。
     * @param booleanValue 设置的布尔值。
     */
    public static void setBoolean(long[] longArray, final int position, final boolean booleanValue) {
        assert position >= 0 && position < longArray.length * Long.SIZE;
        int longIndex = position / Long.SIZE;
        int binaryIndex = position % Long.SIZE;
        if (booleanValue) {
            longArray[longIndex] |= LONG_BOOLEAN_TRUE_TABLE[binaryIndex];
        } else {
            longArray[longIndex] &= LONG_BOOLEAN_FALSE_TABLE[binaryIndex];
        }
    }

    /**
     * 返回给定{@code boolean[]}的克隆结果。
     *
     * @param binary 待克隆的{@code boolean[]}。
     * @return {@code boolean[]}的克隆结果。如果待克隆的{@code boolean[]}为null，则返回null。
     */
    public static boolean[] clone(final boolean[] binary) {
        if (binary == null) {
            return null;
        }
        return Arrays.copyOf(binary, binary.length);
    }

    /**
     * Creates a random boolean array.
     *
     * @param binaryLength binary length.
     * @param secureRandom random state.
     * @return a random boolean array.
     */
    public static boolean[] randomBinary(int binaryLength, SecureRandom secureRandom) {
        assert binaryLength > 0 : "binaryLength must be greater than 0: " + binaryLength;
        boolean[] v = new boolean[binaryLength];
        for (int i = 0; i < binaryLength; i++) {
            v[i] = secureRandom.nextBoolean();
        }
        return v;
    }
}
