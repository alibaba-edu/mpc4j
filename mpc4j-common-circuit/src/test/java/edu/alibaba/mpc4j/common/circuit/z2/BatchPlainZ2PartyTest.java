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
import java.util.stream.IntStream;

/**
 * batch plain Z2 party test.
 *
 * @author Weiran Liu
 * @date 2024/6/20
 */
public class BatchPlainZ2PartyTest {
    /**
     * default bit num
     */
    private static final int DEFAULT_BIT_NUM = 1001;
    /**
     * large num
     */
    private static final int LARGE_BIT_NUM = 1 << 16 - 1;
    /**
     * vector length
     */
    private static final int VECTOR_LENGTH = 13;
    /**
     * random status
     */
    private final SecureRandom secureRandom;

    public BatchPlainZ2PartyTest() {
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
        plainParty.init(bitNum * VECTOR_LENGTH);
        // generate x
        BitVector[] xBitVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> BitVectorFactory.createRandom(bitNum, secureRandom))
            .toArray(BitVector[]::new);
        MpcZ2Vector[] xMpcVectors = plainParty.shareOwn(xBitVectors);
        // generate y
        BitVector[] yBitVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> BitVectorFactory.createRandom(bitNum, secureRandom))
            .toArray(BitVector[]::new);
        MpcZ2Vector[] yMpcVectors = plainParty.shareOwn(yBitVectors);
        // create z
        BitVector[] expectVectors;
        BitVector[] actualVectors;
        switch (operator) {
            case XOR -> {
                expectVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(i -> xBitVectors[i].xor(yBitVectors[i]))
                    .toArray(BitVector[]::new);
                PlainZ2Vector[] resultVectors = plainParty.xor(xMpcVectors, yMpcVectors);
                actualVectors = plainParty.revealOwn(resultVectors);
            }
            case AND -> {
                expectVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(i -> xBitVectors[i].and(yBitVectors[i]))
                    .toArray(BitVector[]::new);
                PlainZ2Vector[] resultVectors = plainParty.and(xMpcVectors, yMpcVectors);
                actualVectors = plainParty.revealOwn(resultVectors);
            }
            case OR -> {
                expectVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(i -> xBitVectors[i].or(yBitVectors[i]))
                    .toArray(BitVector[]::new);
                PlainZ2Vector[] resultVectors = plainParty.or(xMpcVectors, yMpcVectors);
                actualVectors = plainParty.revealOwn(resultVectors);
            }
            default ->
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertArrayEquals(expectVectors, actualVectors);
    }

    private void testUnaryOperator(UnaryBcOperator operator, int bitNum) {
        PlainZ2cParty plainParty = new PlainZ2cParty();
        plainParty.init(bitNum * VECTOR_LENGTH);
        // generate x
        BitVector[] xBitVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> BitVectorFactory.createRandom(bitNum, secureRandom))
            .toArray(BitVector[]::new);
        MpcZ2Vector[] xMpcVectors = plainParty.shareOwn(xBitVectors);
        // create z
        BitVector[] expectVectors;
        BitVector[] actualVectors;
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NOT -> {
                expectVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(i -> xBitVectors[i].not())
                    .toArray(BitVector[]::new);
                PlainZ2Vector[] resultVectors = plainParty.not(xMpcVectors);
                actualVectors = plainParty.revealOwn(resultVectors);
            }
            default ->
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertArrayEquals(expectVectors, actualVectors);
    }
}
