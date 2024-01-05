package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.database.Zl64Database;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Z2 integer circuit Test. Inputs are created in [0, 2^(l-1)) to avoid overflow in subtraction, which is suggested in
 * <p>
 * https://www.doc.ic.ac.uk/~eedwards/compsys/arithmetic/index.html
 * </p>
 *
 * @author Li Peng
 * @date 2023/4/21
 */
public class Z2IntegerCircuitTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2IntegerCircuitTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1024;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * default l
     */
    private static final int DEFAULT_L = IntUtils.MAX_L;
    /**
     * large l
     */
    private static final int LARGE_L = LongUtils.MAX_L;

    @Test
    public void testConstant() {
        testConstant(DEFAULT_L);
        testConstant(LARGE_L);
    }

    public void testConstant(int l) {
        long[] longXs = IntStream.range(0, DEFAULT_NUM)
                .mapToLong(i -> i)
                .toArray();
        long[] longYs = IntStream.range(0, DEFAULT_NUM)
                .mapToLong(i -> DEFAULT_NUM / 2 + i)
                .toArray();
        testPto(true, l, longXs, longYs);
        LOGGER.info("------------------------------");
    }

    @Test
    public void test1Num() {
        testRandom(1);
    }

    @Test
    public void test2Num() {
        testRandom(2);
    }

    @Test
    public void test8Num() {
        testRandom(8);
    }

    @Test
    public void testLargeNum() {
        testRandom(LARGE_NUM);
    }

    private void testRandom(int num) {
        testRandom(DEFAULT_L, num);
        testRandom(LARGE_L, num);
    }

    private void testRandom(int l, int num) {
        long[] longXs = IntStream.range(0, num)
                .mapToLong(i -> LongUtils.randomNonNegative(1L << (l - 1), SECURE_RANDOM))
                .toArray();
        long[] longYs = IntStream.range(0, num)
                .mapToLong(i -> LongUtils.randomNonNegative(1L << (l - 1), SECURE_RANDOM))
                .toArray();
        testPto(false, l, longXs, longYs);
        LOGGER.info("------------------------------");
    }

    private void testPto(boolean constant, int l, long[] longXs, long[] longYs) {
        testPto(constant, Z2IntegerOperator.SUB, l, longXs, longYs);
        testPto(constant, Z2IntegerOperator.INCREASE_ONE, l, longXs, longYs);
        testPto(constant, Z2IntegerOperator.ADD, l, longXs, longYs);
        testPto(constant, Z2IntegerOperator.MUL, l, longXs, longYs);
        testPto(constant, Z2IntegerOperator.LEQ, l, longXs, longYs);
        testPto(constant, Z2IntegerOperator.EQ, l, longXs, longYs);
    }

    private void testPto(boolean constant, Z2IntegerOperator operator, int l, long[] longXs, long[] longYs) {
        int num = longXs.length;
        if (constant) {
            LOGGER.info("test constant ({}), l = {}, num = {}", operator.name(), l, num);
        } else {
            LOGGER.info("test random ({}), l = {}, num = {}", operator.name(), l, num);
        }
        // partition
        Zl64Database zl64Xs = Zl64Database.create(l, longXs);
        Zl64Database zl64Ys = Zl64Database.create(l, longYs);
        BitVector[] xBitVector = zl64Xs.bitPartition(EnvType.STANDARD, false);
        BitVector[] yBitVector = zl64Ys.bitPartition(EnvType.STANDARD, false);
        PlainZ2Vector[] xPlainZ2Vectors = Arrays.stream(xBitVector).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
        PlainZ2Vector[] yPlainZ2Vectors = Arrays.stream(yBitVector).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
        // init the protocol
        PlainZ2cParty party = new PlainZ2cParty();
        Z2IntegerCircuitParty partyThread = new Z2IntegerCircuitParty(party, operator, xPlainZ2Vectors, yPlainZ2Vectors, new Z2CircuitConfig.Builder().build());
        StopWatch stopWatch = new StopWatch();
        // execute the circuit
        stopWatch.start();
        partyThread.run();
        stopWatch.stop();
        stopWatch.reset();
        // verify
        MpcZ2Vector[] zPlainZ2Vectors = partyThread.getZ();
        BitVector[] z = Arrays.stream(zPlainZ2Vectors).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        long[] longZs = Zl64Database.create(EnvType.STANDARD, false, z).getData();
        Z2CircuitTestUtils.assertOutput(operator, l, longXs, longYs, longZs);
    }
}
