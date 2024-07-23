package edu.alibaba.mpc4j.common.tool.network;

import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * permutation decomposer test.
 *
 * @author Weiran Liu
 * @date 2024/3/28
 */
public class PermutationDecomposerTest {

    @Test
    public void test2() {
        int[] pi = new int[] {1, 0};
        testSplitGroups(pi.length);
        testPermutation(pi);
    }

    @Test
    public void test8() {
        int[] pi = new int[]{1, 7, 3, 5, 2, 0, 4, 6};
        testSplitGroups(pi.length);
        testPermutation(pi);
    }

    @Test
    public void testLargeN() {
        int[] pi = IntStream.range(0, 1 << 18).toArray();
        ArrayUtils.shuffle(pi);
        int t = (1 << 8);
        testSplitGroups(pi.length);
        testPermutation(pi, t);
    }

    private void testSplitGroups(int n) {
        assert IntMath.isPowerOfTwo(n);
        int logN = Integer.numberOfTrailingZeros(n);
        for (int logT = 1; logT <= logN; logT++) {
            int t = 1 << logT;
            testSplitGroups(n, t);
        }
    }

    private void testSplitGroups(int n, int t) {
        byte[][] inputVector = IntStream.range(0, n)
            .mapToObj(i -> IntUtils.nonNegIntToFixedByteArray(i, CommonConstants.BLOCK_BYTE_LENGTH))
            .toArray(byte[][]::new);
        PermutationDecomposer decomposer = new PermutationDecomposer(n, t);
        byte[][] outputVector = BytesUtils.clone(inputVector);
        for (int i = 0; i < decomposer.getD(); i++) {
            byte[][][] groups = decomposer.splitVector(inputVector, i);
            outputVector = decomposer.combineGroups(groups, i);
        }
        Assert.assertArrayEquals(inputVector, outputVector);
    }

    private void testPermutation(int[] permutation) {
        int n = permutation.length;
        assert IntMath.isPowerOfTwo(n);
        int logN = Integer.numberOfTrailingZeros(n);
        for (int logT = 1; logT <= logN; logT++) {
            int t = 1 << logT;
            testPermutation(permutation, t);
        }
    }

    private void testPermutation(int[] permutation, int t) {
        int n = permutation.length;
        byte[][] inputVector = IntStream.range(0, n)
            .mapToObj(i -> IntUtils.nonNegIntToFixedByteArray(i, CommonConstants.BLOCK_BYTE_LENGTH))
            .toArray(byte[][]::new);
        PermutationDecomposer decomposer = new PermutationDecomposer(n, t);
        decomposer.setPermutation(permutation);
        byte[][] splitOutputVector = BytesUtils.clone(inputVector);
        byte[][] combineOutputVector = BytesUtils.clone(inputVector);
        for (int i = 0; i < decomposer.getD(); i++) {
            // permutation by groups
            byte[][][] inputGroups = decomposer.splitVector(splitOutputVector, i);
            byte[][][] outputGroups = decomposer.permutation(inputGroups, i);
            splitOutputVector = decomposer.combineGroups(outputGroups, i);
            // permutation by vector
            combineOutputVector = decomposer.permutation(combineOutputVector, i);
        }
        byte[][] expectOutputVector = PermutationNetworkUtils.permutation(permutation, inputVector);
        Assert.assertArrayEquals(expectOutputVector, splitOutputVector);
        Assert.assertArrayEquals(expectOutputVector, combineOutputVector);
    }
}
