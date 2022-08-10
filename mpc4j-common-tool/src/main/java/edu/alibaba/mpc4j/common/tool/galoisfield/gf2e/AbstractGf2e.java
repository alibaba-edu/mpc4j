package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * GF(2^e)抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/7
 */
abstract class AbstractGf2e implements Gf2e {
    /**
     * l比特长度
     */
    protected final int l;
    /**
     * l字节长度
     */
    protected final int byteL;
    /**
     * 有限域
     */
    protected final FiniteField<UnivariatePolynomialZp64> finiteField;
    /**
     * 0元
     */
    protected final byte[] zero;
    /**
     * 1元
     */
    protected final byte[] one;

    AbstractGf2e(int l) {
        assert l > 0;
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        finiteField = Gf2eManager.getFiniteField(l);
        zero = createZero();
        one = createOne();
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    @Override
    public byte[] createZero() {
        return new byte[byteL];
    }

    @Override
    public byte[] createOne() {
        byte[] one = new byte[byteL];
        one[one.length - 1] = (byte) 0x01;
        return one;
    }

    @Override
    public byte[] createRandom(SecureRandom secureRandom) {
        return BytesUtils.randomByteArray(l, byteL, secureRandom);
    }

    @Override
    public byte[] createNonZeroRandom(SecureRandom secureRandom) {
        byte[] random = new byte[byteL];
        while (Arrays.equals(zero, random)) {
            random = BytesUtils.randomByteArray(l, byteL, secureRandom);
        }
        return random;
    }

    @Override
    public boolean isZero(byte[] a) {
        assert validateElement(a);
        return Arrays.equals(a, zero);
    }

    @Override
    public boolean isOne(byte[] a) {
        assert validateElement(a);
        return Arrays.equals(a, one);
    }

    @Override
    public boolean validateElement(byte[] a) {
        return a.length == byteL && BytesUtils.isReduceByteArray(a, l);
    }

    @Override
    public boolean validateNonZeroElement(byte[] a) {
        return a.length == byteL && BytesUtils.isReduceByteArray(a, l) && !Arrays.equals(a, zero);
    }
}
