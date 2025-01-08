package edu.alibaba.mpc4j.common.tool.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Integer Utilities.
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class IntUtils {
    /**
     * max l
     */
    public static final int MAX_L = Integer.SIZE - 2;
    /**
     * maximal signed power of 2
     */
    public static final int MAX_SIGNED_POWER_OF_TWO = 1 << (Integer.SIZE - 2);
    /**
     * unsafe API.
     * See <a href="https://howtodoinjava.com/java-examples/usage-of-class-sun-misc-unsafe/">Usage of class sun.misc.Unsafe</a>
     * for a good explanations with examples. We also note that <code>Unsafe</code> locates in different places in JDK 8
     * and JDK 17.
     */
    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * private constructor.
     */
    private IntUtils() {
        // empty
    }

    /**
     * Clone the data.
     *
     * @param data data.
     * @return clone data.
     */
    public static int[] clone(final int[] data) {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Clone the data.
     *
     * @param data data.
     * @return clone data.
     */
    public static int[][] clone(final int[][] data) {
        int[][] cloneData = new int[data.length][];
        for (int iRow = 0; iRow < data.length; iRow++) {
            cloneData[iRow] = clone(data[iRow]);
        }
        return cloneData;
    }

    /**
     * Converts an int value to a byte array (length = Integer.BYTES) using big-endian format.
     *
     * @param value the given int value.
     * @return the resulting byte array.
     */
    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    /**
     * Converts a byte array (length must be Integer.BYTES) to an int value using big-endian format.
     *
     * @param bytes the given byte array (length must be Integer.BYTES).
     * @return the resulting int value.
     */
    public static int byteArrayToInt(byte[] bytes) {
        assert bytes.length == Integer.BYTES : "value.length must be equal to " + Integer.BYTES + ": " + bytes.length;
        return ByteBuffer.wrap(bytes).getInt();
    }

    /**
     * Returns the byte length for the given int upper bound.
     *
     * @param bound the int upper bound (inclusive).
     * @return the byte length for the given int upper bound.
     */
    public static int boundedNonNegIntByteLength(int bound) {
        assert bound > 0 : "bound must be greater than 0: " + bound;
        if (bound <= Byte.MAX_VALUE) {
            return Byte.BYTES;
        } else if (bound <= Short.MAX_VALUE) {
            return Short.BYTES;
        } else {
            return Integer.BYTES;
        }
    }

    /**
     * Converts a non-negative int value to a byte array, trying to use short byte length.
     *
     * @param value the given value.
     * @param bound the upper bound (inclusive) for the given value.
     * @return the resulting byte array.
     */
    public static byte[] boundedNonNegIntToByteArray(int value, int bound) {
        assert bound > 0 : "bound must be greater than 0: " + bound;
        assert value >= 0 && value <= bound : "value must be in range [0, " + bound + "]: " + value;
        if (bound <= Byte.MAX_VALUE) {
            return ByteBuffer.allocate(Byte.BYTES).put((byte) value).array();
        } else if (bound <= Short.MAX_VALUE) {
            return ByteBuffer.allocate(Short.BYTES).putShort((short) value).array();
        } else {
            return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
        }
    }

    /**
     * Converts a byte array to a non-negative int value, trying to use short byte length.
     *
     * @param bytes the given byte array (length must match the upper bound (inclusive)).
     * @param bound the upper bound (inclusive) for the given value.
     * @return the resulting int value.
     */
    public static int byteArrayToBoundedNonNegInt(byte[] bytes, int bound) {
        int output;
        if (bound <= Byte.MAX_VALUE) {
            assert bytes.length == Byte.BYTES : "byte.length must be equal to " + Byte.BYTES + ": " + bytes.length;
            output = ByteBuffer.wrap(bytes).get();
        } else if (bound <= Short.MAX_VALUE) {
            assert bytes.length == Short.BYTES : "byte.length must be equal to " + Short.BYTES + ": " + bytes.length;
            output = ByteBuffer.wrap(bytes).getShort();
        } else {
            assert bytes.length == Integer.BYTES : "byte.length must be equal to " + Integer.BYTES + ": " + bytes.length;
            output = ByteBuffer.wrap(bytes).getInt();
        }
        assert output >= 0 && output <= bound : "the output must be in range [0, " + bound + "]: " + output;
        return output;
    }

    /**
     * 将{@code int}转换为指定长度的{@code byte[]}，大端表示，不能用于负数。
     * <p>
     * <li>如果指定长度{@code byteLength}小于{@code Integer.BYTES}，则在前面截断。</li>
     * <li>如果指定长度{@code byteLength}大于{@code Integer.BYTES}，则在前面补0。</li>
     * </p>
     *
     * @param value      给定的{@code int}。
     * @param byteLength 要求字节长度。
     * @return 转换结果。
     */
    public static byte[] nonNegIntToFixedByteArray(int value, int byteLength) {
        assert value >= 0;
        assert byteLength > 0;
        if (byteLength >= Integer.BYTES) {
            // 如果要求字节长度大于等于Integer.BYTES，直接分配更大的内存空间，转换后返回结果
            return ByteBuffer.allocate(byteLength).putInt(byteLength - Integer.BYTES, value).array();
        } else {
            // 用assert验证一下value转换后的截断结果不会越界
            assert value <= (1 << byteLength * Byte.SIZE) - 1;
            // 如果要求字节长度小于Integer.BYTES，则需要多复制一次
            byte[] byteArray = ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
            byte[] output = new byte[byteLength];
            System.arraycopy(byteArray, byteArray.length - output.length, output, 0, output.length);
            return output;
        }
    }

    /**
     * 将指定长度的{@code byte[]}转换为{@code int}，大端表示。转换结果一定为正整数。
     * <p>
     * <li>如果{@code byte[]}的长度小于{@code Integer.BYTES}，则在前面补0后转换。</li>
     * <li>如果{@code byte[]}的长度大于{@code Integer.BYTES}，则只取最后{@code Integer.BYTES}个字节转换。</li>
     * </p>
     *
     * @param value 给定的{@code byte[]}。
     * @return 转换结果。
     */
    public static int fixedByteArrayToNonNegInt(byte[] value) {
        assert value.length > 0;
        if (value.length >= Integer.BYTES) {
            // 如果超过了表示范围，只取最后4位
            int output = ByteBuffer.wrap(value).getInt(value.length - Integer.BYTES);
            assert output >= 0;
            return output;
        } else {
            // 如果不够表示范围，则扩展到4位
            byte[] paddingValue = new byte[Integer.BYTES];
            System.arraycopy(value, 0, paddingValue, paddingValue.length - value.length, value.length);
            int output = ByteBuffer.wrap(paddingValue).getInt();
            assert output >= 0;
            return output;
        }
    }

    /**
     * 将{@code int[]}转换为{@code byte[]}。
     *
     * @param intArray 待转换的{@code int[]}。
     * @return 转换结果。
     */
    public static byte[] intArrayToByteArray(int[] intArray) {
        assert intArray.length > 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(intArray.length * Integer.BYTES);
        IntStream.range(0, intArray.length).forEach(index -> byteBuffer.putInt(intArray[index]));

        return byteBuffer.array();
    }

    /**
     * 将{@code byte[]}转换为{@code int[]}。
     *
     * @param byteArray 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static int[] byteArrayToIntArray(byte[] byteArray) {
        assert (byteArray.length > 0 && byteArray.length % Integer.BYTES == 0);
        /*
         * cannot use ByteBuffer.warp(byteArray).asIntBuffer().array(), since IntBuffer is readOnly and cannot array().
         * we tried to use Unsafe, but tests show that when using Unsafe, we need to ahead of time convert
         * byte[0],byte[1],byte[2],byte[3] to byte[3],byte[2],byte[1],byte[0], which involves additional costs.
         */
        int[] intArray = new int[byteArray.length / Integer.BYTES];
        IntBuffer intBuffer = ByteBuffer.wrap(byteArray).asIntBuffer();
        IntStream.range(0, intBuffer.capacity()).forEach(index -> intArray[index] = intBuffer.get());

        return intArray;
    }

    /**
     * Converts a random <code>byte[]</code> to <code>int[]</code>. We do not care about Endian problem in this case
     * so that we can use more efficient way.
     *
     * @param byteArray byte array.
     * @return int array.
     */
    public static int[] randomByteArrayToIntArray(byte[] byteArray) {
        assert (byteArray.length > 0 && byteArray.length % Integer.BYTES == 0);
        int[] intArray = new int[byteArray.length / Integer.BYTES];
        UNSAFE.copyMemory(byteArray, Unsafe.ARRAY_BYTE_BASE_OFFSET, intArray, Unsafe.ARRAY_INT_BASE_OFFSET, byteArray.length);
        return intArray;
    }

    /**
     * Gets the binary value in the given position. The position is in little-endian format.
     *
     * @param intValue the int value.
     * @param position the position.
     * @return the binary value in the given position.
     */
    public static boolean getLittleEndianBoolean(int intValue, int position) {
        assert position >= 0 && position < Integer.SIZE
            : "position must be in range [0, " + Integer.SIZE + "): " + intValue;
        return (intValue & (1 << position)) != 0;
    }


    /**
     * Generates a random int in range (0, n).
     *
     * @param n            the bound.
     * @param secureRandom the random state.
     * @return the random int.
     */
    public static int randomPositive(final int n, SecureRandom secureRandom) {
        assert n > 1 : "n must be greater than 1: " + n;
        while (true) {
            int random = secureRandom.nextInt(n);
            if (random > 0) {
                return random;
            }
        }
    }

    /**
     * Generates a random int in range [0, n).
     *
     * @param n            the bound.
     * @param secureRandom the random state.
     * @return a random int.
     */
    public static int randomNonNegative(final int n, SecureRandom secureRandom) {
        assert n > 0 : "n must be greater than 0: " + n;
        return secureRandom.nextInt(n);
    }
}
