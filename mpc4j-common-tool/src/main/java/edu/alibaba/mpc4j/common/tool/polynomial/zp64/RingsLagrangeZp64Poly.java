package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

import cc.redberry.rings.poly.univar.UnivariateInterpolation;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;

/**
 * 应用Rings实现的Zp64拉格朗日多项式插值。
 *
 * @author Weiran Liu
 * @date 2022/8/4
 */
class RingsLagrangeZp64Poly extends AbstractRingsZp64Poly {

    RingsLagrangeZp64Poly(int l) {
        super(l);
    }

    RingsLagrangeZp64Poly(long p) {
        super(p);
    }

    @Override
    public Zp64PolyFactory.Zp64PolyType getType() {
        return Zp64PolyFactory.Zp64PolyType.RINGS_LAGRANGE;
    }

    @Override
    protected UnivariatePolynomialZp64 polynomialInterpolate(int num, long[] xArray, long[] yArray) {
        return UnivariateInterpolation.interpolateLagrange(p, xArray, yArray);
    }
}
