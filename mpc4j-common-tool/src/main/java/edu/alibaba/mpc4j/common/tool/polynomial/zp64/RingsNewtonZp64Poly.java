package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

import cc.redberry.rings.poly.univar.UnivariateInterpolation;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;

/**
 * 应用Rings实现的Zp64牛顿多项式插值。
 *
 * @author Weiran Liu
 * @date 2022/8/4
 */
class RingsNewtonZp64Poly extends AbstractRingsZp64Poly {

    RingsNewtonZp64Poly(int l) {
        super(l);
    }

    RingsNewtonZp64Poly(long p) {
        super(p);
    }

    @Override
    public Zp64PolyFactory.Zp64PolyType getType() {
        return Zp64PolyFactory.Zp64PolyType.RINGS_NEWTON;
    }

    @Override
    protected UnivariatePolynomialZp64 polynomialInterpolate(int num, long[] xArray, long[] yArray) {
        return UnivariateInterpolation.interpolateNewton(p, xArray, yArray);
    }
}
