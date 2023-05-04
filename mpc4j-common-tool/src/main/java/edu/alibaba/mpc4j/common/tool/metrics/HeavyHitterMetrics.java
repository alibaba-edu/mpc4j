package edu.alibaba.mpc4j.common.tool.metrics;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.math3.util.Precision;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HeavyHitter Metrics.
 *
 * @author Weiran Liu
 * @date 2022/11/17
 */
public class HeavyHitterMetrics {

    private HeavyHitterMetrics() {
        // empty
    }

    /**
     * Calculates the NDCG measure on the prediction list based on the real list.
     * <p>
     * 有关NDCG的解释，参见https://zhuanlan.zhihu.com/p/84206752。
     * </p>
     * <p>
     * NDCG（Normalized Discounted Cumulative Gain，归一化折损累计增益）用作排序结果的评价指标，评价排序的准确性。
     * 推荐系统通常为某用户返回一个item列表，假设列表长度为K，这时可以用NDCG评价该排序列表与用户真实交互列表的差距。
     * </p>
     * <ul>
     *     <li> Gain：表示列表中每一个item的相关性分数，第i个item的相关性分数定义为Gain = rel(i)。</li>
     *     <li> Cumulative Gain：表示对K个item的Gain进行累加：CG = \sum_{i}^{K} {rel(i)}</li>
     *     <li> Discounted Cumulative Gain：考虑排序顺序的因素，使得排名靠前的item增益更高，对排名靠后的item进行折损：
     *     DCG = \sum_{i}^{K} {\frac{rel(i)} / log_2(i + 1)}</li>
     *     <li> Normalized Discounted Cumulative Gain：计算每个用户真实列表的DCG分数，用IDCG表示，然后用每个用户的DCG与IDCG之比作为每个
     *     用户归一化后的分值，最后对每个用户取平均得到最终的分值，即NDCG。NDCG = DCG / IDCG。</li>
     * </ul>
     * <p>
     * Given k-item prediction list and k-item real list, for the i-th item in the prediction list, denote its index in
     * the real list is i'. Then, for the i-th item in the prediction list:
     * <ul>
     *     <li> If the item is not in the real list, then reg(i) = 0. </li>
     *     <li> If the item is in the real list, then reg(i) = k - |i - i'|. </li>
     *     <li> The ireg (ireg) for the i'-th item in the real list is ireg(i') = k. (</li>
     * </ul>
     * </p>
     *
     * @param predictionList the prediction list.
     * @param realList       the real list.
     * @param <T>            the data type.
     * @return the NDCG.
     */
    public static <T> double ndcg(List<T> predictionList, List<T> realList) {
        MathPreconditions.checkEqual("prediction list", "real list", predictionList.size(), realList.size());
        int k = predictionList.size();
        double idcg = idcg(k);
        // k = 0时，idcg = 0
        if (Precision.equals(idcg, 0, DoubleUtils.PRECISION)) {
            return 0;
        }
        double dcg = 0;
        for (int index = 0; index < k; index++) {
            T predictItem = predictionList.get(index);
            if (!realList.contains(predictItem)) {
                // If the item is not in the real list, then reg(i) = 0.
                continue;
            }
            int i = index + 1;
            int j = realList.indexOf(predictItem);
            int rel = k - Math.abs(index - j);
            dcg += rel * Math.log(2) / Math.log(i + 1);
        }
        return dcg / idcg;
    }

    /**
     * Calculate IDCG (Ideal Discounted Cumulative Gain), with the idcg = k for the i'-th item in the real list.
     *
     * @param k the number of items.
     * @return IDCG.
     */
    private static double idcg(int k) {
        double idcg = 0.0;
        for (int i = 1; i <= k; i++) {
            idcg += k * Math.log(2) / Math.log(i + 1);
        }
        return idcg;
    }

    /**
     * Calculates the precision measure on the prediction list based on the real list.
     * <p>
     * The precision is the number of items both in the precision list and in the real list, divided by k.
     * </p>
     *
     * @param predictionList the prediction list.
     * @param realList       the real list.
     * @param <T>            the data type.
     * @return the prediction.
     */
    public static <T> double precision(List<T> predictionList, List<T> realList) {
        Preconditions.checkArgument(
            predictionList.size() == realList.size(),
            "prediction list size = %s, real list size = %s, must be equal.",
            predictionList.size(), realList.size()
        );
        int k = predictionList.size();
        if (k == 0) {
            return 0;
        }
        Set<T> intersectionSet = new HashSet<>(realList);
        intersectionSet.retainAll(predictionList);

        return (double) intersectionSet.size() / k;
    }

    /**
     * Calculates the absolute error measure on the prediction map based on the real map.
     * <p>
     * the absolute error is computed by the sum of the absolute error abe(t) for each item t, where
     * abe(t) = |the prediction value for t - the real item value for t|.
     * </p>
     * Note that we traverse items t in the real item map. If we cannot find t in the prediction map, then
     * abe(t) = |the real item value for t|.
     *
     * @param predictionMap the prediction map.
     * @param realMap       the real map.
     * @param <T>           the data type.
     * @return the absolute error.
     */
    public static <T> double absoluteError(Map<T, Double> predictionMap, Map<T, Integer> realMap) {
        MathPreconditions.checkEqual("prediction size", "real size", predictionMap.size(), realMap.size());
        int size = predictionMap.size();
        if (size == 0) {
            return 0;
        }
        double absoluteError = 0;
        for (Map.Entry<T, Integer> itemEntry : realMap.entrySet()) {
            T item = itemEntry.getKey();
            double prediction = predictionMap.getOrDefault(item, 0.0);
            int real = itemEntry.getValue();
            absoluteError += Math.abs(prediction - real);
        }
        return absoluteError / size;
    }

    /**
     * Calculates the relative error measure on the prediction map based on the real map.
     * <p>
     * the relative error is computed by the sum of the relative error re(t) for each item t, where
     * rt(t) = |the prediction value for t - the real item value for t| / the real item value for t.
     * </p>
     * Note that we traverse items t in the real item map. Some special cases:
     * <ul>
     *     <li> If there is no t in the prediction map, then we set re(t) = 1. </li>
     *     <li> If the real item value for t equals 0, then we set re(t) = 1. </li>
     *     <li> The prediction item value for t can be negative. </li>
     * </ul>
     *
     * @param predictionMap the prediction map.
     * @param realMap       the real map.
     * @param <T>           the data type.
     * @return the relative error.
     */
    public static <T> double relativeError(Map<T, Double> predictionMap, Map<T, Integer> realMap) {
        MathPreconditions.checkEqual("prediction size", "real size", predictionMap.size(), realMap.size());
        int size = predictionMap.size();
        if (size == 0) {
            return 0;
        }
        double relativeError = 0;
        for (Map.Entry<T, Integer> itemEntry : realMap.entrySet()) {
            T item = itemEntry.getKey();
            int real = itemEntry.getValue();
            // the real value must be positive.
            MathPreconditions.checkPositive("real value", real);
            if (!predictionMap.containsKey(item)) {
                relativeError += 1;
            } else {
                double prediction = predictionMap.get(item);
                relativeError += Math.abs(prediction - real) / real;
            }
        }
        return relativeError / size;
    }
}
