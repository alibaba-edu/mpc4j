package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import java.math.BigInteger;

/**
 * Zp二叉树多项式插值接口。
 *
 * @author Weiran Liu
 * @date 2022/11/5
 */
public interface ZpTreePoly {
    /**
     * 返回二叉树多项式插值类型。
     *
     * @return 二叉树多项式插值类型。
     */
    ZpPolyFactory.ZpTreePolyType getType();

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
     * @param pointNum 插值点数量。
     * @return 多项式系数数量。
     */
    default int coefficientNum(int pointNum) {
        assert pointNum > 0 : "point num must be greater than 0: " + pointNum;
        return pointNum + 1;
    }

    /**
     * 准备插值二叉树。
     *
     * @param xArray x数组。
     */
    void prepareInterpolateBinaryTree(BigInteger[] xArray);

    /**
     * 销毁插值二叉树。
     */
    void destroyInterpolateBinaryTree();

    /**
     * 插值。
     *
     * @param yArray y数组。
     * @return 插值多项式系数。
     */
    BigInteger[] interpolate(BigInteger[] yArray);

    /**
     * 准备求值二叉树。
     *
     * @param evaluatePolynomialPointNum 求值多项式插值点数量。
     * @param xArray                     x数组。
     */
    void prepareEvaluateBinaryTrees(int evaluatePolynomialPointNum, BigInteger[] xArray);

    /**
     * 销毁求值二叉树。
     */
    void destroyEvaluateBinaryTree();

    /**
     * 求值。
     *
     * @param coefficients 插值多项式系数。
     * @return y数组。
     */
    BigInteger[] evaluate(BigInteger[] coefficients);
}
