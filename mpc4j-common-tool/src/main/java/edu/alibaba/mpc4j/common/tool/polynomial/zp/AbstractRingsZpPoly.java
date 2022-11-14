package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import cc.redberry.rings.Ring;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 应用Rings的Zp多项式差值抽象类。
 *
 * @author Weiran Liu
 * @date 2021/05/31
 */
abstract class AbstractRingsZpPoly extends AbstractZpPoly {
    /**
     * Zp有限域
     */
    protected final Ring<cc.redberry.rings.bigint.BigInteger> finiteField;

    AbstractRingsZpPoly(int l) {
        super(l);
        finiteField = ZpManager.getFiniteField(l);
    }

    @Override
    public BigInteger[] interpolate(int expectNum, BigInteger[] xArray, BigInteger[] yArray) {
        assert xArray.length == yArray.length
            : "x.length must be equal to y.length, x.length: " + xArray.length + ", y.length: " + yArray.length;
        assert expectNum >= 1 && xArray.length <= expectNum : "x.length must be in range [1, " + expectNum + "]: " + xArray.length;
        for (BigInteger x : xArray) {
            assert validPoint(x);
        }
        for (BigInteger y : yArray) {
            assert validPoint(y);
        }
        // 插值
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> interpolatePolynomial = polynomialInterpolate(xArray, yArray);
        // 如果插值点数量小于最大点数量，则补充虚拟点
        if (xArray.length < expectNum) {
            // 转换成多项式点
            cc.redberry.rings.bigint.BigInteger[] pointXs = Arrays.stream(xArray)
                .map(cc.redberry.rings.bigint.BigInteger::new)
                .toArray(cc.redberry.rings.bigint.BigInteger[]::new);
            // 计算(x - x_1) * ... * (x - x_m')
            UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> p1 = UnivariatePolynomial.one(finiteField);
            for (cc.redberry.rings.bigint.BigInteger pointX : pointXs) {
                p1 = p1.multiply(
                    UnivariatePolynomial.zero(finiteField).createLinear(finiteField.negate(pointX), finiteField.getOne())
                );
            }
            // 构造随机多项式
            cc.redberry.rings.bigint.BigInteger[] prCoefficients
                = new cc.redberry.rings.bigint.BigInteger[expectNum - pointXs.length];
            for (int index = 0; index < prCoefficients.length; index++) {
                prCoefficients[index] = new cc.redberry.rings.bigint.BigInteger(BigIntegerUtils.randomNonNegative(p, secureRandom));
            }
            UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> pr
                = UnivariatePolynomial.create(finiteField, prCoefficients);
            // 计算P_0(x) + P_1(x) * P_r(x)
            interpolatePolynomial = interpolatePolynomial.add(p1.multiply(pr));
        }
        return polynomialToBigIntegers(xArray.length, expectNum, interpolatePolynomial);
    }

    @Override
    public BigInteger[] rootInterpolate(int expectNum, BigInteger[] xArray, BigInteger y) {
        assert expectNum >= 1 && xArray.length <= expectNum : "num must be in range [0, " + expectNum + "]: " + xArray.length;
        if (xArray.length == 0) {
            // 返回随机多项式
            BigInteger[] coefficients = new BigInteger[expectNum + 1];
            for (int index = 0; index < expectNum; index++) {
                coefficients[index] = BigIntegerUtils.randomNonNegative(p, secureRandom);
            }
            // 将最高位设置为1
            coefficients[expectNum] = BigInteger.ONE;
            return coefficients;
        }
        // 如果有插值数据，则继续插值
        for (BigInteger x : xArray) {
            assert validPoint(x);
        }
        assert validPoint(y);
        // 转换成多项式点
        cc.redberry.rings.bigint.BigInteger[] pointXs = Arrays.stream(xArray)
            .map(cc.redberry.rings.bigint.BigInteger::new)
            .toArray(cc.redberry.rings.bigint.BigInteger[]::new);
        // 插值
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomial = UnivariatePolynomial.one(finiteField);
        // f(x) = (x - x_0) * (x - x_1) * ... * (x - x_m)
        for (cc.redberry.rings.bigint.BigInteger pointX : pointXs) {
            UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> linear = polynomial.createLinear(
                finiteField.negate(pointX), finiteField.getOne()
            );
            polynomial = polynomial.multiply(linear);
        }
        if (xArray.length < expectNum) {
            // 构造随机多项式
            cc.redberry.rings.bigint.BigInteger[] prCoefficients = IntStream.range(0, expectNum - xArray.length)
                .mapToObj(index -> new cc.redberry.rings.bigint.BigInteger(BigIntegerUtils.randomNonNegative(p, secureRandom)))
                .toArray(cc.redberry.rings.bigint.BigInteger[]::new);
            UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> dummyPolynomial
                = UnivariatePolynomial.create(finiteField, prCoefficients);
            // 把最高位设置为1
            dummyPolynomial.set(expectNum - xArray.length, finiteField.getOne());
            // 计算P_0(x) * P_r(x)
            polynomial = polynomial.multiply(dummyPolynomial);
        }
        cc.redberry.rings.bigint.BigInteger pointY = new cc.redberry.rings.bigint.BigInteger(y);
        polynomial = polynomial.add(UnivariatePolynomial.constant(finiteField, pointY));

        return rootPolynomialToBigIntegers(xArray.length, expectNum, polynomial);
    }

    /**
     * 多项式插值，得到多项式f(x)，使得y = f(x)，返回多项式本身。
     *
     * @param xArray x数组。
     * @param yArray y数组。
     * @return 多项式。
     */
    protected abstract UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomialInterpolate(
        BigInteger[] xArray, BigInteger[] yArray);

    @Override
    public BigInteger evaluate(BigInteger[] coefficients, BigInteger x) {
        assert coefficients.length >= 1 : "coefficient num must be greater than or equal to 1: " + coefficients.length;
        for (BigInteger coefficient : coefficients) {
            assert validPoint(coefficient);
        }
        // 验证x的有效性
        assert validPoint(x);
        // 恢复多项式
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomial = bigIntegersToPolynomial(coefficients);
        // 求值
        cc.redberry.rings.bigint.BigInteger xRings = new cc.redberry.rings.bigint.BigInteger(x);
        cc.redberry.rings.bigint.BigInteger yRings = polynomial.evaluate(xRings);
        return BigIntegerUtils.byteArrayToBigInteger(yRings.toByteArray());
    }

    @Override
    public BigInteger[] evaluate(BigInteger[] coefficients, BigInteger[] xArray) {
        // 恢复多项式
        UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomial = bigIntegersToPolynomial(coefficients);
        // 求值
        return polynomialEvaluate(polynomial, xArray);
    }

    protected BigInteger[] polynomialEvaluate
        (UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomial, BigInteger[] xArray) {
        return Arrays.stream(xArray)
            .map(cc.redberry.rings.bigint.BigInteger::new)
            .map(polynomial::evaluate)
            .map(y -> BigIntegerUtils.byteArrayToBigInteger(y.toByteArray()))
            .toArray(BigInteger[]::new);
    }

    protected BigInteger[] polynomialToBigIntegers(int pointNum, int expectNum, UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomial) {
        BigInteger[] coefficients = new BigInteger[coefficientNum(pointNum, expectNum)];
        // 低阶系数正常拷贝
        IntStream.range(0, polynomial.degree() + 1).forEach(degreeIndex -> coefficients[degreeIndex]
            = BigIntegerUtils.byteArrayToBigInteger(polynomial.get(degreeIndex).toByteArray())
        );
        // 高阶系数补0
        IntStream.range(polynomial.degree() + 1, coefficients.length).forEach(degreeIndex ->
            coefficients[degreeIndex] = BigInteger.ZERO
        );

        return coefficients;
    }

    private BigInteger[] rootPolynomialToBigIntegers(int pointNum, int expectNum, UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomial) {
        BigInteger[] coefficients = new BigInteger[rootCoefficientNum(pointNum, expectNum)];
        // 低阶系数正常拷贝
        IntStream.range(0, polynomial.degree() + 1).forEach(degreeIndex -> coefficients[degreeIndex]
            = BigIntegerUtils.byteArrayToBigInteger(polynomial.get(degreeIndex).toByteArray())
        );
        // 高阶系数补0
        IntStream.range(polynomial.degree() + 1, coefficients.length).forEach(degreeIndex ->
            coefficients[degreeIndex] = BigInteger.ZERO
        );

        return coefficients;
    }

    protected UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> bigIntegersToPolynomial(
        BigInteger[] coefficients) {
        cc.redberry.rings.bigint.BigInteger[] polyCoefficients = Arrays.stream(coefficients)
            .map(cc.redberry.rings.bigint.BigInteger::new)
            .toArray(cc.redberry.rings.bigint.BigInteger[]::new);

        return UnivariatePolynomial.create(finiteField, polyCoefficients);
    }
}
