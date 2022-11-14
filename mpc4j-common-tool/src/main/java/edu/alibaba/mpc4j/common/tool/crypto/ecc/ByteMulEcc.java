package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import java.security.SecureRandom;

/**
 * 乘法字节椭圆曲线接口。
 *
 * @author Weiran Liu
 * @date 2022/9/1
 */
public interface ByteMulEcc {
    /**
     * 返回随机幂指数。此幂指数不一定小于椭圆曲线的阶，仅保证满足交换律。
     *
     * @param secureRandom 随机状态。
     * @return 随机幂指数。
     */
    byte[] randomScalar(SecureRandom secureRandom);

    /**
     * 验证给定点是否为合法的椭圆曲线点。
     *
     * @param p 给定点。
     * @return 如果合法，返回{@code true}，否则返回{@code false}。
     */
    boolean isValidPoint(byte[] p);

    /**
     * 返回无穷远点。
     *
     * @return 无穷远点。
     */
    byte[] getInfinity();

    /**
     * 返回生成元。
     *
     * @return 生成元。
     */
    byte[] getG();

    /**
     * 返回椭圆曲线上的一个随机点。
     *
     * @param secureRandom 随机状态。
     * @return 椭圆曲线上的随机点。
     */
    byte[] randomPoint(SecureRandom secureRandom);

    /**
     * 将{@code byte[]}表示的数据映射到椭圆曲线上。
     *
     * @param message 数据。
     * @return 椭圆曲线映射点。
     */
    byte[] hashToCurve(byte[] message);

    /**
     * 计算R = k · P。
     *
     * @param p 椭圆曲线点P。
     * @param k 幂指数k。
     * @return 结果R。
     */
    byte[] mul(byte[] p, byte[] k);

    /**
     * 计算R = k · G。
     *
     * @param k 幂指数k。
     * @return 结果R。
     */
    byte[] baseMul(byte[] k);

    /**
     * 返回字节椭圆曲线类型。
     *
     * @return 椭圆曲线类型。
     */
    ByteEccFactory.ByteEccType getByteEccType();

    /**
     * 返回椭圆曲线点的字节长度。
     *
     * @return 椭圆曲线点的字节长度。
     */
    int pointByteLength();

    /**
     * 返回幂指数的字节长度。
     *
     * @return 幂指数的字节长度。
     */
    int scalarByteLength();
}
