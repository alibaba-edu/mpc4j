package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
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

    public static void assertOutput(Z2IntegerOperator operator, int l, long[] longXs, long[] longYs, long[] longZs) {
        int num = longXs.length;
        long andMod = (1L << l) - 1;
        switch (operator) {
            case SUB:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (longXs[i] - longYs[i]) & andMod;
                    long actualZ = longZs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case INCREASE_ONE:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (longXs[i] + 1) & andMod;
                    long actualZ = longZs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case ADD:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (longXs[i] + longYs[i]) & andMod;
                    long actualZ = longZs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case MUL:
                IntStream.range(0, num).forEach(i -> {
                    long expectZ = (longXs[i] * longYs[i]) & andMod;
                    long actualZ = longZs[i];
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case LEQ:
                IntStream.range(0, num).forEach(i -> {
                    boolean expectZ = (longXs[i] <= longYs[i]);
                    boolean actualZ = (longZs[i] % 2) == 1;
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            case EQ:
                IntStream.range(0, num).forEach(i -> {
                    boolean expectZ = (longXs[i] == longYs[i]);
                    boolean actualZ = (longZs[i] % 2) == 1;
                    Assert.assertEquals(expectZ, actualZ);
                });
                break;
            default:
                throw new IllegalStateException("Invalid " + operator.name() + ": " + operator.name());
        }
    }

    public static void assertSortOutput(int l, long[][] longXs, long[][] longZs) {
        int num = longXs[0].length;
        int numOfSorted = longXs.length;
        long andMod = (1L << l) - 1;
        for (int i = 0; i < num; i++) {
            int finalI = i;
            Long[] expected = IntStream.range(0, numOfSorted).mapToObj(j -> longXs[j][finalI]).toArray(Long[]::new);
            Arrays.sort(expected);
            for (int j = 0; j < numOfSorted; j++) {
                long actual = longZs[j][i] & andMod;
                Assert.assertEquals(expected[j].longValue(), actual);
            }
        }
    }
}
