package edu.alibaba.mpc4j.common.tool.metrics;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * Kendall排序关联系数测试类。例子和参考结果来源：
 * <p>
 * https://docs.scipy.org/doc/scipy/reference/generated/scipy.stats.kendalltau.html
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/08/11
 */
public class KendallCorrelationTest {
    /**
     * 数组长度
     */
    private static final int ARRAY_LENGTH = 5;
    /**
     * scipy实例的向量x
     */
    private static final double[] SCIPY_EXAMPLE_X_ARRAY = new double[] {
        12, 2, 1, 12, 2
    };
    /**
     * scipy实例的向量y
     */
    private static final double[] SCIPY_EXAMPLE_Y_ARRAY = new double[] {
        1, 4, 7, 1, 0
    };

    private static final double SCIPY_EXAMPLE_TAU_A = -0.4;
    /**
     * τ_b真实值
     */
    private static final double SCIPY_EXAMPLE_TAU_B = -0.47140452079103173;
    /**
     * τ_rn真实值
     */
    private static final double SCIPY_EXAMPLE_TAU_ROW_NUMBER = -0.6;
    /**
     * τ_dr真实值
     */
    private static final double SCIPY_EXAMPLE_TAU_DENSE_RANK = -0.6;

    /**
     * τ_d真实值
     */
    private static final double SCIPY_EXAMPLE_TAU_D = -0.375;
    /**
     * 顺序无重复向量x
     */
    private static final double[] ORDER_NO_REPUTATION_X_ARRAY = IntStream.range(0, ARRAY_LENGTH)
        .mapToDouble(value -> (double)value).toArray();
    /**
     * 顺序无重复向量y
     */
    private static final double[] ORDER_NO_REPUTATION_Y_ARRAY = IntStream.range(0, ARRAY_LENGTH)
        .mapToDouble(value -> (double)value).toArray();
    /**
     * 顺序无重复向量的τ均为1
     */
    private static final double ORDER_NO_REPUTATION_TAU = 1.0;
    /**
     * 逆序无重复向量x
     */
    private static final double[] REVERSED_ORDER_NO_REPUTATION_X_ARRAY = IntStream.range(0, ARRAY_LENGTH)
        .mapToDouble(value -> (double)value).toArray();
    /**
     * 逆序无重复向量y
     */
    private static final double[] REVERSED_ORDER_NO_REPUTATION_Y_ARRAY = IntStream.range(0, ARRAY_LENGTH)
        .mapToDouble(value -> (double)ARRAY_LENGTH - 1 - value).toArray();
    /**
     * 逆序无重复向量的τ均为-1
     */
    private static final double REVERSED_ORDER_NO_REPUTATION_TAU = -1.0;

    @Test
    public void testDirectTauA() {
        double scipyTau = KendallCorrelation
            .directTauA(SCIPY_EXAMPLE_X_ARRAY, SCIPY_EXAMPLE_Y_ARRAY);
        Assert.assertEquals(SCIPY_EXAMPLE_TAU_A, scipyTau, DoubleUtils.PRECISION);

        double orderNoReputationTau = KendallCorrelation
            .directTauA(ORDER_NO_REPUTATION_X_ARRAY, ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(ORDER_NO_REPUTATION_TAU, orderNoReputationTau, DoubleUtils.PRECISION);

        double rOrderNoReputationTau = KendallCorrelation
            .directTauA(REVERSED_ORDER_NO_REPUTATION_X_ARRAY, REVERSED_ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(REVERSED_ORDER_NO_REPUTATION_TAU, rOrderNoReputationTau, DoubleUtils.PRECISION);
    }

    @Test
    public void testEfficientTauA() {
        double scipyTau = KendallCorrelation
            .efficientTauA(SCIPY_EXAMPLE_X_ARRAY, SCIPY_EXAMPLE_Y_ARRAY);
        Assert.assertEquals(SCIPY_EXAMPLE_TAU_A, scipyTau, DoubleUtils.PRECISION);

        double orderNoReputationTau = KendallCorrelation
            .efficientTauA(ORDER_NO_REPUTATION_X_ARRAY, ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(ORDER_NO_REPUTATION_TAU, orderNoReputationTau, DoubleUtils.PRECISION);

        double rOrderNoReputationTau = KendallCorrelation
            .efficientTauA(REVERSED_ORDER_NO_REPUTATION_X_ARRAY, REVERSED_ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(REVERSED_ORDER_NO_REPUTATION_TAU, rOrderNoReputationTau, DoubleUtils.PRECISION);
    }

    @Test
    public void testDirectTauB() {
        double scipyTau = KendallCorrelation
            .directTauB(SCIPY_EXAMPLE_X_ARRAY, SCIPY_EXAMPLE_Y_ARRAY);
        Assert.assertEquals(SCIPY_EXAMPLE_TAU_B, scipyTau, DoubleUtils.PRECISION);

        double orderNoReputationTau = KendallCorrelation
            .directTauB(ORDER_NO_REPUTATION_X_ARRAY, ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(ORDER_NO_REPUTATION_TAU, orderNoReputationTau, DoubleUtils.PRECISION);

        double rOrderNoReputationTau = KendallCorrelation
            .directTauB(REVERSED_ORDER_NO_REPUTATION_X_ARRAY, REVERSED_ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(REVERSED_ORDER_NO_REPUTATION_TAU, rOrderNoReputationTau, DoubleUtils.PRECISION);
    }

    @Test
    public void testEfficientTauB() {
        double scipyTau = KendallCorrelation
            .efficientTauB(SCIPY_EXAMPLE_X_ARRAY, SCIPY_EXAMPLE_Y_ARRAY);
        Assert.assertEquals(SCIPY_EXAMPLE_TAU_B, scipyTau, DoubleUtils.PRECISION);

        double orderNoReputationTau = KendallCorrelation
            .efficientTauB(ORDER_NO_REPUTATION_X_ARRAY, ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(ORDER_NO_REPUTATION_TAU, orderNoReputationTau, DoubleUtils.PRECISION);

        double rOrderNoReputationTau = KendallCorrelation
            .efficientTauB(REVERSED_ORDER_NO_REPUTATION_X_ARRAY, REVERSED_ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(REVERSED_ORDER_NO_REPUTATION_TAU, rOrderNoReputationTau, DoubleUtils.PRECISION);
    }

    @Test
    public void testDirectTauRn() {
        double scipyTau = KendallCorrelation
            .directTauRn(SCIPY_EXAMPLE_X_ARRAY, SCIPY_EXAMPLE_Y_ARRAY);
        Assert.assertEquals(SCIPY_EXAMPLE_TAU_ROW_NUMBER, scipyTau, DoubleUtils.PRECISION);

        double orderNoReputationTau = KendallCorrelation
            .directTauRn(ORDER_NO_REPUTATION_X_ARRAY, ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(ORDER_NO_REPUTATION_TAU, orderNoReputationTau, DoubleUtils.PRECISION);

        double rOrderNoReputationTau = KendallCorrelation
            .directTauRn(REVERSED_ORDER_NO_REPUTATION_X_ARRAY, REVERSED_ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(REVERSED_ORDER_NO_REPUTATION_TAU, rOrderNoReputationTau, DoubleUtils.PRECISION);
    }

    @Test
    public void testDirectTauDr() {
        double scipyTau = KendallCorrelation
            .directTauDr(SCIPY_EXAMPLE_X_ARRAY, SCIPY_EXAMPLE_Y_ARRAY);
        Assert.assertEquals(SCIPY_EXAMPLE_TAU_DENSE_RANK, scipyTau, DoubleUtils.PRECISION);

        double orderNoReputationTau = KendallCorrelation
            .directTauDr(ORDER_NO_REPUTATION_X_ARRAY, ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(ORDER_NO_REPUTATION_TAU, orderNoReputationTau, DoubleUtils.PRECISION);

        double rOrderNoReputationTau = KendallCorrelation
            .directTauDr(REVERSED_ORDER_NO_REPUTATION_X_ARRAY, REVERSED_ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(REVERSED_ORDER_NO_REPUTATION_TAU, rOrderNoReputationTau, DoubleUtils.PRECISION);
    }

    @Test
    public void testDirectTauD() {
        double scipyTau = KendallCorrelation
            .directTauD(SCIPY_EXAMPLE_X_ARRAY, SCIPY_EXAMPLE_Y_ARRAY);
        Assert.assertEquals(SCIPY_EXAMPLE_TAU_D, scipyTau, DoubleUtils.PRECISION);

        double orderNoReputationTau = KendallCorrelation
            .directTauD(ORDER_NO_REPUTATION_X_ARRAY, ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(ORDER_NO_REPUTATION_TAU, orderNoReputationTau, DoubleUtils.PRECISION);

        double rOrderNoReputationTau = KendallCorrelation
            .directTauD(REVERSED_ORDER_NO_REPUTATION_X_ARRAY, REVERSED_ORDER_NO_REPUTATION_Y_ARRAY);
        Assert.assertEquals(REVERSED_ORDER_NO_REPUTATION_TAU, rOrderNoReputationTau, DoubleUtils.PRECISION);
    }
}
