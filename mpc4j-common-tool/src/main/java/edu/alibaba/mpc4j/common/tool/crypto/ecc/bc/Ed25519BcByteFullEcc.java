package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Bouncy Castle实现的Ed25519全功能字节椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/9/1
 */
public class Ed25519BcByteFullEcc implements ByteFullEcc {
    /**
     * l = 2^{252} + 27742317777372353535851937790883648493
     */
    private static final BigInteger N = BigInteger.ONE.shiftLeft(252)
        .add(new BigInteger("27742317777372353535851937790883648493"));
    /**
     * 椭圆曲线点字节长度
     */
    private static final int POINT_BYTE_LENGTH = Ed25519ByteEccUtils.POINT_BYTES;
    /**
     * 幂指数字节长度
     */
    private static final int SCALAR_BYTE_LENGTH = Ed25519ByteEccUtils.SCALAR_BYTES;
    /**
     * 幂指数-1
     */
    private static final byte[] NEG_SCALAR_ONE;

    static {
        BigInteger negate = BigInteger.ONE.negate().mod(N);
        NEG_SCALAR_ONE = BigIntegerUtils.nonNegBigIntegerToByteArray(negate, POINT_BYTE_LENGTH);
        BytesUtils.innerReverseByteArray(NEG_SCALAR_ONE);
    }

    /**
     * 协因子
     */
    private static final byte[] SCALAR_COFACTOR = new byte[]{
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08,
    };
    /**
     * 哈希函数
     */
    private final Hash hash;

    public Ed25519BcByteFullEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, POINT_BYTE_LENGTH);
        Ed25519ByteEccUtils.precomputeBase();
    }

    @Override
    public BigInteger getN() {
        return N;
    }

    @Override
    public BigInteger randomZn(SecureRandom secureRandom) {
        return BigIntegerUtils.randomPositive(N, secureRandom);
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {
        BigInteger zn = randomZn(secureRandom);
        byte[] k = BigIntegerUtils.nonNegBigIntegerToByteArray(zn, SCALAR_BYTE_LENGTH);
        BytesUtils.innerReverseByteArray(k);
        return k;
    }

    @Override
    public boolean isValidPoint(byte[] p) {
        return Ed25519ByteEccUtils.validPoint(p);
    }

    @Override
    public byte[] getInfinity() {
        return BytesUtils.clone(Ed25519ByteEccUtils.POINT_INFINITY);
    }

    @Override
    public byte[] getG() {
        return BytesUtils.clone(Ed25519ByteEccUtils.POINT_B);
    }

    @Override
    public byte[] randomPoint(SecureRandom secureRandom) {
        byte[] p = new byte[POINT_BYTE_LENGTH];
        boolean success = false;
        while (!success) {
            secureRandom.nextBytes(p);
            success = Ed25519ByteEccUtils.validPoint(p);
        }
        // 需要乘以cofactor
        byte[] r = new byte[POINT_BYTE_LENGTH];
        Ed25519ByteEccUtils.scalarMultEncoded(SCALAR_COFACTOR, p, r);
        return r;
    }

    @Override
    public byte[] hashToCurve(byte[] message) {
        // 简单的重复哈希
        byte[] p = hash.digestToBytes(message);
        boolean success = false;
        while (!success) {
            success = Ed25519ByteEccUtils.validPoint(p);
            if (!success) {
                p = hash.digestToBytes(p);
            }
        }
        // 需要乘以cofactor
        byte[] r = new byte[POINT_BYTE_LENGTH];
        Ed25519ByteEccUtils.scalarMultEncoded(SCALAR_COFACTOR, p, r);
        return r;
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {
        byte[] byteK = BytesUtils.reverseByteArray(k);
        BigInteger bigIntegerK = BigIntegerUtils.byteArrayToNonNegBigInteger(byteK);
        return mul(p, bigIntegerK);
    }

    @Override
    public byte[] baseMul(byte[] k) {
        byte[] byteK = BytesUtils.reverseByteArray(k);
        BigInteger bigIntegerK = BigIntegerUtils.byteArrayToNonNegBigInteger(byteK);
        return baseMul(bigIntegerK);
    }

    @Override
    public byte[] add(byte[] p, byte[] q) {
        assert p.length == POINT_BYTE_LENGTH && q.length == POINT_BYTE_LENGTH;
        byte[] r = BytesUtils.clone(p);
        Ed25519ByteEccUtils.pointAdd(r, q);
        return r;
    }

    @Override
    public void addi(byte[] p, byte[] q) {
        assert p.length == POINT_BYTE_LENGTH && q.length == POINT_BYTE_LENGTH;
        Ed25519ByteEccUtils.pointAdd(p, q);
    }

    @Override
    public byte[] neg(byte[] p) {
        assert p.length == POINT_BYTE_LENGTH;
        byte[] r = BytesUtils.clone(p);
        // 翻转符号位
        Ed25519ByteEccUtils.pointNegate(r);
        return r;
    }

    @Override
    public void negi(byte[] p) {
        assert p.length == POINT_BYTE_LENGTH;
        Ed25519ByteEccUtils.pointNegate(p);
    }

    @Override
    public byte[] sub(byte[] p, byte[] q) {
        assert p.length == POINT_BYTE_LENGTH && q.length == POINT_BYTE_LENGTH;
        byte[] r = BytesUtils.clone(p);
        Ed25519ByteEccUtils.pointSubtract(r, q);
        return r;
    }

    @Override
    public void subi(byte[] p, byte[] q) {
        assert p.length == POINT_BYTE_LENGTH && q.length == POINT_BYTE_LENGTH;
        Ed25519ByteEccUtils.pointSubtract(p, q);
    }

    @Override
    public byte[] mul(byte[] p, BigInteger k) {
        assert p.length == POINT_BYTE_LENGTH;
        byte[] byteK = toByteK(k);
        byte[] r = new byte[POINT_BYTE_LENGTH];
        Ed25519ByteEccUtils.scalarMultEncoded(byteK, p, r);
        return r;
    }

    @Override
    public byte[] baseMul(BigInteger k) {
        byte[] byteK = toByteK(k);
        byte[] r = new byte[POINT_BYTE_LENGTH];
        Ed25519ByteEccUtils.scalarMultBaseEncoded(byteK, r);
        return r;
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.ED25519_BC;
    }

    private byte[] toByteK(BigInteger k) {
        assert BigIntegerUtils.greaterOrEqual(k, BigInteger.ZERO) && BigIntegerUtils.less(k, N) :
            "k must be in range [0, " + N.toString().toUpperCase(Locale.ROOT) + "): " + k;
        byte[] byteK = BigIntegerUtils.nonNegBigIntegerToByteArray(k, SCALAR_BYTE_LENGTH);
        BytesUtils.innerReverseByteArray(byteK);
        return byteK;
    }
}
