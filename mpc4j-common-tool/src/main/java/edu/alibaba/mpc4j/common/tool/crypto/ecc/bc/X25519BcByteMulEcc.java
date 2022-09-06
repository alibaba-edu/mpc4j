package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulEcc;
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
 *
 * @author Weiran Liu
 * @date 2022/9/2
 */
public class X25519BcByteMulEcc implements ByteMulEcc {
    /**
     * 椭圆曲线点字节长度
     */
    private static final int POINT_BYTE_LENGTH = X25519ByteEccUtils.POINT_BYTES;
    /**
     * 幂指数字节长度
     */
    private static final int SCALAR_BYTE_LENGTH = X25519ByteEccUtils.SCALAR_BYTES;
    /**
     * 哈希函数
     */
    private final Hash hash;

    public X25519BcByteMulEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, POINT_BYTE_LENGTH);
        X25519ByteEccUtils.precomputeBase();
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {
        byte[] k = new byte[SCALAR_BYTE_LENGTH];
        secureRandom.nextBytes(k);
        // 后三位设置为0，使结果为8的倍数
        k[0] &= 0xF8;
        // 首位设置为0
        k[POINT_BYTE_LENGTH - 1] &= 0x7F;
        // 次位设置为1，满足安全要求
        k[POINT_BYTE_LENGTH - 1] |= 0x40;
        return k;
    }

    @Override
    public boolean isValidPoint(byte[] p) {
        // X25519的所有店都为有效点
        return p.length == POINT_BYTE_LENGTH;
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
        byte[] p = new byte[POINT_BYTE_LENGTH];
        secureRandom.nextBytes(p);
        return p;
    }

    @Override
    public byte[] hashToCurve(byte[] message) {
        return hash.digestToBytes(message);
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {
        assert p.length == POINT_BYTE_LENGTH;
        assert k.length == POINT_BYTE_LENGTH;
        byte[] r = new byte[POINT_BYTE_LENGTH];
        X25519ByteEccUtils.scalarMult(k, p, r);
        return r;
    }

    @Override
    public byte[] baseMul(byte[] k) {
        assert k.length == POINT_BYTE_LENGTH;
        byte[] r = new byte[POINT_BYTE_LENGTH];
        X25519ByteEccUtils.scalarMultBase(k, r);
        return r;
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.X25519_BC;
    }
}
