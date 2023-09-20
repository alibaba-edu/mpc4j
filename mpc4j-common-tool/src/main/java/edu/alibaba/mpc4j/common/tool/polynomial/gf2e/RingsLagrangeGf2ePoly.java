package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

import cc.redberry.rings.poly.univar.UnivariateInterpolation;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;

/**
 * 应用Rings实现的GF2E拉格朗日多项式插值算法。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
class RingsLagrangeGf2ePoly extends AbstractRingsGf2ePoly {

    RingsLagrangeGf2ePoly(int l) {
        super(Gf2ePolyFactory.Gf2ePolyType.RINGS_LAGRANGE, l);
    }

    @Override
    protected UnivariatePolynomial<UnivariatePolynomialZp64> polynomialInterpolate(
        int num, UnivariatePolynomialZp64[] xArray, UnivariatePolynomialZp64[] yArray) {
        return UnivariateInterpolation.interpolateLagrange(finiteField, xArray, yArray);
    }
}
