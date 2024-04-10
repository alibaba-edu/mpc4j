package edu.alibaba.mpc4j.common.tool.network.benes;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.network.benes.BenesNetworkFactory.BenesNetworkType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Benes network unit test. The test case comes from the example given by:
 * <p>
 * Chang C, Melhem R. Arbitrary size benes networks[J]. Parallel Processing Letters, 1997, 7(03): 279-284.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/3/20
 */
@RunWith(Parameterized.class)
public class BenesNetworkTest {
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // JDK
        configurations.add(new Object[]{BenesNetworkType.JDK.name(), BenesNetworkType.JDK,});
        // Native
        configurations.add(new Object[]{BenesNetworkType.NATIVE.name(), BenesNetworkType.NATIVE,});

        return configurations;
    }

    /**
     * type
     */
    private final BenesNetworkType type;

    public BenesNetworkTest(String name, BenesNetworkType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testBenesType() {
        int[] permutationMap = IntStream.range(0, 9).toArray();
        BenesNetwork<Integer> benesNetwork = BenesNetworkFactory.createInstance(type, permutationMap);
        Assert.assertEquals(type, benesNetwork.getBenesType());
    }

    @Test
    public void testNetworkExample() {
        /* This is the example from the paper. The permutation map is in Page 3, while the network is in Figure 5.
         * ( 0 1 2 3 4 5 6 7 8 )
         * ( 7 4 8 6 2 1 0 3 5 )
         * This means 0-th element is to 7-th position, 1-th element is to 4-th position. Therefore, the actual
         * permutation is (6, 5, 4, 7, 1, 8, 3, 0, 2).
         * When given    u = (0, 1, 2, 3, 4, 5, 6, 7, 8) as input, u[0] is to v[7], u[1] is to v[4], et al.
         * The output is v = (6, 5, 4, 7, 1, 8, 3, 0, 2).
         */
        int n = 9;
        byte[][] network = new byte[][]{
            new byte[]{1, 1, 0, 0},
            new byte[]{1, 0, 0, 0},
            new byte[]{2, 2, 2, 1},
            new byte[]{1, 1, 1, 1},
            new byte[]{2, 2, 2, 0},
            new byte[]{1, 1, 0, 1},
            new byte[]{0, 0, 0, 0},
        };
        BenesNetwork<Integer> benesNetwork = BenesNetworkFactory.createInstance(type, n, network);
        Vector<Integer> inputVector = IntStream.range(0, n)
            .boxed().
            collect(Collectors.toCollection(Vector::new));
        Vector<Integer> outputVector = benesNetwork.permutation(inputVector);
        int[] output = outputVector.stream().mapToInt(i -> i).toArray();
        Assert.assertArrayEquals(new int[]{6, 5, 4, 7, 1, 8, 3, 0, 2}, output);
    }

    @Test
    public void testExample() {
        /* This is the example from the paper. The permutation map is in Page 3, while the network is in Figure 5.
         * ( 7 4 8 6 2 1 0 3 5 )
         * When given    u = (0, 1, 2, 3, 4, 5, 6, 7, 8) as input,
         * the output is v = (7, 4, 8, 6, 2, 1, 0, 3, 5), which is the same as the permutation map.
         */
        int n = 9;
        int[] permutation = new int[]{7, 4, 8, 6, 2, 1, 0, 3, 5};
        BenesNetwork<Integer> network = BenesNetworkFactory.createInstance(type, permutation);
        Vector<Integer> inputVector = IntStream.range(0, n)
            .boxed().
            collect(Collectors.toCollection(Vector::new));
        Vector<Integer> outputVector = network.permutation(inputVector);
        int[] output = outputVector.stream().mapToInt(i -> i).toArray();
        Assert.assertArrayEquals(new int[]{7, 4, 8, 6, 2, 1, 0, 3, 5}, output);
        // the correct widths
        int[] expectWidths = new int[]{4, 4, 1, 4, 1, 4, 4};
        Assert.assertEquals(expectWidths.length, network.getLevel());
        for (int levelIndex = 0; levelIndex < network.getLevel(); levelIndex++) {
            Assert.assertEquals(expectWidths[levelIndex], network.getWidth(levelIndex));
        }
    }

    @Test
    public void testSwitchCount() {
        // the switch count table is from Table 1 of the [PPL02] paper.
        testSwitchCount(2, 1);
        testSwitchCount(3, 3);
        testSwitchCount(4, 6);
        testSwitchCount(5, 8);
        testSwitchCount(6, 12);
        testSwitchCount(7, 15);
        testSwitchCount(8, 20);
        testSwitchCount(9, 22);
        testSwitchCount(10, 26);
        testSwitchCount(11, 30);
        testSwitchCount(12, 36);
        testSwitchCount(13, 39);
        testSwitchCount(14, 44);
        testSwitchCount(15, 49);
        testSwitchCount(16, 56);
        testSwitchCount(32, 144);
    }

    private void testSwitchCount(int n, int expectSwitchCount) {
        List<Integer> shufflePermutationMap = IntStream.range(0, n).boxed().collect(Collectors.toList());
        Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
        int[] permutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
        BenesNetwork<Integer> network = BenesNetworkFactory.createInstance(type, permutationMap);
        Assert.assertEquals(expectSwitchCount, network.getSwitchCount());
    }
}
