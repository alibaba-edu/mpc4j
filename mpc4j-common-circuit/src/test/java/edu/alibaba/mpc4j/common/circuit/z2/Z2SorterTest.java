package edu.alibaba.mpc4j.common.circuit.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
import edu.alibaba.mpc4j.common.circuit.z2.sorter.SorterFactory;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.database.Zl64Database;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Z2 Sorter Test.
 *
 * @author Li Peng
 * @date 2023/6/13
 */
@RunWith(Parameterized.class)
public class Z2SorterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2SorterTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default num of elements to be sorted
     */
    private static final int DEFAULT_SORTED_NUM = 10;
    /**
     * large num of elements to be sorted
     */
    private static final int LARGE_SORTED_NUM = 1 << 8;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1 << 4;
    /**
     * default large num
     */
    private static final int DEFAULT_LARGE_NUM = 1 << 8;
    /**
     * default l
     */
    private static final int DEFAULT_L = IntUtils.MAX_L;
    /**
     * large l
     */
    private static final int LARGE_L = LongUtils.MAX_L;
    /**
     * the config
     */
    private final Z2CircuitConfig config;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Bitonic sorter.
        configurations.add(new Object[]{
            SorterFactory.SorterTypes.BITONIC + " (bitonic sorter)",
            new Z2CircuitConfig.Builder().setSorterType(SorterFactory.SorterTypes.BITONIC).build()
        });
        // Randomized Shell sorter.
        configurations.add(new Object[]{
            SorterFactory.SorterTypes.RANDOMIZED_SHELL_SORTER + " (randomized shell sorter)",
            new Z2CircuitConfig.Builder().setSorterType(SorterFactory.SorterTypes.RANDOMIZED_SHELL_SORTER).build()
        });
        return configurations;
    }

    public Z2SorterTest(String name, Z2CircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
        stopWatch = new StopWatch();
    }

    @Test
    public void testConstant() {
        testConstant(DEFAULT_L);
        testConstant(LARGE_L);
    }

    public void testConstant(int l) {
        long[][] xs = IntStream.range(0, DEFAULT_SORTED_NUM).mapToObj(index -> IntStream.range(0, DEFAULT_NUM)
            .mapToLong(i -> index)
            .toArray()).toArray(long[][]::new);
        testPto(l, xs);
        LOGGER.info("------------------------------");
    }

    @Test
    public void test1SortedNum() {
        testRandom(DEFAULT_NUM, 1);
        testRandom(DEFAULT_LARGE_NUM, 1);

    }

    @Test
    public void test2SortedNum() {
        testRandom(DEFAULT_NUM, 2);
        testRandom(DEFAULT_LARGE_NUM, 2);

    }

    @Test
    public void test8SortedNum() {
        testRandom(DEFAULT_NUM, 8);
        testRandom(DEFAULT_LARGE_NUM, 8);
    }

    @Test
    public void testDefaultSortedNum() {
        testRandom(DEFAULT_NUM, DEFAULT_SORTED_NUM);
        testRandom(DEFAULT_LARGE_NUM, DEFAULT_SORTED_NUM);
    }

    @Test
    public void testLargeSortedNum() {
        testRandom(DEFAULT_NUM, LARGE_SORTED_NUM);
        testRandom(DEFAULT_LARGE_NUM, LARGE_SORTED_NUM);
    }

    private void testRandom(int num, int numOfSorted) {
        testRandom(DEFAULT_L, num, numOfSorted);
        testRandom(LARGE_L, num, numOfSorted);
    }

    private void testRandom(int l, int num, int numOfSorted) {
        long[][] xs = IntStream.range(0, numOfSorted)
            .mapToObj(index -> IntStream.range(0, num)
                .mapToLong(i -> LongUtils.randomNonNegative(1L << (l - 1), SECURE_RANDOM))
                .toArray()
            )
            .toArray(long[][]::new);
        testPto(l, xs);
        LOGGER.info("------------------------------");
    }

    private void testPto(int l, long[][] xs) {
        int num = xs[0].length;
        int numOfSorted = xs.length;
        LOGGER.info("l = {}, num = {}, num of sorted elements = {}", l, num, numOfSorted);
        // partition
        PlainZ2Vector[][] xPlainZ2Vectors = IntStream.range(0, numOfSorted)
            .mapToObj(index -> {
                Zl64Database zl64Xs = Zl64Database.create(l, xs[index]);
                BitVector[] xBitVector = zl64Xs.bitPartition(EnvType.STANDARD, false);
                return Arrays.stream(xBitVector).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
            })
            .toArray(PlainZ2Vector[][]::new);
        // init the protocol
        PlainZ2cParty party = new PlainZ2cParty();
        Z2IntegerCircuitParty partyThread = new Z2IntegerCircuitParty(
            party, Z2IntegerOperator.SORT, xPlainZ2Vectors, null, config
        );
        // execute the circuit
        stopWatch.start();
        partyThread.run();
        stopWatch.stop();
        stopWatch.reset();
        // verify
        BitVector[][] z = IntStream.range(0, numOfSorted)
            .mapToObj(i -> Arrays.stream(xPlainZ2Vectors[i]).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new))
            .toArray(BitVector[][]::new);
        long[][] longZs = IntStream.range(0, numOfSorted)
            .mapToObj(i -> Zl64Database.create(EnvType.STANDARD, false, z[i]).getData())
            .toArray(long[][]::new);
        Z2CircuitTestUtils.assertSortOutput(l, xs, longZs);
    }
}
