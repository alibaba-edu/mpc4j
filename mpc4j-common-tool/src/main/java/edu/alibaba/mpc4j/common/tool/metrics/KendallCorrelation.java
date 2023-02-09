package edu.alibaba.mpc4j.common.tool.metrics;

import edu.alibaba.mpc4j.common.tool.utils.RankUtils;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Kendall排序关联系数（Kendall Rank Correlation Coefficient）实现工具类。
 * 本工具类提供了3种Kendall排序关联系数τ(x, y) = <x, y> / (|x| * |y|)。
 * <p>
 * τ_b关联系数：定义<x, y> = Σ_{i < j}(sgn(x_i, x_j) * sgn(y_i, y_j))，
 * 则|x| = √(<x, x>) = √(Σ_{i < j}(1)) = √(n * (n - 1) / 2)。
 * </p>
 * <p>
 * τ_rn关联系数：定义<x, y> = Σ_{i < j}(sgn(x_i, x_j) * sgn(y_i, y_j) * |rn(x_i) - rn(x_j)|)，其中rn为row_number。
 * 则|x| = √(<x, x>) = √(Σ_{i < j}(|rank(x_i) - rank(x_j)|)) = √((n + 1) * n * (n - 1) / 6)。
 * </p>
 * <p>
 * τ_dr关联系数：定义<x, y> = Σ_{i < j}(sgn(x_i, x_j) * sgn(y_i, y_j) * |dr(x_i) - dr(x_j)|)，其中dr为dense_rank。
 * 此时|x|需要手动完成计算，无法直接应用公式计算。
 * </p>
 * <p>
 * τ_d关联系数：定义<x, y> = Σ_{i < j}(sgn(x_i, x_j) * sgn(y_i, y_j) * |x_i - x_j|)，
 * 则|x| = √(<x, x>) = √(Σ_{i < j}(|x_i - x_j|)。
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/08/11
 */
@SuppressWarnings("AlibabaAvoidDoubleOrFloatEqualCompare")
public class KendallCorrelation {
    /**
     * τ_b关联系数计算函数
     */
    private static final KendallsCorrelation KENDALLS_CORRELATION = new KendallsCorrelation();

    private KendallCorrelation() {
        // empty
    }

    /**
     * 直接计算向量x和y的τ_a关联系数，计算复杂度为O(n^2)。
     *
     * @param xArray 向量x。
     * @param yArray 向量y。
     * @return τ_a关联系数。
     */
    public static double directTauA(double[] xArray, double[] yArray) {
        if (xArray.length != yArray.length) {
            throw new DimensionMismatchException(xArray.length, yArray.length);
        }
        final int n = xArray.length;
        assert n > 1 : "data array length must be greater than 1";
        // n0 = n * (n - 1) / 2
        final long n0 = sum(n - 1);
        // 计算分子
        double numerator = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                numerator += (Math.signum(xArray[i] - xArray[j]) * Math.signum(yArray[i] - yArray[j]));
            }
        }

        return numerator / n0;
    }

    /**
     * 高效计算向量x和y的τ_a关联系数，计算复杂度为O(n·log(n))。
     *
     * @param xArray 向量x。
     * @param yArray 向量y。
     * @return τ_a关联系数。
     */
    public static double efficientTauA(double[] xArray, double[] yArray) {
        if (xArray.length != yArray.length) {
            throw new DimensionMismatchException(xArray.length, yArray.length);
        }
        final int n = xArray.length;
        assert n > 1 : "data array length must be greater than 1";
        // n0 = n * (n - 1) / 2
        final long n0 = sum(n - 1);

        ArrayList<Pair<Double, Double>> sortPairs = getSortPairs(xArray, yArray);
        long n1 = calculateN1(sortPairs);
        long n2 = calculateN2(sortPairs);
        long n3 = calculateN3(sortPairs);
        long swaps = calculateSwaps(sortPairs);
        // 计算分子
        final long numerator = n0 - n1 - n2 + n3 - 2 * swaps;
        return numerator / (double) n0;
    }

    /**
     * 直接计算向量x和y的τ_b关联系数，计算复杂度为O(n^2)。
     * Values of Tau-b range from −1 (100% negative association, or perfect inversion) to +1 (100% positive association,
     * or perfect agreement). A value of 0 indicates the absence of association.
     *
     * @param xArray 向量x。
     * @param yArray 向量y。
     * @return τ_b关联系数。
     */
    public static double directTauB(double[] xArray, double[] yArray) {
        if (xArray.length != yArray.length) {
            throw new DimensionMismatchException(xArray.length, yArray.length);
        }
        final int n = xArray.length;
        assert n > 1 : "data array length must be greater than 1";
        final long n0 = sum(n - 1);

        ArrayList<Pair<Double, Double>> pairs = getSortPairs(xArray, yArray);
        long n1 = calculateN1(pairs);
        long n2 = calculateN2(pairs);

        double numerator = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                numerator += (Math.signum(xArray[i] - xArray[j]) * Math.signum(yArray[i] - yArray[j]));
            }
        }
        final double nonTiedPairsMultiplied = (n0 - n1) * (double) (n0 - n2);
        return numerator / FastMath.sqrt(nonTiedPairsMultiplied);
    }

    /**
     * 高效计算向量x和y的τ_b关联系数，计算复杂度为O(n·log(n))。
     * Values of Tau-b range from −1 (100% negative association, or perfect inversion) to +1 (100% positive association,
     * or perfect agreement). A value of zero indicates the absence of association.
     *
     * @param vectorX 向量x。
     * @param vectorY 向量y。
     * @return τ_b关联系数。
     */
    public static double efficientTauB(double[] vectorX, double[] vectorY) {
        return KENDALLS_CORRELATION.correlation(vectorX, vectorY);
    }

    /**
     * 计算τ_a和τ_b的辅助函数：根据输入向量生成排序好的数据对。
     *
     * @param xArray 向量x。
     * @param yArray 向量y。
     * @return 排序数据对。
     */
    private static ArrayList<Pair<Double, Double>> getSortPairs(double[] xArray, double[] yArray) {
        int n = xArray.length;
        ArrayList<Pair<Double, Double>> pairs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            pairs.add(new Pair<>(xArray[i], yArray[i]));
        }

        return pairs.stream().sorted(Comparator.comparingDouble((ToDoubleFunction<Pair<Double, Double>>) Pair::getFirst)
            .thenComparingDouble(Pair::getSecond)).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 计算τ_a和τ_b的辅助函数：计算并返回n_1。
     * n_1 = Σ_i(t_i * (t_i - 1) / 2), where t_i is the number of tied values in the i-th group of ties for x.
     *
     * @param sortPairs 排序数据对。
     * @return n_1的值。
     */
    private static long calculateN1(final ArrayList<Pair<Double, Double>> sortPairs) {
        long n1 = 0;
        long consecutiveXties = 1;
        Pair<Double, Double> prev = sortPairs.get(0);
        for (int i = 1; i < sortPairs.size(); i++) {
            final Pair<Double, Double> curr = sortPairs.get(i);
            if (curr.getFirst().equals(prev.getFirst())) {
                consecutiveXties++;
            } else {
                n1 += sum(consecutiveXties - 1);
                consecutiveXties = 1;
            }
            prev = curr;
        }
        n1 += sum(consecutiveXties - 1);

        return n1;
    }

    /**
     * 计算τ_a和τ_b的辅助函数：计算并返回n_2。
     * n_2 = Σ_j(u_j * (u_j - 1) / 2), where u_j is the number of tied values in the j-th group of ties for y.
     *
     * @param sortPairs 排序数据对。
     * @return n_2的值。
     */
    private static long calculateN2(final ArrayList<Pair<Double, Double>> sortPairs) {
        long n2 = 0;
        long consecutiveYTies = 1;
        Pair<Double, Double> prev = sortPairs.get(0);
        for (int i = 1; i < sortPairs.size(); i++) {
            final Pair<Double, Double> curr = sortPairs.get(i);
            if (curr.getSecond().equals(prev.getSecond())) {
                consecutiveYTies++;
            } else {
                n2 += sum(consecutiveYTies - 1);
                consecutiveYTies = 1;
            }
            prev = curr;
        }
        n2 += sum(consecutiveYTies - 1);

        return n2;
    }

    /**
     * 计算τ_a和τ_b的辅助函数：计算并返回n_3。
     *
     * @param sortPairs 排序数据对。
     * @return n_3的值。
     */
    private static long calculateN3(final ArrayList<Pair<Double, Double>> sortPairs) {
        long n3 = 0;
        long consecutiveXYTies = 1;
        Pair<Double, Double> prev = sortPairs.get(0);
        for (int i = 1; i < sortPairs.size(); i++) {
            final Pair<Double, Double> curr = sortPairs.get(i);
            if (curr.getFirst().equals(prev.getFirst())) {
                if (curr.getSecond().equals(prev.getSecond())) {
                    consecutiveXYTies++;
                } else {
                    consecutiveXYTies = 1;
                }
            } else {
                n3 += sum(consecutiveXYTies - 1);
                consecutiveXYTies = 1;
            }
            prev = curr;
        }
        n3 += sum(consecutiveXYTies - 1);

        return n3;
    }

    /**
     * 计算τ_a和τ_b的辅助函数：计算并返回交换次数。
     *
     * @param sortPairs 排序数据对。
     * @return 交换次数。
     */
    private static long calculateSwaps(ArrayList<Pair<Double, Double>> sortPairs) {
        long swaps = 0;
        int n = sortPairs.size();
        @SuppressWarnings("unchecked")
        Pair<Double, Double>[] pairsDestination = new Pair[n];
        for (int segmentSize = 1; segmentSize < n; segmentSize <<= 1) {
            for (int offset = 0; offset < n; offset += 2 * segmentSize) {
                int i = offset;
                final int iEnd = FastMath.min(i + segmentSize, n);
                int j = iEnd;
                final int jEnd = FastMath.min(j + segmentSize, n);

                int copyLocation = offset;
                while (i < iEnd || j < jEnd) {
                    if (i < iEnd) {
                        if (j < jEnd) {
                            if (sortPairs.get(i).getSecond().compareTo(sortPairs.get(j).getSecond()) <= 0) {
                                pairsDestination[copyLocation] = sortPairs.get(i);
                                i++;
                            } else {
                                pairsDestination[copyLocation] = sortPairs.get(j);
                                j++;
                                swaps += iEnd - i;
                            }
                        } else {
                            pairsDestination[copyLocation] = sortPairs.get(i);
                            i++;
                        }
                    } else {
                        pairsDestination[copyLocation] = sortPairs.get(j);
                        j++;
                    }
                    copyLocation++;
                }
            }
            @SuppressWarnings("unchecked") final Pair<Double, Double>[] pairsTemp = sortPairs.toArray(new Pair[0]);
            sortPairs = Arrays.stream(pairsDestination).collect(Collectors.toCollection(ArrayList::new));
            pairsDestination = pairsTemp;
        }

        return swaps;
    }

    /**
     * 计算τ_a和τ_b的辅助函数：Returns the sum of the number from 1 .. n according to Gauss' summation formula:
     * 1 + 2 + ... + n = n * (n + 1) / 2.
     *
     * @param n the summation end.
     * @return the sum of the number from 1 to n.
     */
    private static long sum(long n) {
        return n * (n + 1) / 2L;
    }

    /**
     * 直接计算向量x和y的τ_rn关联系数，计算复杂度为O(n^2)。
     *
     * @param xArray 向量x。
     * @param yArray 向量y。
     * @return τ_rn关联系数。
     */
    public static double directTauRn(double[] xArray, double[] yArray) {
        if (xArray.length != yArray.length) {
            throw new DimensionMismatchException(xArray.length, yArray.length);
        }
        final int n = xArray.length;
        assert n > 1 : "data array length must be greater than 1";
        // 计算分母n0 = ((n + 1) * n * (n - 1) / 6)
        long n0 = squareSum(n - 1) / 2;
        // 获得排序值
        int[] rowNumbers = RankUtils.rowNumber(xArray);
        // 计算分子
        double numerator = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                numerator += (Math.signum(xArray[i] - xArray[j])
                    * Math.signum(yArray[i] - yArray[j])
                    * Math.abs(rowNumbers[i] - rowNumbers[j])
                );
            }
        }

        return numerator / n0;
    }

    /**
     * 计算τ_rn的辅助函数：Returns the sum of the number 1 * 2 + 2 * 3 + ... + n * (n + 1) = n * (n + 1) * (n + 2) / 3。
     *
     * @param n the summation end.
     * @return the sum of the number 1 * 2 + 2 * 3 + ... + n * (n + 1).
     */
    private static long squareSum(long n) {
        return n * (n + 1) * (n + 2) / 3L;
    }

    /**
     * 直接计算向量x和y的τ_dr关联系数，计算复杂度为O(n^2)。
     *
     * @param xArray 向量x。
     * @param yArray 向量y。
     * @return τ_dr关联系数。
     */
    public static double directTauDr(double[] xArray, double[] yArray) {
        if (xArray.length != yArray.length) {
            throw new DimensionMismatchException(xArray.length, yArray.length);
        }
        final int n = xArray.length;
        assert n > 1 : "data array length must be greater than 1";
        int[] denseRanks = RankUtils.denseRank(xArray);
        // 计算分子
        double numerator = IntStream.range(0, n).parallel()
            .mapToDouble(i -> {
                double eachNumerator = 0;
                for (int j = 0; j < i; j++) {
                    eachNumerator += (Math.signum(xArray[i] - xArray[j])
                        * Math.signum(yArray[i] - yArray[j])
                        * Math.abs(denseRanks[i] - denseRanks[j])
                    );
                }
                return eachNumerator;
            }).sum();
        // 分子分母都要通过循环计算
        double denominator = IntStream.range(0, n).parallel()
            .mapToDouble(i -> {
                double eachDenominator = 0;
                for (int j = 0; j < i; j++) {
                    eachDenominator += Math.abs(denseRanks[i] - denseRanks[j]);
                }
                return eachDenominator;
            }).sum();
        return numerator / denominator;
    }

    /**
     * 直接计算向量x和y的τ_d关联系数，计算复杂度为O(n^2)。
     *
     * @param xArray 向量x。
     * @param yArray 向量y。
     * @return τ_d关联系数。
     */
    public static double directTauD(double[] xArray, double[] yArray) {
        if (xArray.length != yArray.length) {
            throw new DimensionMismatchException(xArray.length, yArray.length);
        }
        final int n = xArray.length;
        assert n > 1 : "data array length must be greater than 1";
        // 分子分母都要通过循环计算
        long denominator = 0;
        // 计算分子
        double numerator = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                numerator += (Math.signum(xArray[i] - xArray[j])
                    * Math.signum(yArray[i] - yArray[j])
                    * Math.abs(xArray[i] - xArray[j])
                );
                denominator += Math.abs(xArray[i] - xArray[j]);
            }
        }

        return numerator / denominator;
    }
}
