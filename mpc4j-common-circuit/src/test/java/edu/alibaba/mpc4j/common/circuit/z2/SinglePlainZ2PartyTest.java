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
     * default bit num
     */
    private static final int DEFAULT_BIT_NUM = 1001;
    /**
     * large num
     */
    private static final int LARGE_BIT_NUM = 1 << 16 - 1;
    /**
     * random status
     */
    private final SecureRandom secureRandom;

    public SinglePlainZ2PartyTest() {
        secureRandom = new SecureRandom();
    }

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
        BitVector xBitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        MpcZ2Vector xMpcVector = plainParty.create(true, xBitVector);
        // generate y
        BitVector yBitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        MpcZ2Vector yMpcVector = plainParty.create(true, yBitVector);
        // create z
        BitVector expectVector;
        BitVector actualVector;
        switch (operator) {
            case XOR -> {
                expectVector = xBitVector.xor(yBitVector);
                PlainZ2Vector resultVector = plainParty.xor(xMpcVector, yMpcVector);
                actualVector = plainParty.revealOwn(resultVector);
            }
            case AND -> {
                expectVector = xBitVector.and(yBitVector);
                PlainZ2Vector resultVector = plainParty.and(xMpcVector, yMpcVector);
                actualVector = plainParty.revealOwn(resultVector);
            }
            case OR -> {
                expectVector = xBitVector.or(yBitVector);
                PlainZ2Vector resultVector = plainParty.or(xMpcVector, yMpcVector);
                actualVector = plainParty.revealOwn(resultVector);
            }
            default ->
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(expectVector, actualVector);
    }

    private void testUnaryOperator(UnaryBcOperator operator, int bitNum) {
        PlainZ2cParty plainParty = new PlainZ2cParty();
        plainParty.init(bitNum);
        // generate x
        BitVector xBitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        MpcZ2Vector xMpcVector = plainParty.create(true, xBitVector);
        // create z
        BitVector expectVector;
        BitVector actualVector;
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NOT -> {
                expectVector = xBitVector.not();
                PlainZ2Vector resultVector = plainParty.not(xMpcVector);
                actualVector = plainParty.revealOwn(resultVector);
            }
            default ->
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(expectVector, actualVector);
    }
}
