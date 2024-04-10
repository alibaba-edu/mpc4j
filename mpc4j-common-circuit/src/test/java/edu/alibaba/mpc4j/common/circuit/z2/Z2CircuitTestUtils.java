package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import org.junit.Assert;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Z2 Circuit Test Utils.
 *
 * @author Li Peng
 * @date 2023/6/7
 */
public class Z2CircuitTestUtils {
    /**
     * asserts if the outputs are correct.
     *
     * @param operator operator.
     * @param l        bit length.
     * @param xs       input x.
     * @param ys       input y.
     * @param zs       output z.
     */
    static void assertOutput(Z2IntegerOperator operator, int l, long[] xs, long[] ys, long[] zs) {
        int num = xs.length;
        long andMod = (1L << l) - 1;
        switch (operator) {
            case SUB:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (xs[i] - ys[i]) & andMod;
                    long actualZ = zs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case INCREASE_ONE:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (xs[i] + 1) & andMod;
                    long actualZ = zs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case ADD:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (xs[i] + ys[i]) & andMod;
                    long actualZ = zs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case MUL:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (xs[i] * ys[i]) & andMod;
                    long actualZ = zs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case LEQ:
                IntStream.range(0, num).forEach(i -> {
                    boolean expectZ = (xs[i] <= ys[i]);
                    boolean actualZ = (zs[i] % 2) == 1;
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case EQ:
                IntStream.range(0, num).forEach(i -> {
                    boolean expectZ = (xs[i] == ys[i]);
                    boolean actualZ = (zs[i] % 2) == 1;
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            default:
                throw new IllegalStateException("Invalid " + operator.name() + ": " + operator.name());
        }
    }

    /**
     * asserts if the sorting result is correct.
     *
     * @param l  bit length.
     * @param xs input x.
     * @param zs output z.
     */
    static void assertSortOutput(int l, long[][] xs, long[][] zs) {
        int num = xs[0].length;
        int numOfSorted = xs.length;
        long andMod = (1L << l) - 1;
        for (int i = 0; i < num; i++) {
            int finalI = i;
            long[] expected = IntStream.range(0, numOfSorted)
                .mapToObj(j -> xs[j][finalI])
                .mapToLong(z -> z)
                .toArray();
            Arrays.sort(expected);
            for (int j = 0; j < numOfSorted; j++) {
                long actual = zs[j][i] & andMod;
                Assert.assertEquals(expected[j], actual);
            }
        }
    }

    /**
     * asserts if the permutation sorting result is correct.
     *
     * @param xs          input x.
     * @param payloadXs   payload x.
     * @param permutation permutation corresponding to sorting.
     * @param zs          output z.
     * @param payloadZs   payload z.
     */
    static void assertPsortStableOutput(long[] xs, long[][] payloadXs, int[] permutation, long[] zs, long[][] payloadZs) {
        int num = xs.length;
        int payloadNum = payloadXs != null ? payloadXs.length : 0;
        PermutationNetworkUtils.validPermutation(permutation);

        long current = zs[0];
        for (int i = 0; i < num; i++) {
            Assert.assertEquals(xs[permutation[i]], zs[i]);
            for (int j = 0; j < payloadNum; j++) {
                Assert.assertEquals(payloadXs[j][permutation[i]], payloadZs[j][i]);
            }
            if (i > 0) {
                Assert.assertTrue(zs[i] >= zs[i - 1]);
                if (current == zs[i]) {
                    Assert.assertTrue(permutation[i] >= permutation[i - 1]);
                } else {
                    current = zs[i];
                }
            }
        }
    }
}
