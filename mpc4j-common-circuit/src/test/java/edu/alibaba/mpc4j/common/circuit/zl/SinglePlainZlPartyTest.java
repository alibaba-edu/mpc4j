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

/**
 * single plain Zl party test.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
@RunWith(Parameterized.class)
public class SinglePlainZlPartyTest {
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;

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

    public SinglePlainZlPartyTest(String name, Zl zl) {
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
        PlainZlcParty plainParty = new PlainZlcParty();
        plainParty.init(zl.getL(), num);
        // generate x
        ZlVector xBitVector = ZlVector.createRandom(zl, num, secureRandom);
        MpcZlVector xMpcVector = plainParty.create(xBitVector);
        // generate y
        ZlVector yBitVector = ZlVector.createRandom(zl, num, secureRandom);
        MpcZlVector yMpcVector = plainParty.create(yBitVector);
        // create z
        ZlVector expectVector;
        ZlVector actualVector;
        switch (operator) {
            case ADD -> {
                expectVector = xBitVector.add(yBitVector);
                MpcZlVector resultVector = plainParty.add(xMpcVector, yMpcVector);
                actualVector = plainParty.revealOwn(resultVector);
            }
            case SUB -> {
                expectVector = xBitVector.sub(yBitVector);
                MpcZlVector resultVector = plainParty.sub(xMpcVector, yMpcVector);
                actualVector = plainParty.revealOwn(resultVector);
            }
            case MUL -> {
                expectVector = xBitVector.mul(yBitVector);
                MpcZlVector resultVector = plainParty.mul(xMpcVector, yMpcVector);
                actualVector = plainParty.revealOwn(resultVector);
            }
            default ->
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(expectVector, actualVector);
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(UnaryAcOperator operator, int num) {
        PlainZlcParty plainParty = new PlainZlcParty();
        plainParty.init(zl.getL(), num);
        // generate x
        ZlVector xBitVector = ZlVector.createRandom(zl, num, secureRandom);
        MpcZlVector xMpcVector = plainParty.create(xBitVector);
        // create z
        ZlVector expectVector;
        ZlVector actualVector;
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG -> {
                expectVector = xBitVector.neg();
                MpcZlVector resultVector = plainParty.neg(xMpcVector);
                actualVector = plainParty.revealOwn(resultVector);
            }
            default ->
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(expectVector, actualVector);
    }
}
