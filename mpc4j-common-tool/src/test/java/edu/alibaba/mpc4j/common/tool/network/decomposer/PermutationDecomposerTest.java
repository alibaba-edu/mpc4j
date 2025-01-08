package edu.alibaba.mpc4j.common.tool.network.decomposer;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.network.decomposer.PermutationDecomposerFactory.DecomposerType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * permutation decomposer test.
 *
 * @author Weiran Liu
 * @date 2024/3/28
 */
@RunWith(Parameterized.class)
public class PermutationDecomposerTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // CGP20
        configurations.add(new Object[]{DecomposerType.CGP20.name(), DecomposerType.CGP20});
        // LLL24
        configurations.add(new Object[]{DecomposerType.LLL24.name(), DecomposerType.LLL24});

        return configurations;
    }

    /**
     * type
     */
    private final DecomposerType type;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public PermutationDecomposerTest(String name, DecomposerType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        secureRandom = new SecureRandom();
    }

    @Test
    public void test2() {
        int[] pi = new int[]{1, 0};
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
        int[] pi = PermutationNetworkUtils.randomPermutation(1 << 18, secureRandom);
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
        PermutationDecomposer decomposer = PermutationDecomposerFactory.createComposer(type, n, t);
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
        PermutationDecomposer decomposer = PermutationDecomposerFactory.createComposer(type, n, t);
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
