package edu.alibaba.mpc4j.s2pc.opf.osorter.quick;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.IntStream;

/**
 * utilities for quick sort
 *
 * @author Feng Han
 * @date 2024/9/29
 */
public class QuickSortUtils {

    /**
     * re-order the indexes based on pivot comparison result
     *
     * @param range [from, to]
     * @param pivotRank the rank of chosen pivot
     * @return [[rank determined in pivot chosen process], [index to be compared]]
     */
    public static int[][] moveIndex(int[] range, int[] pivotRank){
        int[] all = IntStream.range(range[0], range[1] + 1).toArray();
        int[] partRank = new int[all.length];
        int threshold = pivotRank.length;
        if(threshold > 1){
            for(int j = 0, end = all.length - 1; j < pivotRank.length / 2; j++, end--){
                partRank[j] = pivotRank[j];
                partRank[end] = pivotRank[pivotRank.length - 1 - j];
            }
        }
        HashSet<Integer> set = new HashSet<>();
        Arrays.stream(pivotRank).forEach(set::add);
        int index2Change = 0;
        for (int j : pivotRank) {
            if (j - range[0] >= threshold) {
                // 需要找一个数swap
                while (set.contains(all[index2Change])) {
                    index2Change++;
                }
                swap(all, index2Change, j - range[0]);
                index2Change++;
            }
        }
        all[threshold - 1] = pivotRank[threshold / 2];
        return new int[][]{partRank, Arrays.copyOfRange(all, threshold - 1, all.length)};
    }

    public static void swap(int[] array, int i, int j){
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}
