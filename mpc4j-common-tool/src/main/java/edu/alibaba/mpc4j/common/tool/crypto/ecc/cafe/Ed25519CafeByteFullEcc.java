package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

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
 * Cafe实现的Ed25519全功能字节椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/11/6
 */
public class Ed25519CafeByteFullEcc implements ByteFullEcc {
    /**
     * 哈希函数
     */
    private final Hash hash;

    private Ed25519CafeByteFullEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, Ed25519ByteEccUtils.POINT_BYTES);
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
    public byte[] add(byte[] p, byte[] q) {
        CafeEdwardsPoint pFieldElement = new CafeEdwardsCompressedPoint(p).decompress();
        CafeEdwardsPoint qFieldElement = new CafeEdwardsCompressedPoint(q).decompress();
        return pFieldElement.add(qFieldElement).compress().encode();
    }

    @Override
    public void addi(byte[] p, byte[] q) {
        CafeEdwardsPoint pFieldElement = new CafeEdwardsCompressedPoint(p).decompress();
        CafeEdwardsPoint qFieldElement = new CafeEdwardsCompressedPoint(q).decompress();
        byte[] r = pFieldElement.add(qFieldElement).compress().encode();
        System.arraycopy(r, 0, p, 0, p.length);
    }

    @Override
    public byte[] neg(byte[] p) {
        CafeEdwardsPoint pFieldElement = new CafeEdwardsCompressedPoint(p).decompress();
        return pFieldElement.neg().compress().encode();
    }

    @Override
    public void negi(byte[] p) {
        CafeEdwardsPoint pFieldElement = new CafeEdwardsCompressedPoint(p).decompress();
        byte[] r = pFieldElement.neg().compress().encode();
        System.arraycopy(r, 0, p, 0, p.length);
    }

    @Override
    public byte[] sub(byte[] p, byte[] q) {
        CafeEdwardsPoint pFieldElement = new CafeEdwardsCompressedPoint(p).decompress();
        CafeEdwardsPoint qFieldElement = new CafeEdwardsCompressedPoint(q).decompress();
        return pFieldElement.sub(qFieldElement).compress().encode();
    }

    @Override
    public void subi(byte[] p, byte[] q) {
        CafeEdwardsPoint pFieldElement = new CafeEdwardsCompressedPoint(p).decompress();
        CafeEdwardsPoint qFieldElement = new CafeEdwardsCompressedPoint(q).decompress();
        byte[] r = pFieldElement.sub(qFieldElement).compress().encode();
        System.arraycopy(r, 0, p, 0, p.length);
    }

    @Override
    public byte[] mul(byte[] p, BigInteger k) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES;
        byte[] byteK = Ed25519ByteEccUtils.toByteK(k);
        CafeScalar cafeScalarK = new CafeScalar(byteK);
        CafeEdwardsPoint pFieldElement = new CafeEdwardsCompressedPoint(p).decompress();
        return pFieldElement.mul(cafeScalarK).compress().encode();
    }

    @Override
    public byte[] baseMul(BigInteger k) {
        byte[] byteK = Ed25519ByteEccUtils.toByteK(k);
        CafeScalar cafeScalarK = new CafeScalar(byteK);
        return CafeConstants.ED25519_BASE_POINT_TABLE.mul(cafeScalarK).compress().encode();
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
        return CafeConstants.ED25519_BASE_POINT.compress().encode();
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES;
        CafeScalar cafeScalarK = new CafeScalar(k);
        CafeEdwardsPoint point = new CafeEdwardsCompressedPoint(p).decompress();
        return point.mul(cafeScalarK).compress().encode();
    }

    @Override
    public byte[] baseMul(byte[] k) {
        CafeScalar cafeScalarK = new CafeScalar(k);
        return CafeConstants.ED25519_BASE_POINT_TABLE.mul(cafeScalarK).compress().encode();
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.ED25519_CAFE;
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
    private static final Ed25519CafeByteFullEcc INSTANCE = new Ed25519CafeByteFullEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static Ed25519CafeByteFullEcc getInstance() {
        return INSTANCE;
    }
}
