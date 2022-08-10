package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

/**
 * NTL的GF2E有限域多项式插值本地函数。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
class NtlNativeGf2ePoly {

    private NtlNativeGf2ePoly() {
        // empty
    }

    /**
     * NTL底层库插值。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL    l字节长度。
     * @param num      插值点数量。
     * @param xArray   x数组。
     * @param yArray   y数组。
     * @return 插值多项式的系数。
     */
    static native byte[][] interpolate(byte[] minBytes, int byteL, int num, byte[][] xArray, byte[][] yArray);

    /**
     * NTL底层库插值。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL    l字节长度。
     * @param num      插值点数量。
     * @param xArray   x数组。
     * @param y        y的值。
     * @return 插值多项式的系数。
     */
    static native byte[][] rootInterpolate(byte[] minBytes, int byteL, int num, byte[][] xArray, byte[] y);

    /**
     * 多项式求值。
     *
     * @param minBytes   最小多项式系数。
     * @param byteL      l字节长度。
     * @param polynomial 插值多项式。
     * @param x          x的值。
     * @return f(x)。
     */
    static native byte[] singleEvaluate(byte[] minBytes, int byteL, byte[][] polynomial, byte[] x);

    /**
     * 多项式求值。
     *
     * @param minBytes   最小多项式系数。
     * @param byteL      l字节长度。
     * @param polynomial 插值多项式。
     * @param xArray     x数组。
     * @return f(x)数组。
     */
    static native byte[][] evaluate(byte[] minBytes, int byteL, byte[][] polynomial, byte[][] xArray);
}
