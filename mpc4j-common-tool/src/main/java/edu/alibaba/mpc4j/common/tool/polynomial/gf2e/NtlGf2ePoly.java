package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory.Gf2ePolyType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * NTL实现的GF2E多项式插值抽象类。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
public class NtlGf2ePoly extends AbstractGf2ePoly {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * type
     */
    private static final Gf2ePolyType TYPE = Gf2ePolyType.NTL;
    /**
     * 不可约多项式
     */
    private final byte[] minBytes;

    NtlGf2ePoly(int l) {
        super(l);
        // 设置不可约多项式，系数个数为l + 1
        int minNum = l + 1;
        int minByteNum = CommonUtils.getByteLength(minNum);
        int minRoundBytes = minByteNum * Byte.SIZE;
        minBytes = new byte[minByteNum];
        UnivariatePolynomialZp64 minimalPolynomial = finiteField.getMinimalPolynomial();
        for (int i = 0; i <= minimalPolynomial.degree(); i++) {
            boolean coefficient = minimalPolynomial.get(i) != 0L;
            BinaryUtils.setBoolean(minBytes, minRoundBytes - 1 - i, coefficient);
        }
    }

    @Override
    public Gf2ePolyType getType() {
        return TYPE;
    }

    @Override
    public int coefficientNum(int num) {
        return Gf2ePolyFactory.getCoefficientNum(TYPE, num);
    }

    @Override
    public byte[][] interpolate(int num, byte[][] xArray, byte[][] yArray) {
        assert xArray.length == yArray.length;
        assert num >= 1 && xArray.length <= num;
        for (byte[] x : xArray) {
            assert validPoint(x);
        }
        for (byte[] y : yArray) {
            assert validPoint(y);
        }
        // 调用本地函数完成插值
        return NtlNativeGf2ePoly.interpolate(minBytes, byteL, num, xArray, yArray);
    }

    @Override
    public int rootCoefficientNum(int num) {
        assert num >= 1 : "# of points must be greater than or equal to 1: " + num;
        return num + 1;
    }

    @Override
    public byte[][] rootInterpolate(int num, byte[][] xArray, byte[] y) {
        assert num >= 1 && xArray.length <= num;
        if (xArray.length == 0) {
            // 返回随机多项式
            byte[][] coefficients = new byte[num + 1][byteL];
            for (int index = 0; index < num; index++) {
                coefficients[index] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
            // 将最高位设置为1
            coefficients[num][byteL - 1] = (byte) 0x01;
            return coefficients;
        }
        // 如果有插值数据，则调用本地函数完成插值
        for (byte[] x : xArray) {
            assert validPoint(x);
        }
        return NtlNativeGf2ePoly.rootInterpolate(minBytes, byteL, num, xArray, y);
    }

    @Override
    public byte[] evaluate(byte[][] coefficients, byte[] x) {
        assert coefficients.length >= 1;
        for (byte[] coefficient : coefficients) {
            assert validPoint(coefficient);
        }
        assert validPoint(x);
        return NtlNativeGf2ePoly.singleEvaluate(minBytes, byteL, coefficients, x);
    }

    @Override
    public byte[][] evaluate(byte[][] coefficients, byte[][] xArray) {
        assert coefficients.length >= 1;
        for (byte[] coefficient : coefficients) {
            assert validPoint(coefficient);
        }
        for (byte[] x : xArray) {
            assert validPoint(x);
        }
        return NtlNativeGf2ePoly.evaluate(minBytes, byteL, coefficients, xArray);
    }
}
