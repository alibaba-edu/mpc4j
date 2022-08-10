package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.security.SecureRandom;

/**
 * GF(2^l)有限域运算接口。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public interface Gf2e {
    /**
     * 返回运算类型。
     *
     * @return 运算类型。
     */
    Gf2eType getGf2eType();

    /**
     * 返回有限域比特长度。
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
     * 计算a + b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a + b。
     */
    default byte[] add(final byte[] a, final byte[] b) {
        assert validateElement(a) && validateElement(b);
        return BytesUtils.xor(a, b);
    }

    /**
     * 计算a + b，将结果置于a中。
     *
     * @param a 输入a。
     * @param b 输入b。
     */
    default void addi(byte[] a, final byte[] b) {
        assert validateElement(a) && validateElement(b);
        BytesUtils.xori(a, b);
    }

    /**
     * 计算-a。
     *
     * @param a 输入a。
     * @return -a。
     */
    default byte[] neg(byte[] a) {
        assert validateElement(a);
        // GF(2^l)元素的负数就是其本身
        return BytesUtils.clone(a);
    }

    /**
     * 计算-a，将结果置于a中。
     *
     * @param a 输入a。
     */
    default void negi(byte[] a) {
        // GF(2^l)元素的负数就是其本身，所以不需要做任何操作
        assert validateElement(a);
    }

    /**
     * 计算a - b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a - b。
     */
    default byte[] sub(final byte[] a, final byte[] b) {
        // GF(2^l)元素的负数就是其本身，减法等价于加法
        return add(a, b);
    }

    /**
     * 计算a - b，将结果置于a中。
     *
     * @param a 输入a。
     * @param b 输入b。
     */
    default void subi(byte[] a, final byte[] b) {
        // GF(2^l)元素的负数就是其本身，减法等价于加法
        addi(a, b);
    }

    /**
     * 计算a * b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a * b。
     */
    byte[] mul(byte[] a, byte[] b);

    /**
     * 计算a * b，将结果置于a中。
     *
     * @param a 输入a。
     * @param b 输入b。
     */
    void muli(byte[] a, byte[] b);

    /**
     * 计算a / b。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return a / b。
     */
    byte[] div(byte[] a, byte[] b);

    /**
     * 计算a / b，，将结果置于a中。
     *
     * @param a 输入a。
     * @param b 输入b。
     */
    void divi(byte[] a, byte[] b);

    /**
     * 计算1 / a。
     *
     * @param a 输入a。
     * @return 1 / a。
     */
    byte[] inv(byte[] a);

    /**
     * 计算1 / a，将结果置于a中。
     *
     * @param a 输入a。
     */
    void invi(byte[] a);

    /**
     * 创建0元。
     *
     * @return 0元。
     */
    byte[] createZero();

    /**
     * 创建1元。
     *
     * @return 1元。
     */
    byte[] createOne();

    /**
     * 创建随机群元素。
     *
     * @param secureRandom 随机状态。
     * @return 随机群元素。
     */
    byte[] createRandom(SecureRandom secureRandom);

    /**
     * 创建随机域元素（非0随机群元素）。
     *
     * @param secureRandom 随机状态。
     * @return 随机域元素。
     */
    byte[] createNonZeroRandom(SecureRandom secureRandom);

    /**
     * 判断a是否为0元。
     *
     * @param a 输入a。
     * @return a是否为0元。
     */
    boolean isZero(byte[] a);

    /**
     * 判断a是否为1元。
     *
     * @param a 输入a。
     * @return a是否为1元。
     */
    boolean isOne(byte[] a);

    /**
     * 判断a是否为有效的（加法）群元素。
     *
     * @param a 输入a。
     * @return a是否为有效的群元素。
     */
    boolean validateElement(byte[] a);

    /**
     * 判断a是否为有效的（加法、乘法）域元素。
     *
     * @param a 输入a。
     * @return a是否为有效的域元素。
     */
    boolean validateNonZeroElement(byte[] a);
}
