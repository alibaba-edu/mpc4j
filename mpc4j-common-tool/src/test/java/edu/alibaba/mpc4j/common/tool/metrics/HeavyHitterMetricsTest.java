package edu.alibaba.mpc4j.common.tool.metrics;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * HeavyHitter Metrics tests.
 *
 * @author Weiran Liu
 * @date 2022/11/17
 */
public class HeavyHitterMetricsTest {

    @Test
    public void testNdcgLength0() {
        List<Integer> realList = new LinkedList<>();
        List<Integer> predictionList = new LinkedList<>();
        Assert.assertEquals(0, HeavyHitterMetrics.ndcg(predictionList, realList), DoubleUtils.PRECISION);
    }

    @Test
    public void testNdcgLength1() {
        List<Integer> realList = IntStream.rangeClosed(0, 0).boxed().collect(Collectors.toList());
        List<Integer> correctPredictionList = IntStream.rangeClosed(0, 0).boxed().collect(Collectors.toList());
        Assert.assertEquals(1, HeavyHitterMetrics.ndcg(correctPredictionList, realList), DoubleUtils.PRECISION);

        List<Integer> wrongPredictionList = IntStream.rangeClosed(1, 1).boxed().collect(Collectors.toList());
        Assert.assertEquals(0, HeavyHitterMetrics.ndcg(wrongPredictionList, realList), DoubleUtils.PRECISION);
    }

    @Test
    public void testNdcgLengthK() {
        int k = 20;
        List<Integer> realList = IntStream.rangeClosed(1, k).boxed().collect(Collectors.toList());
        List<Integer> correctPredictionList = IntStream.rangeClosed(1, k).boxed().collect(Collectors.toList());
        Assert.assertEquals(1, HeavyHitterMetrics.ndcg(correctPredictionList, realList), DoubleUtils.PRECISION);

        List<Integer> reversePredictionList = IntStream.rangeClosed(1, k).boxed()
            .sorted(Collections.reverseOrder()).collect(Collectors.toList());
        //    real list = [1, 2, 3, ..., k]
        // reverse list = [k, ..., 3, 2, 1]
        // the i-th item in the reverse list has relevance k - |k + 1 - 2 * i|
        double expectReverseNdcgkNumerator = IntStream.rangeClosed(1, k)
            .mapToDouble(i -> (k - Math.abs(k + 1 - 2 * i)) * Math.log(2) / Math.log(i + 1))
            .sum();
        double expectReverseNdcgkDenominator = IntStream.rangeClosed(1, k)
            .mapToDouble(i -> k * Math.log(2) / Math.log(i + 1))
            .sum();
        double expectReverseNdcgk = expectReverseNdcgkNumerator / expectReverseNdcgkDenominator;
        Assert.assertEquals(expectReverseNdcgk, HeavyHitterMetrics.ndcg(reversePredictionList, realList), DoubleUtils.PRECISION);

        List<Integer> wrongPredictionList = IntStream.rangeClosed(k + 1, 2 * k).boxed().collect(Collectors.toList());
        Assert.assertEquals(0, HeavyHitterMetrics.ndcg(wrongPredictionList, realList), DoubleUtils.PRECISION);
    }

    @Test
    public void testPrecisionLength0() {
        List<Integer> realList = new LinkedList<>();
        List<Integer> predictionList = new LinkedList<>();
        Assert.assertEquals(0, HeavyHitterMetrics.precision(predictionList, realList), DoubleUtils.PRECISION);
    }

    @Test
    public void testPrecisionLength1() {
        List<Integer> realList = IntStream.rangeClosed(0, 0).boxed().collect(Collectors.toList());
        List<Integer> correctPredictionList = IntStream.rangeClosed(0, 0).boxed().collect(Collectors.toList());
        Assert.assertEquals(1, HeavyHitterMetrics.precision(correctPredictionList, realList), DoubleUtils.PRECISION);

        List<Integer> wrongPredictionList = IntStream.rangeClosed(1, 1).boxed().collect(Collectors.toList());
        Assert.assertEquals(0, HeavyHitterMetrics.precision(wrongPredictionList, realList), DoubleUtils.PRECISION);
    }

    @Test
    public void testPrecisionLengthK() {
        int k = 20;
        List<Integer> realList = IntStream.rangeClosed(1, k).boxed().collect(Collectors.toList());
        List<Integer> correctPredictionList = IntStream.rangeClosed(1, k).boxed().collect(Collectors.toList());
        Assert.assertEquals(1, HeavyHitterMetrics.ndcg(correctPredictionList, realList), DoubleUtils.PRECISION);

        List<Integer> reversePredictionList = IntStream.rangeClosed(1, k).boxed()
            .sorted(Collections.reverseOrder()).collect(Collectors.toList());
        Assert.assertEquals(1, HeavyHitterMetrics.precision(reversePredictionList, realList), DoubleUtils.PRECISION);

        List<Integer> wrongPredictionList = IntStream.rangeClosed(k + 1, 2 * k).boxed().collect(Collectors.toList());
        Assert.assertEquals(0, HeavyHitterMetrics.precision(wrongPredictionList, realList), DoubleUtils.PRECISION);
    }

    @Test
    public void testRelativeErrorSize0() {
        Map<Integer, Integer> realMap = new HashMap<>();
        Map<Integer, Double> predictionMap = new HashMap<>();
        Assert.assertEquals(0, HeavyHitterMetrics.relativeError(predictionMap, realMap), DoubleUtils.PRECISION);
    }

    @Test
    public void testRelativeErrorSize1() {
        Map<Integer, Integer> realMap = IntStream.rangeClosed(1, 1).boxed().collect(Collectors.toMap(
            i -> i,
            i -> 100
        ));
        // 预测值等于真实值
        Map<Integer, Double> correctPredictionMap = IntStream.rangeClosed(1, 1).boxed().collect(Collectors.toMap(
            i -> i,
            i -> 100.0
        ));
        Assert.assertEquals(0, HeavyHitterMetrics.relativeError(correctPredictionMap, realMap), DoubleUtils.PRECISION);
        // 预测值偏小
        Map<Integer, Double> lessPredictionMap = IntStream.rangeClosed(1, 1).boxed().collect(Collectors.toMap(
            i -> i,
            i -> 0.0
        ));
        Assert.assertEquals(1, HeavyHitterMetrics.relativeError(lessPredictionMap, realMap), DoubleUtils.PRECISION);
        // 预测值偏大
        Map<Integer, Double> largePredictionMap = IntStream.rangeClosed(1, 1).boxed().collect(Collectors.toMap(
            i -> i,
            i -> 200.0
        ));
        Assert.assertEquals(1, HeavyHitterMetrics.relativeError(largePredictionMap, realMap), DoubleUtils.PRECISION);
    }

    @Test
    public void testRelativeErrorSizeK() {
        int k = 20;
        Map<Integer, Integer> realMap = IntStream.rangeClosed(1, k).boxed().collect(Collectors.toMap(
            i -> i,
            i -> 100 * k
        ));
        // 预测值等于真实值
        Map<Integer, Double> correctPredictionMap = IntStream.rangeClosed(1, k).boxed().collect(Collectors.toMap(
            i -> i,
            i -> 100.0 * k
        ));
        Assert.assertEquals(0, HeavyHitterMetrics.relativeError(correctPredictionMap, realMap), DoubleUtils.PRECISION);
        // 预测值偏小
        Map<Integer, Double> lessPredictionMap = IntStream.rangeClosed(1, k).boxed().collect(Collectors.toMap(
            i -> i,
            i -> 50.0 * k
        ));
        Assert.assertEquals(0.5, HeavyHitterMetrics.relativeError(lessPredictionMap, realMap), DoubleUtils.PRECISION);
        // 预测值偏大
        Map<Integer, Double> largePredictionMap = IntStream.rangeClosed(1, k).boxed().collect(Collectors.toMap(
            i -> i,
            i -> 200.0 * k
        ));
        Assert.assertEquals(1, HeavyHitterMetrics.relativeError(largePredictionMap, realMap), DoubleUtils.PRECISION);
        // 预测值为负数
        Map<Integer, Double> negPredictionMap = IntStream.rangeClosed(1, k).boxed().collect(Collectors.toMap(
            i -> i,
            i -> -100.0 * k
        ));
        Assert.assertEquals(2, HeavyHitterMetrics.relativeError(negPredictionMap, realMap), DoubleUtils.PRECISION);
    }
}
