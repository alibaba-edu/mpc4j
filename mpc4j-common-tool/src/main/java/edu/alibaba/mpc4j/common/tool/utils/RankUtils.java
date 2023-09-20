package edu.alibaba.mpc4j.common.tool.utils;

import smile.math.MathEx;
import smile.sort.QuickSort;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 排序工具类。
 *
 * @author Weiran Liu
 * @date 2021/08/10
 */
public class RankUtils {

    private RankUtils() {
        // empty
    }

    /**
     * 返回输入数组的序号，序号为不重复排序。例如：
     * 输入数据为[10.0, 5.9, 8.8, 0.2, 5.5, 8.8]，输出结果为[5, 2, 3, 0, 1, 4]（即两个8.8有不同的序号）。
     * 调用此函数不会改变原数组。
     *
     * @param array 输入数据。
     * @return 不重复排序的序号。
     */
    public static int[] rowNumber(final double[] array) {
        assert array.length > 0 : "array length must be greater than 0";
        if (array.length == 1) {
            return new int[]{0};
        }
        double[] copyArray = DoubleUtils.clone(array);
        // 如果输入数据为[10.0, 5.9, 8.8, 0.2, 5.5, 8.8]，下面函数的输出结果为[3, 4, 1, 2, 5, 0]，即order[0]表示最小数所在的索引
        int[] order = QuickSort.sort(copyArray);
        // 为了变成正确的排序，需要构造"反查表"，同时处理数值类型取值结果相同的数据
        Map<Integer, Integer> orderMap = new HashMap<>(array.length);
        IntStream.range(0, array.length).forEach(index -> orderMap.put(order[index], index));
        // 根据反查表设置编码值
        return IntStream.range(0, array.length).map(orderMap::get).toArray();
    }

    /**
     * 返回输入数组的序号，序号可以重复，且不会跳。例如：
     * 输入数据为[10.0, 5.9, 8.8, 0.2, 5.5, 8.8]，输出结果为[4, 2, 3, 0, 1, 3]（即两个8.8有相同的序号，且10.0的序号挨着8.8）。
     *
     * @param array 输入数据。
     * @return 可重复、不会跳的序号。
     */
    public static int[] denseRank(final double[] array) {
        assert array.length > 0 : "array length must be greater than 0";
        if (array.length == 1) {
            return new int[]{0};
        }
        double[] copyArray = DoubleUtils.clone(array);
        // 如果输入数据为[10.0, 5.9, 8.8, 0.2, 5.5, 8.8]，下面函数的输出结果为[3, 4, 1, 2, 5, 0]，即order[0]表示最小数所在的索引
        int[] order = QuickSort.sort(copyArray);
        // 为了变成正确的排序，需要构造"反查表"，同时处理数值类型取值结果相同的数据
        Map<Integer, Integer> orderMap = new HashMap<>(array.length);
        int updateIndex = 0;
        double previousData = array[order[0]];
        // 先将第0个映射结果放进来
        orderMap.put(order[0], updateIndex);
        // 处理后面的数据
        for (int index = 1; index < array.length; index++) {
            double currentData = array[order[index]];
            // 如果相等，则updateIndex不变，如果不相等，则updateOrderIndex++
            if (!MathEx.isZero(currentData - previousData, DoubleUtils.PRECISION)) {
                updateIndex++;
            }
            previousData = currentData;
            orderMap.put(order[index], updateIndex);
        }
        // 根据反查表设置编码值
        return IntStream.range(0, array.length).map(orderMap::get).toArray();
    }

    /**
     * 返回输入数组的序号，序号可以重复，且会跳。例如：
     * 输入数据为[10.0, 5.9, 8.8, 0.2, 5.5, 8.8]，输出结果为[5, 2, 3, 0, 1, 3]（即两个8.8有相同的序号，且10.0的序号不挨着8.8）。
     *
     * @param array 输入数据。
     * @return 可重复、会跳的序号。
     */
    public static int[] rank(final double[] array) {
        assert array.length > 0 : "array length must be greater than 0";
        if (array.length == 1) {
            return new int[]{0};
        }
        double[] copyArray = DoubleUtils.clone(array);
        // 如果输入数据为[10.0, 5.9, 8.8, 0.2, 5.5, 8.8]，下面函数的输出结果为[3, 4, 1, 2, 5, 0]，即order[0]表示最小数所在的索引
        int[] order = QuickSort.sort(copyArray);
        // 为了变成正确的排序，需要构造"反查表"，同时处理数值类型取值结果相同的数据
        Map<Integer, Integer> orderMap = new HashMap<>(array.length);
        int updateIndex = 0;
        int cumulateIndex = 0;
        double previousData = array[order[0]];
        // 先将第0个映射结果放进来
        orderMap.put(order[0], updateIndex);
        // 处理后面的数据
        for (int index = 1; index < array.length; index++) {
            double currentData = array[order[index]];
            if (MathEx.isZero(currentData - previousData, DoubleUtils.PRECISION)) {
                // 如果相等，则updateIndex不变，cumulateIndex++
                cumulateIndex++;
            } else {
                // 如果不等，则updateIndex++，且则updateIndex += cumulateIndex，cumulateIndex重置为0
                updateIndex += (cumulateIndex + 1);
                cumulateIndex = 0;
            }
            previousData = currentData;
            orderMap.put(order[index], updateIndex);
        }
        // 根据反查表设置编码值
        return IntStream.range(0, array.length).map(orderMap::get).toArray();
    }
}
