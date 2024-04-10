package edu.alibaba.mpc4j.common.tool.network;

import com.google.common.math.IntMath;
import org.junit.Assert;
import org.junit.Test;

import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * permutation decomposer test.
 *
 * @author Weiran Liu
 * @date 2024/3/28
 */
public class PermutationDecomposerTest {

    @Test
    public void testN2Example() {
        int[] permutation = new int[] {1, 0};
        testExample(permutation);
    }

    @Test
    public void testN8Example() {
        int[] permutation = new int[]{1, 7, 3, 5, 2, 0, 4, 6};
        testExample(permutation);
    }

    private void testExample(int[] permutation) {
        int n = permutation.length;
        assert IntMath.isPowerOfTwo(n);
        int logN = Integer.numberOfTrailingZeros(n);
        for (int logT = 1; logT <= logN; logT++) {
            int t = 1 << logT;
            testExample(permutation, t);
        }
    }

    private void testExample(int[] permutation, int t) {
        int n = permutation.length;
        Vector<Integer> inputVector = IntStream.range(0, n)
            .boxed().
            collect(Collectors.toCollection(Vector::new));
        PermutationDecomposer decomposer = new PermutationDecomposer(permutation, t);
        Vector<Integer> actualOutputVector = new Vector<>(inputVector);
        int[][][] splitGroups = decomposer.getSplitGroups();
        int[][][] subPermutations = decomposer.getSubPermutations();
        for (int i = 0; i < decomposer.getD(); i++) {
            for (int j = 0; j < decomposer.getSubNum(); j++) {
                actualOutputVector = PermutationNetworkUtils.permutation(splitGroups[i][j], subPermutations[i][j], actualOutputVector);
            }
        }
        Vector<Integer> expectOutputVector = PermutationNetworkUtils.permutation(permutation, inputVector);
        Assert.assertEquals(expectOutputVector, actualOutputVector);
    }
}
