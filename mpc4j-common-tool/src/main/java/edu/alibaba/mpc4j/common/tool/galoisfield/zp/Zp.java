package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Zp有限域运算接口。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public interface Zp {
    /**
     * 返回运算类型。
     *
     * @return 运算类型。
     */
    ZpFactory.ZpType getZpType();

    /**
     * 返回质数。
     *
     * @return 质数。
     */
    BigInteger getPrime();

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
    BigInteger getRangeBound();

    /**
     * 计算a mod p。
     *
     * @param a 输入a。
     * @return a mod p。
     */
    BigInteger module(final BigInteger a);

    /**
     * 计算a + b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a + b。
     */
    BigInteger add(final BigInteger a, final BigInteger b);

    /**
     * 计算-a。
     *
     * @param a 输入a。
     * @return -a。
     */
    BigInteger neg(final BigInteger a);

    /**
     * 计算a - b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a - b。
     */
    BigInteger sub(final BigInteger a, final BigInteger b);

    /**
     * 计算a * b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a * b。
     */
    BigInteger mul(final BigInteger a, final BigInteger b);

    /**
     * 计算a / b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a / b。
     */
    BigInteger div(final BigInteger a, final BigInteger b);

    /**
     * 计算1 / a。
     *
     * @param a 输入a。
     * @return 1 / a。
     */
    BigInteger inv(final BigInteger a);

    /**
     * 计算a^b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a^b。
     */
    BigInteger mulPow(final BigInteger a, final BigInteger b);

    /**
     * 计算Zp元素向量a和布尔向量b的内积。
     *
     * @param zpVector     Zp元素向量。
     * @param binaryVector 布尔向量。
     * @return 内积结果。
     */
    BigInteger innerProduct(final BigInteger[] zpVector, final boolean[] binaryVector);

    /**
     * 创建0元。
     *
     * @return 0元。
     */
    default BigInteger createZero() {
        return BigInteger.ZERO;
    }

    /**
     * 创建1元。
     *
     * @return 1元。
     */
    default BigInteger createOne() {
        return BigInteger.ONE;
    }

    /**
     * 创建随机元素。
     *
     * @param secureRandom 随机状态。
     * @return 随机元素。
     */
    BigInteger createRandom(SecureRandom secureRandom);

    /**
     * 创建随机元素。
     *
     * @param seed 种子。
     * @return 随机元素。
     */
    BigInteger createRandom(byte[] seed);

    /**
     * 创建非0随机元素。
     *
     * @param secureRandom 随机状态。
     * @return 非0随机元素。
     */
    BigInteger createNonZeroRandom(SecureRandom secureRandom);

    /**
     * 创建非0随机元素。
     *
     * @param seed 种子。
     * @return 非0随机元素。
     */
    BigInteger createNonZeroRandom(byte[] seed);

    /**
     * 创建[0, 2^l)范围内的随机元素。
     *
     * @param secureRandom 随机状态。
     * @return [0, 2^l)范围内的随机元素。
     */
    BigInteger createRangeRandom(SecureRandom secureRandom);

    /**
     * 创建[0, 2^l)范围内的随机元素。
     *
     * @param seed 种子。
     * @return [0, 2^l)范围内的随机元素。
     */
    BigInteger createRangeRandom(byte[] seed);

    /**
     * 判断a是否为0元。
     *
     * @param a 输入a。
     * @return a是否为0元。
     */
    default boolean isZero(BigInteger a) {
        assert validateElement(a);
        return a.equals(BigInteger.ZERO);
    }

    /**
     * 判断a是否为1元。
     *
     * @param a 输入a。
     * @return a是否为1元。
     */
    default boolean isOne(BigInteger a) {
        assert validateElement(a);
        return a.equals(BigInteger.ONE);
    }

    /**
     * 判断a是否为有效的（加法）群元素。
     *
     * @param a 输入a。
     * @return a是否为有效的群元素。
     */
    boolean validateElement(BigInteger a);

    /**
     * 判断a是否为有效的（加法、乘法）域元素。
     *
     * @param a 输入a。
     * @return a是否为有效的域元素。
     */
    boolean validateNonZeroElement(BigInteger a);

    /**
     * 判断a是否在[0, 2^l)范围内。
     *
     * @param a 输入a。
     * @return a是否在[0, 2^l)范围内。
     */
    boolean validateRangeElement(BigInteger a);
}
