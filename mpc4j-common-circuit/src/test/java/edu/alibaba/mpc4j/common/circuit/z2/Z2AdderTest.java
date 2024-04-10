package edu.alibaba.mpc4j.common.circuit.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory;
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
 * Z2 Adder Test.
 *
 * @author Li Peng
 * @date 2023/6/7
 */
@RunWith(Parameterized.class)
public class Z2AdderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2AdderTest.class);
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
    /**
     * the config
     */
    private final Z2CircuitConfig config;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Ripple-carry adder
        configurations.add(new Object[]{
                AdderFactory.AdderTypes.RIPPLE_CARRY + " (ripple-carry adder)",
                new Z2CircuitConfig.Builder().setAdderType(AdderFactory.AdderTypes.RIPPLE_CARRY).build()
        });
        // Sklansky adder
        configurations.add(new Object[]{
                AdderFactory.AdderTypes.SKLANSKY + " (sklansky adder)",
                new Z2CircuitConfig.Builder().setAdderType(AdderFactory.AdderTypes.SKLANSKY).build()
        });
        // Brent-kung adder
        configurations.add(new Object[]{
                AdderFactory.AdderTypes.BRENT_KUNG + " (brent-kung adder)",
                new Z2CircuitConfig.Builder().setAdderType(AdderFactory.AdderTypes.BRENT_KUNG).build()
        });
        // Kogge-stone adder
        configurations.add(new Object[]{
                AdderFactory.AdderTypes.KOGGE_STONE + " (kogge-stone adder)",
                new Z2CircuitConfig.Builder().setAdderType(AdderFactory.AdderTypes.KOGGE_STONE).build()
        });
        return configurations;
    }

    public Z2AdderTest(String name, Z2CircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

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
        int num = longXs.length;
        if (constant) {
            LOGGER.info("test constant ({}), l = {}, num = {}", Z2IntegerOperator.ADD, l, num);
        } else {
            LOGGER.info("test random ({}), l = {}, num = {}", Z2IntegerOperator.ADD, l, num);
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
        Z2IntegerCircuitParty partyThread = new Z2IntegerCircuitParty(party, Z2IntegerOperator.ADD, xPlainZ2Vectors, yPlainZ2Vectors, config);
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
        Z2CircuitTestUtils.assertOutput(Z2IntegerOperator.ADD, l, longXs, longYs, longZs);
    }
}
