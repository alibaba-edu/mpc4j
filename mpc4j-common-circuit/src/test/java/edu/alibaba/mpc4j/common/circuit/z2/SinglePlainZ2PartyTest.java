package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.DyadicBcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryBcOperator;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * single plain z2 party test.
 *
 * @author Weiran Liu
 * @date 2023/5/9
 */
public class SinglePlainZ2PartyTest {
    /**
     * random status
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default bit num
     */
    private static final int DEFAULT_BIT_NUM = 1001;
    /**
     * large num
     */
    private static final int LARGE_BIT_NUM = 1 << 16 - 1;

    @Test
    public void test1BitNum() {
        testPto(1);
    }

    @Test
    public void test2BitNum() {
        testPto(2);
    }

    @Test
    public void test8BitNum() {
        testPto(8);
    }

    @Test
    public void test15BitNum() {
        testPto(15);
    }

    @Test
    public void testDefaultBitNum() {
        testPto(DEFAULT_BIT_NUM);
    }

    @Test
    public void testLargeBitNum() {
        testPto(LARGE_BIT_NUM);
    }

    private void testPto(int bitNum) {
        for (DyadicBcOperator operator : DyadicBcOperator.values()) {
            testDyadicOperator(operator, bitNum);
        }
        for (UnaryBcOperator operator : UnaryBcOperator.values()) {
            testUnaryOperator(operator, bitNum);
        }
    }

    private void testDyadicOperator(DyadicBcOperator operator, int bitNum) {
        PlainZ2cParty plainParty = new PlainZ2cParty();
        plainParty.init(bitNum);
        // generate x
        BitVector xVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        MpcZ2Vector xPlainVector = plainParty.create(true, xVector);
        // generate y
        BitVector yVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        MpcZ2Vector yPlainVector = plainParty.create(true, yVector);
        // create z
        BitVector zVector;
        MpcZ2Vector zPlainVector;
        switch (operator) {
            case XOR:
                zVector = xVector.xor(yVector);
                zPlainVector = plainParty.xor(xPlainVector, yPlainVector);
                break;
            case AND:
                zVector = xVector.and(yVector);
                zPlainVector = plainParty.and(xPlainVector, yPlainVector);
                break;
            case OR:
                zVector = xVector.or(yVector);
                zPlainVector = plainParty.or(xPlainVector, yPlainVector);
                break;
            default:
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(zVector, zPlainVector.getBitVector());
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(UnaryBcOperator operator, int num) {
        PlainZ2cParty plainParty = new PlainZ2cParty();
        plainParty.init(num);
        // generate x
        BitVector xVector = BitVectorFactory.createRandom(num, SECURE_RANDOM);
        MpcZ2Vector xPlainVector = plainParty.create(true, xVector);
        // create z
        BitVector zVector;
        MpcZ2Vector zPlainVector;
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NOT:
                zVector = xVector.not();
                zPlainVector = plainParty.not(xPlainVector);
                break;
            default:
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(zVector, zPlainVector.getBitVector());
    }
}
