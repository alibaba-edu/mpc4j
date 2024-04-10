package edu.alibaba.mpc4j.common.tool.crypto.ecc.sodium;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.Ed25519ByteEccUtils;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Sodium实现的Ed25519全功能字节椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/9/6
 */
public class Ed25519SodiumByteFullEcc implements ByteFullEcc {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private Ed25519SodiumByteFullEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, Ed25519ByteEccUtils.POINT_BYTES);
        Ed25519ByteEccUtils.precomputeBase();
    }

    @Override
    public BigInteger getN() {
        return Ed25519ByteEccUtils.N;
    }

    @Override
    public BigInteger randomZn(SecureRandom secureRandom) {
        return BigIntegerUtils.randomPositive(Ed25519ByteEccUtils.N, secureRandom);
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {
        BigInteger zn = randomZn(secureRandom);
        byte[] k = BigIntegerUtils.nonNegBigIntegerToByteArray(zn, Ed25519ByteEccUtils.SCALAR_BYTES);
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
        byte[] p = new byte[Ed25519ByteEccUtils.POINT_BYTES];
        boolean success = false;
        while (!success) {
            secureRandom.nextBytes(p);
            p[Ed25519ByteEccUtils.POINT_BYTES - 1] &= 0x7F;
            success = Ed25519ByteEccUtils.validPoint(p);
        }
        // 需要乘以cofactor
        byte[] r = new byte[Ed25519ByteEccUtils.POINT_BYTES];
        Ed25519ByteEccUtils.scalarMulEncoded(Ed25519ByteEccUtils.SCALAR_COFACTOR, p, r);
        return r;
    }

    @Override
    public byte[] hashToCurve(byte[] message) {
        // 简单的重复哈希
        byte[] p = hash.digestToBytes(message);
        p[Ed25519ByteEccUtils.POINT_BYTES - 1] &= 0x7F;
        boolean success = false;
        while (!success) {
            success = Ed25519ByteEccUtils.validPoint(p);
            if (!success) {
                p = hash.digestToBytes(p);
                p[Ed25519ByteEccUtils.POINT_BYTES - 1] &= 0x7F;
            }
        }
        byte[] r = new byte[Ed25519ByteEccUtils.POINT_BYTES];
        Ed25519ByteEccUtils.scalarMulEncoded(Ed25519ByteEccUtils.SCALAR_COFACTOR, p, r);
        return r;
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {
        assert k.length == Ed25519ByteEccUtils.SCALAR_BYTES;
        return nativeMul(p, k);
    }

    @Override
    public byte[] baseMul(byte[] k) {
        assert k.length == Ed25519ByteEccUtils.SCALAR_BYTES;
        return nativeBaseMul(k);
    }

    @Override
    public byte[] add(byte[] p, byte[] q) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES && q.length == Ed25519ByteEccUtils.POINT_BYTES;
        byte[] r = BytesUtils.clone(p);
        Ed25519ByteEccUtils.pointAdd(r, q);
        return r;
    }

    @Override
    public void addi(byte[] p, byte[] q) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES && q.length == Ed25519ByteEccUtils.POINT_BYTES;
        Ed25519ByteEccUtils.pointAdd(p, q);
    }

    @Override
    public byte[] neg(byte[] p) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES;
        byte[] r = BytesUtils.clone(p);
        // 翻转符号位
        Ed25519ByteEccUtils.pointNegate(r);
        return r;
    }

    @Override
    public void negi(byte[] p) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES;
        Ed25519ByteEccUtils.pointNegate(p);
    }

    @Override
    public byte[] sub(byte[] p, byte[] q) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES && q.length == Ed25519ByteEccUtils.POINT_BYTES;
        byte[] r = BytesUtils.clone(p);
        Ed25519ByteEccUtils.pointSubtract(r, q);
        return r;
    }

    @Override
    public void subi(byte[] p, byte[] q) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES && q.length == Ed25519ByteEccUtils.POINT_BYTES;
        Ed25519ByteEccUtils.pointSubtract(p, q);
    }

    @Override
    public byte[] mul(byte[] p, BigInteger k) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES;
        byte[] byteK = Ed25519ByteEccUtils.toByteK(k);
        return nativeMul(p, byteK);
    }

    @Override
    public byte[] baseMul(BigInteger k) {
        byte[] byteK = Ed25519ByteEccUtils.toByteK(k);
        return nativeBaseMul(byteK);
    }

    private native byte[] nativeMul(byte[] p, byte[] k);

    private native byte[] nativeBaseMul(byte[] k);

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.ED25519_SODIUM;
    }

    @Override
    public int pointByteLength() {
        return Ed25519ByteEccUtils.POINT_BYTES;
    }

    @Override
    public int scalarByteLength() {
        return Ed25519ByteEccUtils.SCALAR_BYTES;
    }

    /**
     * singleton mode
     */
    private static final Ed25519SodiumByteFullEcc INSTANCE = new Ed25519SodiumByteFullEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static Ed25519SodiumByteFullEcc getInstance() {
        return INSTANCE;
    }
}
