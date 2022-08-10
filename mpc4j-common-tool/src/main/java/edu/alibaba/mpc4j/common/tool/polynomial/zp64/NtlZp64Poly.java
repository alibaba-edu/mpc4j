package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory.Zp64PolyType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.security.SecureRandom;

/**
 * NTL的Zp64有限域多项式插值本地函数。
 *
 * @author Weiran Liu
 * @date 2022/8/5
 */
class NtlZp64Poly extends AbstractZp64Poly {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 随机状态
     */
    private final SecureRandom secureRandom;

    public NtlZp64Poly(int l) {
        super(l);
        secureRandom = new SecureRandom();
    }

    public NtlZp64Poly(long p) {
        super(p);
        secureRandom = new SecureRandom();
    }

    @Override
    public Zp64PolyType getType() {
        return Zp64PolyType.NTL;
    }

    @Override
    public int coefficientNum(int num) {
        assert num >= 1 : "# of points must be greater than or equal to 1: " + num;
        return num;
    }

    @Override
    public long[] interpolate(int num, long[] xArray, long[] yArray) {
        assert xArray.length == yArray.length;
        // 不要求至少有1个插值点，只要求总数量大于1
        assert num >= 1 && xArray.length <= num;
        for (long x : xArray) {
            assert validPoint(x);
        }
        for (long y : yArray) {
            assert validPoint(y);
        }
        // 调用本地函数完成插值
        return nativeInterpolate(p, num, xArray, yArray);
    }

    @Override
    public int rootCoefficientNum(int num) {
        assert num >= 1 : "# of points must be greater than or equal to 1: " + num;
        return num + 1;
    }

    @Override
    public long[] rootInterpolate(int num, long[] xArray, long y) {
        // 不要求至少有1个插值点，只要求总数量大于1
        assert num >= 1 && xArray.length <= num;
        if (xArray.length == 0) {
            // 返回随机多项式
            long[] coefficients = new long[num + 1];
            for (int index = 0; index < num; index++) {
                coefficients[index] = LongUtils.randomPositive(p, secureRandom);
            }
            // 将最高位设置为1
            coefficients[num] = 1L;
            return coefficients;
        }
        // 如果有插值数据，则继续插值
        for (long x : xArray) {
            assert validPoint(x);
        }
        assert validPoint(y);
        // 调用本地函数完成插值
        return nativeRootInterpolate(p, num, xArray, y);
    }

    /**
     * NTL底层库的虚拟点插值。
     *
     * @param prime  质数。
     * @param num    插值点数量。
     * @param xArray x_i数组。
     * @param y      y。
     * @return 插值多项式的系数。
     */
    private static native long[] nativeRootInterpolate(long prime, int num, long[] xArray, long y);

    /**
     * NTL底层库的虚拟点插值。
     *
     * @param prime  质数。
     * @param num    插值点数量。
     * @param xArray x_i数组。
     * @param yArray y_i数组。
     * @return 插值多项式的系数。
     */
    private static native long[] nativeInterpolate(long prime, int num, long[] xArray, long[] yArray);

    @Override
    public long evaluate(long[] coefficients, long x) {
        // 至少包含1个系数，每个系数都属于Zp
        assert coefficients.length >= 1;
        for (long coefficient : coefficients) {
            assert validPoint(coefficient);
        }
        // 验证x的有效性
        assert validPoint(x);
        // 调用本地函数完成求值
        return nativeSingleEvaluate(p, coefficients, x);
    }

    /**
     * 多项式求值。
     *
     * @param prime        质数。
     * @param coefficients 插值多项式系数。
     * @param x            输入x。
     * @return f(x)。
     */
    private static native long nativeSingleEvaluate(long prime, long[] coefficients, long x);

    @Override
    public long[] evaluate(long[] coefficients, long[] xArray) {
        assert coefficients.length >= 1;
        for (long coefficient : coefficients) {
            assert validPoint(coefficient);
        }
        // 验证xArray的有效性
        for (long x : xArray) {
            assert validPoint(x);
        }
        // 调用本地函数完成求值
        return nativeEvaluate(p, coefficients, xArray);
    }

    /**
     * 多项式求值。
     *
     * @param prime        质数。
     * @param coefficients 插值多项式。
     * @param xArray       x_i数组。
     * @return f(x_i)数组。
     */
    private static native long[] nativeEvaluate(long prime, long[] coefficients, long[] xArray);
}
