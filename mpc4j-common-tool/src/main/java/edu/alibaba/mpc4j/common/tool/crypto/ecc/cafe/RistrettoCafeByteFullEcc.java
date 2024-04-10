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
 * Cafe实现的Ristretto全功能字节椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/11/14
 */
public class RistrettoCafeByteFullEcc implements ByteFullEcc {
    /**
     * hash used in hash_to_curve
     */
    private final Hash hash;

    private RistrettoCafeByteFullEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.BC_SHA3_512, Ed25519ByteEccUtils.POINT_BYTES * 2);
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
        byte[] p = new byte[Ed25519ByteEccUtils.POINT_BYTES * 2];
        secureRandom.nextBytes(p);
        while (true) {
            try {
                return CafeRistrettoPoint.fromUniformBytes(p).compress().encode();
            } catch (IllegalArgumentException e) {
                secureRandom.nextBytes(p);
            }
        }
    }

    @Override
    public byte[] hashToCurve(byte[] message) {
        // 简单的重复哈希
        byte[] p = hash.digestToBytes(message);
        while (true) {
            try {
                return CafeRistrettoPoint.fromUniformBytes(p).compress().encode();
            } catch (IllegalArgumentException e) {
                p = hash.digestToBytes(p);
            }
        }
    }

    @Override
    public byte[] add(byte[] p, byte[] q) {
        CafeRistrettoPoint pointP = new CafeRistrettoCompressedPoint(p).decompress();
        CafeRistrettoPoint pointQ = new CafeRistrettoCompressedPoint(q).decompress();
        return pointP.add(pointQ).compress().encode();
    }

    @Override
    public void addi(byte[] p, byte[] q) {
        CafeRistrettoPoint pointP = new CafeRistrettoCompressedPoint(p).decompress();
        CafeRistrettoPoint pointQ = new CafeRistrettoCompressedPoint(q).decompress();
        byte[] r = pointP.add(pointQ).compress().encode();
        System.arraycopy(r, 0, p, 0, p.length);
    }

    @Override
    public byte[] neg(byte[] p) {
        CafeRistrettoPoint pointP = new CafeRistrettoCompressedPoint(p).decompress();
        return pointP.neg().compress().encode();
    }

    @Override
    public void negi(byte[] p) {
        CafeRistrettoPoint pointP = new CafeRistrettoCompressedPoint(p).decompress();
        byte[] r = pointP.neg().compress().encode();
        System.arraycopy(r, 0, p, 0, p.length);
    }

    @Override
    public byte[] sub(byte[] p, byte[] q) {
        CafeRistrettoPoint pointP = new CafeRistrettoCompressedPoint(p).decompress();
        CafeRistrettoPoint pointQ = new CafeRistrettoCompressedPoint(q).decompress();
        return pointP.sub(pointQ).compress().encode();
    }

    @Override
    public void subi(byte[] p, byte[] q) {
        CafeRistrettoPoint pointP = new CafeRistrettoCompressedPoint(p).decompress();
        CafeRistrettoPoint pointQ = new CafeRistrettoCompressedPoint(q).decompress();
        byte[] r = pointP.sub(pointQ).compress().encode();
        System.arraycopy(r, 0, p, 0, p.length);
    }

    @Override
    public byte[] mul(byte[] p, BigInteger k) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES;
        byte[] byteK = Ed25519ByteEccUtils.toByteK(k);
        CafeScalar cafeScalarK = new CafeScalar(byteK);
        CafeRistrettoPoint pointP = new CafeRistrettoCompressedPoint(p).decompress();
        return pointP.mul(cafeScalarK).compress().encode();
    }

    @Override
    public byte[] baseMul(BigInteger k) {
        byte[] byteK = Ed25519ByteEccUtils.toByteK(k);
        CafeScalar cafeScalarK = new CafeScalar(byteK);
        return CafeConstants.RISTRETTO_GENERATOR_TABLE.mul(cafeScalarK).compress().encode();
    }

    @Override
    public boolean isValidPoint(byte[] p) {
        try {
            new CafeRistrettoCompressedPoint(p).decompress().compress().encode();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public byte[] getInfinity() {
        return CafeRistrettoPoint.IDENTITY.compress().encode();
    }

    @Override
    public byte[] getG() {
        return CafeConstants.RISTRETTO_GENERATOR.compress().encode();
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {
        assert p.length == Ed25519ByteEccUtils.POINT_BYTES;
        CafeScalar cafeScalarK = new CafeScalar(k);
        CafeRistrettoPoint point = new CafeRistrettoCompressedPoint(p).decompress();
        return point.mul(cafeScalarK).compress().encode();
    }

    @Override
    public byte[] baseMul(byte[] k) {
        CafeScalar cafeScalarK = new CafeScalar(k);
        return CafeConstants.RISTRETTO_GENERATOR_TABLE.mul(cafeScalarK).compress().encode();
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.RISTRETTO_CAFE;
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
    private static final RistrettoCafeByteFullEcc INSTANCE = new RistrettoCafeByteFullEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static RistrettoCafeByteFullEcc getInstance() {
        return INSTANCE;
    }
}
