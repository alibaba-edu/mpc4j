package edu.alibaba.mpc4j.common.tool.network.waksman;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.network.waksman.WaksmanNetworkFactory.WaksmanNetworkType;
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
 * Waksman network unit test. The test case comes from the example given by:
 * <p>
 * B. Beauquier, E. Darrot. On arbitrary Waksman networks and their vulnerability. Parallel Processing Letters, 12: 287-296.
 * </p>
 * We note that the example shown in the above paper (Figure 9) is not correct. Specifically, one can compare the
 * network with the original Benes, then one may find that the connection between each node is not the same. The example
 * shown in the following paper (Figure 4) is correct.
 * <p>
 * W. Holland, O. Ohrimenko, A. Wirth. Efficient Oblivious Permutation via the Waksman Network. ASIACCS 2022, pp. 771-783.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/3/21
 */
@RunWith(Parameterized.class)
public class WaksmanNetworkTest {
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // JDK
        configurations.add(new Object[]{WaksmanNetworkType.JDK.name(), WaksmanNetworkType.JDK,});
        // Native
        configurations.add(new Object[] {WaksmanNetworkType.NATIVE.name(), WaksmanNetworkType.NATIVE,});

        return configurations;
    }

    /**
     * type
     */
    private final WaksmanNetworkType type;

    public WaksmanNetworkTest(String name, WaksmanNetworkType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testWaksmanType() {
        int[] permutationMap = IntStream.range(0, 9).toArray();
        WaksmanNetwork<Integer> network = WaksmanNetworkFactory.createInstance(type, permutationMap);
        Assert.assertEquals(type, network.getWaksmanType());
    }

    @Test
    public void testNetworkExample() {
        /* the example is the ASIACCS 2022 paper. The permutation map and the network is given in Figure 4.
         * ( 0 1 2 3 4 5 6 7 8 )
         * ( 8 2 1 5 0 7 3 4 6 )
         * This means 0-th element is to 8-th position, 1-th element is to 2-th position, et al. Therefore, the actual
         * permutation is (4, 2, 1, 6, 7, 3, 8, 5, 0).
         * When given    u = (0, 1, 2, 3, 4, 5, 6, 7, 8) as input, u[0] is to v[8], u[1] is to v[2], et al.
         * The output is v = (4, 2, 1, 6, 7, 3, 8, 5, 0).
         */
        int n = 9;
        byte[][] network = new byte[][]{
            new byte[]{1, 0, 1, 1},
            new byte[]{1, 1, 1, 0},
            new byte[]{2, 2, 2, 1},
            new byte[]{0, 0, 1, 1},
            new byte[]{2, 2, 2, 0},
            new byte[]{0, 2, 0, 0},
            new byte[]{1, 0, 0, 1},
        };
        WaksmanNetwork<Integer> waksmanNetwork = WaksmanNetworkFactory.createInstance(type, n, network);
        Vector<Integer> inputVector = IntStream.range(0, n)
            .boxed()
            .collect(Collectors.toCollection(Vector::new));
        Vector<Integer> outputVector = waksmanNetwork.permutation(inputVector);
        int[] output = outputVector.stream().mapToInt(i -> i).toArray();
        Assert.assertArrayEquals(new int[]{4, 2, 1, 6, 7, 3, 8, 5, 0}, output);
    }

    @Test
    public void testExample() {
        /* the example is the ASIACCS 2022 paper. The permutation map and the network is given in Figure 4.
         * ( 8 2 1 5 0 7 3 4 6 )
         * When given    u = (0, 1, 2, 3, 4, 5, 6, 7, 8) as input,
         * the output is v = (8, 2, 1, 5, 0, 7, 3, 4, 6), which is the same as the permutation map).
         */
        int n = 9;
        int[] permutationMap = new int[]{8, 2, 1, 5, 0, 7, 3, 4, 6};
        WaksmanNetwork<Integer> network = WaksmanNetworkFactory.createInstance(type, permutationMap);
        Vector<Integer> inputVector = IntStream.range(0, n)
            .boxed().
            collect(Collectors.toCollection(Vector::new));
        Vector<Integer> outputVector = network.permutation(inputVector);
        int[] output = outputVector.stream().mapToInt(i -> i).toArray();
        Assert.assertArrayEquals(new int[]{8, 2, 1, 5, 0, 7, 3, 4, 6}, output);
        // the correct widths
        int[] expectWidths = new int[]{4, 4, 1, 4, 1, 3, 4};
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
        testSwitchCount(4, 5);
        testSwitchCount(5, 8);
        testSwitchCount(6, 11);
        testSwitchCount(7, 14);
        testSwitchCount(8, 17);
        testSwitchCount(9, 21);
        testSwitchCount(10, 25);
        testSwitchCount(11, 29);
        testSwitchCount(12, 33);
        testSwitchCount(13, 37);
        testSwitchCount(14, 41);
        testSwitchCount(15, 45);
        testSwitchCount(16, 49);
        testSwitchCount(32, 129);
    }

    private void testSwitchCount(int n, int expectSwitchCount) {
        List<Integer> shufflePermutationMap = IntStream.range(0, n).boxed().collect(Collectors.toList());
        Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
        int[] permutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
        WaksmanNetwork<Integer> network = WaksmanNetworkFactory.createInstance(type, permutationMap);
        Assert.assertEquals(expectSwitchCount, network.getSwitchCount());
    }
}
