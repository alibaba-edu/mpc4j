package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.X25519ByteEccUtils;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.security.SecureRandom;

/**
 * Bouncy Castle实现的X25519乘法字节椭圆曲线。协因子处理方式参见：
 * <p>
 * https://neilmadden.blog/2020/05/28/whats-the-curve25519-clamping-all-about/
 * </p>
 * 快速求幂算法参见：
 * <p>
 * https://martin.kleppmann.com/papers/curve25519.pdf
 * </p>
 * 注意，X25519无法实现inverseScalar操作，这是因为任意一个随机点既可能在X25519上，也可能在扭曲X25519上，而两个曲线的阶不相等。参见：
 * <p>
 * https://loup-vaillant.fr/tutorials/cofactor
 * </p>
 * 详细描述为：
 * <p>
 * X25519 however only transmits the x-coordinate of the point, so the worst you can have is a point on the "twist".
 * Since the twist of Curve25519 also has a big prime order (2^{253} minus something) and a small cofactor (4), the
 * results will be similar, and the attacker will learn nothing. Curve25519 is thus "twist secure".
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/9/2
 */
public class X25519BcByteMulEcc implements ByteMulEcc {
    /**
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private X25519BcByteMulEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, X25519ByteEccUtils.POINT_BYTES);
        X25519ByteEccUtils.precomputeBase();
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {
        return X25519ByteEccUtils.randomClampScalar(secureRandom);
    }

    @Override
    public boolean isValidPoint(byte[] p) {
        return X25519ByteEccUtils.checkPoint(p);
    }

    @Override
    public byte[] getInfinity() {
        return BytesUtils.clone(X25519ByteEccUtils.POINT_INFINITY);
    }

    @Override
    public byte[] getG() {
        return BytesUtils.clone(X25519ByteEccUtils.POINT_B);
    }

    @Override
    public byte[] randomPoint(SecureRandom secureRandom) {
        return X25519ByteEccUtils.randomPoint(secureRandom);
    }

    @Override
    public byte[] hashToCurve(byte[] message) {
        byte[] p = hash.digestToBytes(message);
        p[X25519ByteEccUtils.POINT_BYTES - 1] &= 0x7F;
        return p;
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {
        assert X25519ByteEccUtils.checkPoint(p);
        assert X25519ByteEccUtils.checkClampScalar(k);
        byte[] r = new byte[X25519ByteEccUtils.POINT_BYTES];
        X25519ByteEccUtils.clampScalarMul(k, p, r);
        return r;
    }

    @Override
    public byte[] baseMul(byte[] k) {
        assert X25519ByteEccUtils.checkClampScalar(k);
        byte[] r = new byte[X25519ByteEccUtils.POINT_BYTES];
        X25519ByteEccUtils.clampScalarBaseMul(k, r);
        return r;
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.X25519_BC;
    }

    @Override
    public int pointByteLength() {
        return X25519ByteEccUtils.POINT_BYTES;
    }

    @Override
    public int scalarByteLength() {
        return X25519ByteEccUtils.SCALAR_BYTES;
    }

    /**
     * singleton mode
     */
    private static final X25519BcByteMulEcc INSTANCE = new X25519BcByteMulEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static X25519BcByteMulEcc getInstance() {
        return INSTANCE;
    }
}
