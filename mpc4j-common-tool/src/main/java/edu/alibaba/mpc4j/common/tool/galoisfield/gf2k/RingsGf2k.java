package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eManager;
import edu.alibaba.mpc4j.common.tool.utils.RingsUtils;

/**
 * Rings GF(2^κ).
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
class RingsGf2k extends AbstractGf2k {
    /**
     * GF(2^κ) default finite field
     */
    private static final FiniteField<UnivariatePolynomialZp64> GF2K = Gf2eManager.getFiniteField(128);

    RingsGf2k(EnvType envType) {
        super(envType);
    }

    @Override
    public Gf2kFactory.Gf2kType getGf2kType() {
        return Gf2kFactory.Gf2kType.RINGS;
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        UnivariatePolynomialZp64 x1Polynomial = RingsUtils.byteArrayToGf2e(p);
        UnivariatePolynomialZp64 x2Polynomial = RingsUtils.byteArrayToGf2e(q);
        UnivariatePolynomialZp64 yPolynomial = GF2K.multiply(x1Polynomial, x2Polynomial);

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
        UnivariatePolynomialZp64 rPolynomial = GF2K.divideExact(pPolynomial, qPolynomial);

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
        UnivariatePolynomialZp64 yPolynomial = GF2K.divideExact(GF2K.getOne(), xPolynomial);

        return RingsUtils.gf2eToByteArray(yPolynomial, BYTE_L);
    }

    @Override
    public void invi(byte[] p) {
        byte[] r = inv(p);
        System.arraycopy(r, 0, p, 0, BYTE_L);
    }
}
