package edu.alibaba.mpc4j.common.tool.galoisfield.gf64;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eManager;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf64.Gf64Factory.Gf64Type;
import edu.alibaba.mpc4j.common.tool.utils.RingsUtils;

/**
 * @author Weiran Liu
 * @date 2023/8/28
 */
class RingsGf64 extends AbstractGf64 {
    /**
     * GF(2^64) default finite field
     */
    private static final FiniteField<UnivariatePolynomialZp64> GF64 = Gf2eManager.getFiniteField(64);

    RingsGf64(EnvType envType) {
        super(envType);
    }

    @Override
    public Gf64Type getGf64Type() {
        return Gf64Type.RINGS;
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        UnivariatePolynomialZp64 x1Polynomial = RingsUtils.byteArrayToGf2e(p);
        UnivariatePolynomialZp64 x2Polynomial = RingsUtils.byteArrayToGf2e(q);
        UnivariatePolynomialZp64 yPolynomial = GF64.multiply(x1Polynomial, x2Polynomial);

        return RingsUtils.gf2eToByteArray(yPolynomial, BYTE_L);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        byte[] r = mul(p, q);
        System.arraycopy(r, 0, p, 0, BYTE_L);
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateNonZeroElement(q);
        UnivariatePolynomialZp64 pPolynomial = RingsUtils.byteArrayToGf2e(p);
        UnivariatePolynomialZp64 qPolynomial = RingsUtils.byteArrayToGf2e(q);
        UnivariatePolynomialZp64 rPolynomial = GF64.divideExact(pPolynomial, qPolynomial);

        return RingsUtils.gf2eToByteArray(rPolynomial, BYTE_L);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        byte[] r = div(p, q);
        System.arraycopy(r, 0, p, 0, BYTE_L);
    }

    @Override
    public byte[] inv(byte[] p) {
        assert validateNonZeroElement(p);
        UnivariatePolynomialZp64 xPolynomial = RingsUtils.byteArrayToGf2e(p);
        UnivariatePolynomialZp64 yPolynomial = GF64.divideExact(GF64.getOne(), xPolynomial);

        return RingsUtils.gf2eToByteArray(yPolynomial, BYTE_L);
    }

    @Override
    public void invi(byte[] p) {
        byte[] r = inv(p);
        System.arraycopy(r, 0, p, 0, BYTE_L);
    }
}
