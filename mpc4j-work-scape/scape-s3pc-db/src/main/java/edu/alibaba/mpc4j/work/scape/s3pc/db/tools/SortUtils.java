package edu.alibaba.mpc4j.work.scape.s3pc.db.tools;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Utils for sorting
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class SortUtils {
    /**
     * sort the BigInteger array, and return the permutation representing the sort order,
     * such that the i-th smallest element is the x[pai[i]]
     *
     * @param x the input array
     * @return the permutation
     */
    public static int[] getPermutation(BigInteger[] x) {
        // 获取输入数组的长度
        int n = x.length;

        // 创建一个索引数组，初始值为 [0, 1, 2, ..., n-1]
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        // 根据 x 的值对索引数组进行排序
        Arrays.sort(indices, Comparator.comparing(i -> x[i]));

        return Arrays.stream(indices).mapToInt(Integer::intValue).toArray();
    }
}
