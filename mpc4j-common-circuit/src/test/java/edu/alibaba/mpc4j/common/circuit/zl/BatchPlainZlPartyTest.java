package edu.alibaba.mpc4j.common.circuit.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * batch plain Zl party test.
 *
 * @author Weiran Liu
 * @date 2023/5/9
 */
@RunWith(Parameterized.class)
public class BatchPlainZlPartyTest {
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * vector length
     */
    private static final int VECTOR_LENGTH = 13;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        Zl[] zls = new Zl[]{
            ZlFactory.createInstance(EnvType.STANDARD, 1),
            ZlFactory.createInstance(EnvType.STANDARD, 3),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L_FOR_MODULE_N - 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L_FOR_MODULE_N),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L_FOR_MODULE_N + 1),
            ZlFactory.createInstance(EnvType.STANDARD, CommonConstants.BLOCK_BIT_LENGTH),
        };
        for (Zl zl : zls) {
            configurations.add(new Object[]{"l = " + zl.getL(), zl});
        }

        return configurations;
    }

    /**
     * Zl instance
     */
    private final Zl zl;
    /**
     * random status
     */
    private final SecureRandom secureRandom;

    public BatchPlainZlPartyTest(String name, Zl zl) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.zl = zl;
        secureRandom = new SecureRandom();
    }

    @Test
    public void test1Num() {
        testPto(1);
    }

    @Test
    public void test2Num() {
        testPto(2);
    }

    @Test
    public void test8Num() {
        testPto(8);
    }

    @Test
    public void test15Num() {
        testPto(15);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM);
    }

    private void testPto(int num) {
        for (DyadicAcOperator operator : DyadicAcOperator.values()) {
            testDyadicOperator(operator, num);
        }
        for (UnaryAcOperator operator : UnaryAcOperator.values()) {
            testUnaryOperator(operator, num);
        }
    }

    private void testDyadicOperator(DyadicAcOperator operator, int num) {
        PlainZlcParty plainZlParty = new PlainZlcParty();
        plainZlParty.init(zl.getL(), num * VECTOR_LENGTH);
        // generate x
        ZlVector[] xBitVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> ZlVector.createRandom(zl, num, secureRandom))
            .toArray(ZlVector[]::new);
        MpcZlVector[] xMpcVectors = plainZlParty.shareOwn(xBitVectors);
        // generate y
        ZlVector[] yBitVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> ZlVector.createRandom(zl, num, secureRandom))
            .toArray(ZlVector[]::new);
        MpcZlVector[] yPlainVectors = plainZlParty.shareOwn(yBitVectors);
        // create z
        ZlVector[] expectVectors;
        ZlVector[] actualVectors;
        switch (operator) {
            case ADD -> {
                expectVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(index -> xBitVectors[index].add(yBitVectors[index]))
                    .toArray(ZlVector[]::new);
                MpcZlVector[] resultVectors = plainZlParty.add(xMpcVectors, yPlainVectors);
                actualVectors = plainZlParty.revealOwn(resultVectors);
            }
            case SUB -> {
                expectVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(index -> xBitVectors[index].sub(yBitVectors[index]))
                    .toArray(ZlVector[]::new);
                MpcZlVector[] resultVectors = plainZlParty.sub(xMpcVectors, yPlainVectors);
                actualVectors = plainZlParty.revealOwn(resultVectors);
            }
            case MUL -> {
                expectVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(index -> xBitVectors[index].mul(yBitVectors[index]))
                    .toArray(ZlVector[]::new);
                MpcZlVector[] resultVectors = plainZlParty.mul(xMpcVectors, yPlainVectors);
                actualVectors = plainZlParty.revealOwn(resultVectors);
            }
            default ->
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertArrayEquals(expectVectors, actualVectors);
    }

    private void testUnaryOperator(UnaryAcOperator operator, int num) {
        PlainZlcParty plainZlParty = new PlainZlcParty();
        plainZlParty.init(zl.getL(), num * VECTOR_LENGTH);
        // generate x
        ZlVector[] xBitVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> ZlVector.createRandom(zl, num, secureRandom))
            .toArray(ZlVector[]::new);
        MpcZlVector[] xMpcVectors = plainZlParty.shareOwn(xBitVectors);
        // create z
        ZlVector[] expectVectors;
        ZlVector[] actualVectors;
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG -> {
                expectVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(index -> xBitVectors[index].neg())
                    .toArray(ZlVector[]::new);
                MpcZlVector[] resultVectors = plainZlParty.neg(xMpcVectors);
                actualVectors = plainZlParty.revealOwn(resultVectors);
            }
            default ->
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertArrayEquals(expectVectors, actualVectors);
    }
}
