package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.RingsUtils;

/**
 * 用Rings实现的GF(2^l)运算。
 * <p>
 * 注意，{@code FiniteField<UnivariatePolynomialZp64>}下的运算不是线程安全的，需要增加synchronized关键字，这将严重影响计算效率。
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
class RingsGf2e extends AbstractGf2e {

    RingsGf2e(EnvType envType, int l) {
        super(envType, l);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.RINGS;
    }

    @Override
    public synchronized byte[] mul(byte[] a, byte[] b) {
        assert validateElement(a) && validateElement(b);
        if (l == CommonConstants.BLOCK_BIT_LENGTH) {
            return gf2k.mul(a, b);
        } else {
            // 为与C层的表示保持一致，需要按照小端表示方法将字节数组转换为多项式，计算乘法后再按照小端表示转换回字节数组
            UnivariatePolynomialZp64 aPolynomial = RingsUtils.byteArrayToGf2e(a);
            UnivariatePolynomialZp64 bPolynomial = RingsUtils.byteArrayToGf2e(b);
            UnivariatePolynomialZp64 cPolynomial = finiteField.multiply(aPolynomial, bPolynomial);

            return RingsUtils.gf2eToByteArray(cPolynomial, byteL);
        }
    }

    @Override
    public void muli(byte[] a, byte[] b) {
        byte[] c = mul(a, b);
        System.arraycopy(c, 0, a, 0, byteL);
    }

    @Override
    public synchronized byte[] div(byte[] a, byte[] b) {
        assert validateElement(a) && validateNonZeroElement(b);
        UnivariatePolynomialZp64 aPolynomial = RingsUtils.byteArrayToGf2e(a);
        UnivariatePolynomialZp64 bPolynomial = RingsUtils.byteArrayToGf2e(b);
        // c = a / b = a * (1 / b)
        UnivariatePolynomialZp64 cPolynomial = finiteField.multiply(
            aPolynomial, finiteField.divideExact(finiteField.getOne(), bPolynomial)
        );
        return RingsUtils.gf2eToByteArray(cPolynomial, byteL);
    }

    @Override
    public void divi(byte[] a, byte[] b) {
        byte[] c = div(a, b);
        System.arraycopy(c, 0, a, 0, byteL);
    }


    @Override
    public synchronized byte[] inv(byte[] a) {
        assert validateElement(a);
        UnivariatePolynomialZp64 aPolynomial = RingsUtils.byteArrayToGf2e(a);
        UnivariatePolynomialZp64 cPolynomial = finiteField.divideExact(finiteField.getOne(), aPolynomial);

        return RingsUtils.gf2eToByteArray(cPolynomial, byteL);
    }

    @Override
    public void invi(byte[] a) {
        byte[] c = inv(a);
        System.arraycopy(c, 0, a, 0, byteL);
    }
}
