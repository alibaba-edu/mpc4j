package edu.alibaba.mpc4j.common.tool.utils;

import ch.obermuhlner.math.big.BigDecimalMath;
import org.apache.commons.math3.util.Precision;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.MathContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 高精度浮点数运算测试。代码来自https://github.com/eobermuhlner/big-math/下面的BigDecimalMathTest.java，有修改。
 *
 * @author Weiran Liu
 * @date 2021/12/20
 */
public class BigDecimalMathTest {
    /**
     * 精度
     */
    private static final MathContext MC = MathContext.DECIMAL128;
    /**
     * 双精度浮点数精度
     */
    private static final MathContext MC_CHECK_DOUBLE = new MathContext(10);

    @Test
    public void testPowIntPositiveY() {
        // x^y，且y为正整数
        for (int x : new int[] {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5}) {
            for (int y : new int[] {0, 1, 2, 3, 4, 5}) {
                assertEquals(
                    x + "^" + y,
                    BigDecimalMath.round(BigDecimal.valueOf((int)Math.pow(x, y)), MC),
                    BigDecimalMath.pow(BigDecimal.valueOf(x), y, MC));
            }
        }
    }

    @Test
    public void testPowIntNegativeY() {
        // x^y，且y为负数，注意x不能取0
        for(int x : new int[] { -5, -4, -3, -2, -1, 1, 2, 3, 4, 5 }) {
            for(int y : new int[] { -5, -4, -3, -2, -1}) {
                assertEquals(
                    x + "^" + y,
                    BigDecimalMath.round(BigDecimal.ONE.divide(BigDecimal.valueOf((int) Math.pow(x, -y)), MC), MC),
                    BigDecimalMath.pow(BigDecimal.valueOf(x), y, MC));
            }
        }
    }

    @Test
    public void testPowIntSpecialCases() {
        // 0^0 = 1
        assertEquals(
            BigDecimalMath.round(BigDecimal.valueOf(1), MC), BigDecimalMath.pow(BigDecimal.valueOf(0), 0, MC)
        );
        // 0^x = 0 for x > 0
        assertEquals(
            BigDecimalMath.round(BigDecimal.valueOf(0), MC), BigDecimalMath.pow(BigDecimal.valueOf(0), +5, MC)
        );
        // x^0 = 1 for all x
        assertEquals(
            BigDecimalMath.round(BigDecimal.valueOf(1), MC), BigDecimalMath.pow(BigDecimal.valueOf(-5), 0, MC)
        );
        assertEquals(
            BigDecimalMath.round(BigDecimal.valueOf(1), MC), BigDecimalMath.pow(BigDecimal.valueOf(+5), 0, MC)
        );
    }

    @Test
    public void testSqrt() {
        for(double value : new double[] { 0, 0.1, 2, 4, 10, 16, 33.3333 }) {
            assertBigDecimal(
                "sqrt(" + value + ")",
                toCheck(Math.sqrt(value)),
                BigDecimalMath.sqrt(BigDecimal.valueOf(value), MC)
            );
        }
    }

    @Test
    public void testSqrtHuge() {
        // Result from wolframalpha.com: sqrt(1e399)
        BigDecimal expected = new BigDecimal("3.1622776601683793319988935444327185337195551393252168E199");
        assertEquals(expected.round(MC), BigDecimalMath.sqrt(new BigDecimal("1E399"), MC));
    }

    /**
     * 验证高精度浮点数的准确性。
     *
     * @param description 描述信息。
     * @param expected 期望结果。
     * @param actual 实际结果。
     */
    private static void assertBigDecimal(
        String description, BigDecimal expected, BigDecimal actual) {
        MathContext calculationMathContext = new MathContext(MC_CHECK_DOUBLE.getPrecision() + 10);
        BigDecimal error = expected.subtract(actual, calculationMathContext).abs();
        BigDecimal acceptableError = actual.round(MC_CHECK_DOUBLE).ulp();

        String fullDescription = description
            + " expected=" + expected
            + " actual=" + actual
            + " precision=" + MC_CHECK_DOUBLE.getPrecision()
            + " error=" + error
            + " acceptableError=" + acceptableError;
        assertTrue(fullDescription, error.compareTo(acceptableError) <= 0);
    }

    /**
     * 检查输入的双精度浮点数并返回适当的{@code BigDecimal}。
     *
     * @param value 双精度浮点数。
     * @return 双精度浮点数所对应的{@code BigDecimal}。
     */
    private static BigDecimal toCheck(double value) {
        long longValue = (long) value;
        if (Precision.equals(value, longValue, Double.MIN_VALUE)) {
            return BigDecimal.valueOf(longValue);
        }
        return BigDecimal.valueOf(value);
    }
}
