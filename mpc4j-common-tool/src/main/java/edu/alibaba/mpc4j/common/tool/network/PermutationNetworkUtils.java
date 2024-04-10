package edu.alibaba.mpc4j.common.tool.network;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

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
     * Permutes the input vector by for the given positions and the given permutation. For example, if positions =
     * [0, 2, 4, 6] and permutation = [3, 1, 2, 0], then we set:
     * <li>the 6-th element to the 0-th position.</li>
     * <li>the 2-th element to the 2-th position.</li>
     * <li>the 4-th element to the 4-th position.</li>
     * <li>the 0-th element to the 6-th position.</li>
     *
     * @param positions   positions.
     * @param permutation permutation.
     * @param inputVector input vector.
     * @param <T>         input type.
     * @return the permuted input vector.
     */
    public static <T> Vector<T> permutation(int[] positions, int[] permutation, Vector<T> inputVector) {
        int subN = permutation.length;
        assert positions.length == subN;
        Vector<T> subInputVector = new Vector<>(subN);
        for (int i = 0; i < subN; i++) {
            subInputVector.add(inputVector.get(positions[i]));
        }
        Vector<T> subOutputVector = PermutationNetworkUtils.permutation(permutation, subInputVector);
        Vector<T> outputVector = new Vector<>(inputVector);
        for (int i = 0; i < subN; i++) {
            outputVector.set(positions[i], subOutputVector.get(i));
        }

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
