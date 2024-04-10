package edu.alibaba.mpc4j.common.tool.crypto.ecc.bc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteMulElligatorEcc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.Curve25519FieldUtils;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.Ed25519ByteEccUtils;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.X25519ByteEccUtils;

import java.security.SecureRandom;

/**
 * Bouncy Castle实现的X25519乘法Elligator字节椭圆曲线。
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class X25519BcByteMulElligatorEcc implements ByteMulElligatorEcc {

    private X25519BcByteMulElligatorEcc() {
        // empty
    }

    @Override
    public byte[] randomScalar(SecureRandom secureRandom) {
        return X25519ByteEccUtils.randomClampScalar(secureRandom);
    }

    @Override
    public ByteEccFactory.ByteEccType getByteEccType() {
        return ByteEccFactory.ByteEccType.X25519_ELLIGATOR_BC;
    }

    @Override
    public boolean baseMul(final byte[] k, byte[] point, byte[] uniformPoint) {
        assert point.length == Ed25519ByteEccUtils.POINT_BYTES;
        assert uniformPoint.length == Ed25519ByteEccUtils.POINT_BYTES;

        byte[] baseMulResult = new byte[X25519ByteEccUtils.POINT_BYTES];
        Ed25519ByteEccUtils.scalarBaseMulEncoded(k, baseMulResult);

        return Curve25519FieldUtils.elligatorEncode(baseMulResult, point, uniformPoint);
    }

    @Override
    public byte[] uniformMul(final byte[] uniformPoint, final byte[] k) {
        byte[] point = Curve25519FieldUtils.elligatorDecode(uniformPoint);
        byte[] result = new byte[X25519ByteEccUtils.POINT_BYTES];
        X25519ByteEccUtils.clampScalarMul(k, point, result);
        return result;
    }

    @Override
    public byte[] mul(byte[] point, byte[] k) {
        assert X25519ByteEccUtils.checkClampScalar(k);
        assert X25519ByteEccUtils.checkPoint(point);
        byte[] result = new byte[X25519ByteEccUtils.POINT_BYTES];
        X25519ByteEccUtils.clampScalarMul(k, point, result);
        return result;
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
    private static final X25519BcByteMulElligatorEcc INSTANCE = new X25519BcByteMulElligatorEcc();

    /**
     * Gets the instance.
     *
     * @return the instance.
     */
    public static X25519BcByteMulElligatorEcc getInstance() {
        return INSTANCE;
    }
}
