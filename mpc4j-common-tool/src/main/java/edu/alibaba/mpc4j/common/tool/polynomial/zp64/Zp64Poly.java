package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

/**
 * Zp64多项式插值接口。
 *
 * @author Weiran Liu
 * @date 2022/8/3
 */
public interface Zp64Poly {
    /**
     * 返回多项式插值类型。
     *
     * @return 多项式插值类型。
     */
    Zp64PolyFactory.Zp64PolyType getType();

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
    long getPrime();

    /**
     * 插值多项式系数数量。
     *
     * @param num 插值点数量。
     * @return 多项式系数数量
     */
    int coefficientNum(int num);

    /**
     * 得到插值多项式f(x)，使得y = f(x)。在插值点中补充随机元素，使插值数量为num。
     *
     * @param num    插值点数量。
     * @param xArray x数组。
     * @param yArray y数组。
     * @return 插值多项式的系数。
     */
    long[] interpolate(int num, long[] xArray, long[] yArray);

    /**
     * 根插值多项式系数数量。
     *
     * @param num 插值点数量。
     * @return 多项式系数数量
     */
    int rootCoefficientNum(int num);

    /**
     * 得到插值多项式f(x)，使得对于所有x，都有y = f(x)，在插值点中补充随机元素，使插值数量为num。
     *
     * @param num    所需插值点数量。
     * @param xArray x数组。
     * @param y      y的值。
     * @return 插值多项式的系数。
     */
    long[] rootInterpolate(int num, long[] xArray, long y);

    /**
     * 计算y = f(x)。
     *
     * @param coefficients 多项式系数。
     * @param x            输入x。
     * @return f(x)。
     */
    long evaluate(long[] coefficients, long x);

    /**
     * 计算y = f(x)。
     *
     * @param coefficients 多项式系数。
     * @param xArray       x数组。
     * @return f(x)数组。
     */
    long[] evaluate(long[] coefficients, long[] xArray);
}
