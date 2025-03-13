package edu.alibaba.mpc4j.common.circuit.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Comparator test
 *
 * @author Feng Han
 * @date 2025/2/27
 */
@RunWith(Parameterized.class)
public class Z2ComparatorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2ComparatorTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * default l
     */
    private static final int DEFAULT_L = Long.SIZE;
    /**
     * large l
     */
    private static final int LARGE_L = 251;
    /**
     * the config
     */
    private final Z2CircuitConfig config;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // tree comparator
        configurations.add(new Object[]{
            ComparatorType.TREE_COMPARATOR + " (tree-form comparator)",
            new Z2CircuitConfig.Builder().setComparatorType(ComparatorType.TREE_COMPARATOR).build()
        });
        // serial comparator
        configurations.add(new Object[]{
            ComparatorType.SERIAL_COMPARATOR + " (serial-form comparator)",
            new Z2CircuitConfig.Builder().setComparatorType(ComparatorType.SERIAL_COMPARATOR).build()
        });
        return configurations;
    }

    public Z2ComparatorTest(String name, Z2CircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void testDefault() {
        testPto(1,
            IntStream.range(0, 1 << 10).mapToObj(i -> SECURE_RANDOM.nextBoolean() ? BigInteger.ONE : BigInteger.ZERO).toArray(BigInteger[]::new),
            IntStream.range(0, 1 << 10).mapToObj(i -> SECURE_RANDOM.nextBoolean() ? BigInteger.ONE : BigInteger.ZERO).toArray(BigInteger[]::new));
        testPto(7,
            IntStream.range(0, 1 << 10).mapToObj(i -> BigInteger.valueOf(SECURE_RANDOM.nextInt(1 << 5))).toArray(BigInteger[]::new),
            IntStream.range(0, 1 << 10).mapToObj(i -> BigInteger.valueOf(SECURE_RANDOM.nextInt(1 << 5))).toArray(BigInteger[]::new));
        testPto(10,
            IntStream.range(0, 1 << 10).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new),
            IntStream.range(0, 1 << 10).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new));
        testPto(13,
            IntStream.range(0, 1 << 10).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new),
            IntStream.range(0, 1 << 10).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new));
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
        BigInteger[] xs = IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(l, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] ys = IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(l, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        testPto(l, xs, ys);
        LOGGER.info("------------------------------");
    }


    private void testPto(int l, BigInteger[] xs, BigInteger[] ys) {
        int num = xs.length;
        LOGGER.info("test ({}), l = {}, num = {}", Z2IntegerOperator.LEQ, l, num);

        // partition
        int byteL = CommonUtils.getByteLength(l);
        ZlDatabase zlXs = ZlDatabase.create(l, Arrays.stream(xs).map(ea -> BigIntegerUtils.nonNegBigIntegerToByteArray(ea, byteL)).toArray(byte[][]::new));
        ZlDatabase zlYs = ZlDatabase.create(l, Arrays.stream(ys).map(ea -> BigIntegerUtils.nonNegBigIntegerToByteArray(ea, byteL)).toArray(byte[][]::new));
        BitVector[] xBitVector = zlXs.bitPartition(EnvType.STANDARD, true);
        BitVector[] yBitVector = zlYs.bitPartition(EnvType.STANDARD, true);
        PlainZ2Vector[] xPlainZ2Vectors = Arrays.stream(xBitVector).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
        PlainZ2Vector[] yPlainZ2Vectors = Arrays.stream(yBitVector).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
        // init the protocol
        PlainZ2cParty party = new PlainZ2cParty();
        Z2IntegerCircuitParty partyThread = new Z2IntegerCircuitParty(party, Z2IntegerOperator.LEQ, xPlainZ2Vectors, yPlainZ2Vectors, config);
        StopWatch stopWatch = new StopWatch();
        // execute the circuit
        stopWatch.start();
        partyThread.run();
        stopWatch.stop();
        stopWatch.reset();
        // verify
        BitVector zPlainZ2Vector = partyThread.getZ()[0].getBitVector();
        Assert.assertEquals(num, zPlainZ2Vector.bitNum());
        for (int i = 0; i < num; i++) {
            Assert.assertEquals(xs[i].compareTo(ys[i]) <= 0, zPlainZ2Vector.get(i));
        }
    }
}
