package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Rings Subfield GF2K for l ∈ {2, 4, 8, 16, 32, 64}.
 *
 * @author Weiran Liu
 * @date 2024/6/2
 */
public class RingsSubSgf2k extends AbstractSubSgf2k {

    public RingsSubSgf2k(EnvType envType, int subfieldL) {
        super(envType, subfieldL);
    }

    @Override
    public synchronized byte[] mul(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        UnivariatePolynomial<UnivariatePolynomialZp64> ringsP = createRingsFieldElement(p);
        UnivariatePolynomial<UnivariatePolynomialZp64> ringsQ = createRingsFieldElement(q);
        UnivariatePolynomial<UnivariatePolynomialZp64> ringsR = fieldFiniteField.multiply(ringsP, ringsQ);
        return createFieldElement(ringsR);
    }

    @Override
    public synchronized void muli(byte[] p, byte[] q) {
        byte[] result = mul(p, q);
        System.arraycopy(result, 0, p, 0, fieldByteL);
    }

    @Override
    public synchronized byte[] inv(byte[] p) {
        assert validateNonZeroElement(p);
        UnivariatePolynomial<UnivariatePolynomialZp64> ringsP = createRingsFieldElement(p);
        UnivariatePolynomial<UnivariatePolynomialZp64> ringsR = fieldFiniteField.divideExact(fieldFiniteField.getOne(), ringsP);
        return createFieldElement(ringsR);
    }

    @Override
    public synchronized void invi(byte[] p) {
        byte[] result = inv(p);
        System.arraycopy(result, 0, p, 0, fieldByteL);
    }

    @Override
    public synchronized byte[] div(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateNonZeroElement(p);
        UnivariatePolynomial<UnivariatePolynomialZp64> ringsP = createRingsFieldElement(p);
        UnivariatePolynomial<UnivariatePolynomialZp64> ringsQ = createRingsFieldElement(q);
        UnivariatePolynomial<UnivariatePolynomialZp64> ringsR = fieldFiniteField.divideExact(ringsP, ringsQ);
        return createFieldElement(ringsR);
    }

    @Override
    public synchronized void divi(byte[] p, byte[] q) {
        byte[] result = div(p, q);
        System.arraycopy(result, 0, p, 0, fieldByteL);
    }
}
