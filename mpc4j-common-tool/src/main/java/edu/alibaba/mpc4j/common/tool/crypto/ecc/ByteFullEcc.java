package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 全功能字节椭圆曲线接口。
 * <p></p>
 * One may think that we need to add a similar precompute / destroy_precompute functionality to support fixed-point
 * multiplication. We have tried that but the efficiency result shows that precompute fixed-point multiplication has
 * similar performance compared with direct multiplication. We believe the reason is that for a specific ECC, the
 * underlying implementation has done many optimizations so that the gap between addition and multiplication is very
 * similar.
 *
 * @author Weiran Liu
 * @date 2022/9/1
 */
public interface ByteFullEcc extends ByteMulEcc {
    /**
     * 返回椭圆曲线的阶。
     *
     * @return 椭圆曲线的阶。
     */
    BigInteger getN();

    /**
     * 返回随机幂指数。
     *
     * @param secureRandom 随机状态。
     * @return 随机幂指数。
     */
    BigInteger randomZn(SecureRandom secureRandom);

    /**
     * 计算R = P + Q。
     *
     * @param p 椭圆曲线点P。
     * @param q 椭圆曲线点Q.
     * @return 结果R。
     */
     byte[] add(byte[] p, byte[] q);

    /**
     * 计算P = P + Q。
     *
     * @param p 椭圆曲线点P。
     * @param q 椭圆曲线点Q。
     */
     void addi(byte[] p, byte[] q);

    /**
     * 计算R = -1 · P
     * @param p 椭圆曲线点P。
     * @return 结果R。
     */
     byte[] neg(byte[] p);

    /**
     * 计算P = -1 · P
     * @param p 椭圆曲线点P。
     */
    void negi(byte[] p);

    /**
     * 计算R = P - Q。
     *
     * @param p 椭圆曲线点P。
     * @param q 椭圆曲线点Q。
     * @return 结果R。
     */
     byte[] sub(byte[] p, byte[] q);

    /**
     * 计算P = P - Q。
     *
     * @param p 椭圆曲线点P。
     * @param q 椭圆曲线点Q。
     */
     void subi(byte[] p, byte[] q);

    /**
     * 计算R = k · P。
     *
     * @param p 椭圆曲线点P。
     * @param k 幂指数k。
     * @return 结果R。
     */
    byte[] mul(byte[] p, BigInteger k);

    /**
     * 计算R = k · G。
     *
     * @param k 幂指数k。
     * @return 结果R。
     */
    byte[] baseMul(BigInteger k);
}
