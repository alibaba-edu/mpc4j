package edu.alibaba.mpc4j.common.tool.crypto.ecc.fourq;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.FourqByteEccUtils;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * FourQ fully-functional byte ecc.
 *
 * @author Qixian Zhou
 * @date 2023/4/6
 */
public class FourqByteFullEcc implements ByteFullEcc {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * the hash function
     */
    private final Hash hash;

    private FourqByteFullEcc() {
        hash = HashFactory.createInstance(HashFactory.HashType.JDK_SHA256, FourqByteEccUtils.POINT_BYTES);
    }

    @Override
    public BigInteger getN() {
        return FourqByteEccUtils.N;
    }

    @Override
    public BigInteger randomZn(SecureRandom secureRandom) {
        return BigIntegerUtils.randomPositive(FourqByteEccUtils.N, secureRandom);
    }

    @Override
    public byte[] add(byte[] p, byte[] q) {
        assert p.length == FourqByteEccUtils.POINT_BYTES && q.length == FourqByteEccUtils.POINT_BYTES;
        assert isValidPoint(p) && isValidPoint(q);

        return nativeAdd(p, q);
    }

    @Override
    public void addi(byte[] p, byte[] q) {
        assert p.length == FourqByteEccUtils.POINT_BYTES && q.length == FourqByteEccUtils.POINT_BYTES;
        assert isValidPoint(p) && isValidPoint(q);

        byte[] r = nativeAdd(p, q);
        // reset p
        System.arraycopy(r, 0, p, 0, FourqByteEccUtils.POINT_BYTES);
    }

    @Override
    public byte[] neg(byte[] p) {
        assert p.length == FourqByteEccUtils.POINT_BYTES;
        assert isValidPoint(p);

        return nativeNeg(p);

    }

    @Override
    public void negi(byte[] p) {
        assert p.length == FourqByteEccUtils.POINT_BYTES;
        assert isValidPoint(p);

        byte[] r = nativeNeg(p);
        // reset p
        System.arraycopy(r, 0, p, 0, FourqByteEccUtils.POINT_BYTES);
    }

    @Override
    public byte[] sub(byte[] p, byte[] q) {
        assert p.length == FourqByteEccUtils.POINT_BYTES && q.length == FourqByteEccUtils.POINT_BYTES;
        assert isValidPoint(p) && isValidPoint(q);
        // p + (-q)
        byte[] negQ = nativeNeg(q);
        return nativeAdd(p, negQ);
    }

    @Override
    public void subi(byte[] p, byte[] q) {
        assert p.length == FourqByteEccUtils.POINT_BYTES && q.length == FourqByteEccUtils.POINT_BYTES;
        assert isValidPoint(p) && isValidPoint(q);
        // p + (-q)
        byte[] negQ = nativeNeg(q);

        byte[] r = nativeAdd(p, negQ);
        // reset p
        System.arraycopy(r, 0, p, 0, FourqByteEccUtils.POINT_BYTES);
    }


    @Override
    public byte[] mul(byte[] p, BigInteger k) {
        assert p.length == FourqByteEccUtils.POINT_BYTES;
        assert isValidPoint(p);

        byte[] byteK = FourqByteEccUtils.toByteK(k);
        return nativeMul(p, byteK);
    }

    @Override
    public byte[] baseMul(BigInteger k) {
        byte[] byteK = FourqByteEccUtils.toByteK(k);
        return nativeBaseMul(byteK);
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {
        BigInteger zn = randomZn(secureRandom);
        byte[] k = BigIntegerUtils.nonNegBigIntegerToByteArray(zn, FourqByteEccUtils.SCALAR_BYTES);
        BytesUtils.innerReverseByteArray(k);
        return k;
    }

    @Override
    public boolean isValidPoint(byte[] p) {
        return nativeIsValidPoint(p);
    }

    @Override
    public byte[] getInfinity() {
        return BytesUtils.clone(FourqByteEccUtils.POINT_INFINITY);
    }

    @Override
    public byte[] getG() {
        return BytesUtils.clone(FourqByteEccUtils.POINT_B);
    }

    @Override
    public byte[] randomPoint(SecureRandom secureRandom) {
        // Directly generating random byte array may fail the test. We generate a random scalar and do multiplication.
        byte[] r = randomScalar(secureRandom);
        return nativeBaseMul(r);
    }

    @Override
    public byte[] hashToCurve(byte[] message) {
        assert message.length > 0;
        // hash
        byte[] hashed = hash.digestToBytes(message);
        return nativeHashToCurve(hashed);
    }

    @Override
    public byte[] mul(byte[] p, byte[] k) {
        assert p.length == FourqByteEccUtils.POINT_BYTES;
        assert isValidPoint(p);
        assert k.length == FourqByteEccUtils.SCALAR_BYTES;

        return nativeMul(p, k);
    }

    @Override
    public byte[] baseMul(byte[] k) {
        assert k.length == FourqByteEccUtils.SCALAR_BYTES;
        return nativeBaseMul(k);
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.FOUR_Q;
    }

    @Override
    public int pointByteLength() {
        return FourqByteEccUtils.POINT_BYTES;
    }

    @Override
    public int scalarByteLength() {
        return FourqByteEccUtils.SCALAR_BYTES;
    }

    private native byte[] nativeMul(byte[] p, byte[] k);

    private native byte[] nativeBaseMul(byte[] k);

    private native boolean nativeIsValidPoint(byte[] p);

    private native byte[] nativeNeg(byte[] p);

    private native byte[] nativeAdd(byte[] p, byte[] q);

    private native byte[] nativeHashToCurve(byte[] message);

    /**
     * singleton mode
     */
    private static final FourqByteFullEcc INSTANCE = new FourqByteFullEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static FourqByteFullEcc getInstance() {
        return INSTANCE;
    }
}
