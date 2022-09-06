package edu.alibaba.mpc4j.common.tool.benes;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.IntStream;

/**
 * 贝奈斯网络工具类。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public class BenesNetworkUtils {
    /**
     * 私有构造函数
     */
    private BenesNetworkUtils() {
        // empty
    }

    /**
     * 验证置换表。
     *
     * @param permutationMap 置换表。
     */
    public static boolean validPermutation(int[] permutationMap) {
        // 置换表至少要包含2个元素
        if (permutationMap.length <= 1) {
            return false;
        }
        int n = permutationMap.length;
        // 置换表中每一个索引值都应满足0 <= index < n
        for (int index : permutationMap) {
            if (index < 0 || index >= n) {
                return false;
            }
        }
        // 去重后的索引值数量应为n
        long distinctNum = Arrays.stream(permutationMap).distinct().count();
        return distinctNum == n;
    }

    /**
     * 返回贝奈斯网络层数。
     *
     * @param n 置换表包含的元素数量。
     * @return 贝奈斯网络层数。
     */
    public static int getLevel(int n) {
        assert n > 1;
        return 2 * LongUtils.ceilLog2(n) - 1;
    }

    /**
     * 返回贝奈斯网络宽度。
     *
     * @param n 置换表包含的元素数量。
     * @return 贝奈斯网络宽度。
     */
    public static int getWidth(int n) {
        assert n > 1;
        return n / 2;
    }

    /**
     * 根据给定的置换表置换输入向量。
     *
     * @param permutationMap 置换表。
     * @param inputVector    输入向量。
     * @return 输出向量。
     */
    public static <T> Vector<T> permutation(int[] permutationMap, Vector<T> inputVector) {
        assert validPermutation(permutationMap);
        assert permutationMap.length == inputVector.size();
        int n = permutationMap.length;
        // 构建输入到输出的完整映射表
        Map<Integer, Integer> map = new HashMap<>(n);
        IntStream.range(0, n).forEach(permuteIndex -> {
            int index = permutationMap[permuteIndex];
            map.put(index, permuteIndex);
        });
        Vector<T> outputVector = new Vector<>(inputVector);
        IntStream.range(0, n).forEach(position -> outputVector.set(map.get(position), inputVector.elementAt(position)));

        return outputVector;
    }
}
