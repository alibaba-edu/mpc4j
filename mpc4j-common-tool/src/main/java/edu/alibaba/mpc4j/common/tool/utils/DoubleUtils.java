package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 浮点数工具类。
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class DoubleUtils {
    /**
     * 浮点数默认精度
     */
    public static final double PRECISION = 1e-7;
    /**
     * 统计安全性对应的概率值
     */
    public static final double STATS_NEG_PROBABILITY = 1.0 / Math.pow(2, CommonConstants.STATS_BIT_LENGTH);
    /**
     * 计算安全性对应的概率值
     */
    public static final double COMP_NEG_PROBABILITY = 1.0 / Math.pow(2, CommonConstants.BLOCK_BIT_LENGTH);
    /**
     * ln(10)
     */
    public static final double LOG10 = Math.log(10.0);
    /**
     * ln(2)
     */
    public static final double LOG2 = Math.log(2.0);
    /**
     * exp(-1)
     */
    public static final double EXP_NEGATIVE_1 = Math.exp(-1.0);

    /**
     * private constructor.
     */
    private DoubleUtils() {
        // empty
    }

    /**
     * Clone the data.
     *
     * @param data data.
     * @return clone data.
     */
    public static double[] clone(final double[] data) {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Clone the data.
     *
     * @param data data.
     * @return clone data.
     */
    public static double[][] clone(final double[][] data) {
        double[][] cloneData = new double[data.length][];
        for (int iRow = 0; iRow < data.length; iRow++) {
            cloneData[iRow] = clone(data[iRow]);
        }
        return cloneData;
    }

    /**
     * 将{@code double}转换为{@code byte[]}，大端表示。
     *
     * @param value 待转换的{@code double}。
     * @return 转换结果。
     */
    public static byte[] doubleToByteArray(double value) {
        return ByteBuffer.allocate(Double.BYTES).putDouble(value).array();
    }

    /**
     * 将{@code byte[]}转换为{@code double}，大端表示。
     *
     * @param value 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static double byteArrayToDouble(byte[] value) {
        assert value.length == Double.BYTES;
        return ByteBuffer.wrap(value).getDouble();
    }

    /**
     * 将{@code double[]}转换为{@code byte[]}。
     *
     * @param doubleArray 待转换的{@code double[]}。
     * @return 转换结果。
     */
    public static byte[] doubleArrayToByteArray(double[] doubleArray) {
        assert doubleArray.length > 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(doubleArray.length * Double.BYTES);
        IntStream.range(0, doubleArray.length).forEach(index -> byteBuffer.putDouble(doubleArray[index]));

        return byteBuffer.array();
    }

    /**
     * 将{@code byte[]}转换为{@code double[]}。
     *
     * @param byteArray 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static double[] byteArrayToDoubleArray(byte[] byteArray) {
        assert (byteArray.length > 0 && byteArray.length % Double.BYTES == 0);
        // 不能用ByteBuffer.warp(byteArray).asDoubleBuffer().array()操作，因为此时的DoubleBuffer是readOnly的，无法array()
        double[] doubleArray = new double[byteArray.length / Double.BYTES];
        DoubleBuffer doubleBuffer = ByteBuffer.wrap(byteArray).asDoubleBuffer();
        IntStream.range(0, doubleBuffer.capacity()).forEach(index -> doubleArray[index] = doubleBuffer.get());

        return doubleArray;
    }

    /**
     * 计算组合数估计值：从n个不同元素中任取m ≤ n个元素所有可能的个数，用符号C(n, m)表示。
     *
     * @param n 共有n个元素。
     * @param m 选择m个元素。
     * @return C(n, m)的近似值。
     */
    public static double estimateCombinatorial(int n, int m) {
        assert m >= 0 && m <= n;
        double combinatorial = 1.0;
        // C(n, m) = C(n, n - m)，选择小的m
        long minM = m > n / 2 ? n - m : m;
        for (int i = 1; i <= minM; i++) {
            combinatorial = combinatorial * (n + 1 - i);
            combinatorial = combinatorial / i;
        }
        // 一定能除尽，不需要保留小数
        return combinatorial;
    }

    /**
     * 计算log_2(x)。
     *
     * @param x 输入值。
     * @return log_2(x)。
     */
    public static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
