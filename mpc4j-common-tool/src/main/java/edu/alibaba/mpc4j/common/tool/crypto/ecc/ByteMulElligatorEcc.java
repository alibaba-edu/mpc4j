package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import java.security.SecureRandom;

/**
 * 乘法Elligator字节椭圆曲线接口。
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public interface ByteMulElligatorEcc {
    /**
     * 返回随机幂指数。此幂指数不一定小于椭圆曲线的阶，仅保证满足交换律。
     *
     * @param secureRandom 随机状态。
     * @return 随机幂指数。
     */
    byte[] randomScalar(SecureRandom secureRandom);

    /**
     * The base point multiplication under the elligator encoding.
     * <p>
     * See https://www.imperialviolet.org/2013/12/25/elligator.html for more details.
     * </p>
     *
     * @param k            the scalar k.
     * @param point        the obtained non-uniform representative to write to.
     * @param uniformPoint the obtained uniform representative to write to.
     * @return true if success, false otherwise.
     */
    boolean baseMul(final byte[] k, byte[] point, byte[] uniformPoint);

    /**
     * The point multiplication under the elligator decoding.
     * <p>
     * See https://www.imperialviolet.org/2013/12/25/elligator.html for more details.
     * </p>
     *
     * @param uniformPoint the uniform or non-uniform representative for the point.
     * @param k                  the scalar k.
     * @return k · P.
     */
    byte[] uniformMul(final byte[] uniformPoint, final byte[] k);

    /**
     * The point multiplication without elligator decoding.
     *
     * @param point the uniform or non-uniform representative for the point.
     * @param k     the scalar k.
     * @return k · P.
     */
    byte[] mul(final byte[] point, final byte[] k);

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

    /**
     * 返回字节椭圆曲线类型。
     *
     * @return 椭圆曲线类型。
     */
    ByteEccFactory.ByteEccType getByteEccType();
}
