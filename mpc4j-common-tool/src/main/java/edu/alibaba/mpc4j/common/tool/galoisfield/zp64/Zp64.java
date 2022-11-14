package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import java.security.SecureRandom;

/**
 * Zp64有限域运算接口。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
public interface Zp64 {
    /**
     * 返回运算类型。
     *
     * @return 运算类型。
     */
    Zp64Factory.Zp64Type getZp64Type();

    /**
     * 返回模数。
     *
     * @return 模数。
     */
    long getPrime();

    /**
     * 返回有限域比特长度l，使得p = 2^l + µ
     *
     * @return 有限域比特长度。
     */
    int getL();

    /**
     * 返回有限域字节长度。
     *
     * @return 有限域字节长度。
     */
    int getByteL();

    /**
     * 返回质数比特长度。
     *
     * @return 质数比特长度。
     */
    int getPrimeBitLength();

    /**
     * 返回质数字节长度。
     *
     * @return 质数字节长度。
     */
    int getPrimeByteLength();

    /**
     * 返回有效范围上界（即2^l）。
     *
     * @return 有效范围上界。
     */
    long getRangeBound();

    /**
     * 计算a mod p。
     *
     * @param a 输入a。
     * @return a mod p。
     */
    long module(final long a);

    /**
     * 计算a + b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a + b。
     */
    long add(final long a, final long b);

    /**
     * 计算-a。
     *
     * @param a 输入a。
     * @return -a。
     */
    long neg(final long a);

    /**
     * 计算a - b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a - b。
     */
    long sub(final long a, final long b);

    /**
     * 计算a * b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a * b。
     */
    long mul(final long a, final long b);

    /**
     * 计算a / b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a / b。
     */
    long div(final long a, final long b);

    /**
     * 计算1 / a。
     *
     * @param a 输入a。
     * @return 1 / a。
     */
    long inv(final long a);

    /**
     * 计算a^b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a^b。
     */
    long mulPow(final long a, final long b);

    /**
     * 创建0元。
     *
     * @return 0元。
     */
    default long createZero() {
        return 0L;
    }

    /**
     * 创建1元。
     *
     * @return 1元。
     */
    default long createOne() {
        return 1L;
    }

    /**
     * 创建随机元素。
     *
     * @param secureRandom 随机状态。
     * @return 随机元素。
     */
    long createRandom(SecureRandom secureRandom);

    /**
     * 创建随机元素。
     *
     * @param seed 种子。
     * @return 随机元素。
     */
    long createRandom(byte[] seed);

    /**
     * 创建非0随机元素。
     *
     * @param secureRandom 随机状态。
     * @return 非0随机元素。
     */
    long createNonZeroRandom(SecureRandom secureRandom);

    /**
     * 创建非0随机元素。
     *
     * @param seed 种子。
     * @return 非0随机元素。
     */
    long createNonZeroRandom(byte[] seed);

    /**
     * 创建[0, 2^l)范围内的随机元素。
     *
     * @param secureRandom 随机状态。
     * @return [0, 2^l)范围内的随机元素。
     */
    long createRangeRandom(SecureRandom secureRandom);

    /**
     * 创建[0, 2^l)范围内的随机元素。
     *
     * @param seed 种子。
     * @return [0, 2^l)范围内的随机元素。
     */
    long createRangeRandom(byte[] seed);

    /**
     * 判断a是否为0元。
     *
     * @param a 输入a。
     * @return a是否为0元。
     */
    default boolean isZero(final long a) {
        assert validateElement(a);
        return a == 0L;
    }

    /**
     * 判断a是否为1元。
     *
     * @param a 输入a。
     * @return a是否为1元。
     */
    default boolean isOne(final long a) {
        assert validateElement(a);
        return a == 1L;
    }

    /**
     * 判断a是否为有效的（加法）群元素。
     *
     * @param a 输入a。
     * @return a是否为有效的群元素。
     */
    boolean validateElement(final long a);

    /**
     * 判断a是否为有效的（加法、乘法）域元素。
     *
     * @param a 输入a。
     * @return a是否为有效的域元素。
     */
    boolean validateNonZeroElement(final long a);

    /**
     * 判断a是否在[0, 2^l)范围内。
     *
     * @param a 输入a。
     * @return a是否在[0, 2^l)范围内。
     */
    boolean validateRangeElement(final long a);
}
