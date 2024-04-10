package edu.alibaba.mpc4j.common.tool.network;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkFactory.PermutationNetworkType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * permutation network test.
 *
 * @author Weiran Liu
 * @date 2024/3/22
 */
@RunWith(Parameterized.class)
public class PermutationNetworkTest {
    /**
     * random test round
     */
    private static final int RANDOM_ROUND = 40;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // Benes JDK
        configurations.add(new Object[]{PermutationNetworkType.BENES_JDK.name(), PermutationNetworkType.BENES_JDK,});
        // Benes Native
        configurations.add(new Object[]{PermutationNetworkType.BENES_NATIVE.name(), PermutationNetworkType.BENES_NATIVE,});
        // Waksman JDK
        configurations.add(new Object[]{PermutationNetworkType.WAKSMAN_JDK.name(), PermutationNetworkType.WAKSMAN_JDK,});
        // Waksman Native
        configurations.add(new Object[]{PermutationNetworkType.WAKSMAN_NATIVE.name(), PermutationNetworkType.WAKSMAN_NATIVE,});

        return configurations;
    }

    /**
     * type
     */
    private final PermutationNetworkType type;

    public PermutationNetworkTest(String name, PermutationNetworkType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        int[] permutationMap = IntStream.range(0, 9).toArray();
        PermutationNetwork<Integer> network = PermutationNetworkFactory.createInstance(type, permutationMap);
        Assert.assertEquals(type, network.getType());
    }

    @Test
    public void testUnitPermute2() {
        int[] permutationMap;
        // enumerate all permutations for n = 2
        permutationMap = new int[]{0, 1};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{1, 0};
        assertUnitCorrect(permutationMap);
    }

    @Test
    public void testUnitPermute3() {
        int[] permutationMap;
        // enumerate all permutations for n = 3
        permutationMap = new int[]{0, 1, 2};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{0, 2, 1};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{1, 0, 2};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{1, 2, 0};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{2, 0, 1};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{2, 1, 0};
        assertUnitCorrect(permutationMap);
    }

    @Test
    public void testUnitPermute4() {
        int[] permutationMap;
        // enumerate all permutations for n = 4
        permutationMap = new int[]{0, 1, 2, 3};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{0, 1, 3, 2};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{0, 2, 1, 3};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{0, 2, 3, 1};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{0, 3, 1, 2};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{0, 3, 2, 1};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{1, 0, 2, 3};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{1, 0, 3, 2};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{1, 2, 0, 3};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{1, 2, 3, 0};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{1, 3, 0, 2};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{1, 3, 2, 0};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{2, 0, 1, 3};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{2, 0, 3, 1};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{2, 1, 0, 3};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{2, 1, 3, 0};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{2, 3, 0, 1};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{2, 3, 1, 0};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{3, 0, 1, 2};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{3, 0, 2, 1};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{3, 1, 0, 2};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{3, 1, 2, 0};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{3, 2, 0, 1};
        assertUnitCorrect(permutationMap);
        permutationMap = new int[]{3, 2, 1, 0};
        assertUnitCorrect(permutationMap);
    }

    private void assertUnitCorrect(int[] permutationMap) {
        PermutationNetwork<Integer> network = PermutationNetworkFactory.createInstance(type, permutationMap);
        int n = permutationMap.length;
        // verify network size
        Assert.assertEquals(PermutationNetworkUtils.getLevel(n), network.getLevel());
        Assert.assertEquals(PermutationNetworkUtils.getMaxWidth(n), network.getMaxWidth());
        // verify fixed input permutation
        Vector<Integer> inputVector = IntStream.range(0, n)
            .boxed().
            collect(Collectors.toCollection(Vector::new));
        Vector<Integer> expectOutputVector = PermutationNetworkUtils.permutation(permutationMap, inputVector);
        Vector<Integer> actualOutputVector = network.permutation(inputVector);
        Assert.assertEquals(expectOutputVector, actualOutputVector);
        // verify random input permutation
        Vector<Integer> randomInputVector = IntStream.range(0, n)
            .map(position -> SECURE_RANDOM.nextInt(n))
            .boxed()
            .collect(Collectors.toCollection(Vector::new));
        Vector<Integer> expectRandomOutputVector = PermutationNetworkUtils.permutation(permutationMap, randomInputVector);
        Vector<Integer> actualRandomOutputVector = network.permutation(randomInputVector);
        Assert.assertEquals(expectRandomOutputVector, actualRandomOutputVector);
    }

    @Test
    public void testIntegerRandom() {
        int[] basicN = new int[]{4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 32};
        for (int n : basicN) {
            testIntegerRandom(n);
        }
        // n = 2^k
        testIntegerRandom(1 << 10);
        // n != 2^k
        testIntegerRandom((1 << 10) - 1);
        testIntegerRandom((1 << 10) + 1);
    }

    private void testIntegerRandom(int n) {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            List<Integer> shufflePermutationMap = IntStream.range(0, n).boxed().collect(Collectors.toList());
            Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
            int[] permutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
            PermutationNetwork<Integer> network = PermutationNetworkFactory.createInstance(type, permutationMap);
            assertIntegerCorrect(permutationMap, network);
        }
    }

    private void assertIntegerCorrect(int[] permutationMap, PermutationNetwork<Integer> network) {
        int n = permutationMap.length;
        // verify network size
        Assert.assertEquals(PermutationNetworkUtils.getLevel(n), network.getLevel());
        Assert.assertEquals(PermutationNetworkUtils.getMaxWidth(n), network.getMaxWidth());
        // verify fixed input permutation
        Vector<Integer> inputVector = IntStream.range(0, n)
            .boxed().
            collect(Collectors.toCollection(Vector::new));
        Vector<Integer> expectOutputVector = PermutationNetworkUtils.permutation(permutationMap, inputVector);
        Vector<Integer> actualOutputVector = network.permutation(inputVector);
        Assert.assertEquals(expectOutputVector, actualOutputVector);
        // verify random input permutation
        Vector<Integer> randomInputVector = IntStream.range(0, n)
            .map(position -> SECURE_RANDOM.nextInt(n))
            .boxed()
            .collect(Collectors.toCollection(Vector::new));
        Vector<Integer> expectRandomOutputVector = PermutationNetworkUtils.permutation(permutationMap, randomInputVector);
        Vector<Integer> actualRandomOutputVector = network.permutation(randomInputVector);
        Assert.assertEquals(expectRandomOutputVector, actualRandomOutputVector);
    }

    @Test
    public void testByteBufferRandom() {
        int[] basicN = new int[]{4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 32};
        for (int n : basicN) {
            testIntegerRandom(n);
        }
        // n = 2^k
        testByteBufferRandom(1 << 10);
        // n != 2^k
        testByteBufferRandom((1 << 10) - 1);
        testByteBufferRandom((1 << 10) + 1);
    }

    private void testByteBufferRandom(int n) {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            List<Integer> shufflePermutationMap = IntStream.range(0, n).boxed().collect(Collectors.toList());
            Collections.shuffle(shufflePermutationMap, SECURE_RANDOM);
            int[] permutationMap = shufflePermutationMap.stream().mapToInt(permutation -> permutation).toArray();
            PermutationNetwork<ByteBuffer> network = PermutationNetworkFactory.createInstance(type, permutationMap);
            assertByteBufferCorrect(permutationMap, network);
        }
    }

    private void assertByteBufferCorrect(int[] permutationMap, PermutationNetwork<ByteBuffer> network) {
        int n = permutationMap.length;
        // verify network size
        Assert.assertEquals(PermutationNetworkUtils.getLevel(n), network.getLevel());
        Assert.assertEquals(PermutationNetworkUtils.getMaxWidth(n), network.getMaxWidth());
        // verify fixed input permutation
        Vector<ByteBuffer> sequentialInputVector = IntStream.range(0, n)
            .mapToObj(IntUtils::intToByteArray)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(Vector::new));
        Vector<ByteBuffer> expectOutputVector = PermutationNetworkUtils.permutation(permutationMap, sequentialInputVector);
        Vector<ByteBuffer> actualOutputVector = network.permutation(sequentialInputVector);
        Assert.assertEquals(expectOutputVector, actualOutputVector);
        // verify random input permutation
        Vector<ByteBuffer> randomInputVector = IntStream.range(0, n)
            .mapToObj(position -> {
                byte[] input = new byte[CommonConstants.STATS_BYTE_LENGTH];
                SECURE_RANDOM.nextBytes(input);
                return ByteBuffer.wrap(input);
            })
            .collect(Collectors.toCollection(Vector::new));
        Vector<ByteBuffer> expectRandomOutputVector = PermutationNetworkUtils.permutation(permutationMap, randomInputVector);
        Vector<ByteBuffer> actualRandomOutputVector = network.permutation(randomInputVector);
        Assert.assertEquals(expectRandomOutputVector, actualRandomOutputVector);
    }
}
