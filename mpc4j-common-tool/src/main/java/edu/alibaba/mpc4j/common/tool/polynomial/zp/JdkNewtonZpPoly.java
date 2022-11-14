package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import cc.redberry.rings.bigint.BigInteger;
import cc.redberry.rings.poly.univar.UnivariateInterpolation;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;

import java.util.Arrays;

/**
 * 用JDK实现的Zp牛顿多项式插值。
 *
 * @author Weiran Liu
 * @date 2022/8/9
 */
class JdkNewtonZpPoly extends AbstractRingsZpPoly {

    JdkNewtonZpPoly(int l) {
        super(l);
    }

    @Override
    public ZpPolyFactory.ZpPolyType getType() {
        return ZpPolyFactory.ZpPolyType.JDK_NEWTON;
    }

    @Override
    protected UnivariatePolynomial<BigInteger> polynomialInterpolate(java.math.BigInteger[] xArray, java.math.BigInteger[] yArray) {
        // 转换成多项式点
        BigInteger[] points = Arrays.stream(xArray)
            .map(BigInteger::new)
            .toArray(BigInteger[]::new);
        BigInteger[] values = Arrays.stream(yArray)
            .map(BigInteger::new)
            .toArray(BigInteger[]::new);
        // 插值
        return UnivariateInterpolation.interpolateNewton(finiteField, points, values);
    }
}
