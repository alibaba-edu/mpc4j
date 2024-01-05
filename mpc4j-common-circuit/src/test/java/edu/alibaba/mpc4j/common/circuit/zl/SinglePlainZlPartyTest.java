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
     * random status
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L - 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L + 1),
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

    public SinglePlainZlPartyTest(String name, Zl zl) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.zl = zl;
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
        PlainZlParty plainParty = new PlainZlParty(zl);
        plainParty.init(num);
        // generate x
        ZlVector xVector = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        MpcZlVector xPlainVector = plainParty.create(xVector);
        // generate y
        ZlVector yVector = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        MpcZlVector yPlainVector = plainParty.create(yVector);
        // create z
        ZlVector zVector;
        MpcZlVector zPlainVector;
        switch (operator) {
            case ADD:
                zVector = xVector.add(yVector);
                zPlainVector = plainParty.add(xPlainVector, yPlainVector);
                break;
            case SUB:
                zVector = xVector.sub(yVector);
                zPlainVector = plainParty.sub(xPlainVector, yPlainVector);
                break;
            case MUL:
                zVector = xVector.mul(yVector);
                zPlainVector = plainParty.mul(xPlainVector, yPlainVector);
                break;
            default:
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(zVector, zPlainVector.getZlVector());
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(UnaryAcOperator operator, int num) {
        PlainZlParty plainParty = new PlainZlParty(zl);
        plainParty.init(num);
        // generate x
        ZlVector xVector = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        MpcZlVector xPlainVector = plainParty.create(xVector);
        // create z
        ZlVector zVector;
        MpcZlVector zPlainVector;
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG:
                zVector = xVector.neg();
                zPlainVector = plainParty.neg(xPlainVector);
                break;
            default:
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(zVector, zPlainVector.getZlVector());
    }
}
