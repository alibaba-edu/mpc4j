package edu.alibaba.mpc4j.common.tool.utils;

import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;

import java.util.stream.IntStream;

/**
 * 环库（Rings Library）工具类。
 *
 * @author Weiran Liu
 * @date 2021/12/10
 */
public class RingsUtils {
    /**
     * 私有构造函数。
     */
    private RingsUtils() {
        // empty
    }

    /**
     * 将GF(2^l)有限域元素转换为{@code byte[]}，大端表示。
     *
     * @param value 待转换的GF(2^l)有限域元素。
     * @param lByteLength l的字节长度。
     * @return 转换结果。
     */
    public static byte[] gf2eToByteArray(UnivariatePolynomialZp64 value, int lByteLength) {
        // OKVE里面要考虑value = null的情况
        if (value == null) {
            return null;
        }
        int lBitLength = lByteLength * Byte.SIZE;
        assert value.modulus() == 2;
        assert value.degree() < lBitLength;
        byte[] byteArray = new byte[lByteLength];
        for (int i = 0; i <= value.degree(); i++) {
            boolean coefficient = value.get(i) != 0L;
            BinaryUtils.setBoolean(byteArray, lBitLength - 1 - i, coefficient);
        }
        return byteArray;
    }

    /**
     * 将{@code byte[]}转换为GF(2^l)有限域元素，大端表示，即byte[0]的最高位为GF(2^l)对应多项式的最高阶系数。
     *
     * @param value 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static UnivariatePolynomialZp64 byteArrayToGf2e(byte[] value) {
        int lBitLength = value.length * Byte.SIZE;
        long[] coefficientLongArray = IntStream.range(0, lBitLength)
            // 字节数组为大端表示，转换为GF(2^l)多项式时，最高位是最高系数，因此要反过来转换
            .mapToLong(degree -> BinaryUtils.getBoolean(value, lBitLength - 1 - degree) ? 1L : 0L)
            .toArray();
        return UnivariatePolynomialZp64.create(2L, coefficientLongArray);
    }

    /**
     * 将GF(2^l)有限域元素转换为{@code byte[]}，小端表示。
     *
     * @param value 待转换的GF(2^l)有限域元素。
     * @param lByteLength l的字节长度。
     * @return 转换结果。
     */
    public static byte[] gf2xToLittleEndianByteArray(UnivariatePolynomialZp64 value, int lByteLength) {
        // OKVE里面要考虑value = null的情况
        if (value == null) {
            return null;
        }
        int lBitLength = lByteLength * Byte.SIZE;
        assert value.modulus() == 2;
        assert value.degree() < lBitLength;
        byte[] byteArray = new byte[lByteLength];
        for (int i = 0; i <= value.degree(); i++) {
            boolean coefficient = value.get(i) != 0L;
            BinaryUtils.setBoolean(byteArray, i, coefficient);
        }
        return byteArray;
    }

    /**
     * 将{@code byte[]}转换为GF(2^l)有限域元素，小端表示，即byte[0]的最高位为GF(2^l)对应多项式的最低阶系数（常数项）。
     *
     * @param value 待转换的{@code byte[]}。
     * @return 转换结果。
     */
    public static UnivariatePolynomialZp64 littleEndianByteArrayToGf2x(byte[] value) {
        int lBitLength = value.length * Byte.SIZE;
        long[] coefficientLongArray = IntStream.range(0, lBitLength)
            // 字节数组为小端表示，转换为GF(2^l)多项式时，最高位是最低系数
            .mapToLong(degree -> BinaryUtils.getBoolean(value, degree) ? 1L : 0L)
            .toArray();
        return UnivariatePolynomialZp64.create(2L, coefficientLongArray);
    }
}
