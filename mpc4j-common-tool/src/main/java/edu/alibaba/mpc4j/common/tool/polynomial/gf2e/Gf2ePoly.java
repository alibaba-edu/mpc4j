package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory.Gf2ePolyType;

/**
 * GF2E多项式插值接口。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
public interface Gf2ePoly {
    /**
     * 返回多项式插值类型。
     *
     * @return 多项式插值类型。
     */
    Gf2ePolyType getType();

    /**
     * 返l对应的字节长度。
     *
     * @return l对应的字节长度。
     */
    int getByteL();

    /**
     * 返回l的比特长度。
     *
     * @return l的比特长度。
     */
    int getL();

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
     * @param num    所需插值点数量。
     * @param xArray x_i数组。
     * @param yArray y_i数组。
     * @return 插值多项式的系数。
     */
    byte[][] interpolate(int num, byte[][] xArray, byte[][] yArray);

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
    byte[][] rootInterpolate(int num, byte[][] xArray, byte[] y);

    /**
     * 计算y = f(x)。
     *
     * @param coefficients 多项式系数。
     * @param x            输入x。
     * @return f(x)。
     */
    byte[] evaluate(byte[][] coefficients, byte[] x);

    /**
     * 计算y = f(x)。
     *
     * @param coefficients 多项式系数。
     * @param xArray       x数组。
     * @return f(x)数组。
     */
    byte[][] evaluate(byte[][] coefficients, byte[][] xArray);
}
