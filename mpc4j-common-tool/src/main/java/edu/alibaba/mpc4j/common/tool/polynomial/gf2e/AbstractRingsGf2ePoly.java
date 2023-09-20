package edu.alibaba.mpc4j.common.tool.polynomial.gf2e;

import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.RingsUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Rings的GF2E多项式插值抽象类。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
abstract class AbstractRingsGf2ePoly extends AbstractGf2ePoly {
    /**
     * type
     */
    private final Gf2ePolyFactory.Gf2ePolyType type;

    AbstractRingsGf2ePoly(Gf2ePolyFactory.Gf2ePolyType type, int l) {
        super(l);
        this.type = type;
    }

    @Override
    public Gf2ePolyFactory.Gf2ePolyType getType() {
        return type;
    }

    @Override
    public int coefficientNum(int num) {
        return Gf2ePolyFactory.getCoefficientNum(type, num);
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
        // 转换成多项式点
        UnivariatePolynomialZp64[] pointXs = Arrays.stream(xArray)
            .map(RingsUtils::byteArrayToGf2e)
            .toArray(UnivariatePolynomialZp64[]::new);
        UnivariatePolynomialZp64[] pointYs = Arrays.stream(yArray)
            .map(RingsUtils::byteArrayToGf2e)
            .toArray(UnivariatePolynomialZp64[]::new);
        // 得到插值多项式
        UnivariatePolynomial<UnivariatePolynomialZp64> polynomial = polynomialInterpolate(num, pointXs, pointYs);
        // 如果插值点数量小于最大点数量，则补充虚拟点
        if (pointXs.length < num) {
            // 计算(x - x_1) * ... * (x - x_m')
            UnivariatePolynomial<UnivariatePolynomialZp64> p1 = UnivariatePolynomial.one(finiteField);
            for (UnivariatePolynomialZp64 point : pointXs) {
                p1 = p1.multiply(p1.createLinear(finiteField.negate(point), finiteField.getOne()));
            }
            // 构造随机多项式
            UnivariatePolynomialZp64[] prCoefficients = new UnivariatePolynomialZp64[num - pointXs.length];
            for (int index = 0; index < prCoefficients.length; index++) {
                byte[] coefficient = BytesUtils.randomByteArray(byteL, l, secureRandom);
                prCoefficients[index] = RingsUtils.byteArrayToGf2e(coefficient);
            }
            UnivariatePolynomial<UnivariatePolynomialZp64> pr = UnivariatePolynomial.create(finiteField, prCoefficients);
            // 计算P_0(x) + P_1(x) * P_r(x)
            polynomial = polynomial.add(p1.multiply(pr));
        }
        return polynomialToBytes(num, polynomial);
    }

    /**
     * 多项式插值，得到多项式f(x)，使得y = f(x)，返回多项式本身。
     *
     * @param num    插入点的最大数量。
     * @param xArray x数组。
     * @param yArray y数组。
     * @return 多项式。
     */
    protected abstract UnivariatePolynomial<UnivariatePolynomialZp64> polynomialInterpolate(
        int num, UnivariatePolynomialZp64[] xArray, UnivariatePolynomialZp64[] yArray);

    @Override
    public int rootCoefficientNum(int num) {
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
        // 如果有插值数据，则继续插值
        for (byte[] x : xArray) {
            assert validPoint(x);
        }
        assert validPoint(y);
        // 插值
        UnivariatePolynomial<UnivariatePolynomialZp64> polynomial = UnivariatePolynomial.one(finiteField);
        // f(x) = (x - x_0) * (x - x_1) * ... * (x - x_m)
        for (byte[] x : xArray) {
            UnivariatePolynomialZp64 pointX = RingsUtils.byteArrayToGf2e(x);
            UnivariatePolynomial<UnivariatePolynomialZp64> linear = polynomial.createLinear(
                finiteField.negate(pointX), finiteField.getOne()
            );
            polynomial = polynomial.multiply(linear);
        }
        if (xArray.length < num) {
            // 构造随机多项式
            UnivariatePolynomialZp64[] prCoefficients = IntStream.range(0, num - xArray.length)
                .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, secureRandom))
                .map(RingsUtils::byteArrayToGf2e)
                .toArray(UnivariatePolynomialZp64[]::new);
            UnivariatePolynomial<UnivariatePolynomialZp64> dummyPolynomial
                = UnivariatePolynomial.create(finiteField, prCoefficients);
            // 把最高位设置为1
            dummyPolynomial.set(num - xArray.length, finiteField.getOne());
            // 计算P_0(x) * P_r(x)
            polynomial = polynomial.multiply(dummyPolynomial);
        }
        UnivariatePolynomialZp64 pointY = RingsUtils.byteArrayToGf2e(y);
        polynomial = polynomial.add(UnivariatePolynomial.constant(finiteField, pointY));

        return rootPolynomialToBytes(num, polynomial);
    }

    @Override
    public byte[] evaluate(byte[][] coefficients, byte[] x) {
        assert coefficients.length >= 1;
        for (byte[] coefficient : coefficients) {
            assert validPoint(coefficient);
        }
        assert validPoint(x);
        // 恢复多项式
        UnivariatePolynomial<UnivariatePolynomialZp64> polynomial = bytesToPolynomial(coefficients);
        // 求值
        UnivariatePolynomialZp64 pointX = RingsUtils.byteArrayToGf2e(x);
        UnivariatePolynomialZp64 pointY = polynomial.evaluate(pointX);
        return RingsUtils.gf2eToByteArray(pointY, byteL);
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
        // 恢复多项式
        UnivariatePolynomial<UnivariatePolynomialZp64> polynomial = bytesToPolynomial(coefficients);
        // 求值
        return Arrays.stream(xArray)
            .map(RingsUtils::byteArrayToGf2e)
            .map(pointX -> polynomialEvaluate(polynomial, pointX))
            .map(pointY -> RingsUtils.gf2eToByteArray(pointY, byteL))
            .toArray(byte[][]::new);
    }

    /**
     * 给定多项式f，求y = f(x)。
     *
     * @param polynomial 给定多项式。
     * @param x          x。
     * @return f(x)。
     */
    private UnivariatePolynomialZp64 polynomialEvaluate
    (UnivariatePolynomial<UnivariatePolynomialZp64> polynomial, UnivariatePolynomialZp64 x) {
        assert polynomial.ring.equals(finiteField);
        return polynomial.evaluate(x);
    }

    protected byte[][] polynomialToBytes(int num, UnivariatePolynomial<UnivariatePolynomialZp64> polynomial) {
        byte[][] coefficients = new byte[num][byteL];
        IntStream.range(0, polynomial.degree() + 1).forEach(degreeIndex ->
            coefficients[degreeIndex] = RingsUtils.gf2eToByteArray(polynomial.get(degreeIndex), byteL)
        );

        return coefficients;
    }

    private byte[][] rootPolynomialToBytes(int num, UnivariatePolynomial<UnivariatePolynomialZp64> polynomial) {
        byte[][] coefficients = new byte[num + 1][byteL];
        IntStream.range(0, polynomial.degree() + 1).forEach(degreeIndex ->
            coefficients[degreeIndex] = RingsUtils.gf2eToByteArray(polynomial.get(degreeIndex), byteL)
        );

        return coefficients;
    }

    protected UnivariatePolynomial<UnivariatePolynomialZp64> bytesToPolynomial(byte[][] coefficients) {
        UnivariatePolynomialZp64[] polyCoefficients = Arrays.stream(coefficients)
            .map(RingsUtils::byteArrayToGf2e)
            .toArray(UnivariatePolynomialZp64[]::new);

        return UnivariatePolynomial.create(finiteField, polyCoefficients);
    }
}
