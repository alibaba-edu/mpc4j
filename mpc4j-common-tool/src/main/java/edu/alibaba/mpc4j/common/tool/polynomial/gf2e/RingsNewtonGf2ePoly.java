package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

import cc.redberry.rings.poly.univar.UnivariateInterpolation;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory.Gf2ePolyType;

/**
 * 应用Rings实现的GF2E牛顿多项式插值算法。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
class RingsNewtonGf2ePoly extends AbstractRingsGf2ePoly {

    RingsNewtonGf2ePoly(int l) {
        super(Gf2ePolyType.RINGS_NEWTON, l);
    }

    @Override
    protected UnivariatePolynomial<UnivariatePolynomialZp64> polynomialInterpolate(
        int num, UnivariatePolynomialZp64[] xArray, UnivariatePolynomialZp64[] yArray) {
        return UnivariateInterpolation.interpolateNewton(finiteField, xArray, yArray);
    }
}
