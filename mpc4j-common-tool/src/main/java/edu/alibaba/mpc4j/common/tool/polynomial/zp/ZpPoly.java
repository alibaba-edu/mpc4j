package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import java.math.BigInteger;

/**
 * Zp多项式插值接口。
 *
 * @author Weiran Liu
 * @date 2022/01/05
 */
public interface ZpPoly {
    /**
     * 返回多项式插值类型。
     *
     * @return 多项式插值类型。
     */
    ZpPolyFactory.ZpPolyType getType();

    /**
     * 返回l的比特长度。
     *
     * @return l的比特长度。
     */
    int getL();

    /**
     * 返回质数p。
     *
     * @return 质数p。
     */
    BigInteger getPrime();

    /**
     * 验证点的合法性。
     *
     * @param point 点。
     * @return 点是否合法。
     */
    boolean validPoint(BigInteger point);

    /**
     * 插值多项式系数数量。
     *
     * @param pointNum  插值点数量。
     * @param expectNum 期望总数量。
     * @return 多项式系数数量。
     */
    default int coefficientNum(int pointNum, int expectNum) {
        assert expectNum > 0 : "expect num must be greater than 0: " + expectNum;
        assert pointNum >= 0 && pointNum <= expectNum : "point num must be in range [0, " + expectNum + "]: " + pointNum;
        return expectNum;
    }

    /**
     * 得到插值多项式f(x)，使得y = f(x)。在插值点中补充随机元素，使插值数量为num。
     *
     * @param expectNum 期望总数量。
     * @param xArray    x数组。
     * @param yArray    y数组。
     * @return 插值多项式的系数。
     */
    BigInteger[] interpolate(int expectNum, BigInteger[] xArray, BigInteger[] yArray);

    /**
     * 根插值多项式系数数量。
     *
     * @param pointNum  插值点数量。
     * @param expectNum 期望总数量。
     * @return 多项式系数数量。
     */
    default int rootCoefficientNum(int pointNum, int expectNum) {
        assert expectNum > 0 : "expect num must be greater than 0: " + expectNum;
        assert pointNum >= 0 && pointNum <= expectNum : "point num must be in range [0, " + expectNum + "]: " + pointNum;
        return expectNum + 1;
    }

    /**
     * 得到插值多项式f(x)，使得对于所有x，都有y = f(x)，在插值点中补充随机元素，使插值数量为num。
     *
     * @param expectNum 期望总数量。
     * @param xArray    x数组。
     * @param y         y的值。
     * @return 插值多项式的系数。
     */
    BigInteger[] rootInterpolate(int expectNum, BigInteger[] xArray, BigInteger y);

    /**
     * 计算y = f(x)。
     *
     * @param coefficients 多项式系数。
     * @param x            输入x。
     * @return f(x)。
     */
    BigInteger evaluate(BigInteger[] coefficients, BigInteger x);

    /**
     * 计算y = f(x)。
     *
     * @param coefficients 多项式系数。
     * @param xArray       x数组。
     * @return f(x)数组。
     */
    BigInteger[] evaluate(BigInteger[] coefficients, BigInteger[] xArray);
}
