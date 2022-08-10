package edu.alibaba.mpc4j.common.tool.utils;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 长整数工具类。
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class LongUtils {

    /**
     * 私有构造函数
     */
    private LongUtils() {
        // empty
    }

    /**
     * 将{@code long}转换为{@code byte[]}，大端表示。
     *
     * @param value 待转换的{@code long}。
     * @return 转换结果。
     */
    public static byte[] longToByteArray(long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    }

    /**
     * 将{@code byte[]}转换为{@code long}，大端表示。
     *
     * @param value 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static long byteArrayToLong(byte[] value) {
        assert value.length == Long.BYTES;
        return ByteBuffer.wrap(value).getLong();
    }

    /**
     * 将{@code long[]}转换为{@code byte[]}。
     *
     * @param longArray 待转换的{@code long[]}。
     * @return 转换结果。
     */
    public static byte[] longArrayToByteArray(long[] longArray) {
        assert longArray.length > 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(longArray.length * Long.BYTES);
        IntStream.range(0, longArray.length).forEach(index -> byteBuffer.putLong(longArray[index]));
        return byteBuffer.array();
    }

    /**
     * 将{@code long[]}转换为{@code byte[]}。
     *
     * @param longArray  待转换的{@code long[]}。
     * @param byteLength 字节长度。
     * @return 转换结果。
     */
    public static byte[] longArrayToByteArray(long[] longArray, int byteLength) {
        assert byteLength <= longArray.length * Long.BYTES;
        if (longArray.length == 0) {
            return new byte[0];
        }
        assert LongUtils.isReduceLongArray(longArray, byteLength * Byte.SIZE);
        byte[] directByteArray = longArrayToByteArray(longArray);
        if (byteLength == longArray.length * Long.BYTES) {
            return directByteArray;
        } else {
            byte[] resultByteArray = new byte[byteLength];
            // 如果所要求的字节长度小于实际转换的字节长度，则前面截断
            System.arraycopy(directByteArray, directByteArray.length - resultByteArray.length, resultByteArray, 0,
                resultByteArray.length);
            return resultByteArray;
        }
    }

    /**
     * 将{@code byte[]}转换为{@code long[]}，大端表示。此转换要求{@code byte[]}的长度可以被{@code Long.BYTES}整除。
     *
     * @param byteArray 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static long[] byteArrayToLongArray(byte[] byteArray) {
        assert byteArray.length % Long.BYTES == 0 : "byteArray.length must divides Long.BYTES: " + byteArray.length;
        if (byteArray.length == 0) {
            return new long[0];
        }
        // 不能用ByteBuffer.warp(byteArray).asLongBuffer().array()操作，因为此时的LongBuffer是readOnly的，无法array()
        long[] longArray = new long[byteArray.length / Long.BYTES];
        LongBuffer longBuffer = ByteBuffer.wrap(byteArray).asLongBuffer();
        IntStream.range(0, longBuffer.capacity()).forEach(index -> longArray[index] = longBuffer.get());
        return longArray;
    }

    /**
     * 将{@code byte[]}转换为{@code long[]}。
     *
     * @param byteArray 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static long[] byteArrayToRoundLongArray(byte[] byteArray) {
        if (byteArray.length == 0) {
            return new long[0];
        }
        // 不能用ByteBuffer.warp(byteArray).asLongBuffer().array()操作，因为此时的LongBuffer是readOnly的，无法array()
        long[] longArray = new long[CommonUtils.getUnitNum(byteArray.length, Long.BYTES)];
        int offset = longArray.length * Long.BYTES - byteArray.length;
        byte[] paddingByteArray = new byte[longArray.length * Long.BYTES];
        System.arraycopy(byteArray, 0, paddingByteArray, offset, byteArray.length);
        LongBuffer longBuffer = ByteBuffer.wrap(paddingByteArray).asLongBuffer();
        IntStream.range(0, longBuffer.capacity()).forEach(index -> longArray[index] = longBuffer.get());
        return longArray;
    }

    /**
     * 计算Math.min(1, log_2(x))，计算结果向上取整。
     *
     * @param x 输入值。
     * @return log_2(x)。
     */
    public static int ceilLog2(long x) {
        assert x > 0 : "x must be greater than 0: " + x;
        if (x == 1) {
            // 输入为n = 1要特殊处理，因为下面的循环会让n取不到1
            return 1;
        }
        // 感谢@麟琦的意见，需要单独处理63比特和62比特，因为1L << 63会变成负数，不能使用下述powK的方法执行
        if (x > (1L << 62)) {
            return 63;
        }
        int k = 0;
        long powK = 1;
        while (powK < x) {
            k++;
            powK = powK << 1;
        }
        return k;
    }

    /**
     * 计算Math.min(minCeilLog2, log_2(x))，计算结果向上取整。
     *
     * @param x           输入值。
     * @param minCeilLog2 最小值。
     * @return Math.min(minCeilLog2, log_2 ( x))。
     */
    public static int ceilLog2(long x, int minCeilLog2) {
        assert minCeilLog2 >= 1 : "minCeilLog2 must be greater than 0: " + minCeilLog2;
        return Math.max(ceilLog2(x), minCeilLog2);
    }

    /**
     * 将给定的{@code long[]}修正为有效位数是{@code bitLength}的{@code long[]}，大端表示。
     *
     * @param longArray 给定的{@code long[]}。
     * @param bitLength 有效比特位数。
     */
    public static void reduceLongArray(long[] longArray, final int bitLength) {
        assert bitLength >= 0 && bitLength <= longArray.length * Long.SIZE;
        for (int binaryIndex = 0; binaryIndex < longArray.length * Long.SIZE - bitLength; binaryIndex++) {
            BinaryUtils.setBoolean(longArray, binaryIndex, false);
        }
    }

    /**
     * 验证给定{@code long[]}的有效位数是否为{@code bitLength}，大端表示。要求{@code bitLength >= 0}但不会验证。
     *
     * @param longArray 给定的{@code long[]}。
     * @param bitLength 期望的比特长度。
     */
    public static boolean isReduceLongArray(long[] longArray, final int bitLength) {
        assert bitLength >= 0 && bitLength <= longArray.length * Long.SIZE;
        for (int binaryIndex = 0; binaryIndex < longArray.length * Long.SIZE - bitLength; binaryIndex++) {
            if (BinaryUtils.getBoolean(longArray, binaryIndex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 返回给定{@code long[]}的克隆结果。
     *
     * @param longArray 待克隆的{@code long[]}。
     * @return {@code long[]}的克隆结果。如果待克隆的{@code long[]}为null，则返回null。
     */
    public static long[] clone(final long[] longArray) {
        if (longArray == null) {
            return null;
        }
        return Arrays.copyOf(longArray, longArray.length);
    }

    /**
     * 计算两个数组的XOR结果。
     *
     * @param x1 第一个数组。
     * @param x2 第二个数组。
     * @return x1 XOR x2。
     */
    public static long[] xor(final long[] x1, final long[] x2) {
        assert x1.length == x2.length;
        long[] out = new long[x1.length];
        for (int i = x1.length - 1; i >= 0; i--) {
            out[i] = (x1[i] ^ x2[i]);
        }

        return out;
    }

    /**
     * 计算两个数组的XOR结果，并把结果放在第一个数组上。
     *
     * @param x1 第一个数组。
     * @param x2 第二个数组。
     */
    public static void xori(long[] x1, final long[] x2) {
        assert x1.length == x2.length;
        for (int i = x1.length - 1; i >= 0; i--) {
            x1[i] = (x1[i] ^ x2[i]);
        }
    }

    /**
     * 计算两个数组的AND结果。
     *
     * @param x1 第一个数组。
     * @param x2 第二个数组。
     * @return x1 AND x2。
     */
    public static long[] and(final long[] x1, final long[] x2) {
        assert x1.length == x2.length;
        long[] out = new long[x1.length];
        for (int i = x1.length - 1; i >= 0; i--) {
            out[i] = (x1[i] & x2[i]);
        }

        return out;
    }

    /**
     * 计算两个数组的AND结果，并把结果放在第一个数组上。
     *
     * @param x1 第一个数组。
     * @param x2 第二个数组。
     */
    public static void andi(long[] x1, final long[] x2) {
        assert x1.length == x2.length;
        for (int i = x1.length - 1; i >= 0; i--) {
            x1[i] = (x1[i] & x2[i]);
        }
    }

    /**
     * 计算两个数组的OR结果。
     *
     * @param x1 第一个数组。
     * @param x2 第二个数组。
     * @return x1 OR x2。
     */
    public static long[] or(final long[] x1, final long[] x2) {
        assert x1.length == x2.length;
        long[] out = new long[x1.length];
        for (int i = x1.length - 1; i >= 0; i--) {
            out[i] = (x1[i] | x2[i]);
        }

        return out;
    }

    /**
     * 计算两个数组的OR结果，并把结果放在第一个数组上。
     *
     * @param x1 第一个数组。
     * @param x2 第二个数组。
     */
    public static void ori(long[] x1, final long[] x2) {
        assert x1.length == x2.length;
        for (int i = x1.length - 1; i >= 0; i--) {
            x1[i] = (x1[i] | x2[i]);
        }
    }

    /**
     * 计算数的NOT结果。
     *
     * @param x         数组。
     * @param bitLength 比特长度。
     * @return NOT x。
     */
    public static long[] not(final long[] x, final int bitLength) {
        assert bitLength >= 0 && bitLength <= x.length * Long.SIZE;
        long[] ones = new long[x.length];
        Arrays.fill(ones, 0xFFFFFFFFFFFFFFFFL);
        reduceLongArray(ones, bitLength);

        return xor(x, ones);
    }


    /**
     * 计算数组的NOT结果，并把结果更新在数组上。
     *
     * @param x         数组。
     * @param bitLength 比特长度。
     */
    public static void noti(long[] x, final int bitLength) {
        long[] ones = new long[x.length];
        Arrays.fill(ones, 0xFFFFFFFFFFFFFFFFL);
        reduceLongArray(ones, bitLength);
        xori(x, ones);
    }

    /**
     * 生成一个范围在(0, n)的随机数。
     *
     * @param n            上界。
     * @param secureRandom 随机状态。
     * @return 随机数。
     */
    public static long randomPositive(final long n, SecureRandom secureRandom) {
        assert n > 1 : "n must be greater than 1: " + n;
        while (true) {
            long random = Math.floorMod(secureRandom.nextLong(), n);
            if (random > 0) {
                return random;
            }
        }
    }

    /**
     * 生成一个范围在[0, n)的随机数。
     *
     * @param n            上界。
     * @param secureRandom 随机状态。
     * @return 随机数。
     */
    public static long randomNonNegative(final long n, SecureRandom secureRandom) {
        assert n > 0 : "n must be greater than 0: " + n;
        return Math.floorMod(secureRandom.nextLong(), n);
    }
}
