package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.security.SecureRandom;

/**
 * GF(2^128)有限域运算接口。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
public interface Gf2k {
    /**
     * 返回GF(2^128)运算类型。
     *
     * @return 运算类型。
     */
    Gf2kType getGf2kType();

    /**
     * 计算两个GF(2^128)元素的加法。
     *
     * @param a 输入a，小端表示。
     * @param b 输入b，小端表示。
     * @return 加法结果。
     */
    default byte[] add(final byte[] a, final byte[] b) {
        assert a.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert b.length == CommonConstants.BLOCK_BYTE_LENGTH;
        return BytesUtils.xor(a, b);
    }

    /**
     * 计算两个GF(2^128)元素的加法，将结果置于a中。
     *
     * @param a 输入a，小端表示。
     * @param b 输入b，小端表示。
     */
    default void addi(byte[] a, final byte[] b) {
        assert a.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert b.length == CommonConstants.BLOCK_BYTE_LENGTH;
        BytesUtils.xori(a, b);
    }

    /**
     * 计算GF(2^128)元素的负数。
     *
     * @param a 输入a。
     * @return -a。
     */
    default byte[] neg(byte[] a) {
        assert a.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // GF(2^128)元素的负数就是其本身
        return BytesUtils.clone(a);
    }

    /**
     * 计算GF(2^128)元素的负数，将结果置于a中。
     *
     * @param a 输入a。
     */
    default void negi(byte[] a) {
        // GF(2^128)元素的负数就是其本身，所以不需要做任何操作
        assert a.length == CommonConstants.BLOCK_BYTE_LENGTH;
    }

    /**
     * 计算两个GF(2^128)元素的减法。
     *
     * @param a 输入a。
     * @param b 输入b。
     * @return 减法结果。
     */
    default byte[] sub(final byte[] a, final byte[] b) {
        // GF(2^128)元素的负数就是其本身，减法等价于加法
        return add(a, b);
    }

    /**
     * 计算两个GF(2^128)元素的减法，将结果置于a中。
     *
     * @param a 输入a。
     * @param b 输入b。
     */
    default void subi(byte[] a, final byte[] b) {
        // GF(2^128)元素的负数就是其本身，减法等价于加法
        addi(a, b);
    }

    /**
     * 计算两个GF(2^128)元素的乘法。
     *
     * @param a 输入a，小端表示。
     * @param b 输入b，小端表示。
     * @return 乘法结果。
     */
    byte[] mul(byte[] a, byte[] b);

    /**
     * 计算两个GF(2^128)元素的乘法，将结果置于a中。
     *
     * @param a 输入a，小端表示。
     * @param b 输入b，小端表示。
     */
    void muli(byte[] a, byte[] b);

    /**
     * 创建0^128。
     *
     * @return 0^128。
     */
    default byte[] createZero() {
        return new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    }

    /**
     * 创建1元，小端表示。
     *
     * @return 1元，小端表示。
     */
    default byte[] createOne() {
        return new byte[]{
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
        };
    }

    /**
     * 创建GF(2^128)中的随机元素。
     *
     * @param secureRandom 随机状态。
     * @return GF(2 ^ 128)中的随机元素。
     */
    default byte[] createRandom(SecureRandom secureRandom) {
        return BytesUtils.randomByteArray(
            CommonConstants.BLOCK_BIT_LENGTH, CommonConstants.BLOCK_BYTE_LENGTH, secureRandom
        );
    }

    /**
     * 返回有限域字节长度。
     *
     * @return 有限域字节长度。
     */
    default int getByteL() {
        return CommonConstants.BLOCK_BYTE_LENGTH;
    }

    /**
     * 返回有限域比特长度。
     *
     * @return 有限域比特长度。
     */
    default int getL() {
        return CommonConstants.BLOCK_BIT_LENGTH;
    }
}
