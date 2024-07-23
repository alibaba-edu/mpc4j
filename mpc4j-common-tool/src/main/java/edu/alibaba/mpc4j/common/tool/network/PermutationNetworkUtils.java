package edu.alibaba.mpc4j.common.tool.network;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.ArrayUtils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.IntStream;

/**
 * permutation network utilities.
 *
 * @author Weiran Liu
 * @date 2024/3/20
 */
public class PermutationNetworkUtils {
    /**
     * private constructor.
     */
    private PermutationNetworkUtils() {
        // empty
    }

    /**
     * Generates a random permutation.
     *
     * @param num          num.
     * @param secureRandom random state.
     * @return a random permutation.
     */
    public static int[] randomPermutation(int num, SecureRandom secureRandom) {
        MathPreconditions.checkGreater("num", num, 1);
        int[] pi = IntStream.range(0, num).toArray();
        ArrayUtils.shuffle(pi, secureRandom);
        return pi;
    }

    /**
     * Verifies whether the given permutation map is valid.
     *
     * @param permutationMap the permutation.
     * @return true if it is a valid permutation map, false otherwise.
     */
    public static boolean validPermutation(int[] permutationMap) {
        // the permutation map must contain at least 2 elements.
        if (permutationMap.length <= 1) {
            return false;
        }
        int n = permutationMap.length;
        // each element in the permutation should be in range [0, n)
        for (int element : permutationMap) {
            if (element < 0 || element >= n) {
                return false;
            }
        }
        // the number of distinct elements is n, which means that elements enumerate [0, n)
        long distinctNum = Arrays.stream(permutationMap).distinct().count();
        return distinctNum == n;
    }

    /**
     * Permutes the input vector by the given permutation map. For example, if permutation = [3, 1, 2, 0], then we set:
     * <li>the 3-th element to the 0-th position.</li>
     * <li>the 1-th element to the 1-th position.</li>
     * <li>the 2-th element to the 2-th position.</li>
     * <li>the 0-th element to the 3-th position.</li>
     *
     * @param permutation the permutation map.
     * @param inputVector the input vector.
     * @param <T>         input type.
     * @return the permuted input vector.
     */
    public static <T> Vector<T> permutation(int[] permutation, Vector<T> inputVector) {
        assert validPermutation(permutation);
        assert permutation.length == inputVector.size();
        int n = permutation.length;
        // Creates the actual permutation map
        TIntIntMap map = new TIntIntHashMap(n);
        IntStream.range(0, n).forEach(inputPosition -> map.put(permutation[inputPosition], inputPosition));
        Vector<T> outputVector = new Vector<>(inputVector);
        IntStream.range(0, n).forEach(inputPosition ->
            outputVector.set(map.get(inputPosition), inputVector.elementAt(inputPosition))
        );

        return outputVector;
    }

    /**
     * Permutes the input vector by the given permutation map. For example, if permutation = [3, 1, 2, 0], then we set:
     * <li>the 3-th element to the 0-th position.</li>
     * <li>the 1-th element to the 1-th position.</li>
     * <li>the 2-th element to the 2-th position.</li>
     * <li>the 0-th element to the 3-th position.</li>
     *
     * @param permutation the permutation map.
     * @param inputVector the input vector.
     * @return the permuted input vector.
     */
    public static byte[][] permutation(int[] permutation, byte[][] inputVector) {
        assert validPermutation(permutation);
        assert permutation.length == inputVector.length;
        int n = permutation.length;
        // Creates the actual permutation map
        TIntIntMap map = new TIntIntHashMap(n);
        IntStream.range(0, n).forEach(inputPosition -> map.put(permutation[inputPosition], inputPosition));
        byte[][] outputVector = new byte[n][];
        IntStream.range(0, n).forEach(inputPosition ->
            outputVector[map.get(inputPosition)] = inputVector[inputPosition]
        );

        return outputVector;
    }

    /**
     * Permutes the input vector by the given permutation map. For example, if permutation = [3, 1, 2, 0], then we set:
     * <li>the 3-th element to the 0-th position.</li>
     * <li>the 1-th element to the 1-th position.</li>
     * <li>the 2-th element to the 2-th position.</li>
     * <li>the 0-th element to the 3-th position.</li>
     *
     * @param permutation the permutation map.
     * @param inputVector the input vector.
     * @return the permuted input vector.
     */
    public static int[] permutation(int[] permutation, int[] inputVector) {
        assert validPermutation(permutation);
        assert permutation.length == inputVector.length;
        int n = permutation.length;
        // Creates the actual permutation map
        TIntIntMap map = new TIntIntHashMap(n);
        IntStream.range(0, n).forEach(inputPosition -> map.put(permutation[inputPosition], inputPosition));
        int[] outputVector = new int[n];
        IntStream.range(0, n).forEach(inputPosition ->
            outputVector[map.get(inputPosition)] = inputVector[inputPosition]
        );

        return outputVector;
    }

    /**
     * Gets the number of levels in the permutation network.
     *
     * @param n the number of inputs.
     * @return the number of levels in the permutation network.
     */
    public static int getLevel(int n) {
        assert n > 1;
        return 2 * LongUtils.ceilLog2(n) - 1;
    }

    /**
     * Gets the maximal width in the permutation network.
     *
     * @param n the number of inputs.
     * @return the maximal width in the permutation network.
     */
    public static int getMaxWidth(int n) {
        assert n > 1;
        return n / 2;
    }
}
