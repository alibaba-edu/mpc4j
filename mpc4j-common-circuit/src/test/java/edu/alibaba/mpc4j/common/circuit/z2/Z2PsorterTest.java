package edu.alibaba.mpc4j.common.circuit.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.circuit.z2.psorter.PsorterFactory;
import edu.alibaba.mpc4j.common.structure.database.Zl64Database;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Z2 permutation generation sorter Test.
 *
 * @author Feng Han
 * @date 2023/10/27
 */
@RunWith(Parameterized.class)
public class Z2PsorterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2SorterTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default num of payload to be sorted
     */
    private static final int DEFAULT_PAYLOAD_NUM = 0;
    /**
     * large num of payload to be sorted
     */
    private static final int LARGE_PAYLOAD_NUM = 5;
    /**
     * default num of elements to be sorted
     */
    private static final int DEFAULT_SORTED_NUM = 7;
    /**
     * large num of elements to be sorted
     */
    private static final int LARGE_SORTED_NUM = 1431;
    /**
     * default l
     */
    private static final int DEFAULT_L = 10;
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
        configurations.add(
            new Object[]{
                PsorterFactory.SorterTypes.BITONIC.name(),
                new Z2CircuitConfig.Builder().setPsorterType(PsorterFactory.SorterTypes.BITONIC).build()
            });
        return configurations;
    }

    public Z2PsorterTest(String name, Z2CircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
        stopWatch = new StopWatch();
    }

    @Test
    public void test1SortedNum() {
        testRandom(1, DEFAULT_PAYLOAD_NUM);
        testRandom(1, LARGE_PAYLOAD_NUM);
    }

    @Test
    public void test4SortedNum() {
        testRandom(4, DEFAULT_PAYLOAD_NUM);
        testRandom(4, LARGE_PAYLOAD_NUM);
    }

    @Test
    public void test8SortedNum() {
        testRandom(8, DEFAULT_PAYLOAD_NUM);
        testRandom(8, LARGE_PAYLOAD_NUM);
    }

    @Test
    public void testDefaultSortedNum() {
        testRandom(DEFAULT_SORTED_NUM, DEFAULT_PAYLOAD_NUM);
        testRandom(DEFAULT_SORTED_NUM, LARGE_PAYLOAD_NUM);
    }

    @Test
    public void testLargeSortedNum() {
        testRandom(LARGE_SORTED_NUM, DEFAULT_PAYLOAD_NUM);
        testRandom(LARGE_SORTED_NUM, LARGE_PAYLOAD_NUM);
    }

    private void testRandom(int numOfSorted, int payloadNum) {
        testRandom(DEFAULT_L, numOfSorted, payloadNum);
        testRandom(LARGE_L, numOfSorted, payloadNum);
    }

    private void testRandom(int l, int numOfSorted, int payloadNum) {
        long[] xs = LongStream.range(0, numOfSorted).map(i ->
            LongUtils.randomNonNegative(1L << (l - 1), SECURE_RANDOM)).toArray();
        long[][] payloads = payloadNum == 0 ? null : IntStream.range(0, payloadNum)
            .mapToObj(i -> LongStream.range(0, numOfSorted)
                .map(j -> LongUtils.randomNonNegative(1L << (l - 1), SECURE_RANDOM))
                .toArray()
            )
            .toArray(long[][]::new);
        testPto(l, xs, payloads);
    }


    private void testPto(int l, long[] xs, long[][] payloads) {
        int numOfSorted = xs.length;
        LOGGER.info(
            "test random ({}), l = {}, num of sorted elements = {}, num of payload = {}",
            Z2IntegerOperator.P_SORT, l, numOfSorted, payloads == null ? 0 : payloads.length
        );
        // partition
        PlainZ2Vector[][] xPlainZ2Vectors = new PlainZ2Vector[][]{
            Arrays.stream(Zl64Database.create(l, xs).bitPartition(EnvType.STANDARD, true))
                .map(PlainZ2Vector::create)
                .toArray(PlainZ2Vector[]::new)
        };
        PlainZ2Vector[][] pPlainZ2Vectors = payloads == null ? null : Arrays.stream(payloads)
            .map(p -> Arrays.stream(Zl64Database.create(l, p).bitPartition(EnvType.STANDARD, true))
                .map(PlainZ2Vector::create)
                .toArray(PlainZ2Vector[]::new)
            )
            .toArray(PlainZ2Vector[][]::new);
        // init the protocol
        PlainZ2cParty party = new PlainZ2cParty();
        Z2IntegerCircuitParty partyThread = new Z2IntegerCircuitParty(
            party, Z2IntegerOperator.P_SORT, xPlainZ2Vectors, pPlainZ2Vectors, config
        );
        partyThread.setPsorterConfig(PlainZ2Vector.createOnes(1), true, true);
        // execute the circuit
        LOGGER.info("sorting start");
        stopWatch.start();
        partyThread.run();
        stopWatch.stop();
        long sortTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        LOGGER.info("sort time: {}", sortTime);
        LOGGER.info("------------------------------");
        // verify
        int[] permutation = Arrays.stream(Z2VectorUtils.transport(partyThread.getZ())).mapToInt(x -> (int) x).toArray();
        long[] xSorted = Z2VectorUtils.transport(xPlainZ2Vectors[0]);
        long[][] payloadSorted = pPlainZ2Vectors == null ? null : Arrays.stream(pPlainZ2Vectors)
            .map(Z2VectorUtils::transport)
            .toArray(long[][]::new);
        Z2CircuitTestUtils.assertPsortStableOutput(xs, payloads, permutation, xSorted, payloadSorted);
    }
}
