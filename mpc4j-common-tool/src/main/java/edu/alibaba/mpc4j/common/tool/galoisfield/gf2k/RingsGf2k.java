package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eManager;
import edu.alibaba.mpc4j.common.tool.utils.RingsUtils;

/**
 * 用Rings实现的GF(2^128)运算。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
class RingsGf2k implements Gf2k {
    /**
     * GF(2^128)默认有限域
     */
    private static final FiniteField<UnivariatePolynomialZp64> GF2K = Gf2eManager.getFiniteField(128);

    RingsGf2k() {
        // empty
    }

    @Override
    public Gf2kFactory.Gf2kType getGf2kType() {
        return Gf2kFactory.Gf2kType.RINGS;
    }

    @Override
    public byte[] mul(byte[] a, byte[] b) {
        assert a.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert b.length == CommonConstants.BLOCK_BYTE_LENGTH;
        // 为与GCM的表示保持一致，需要按照小端表示方法将字节数组转换为多项式，计算乘法后再按照小端表示转换回字节数组
        UnivariatePolynomialZp64 x1Polynomial = RingsUtils.byteArrayToGf2e(a);
        UnivariatePolynomialZp64 x2Polynomial = RingsUtils.byteArrayToGf2e(b);
        UnivariatePolynomialZp64 yPolynomial = GF2K.multiply(x1Polynomial, x2Polynomial);

        return RingsUtils.gf2eToByteArray(yPolynomial, CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Override
    public void muli(byte[] a, byte[] b) {
        byte[] c = mul(a, b);
        System.arraycopy(c, 0, a, 0, CommonConstants.BLOCK_BYTE_LENGTH);
    }
}
