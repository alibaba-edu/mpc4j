package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.StructureUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

/**
 * byte vector test.
 *
 * @author Weiran Liu
 * @date 2024/6/7
 */
public class ByteVectorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LongVectorTest.class);
    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * default rows
     */
    private static final int DEFAULT_NUM = 1 << 16;
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 64;

    public ByteVectorTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        // create a vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> ByteVector.create(new byte[0]));
        // create a random vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> ByteVector.createRandom(0, secureRandom));

        ByteVector vector = ByteVector.createRandom(DEFAULT_NUM, secureRandom);
        // split vector with split num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.split(0));
        // split vector with split num > num
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.split(DEFAULT_NUM + 1));
        // reduce vector with reduce num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.reduce(0));
        // reduce vector with reduce num > num
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.reduce(DEFAULT_NUM + 1));
    }

    @Test
    public void testDisplay() {
        // empty
        LongVector vectorEmpty = LongVector.createEmpty();
        LOGGER.info(vectorEmpty.toString());
        // num = 1
        LongVector vector1 = LongVector.createRandom(1, secureRandom);
        LOGGER.info(vector1.toString());
        // num = DISPLAY_NUM
        LongVector vectorDisplayNum = LongVector.createRandom(StructureUtils.DISPLAY_NUM, secureRandom);
        LOGGER.info(vectorDisplayNum.toString());
        // num = DISPLAY_NUM + 1
        LongVector vectorDisplayNum1 = LongVector.createRandom(StructureUtils.DISPLAY_NUM + 1, secureRandom);
        LOGGER.info(vectorDisplayNum1.toString());
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        // reduce 1
        ByteVector vector1 = ByteVector.createRandom(num, secureRandom);
        vector1.reduce(1);
        Assert.assertEquals(1, vector1.getNum());
        // reduce all
        ByteVector vectorAll = ByteVector.createRandom(num, secureRandom);
        vectorAll.reduce(num);
        Assert.assertEquals(num, vectorAll.getNum());
        if (num > 1) {
            // reduce num - 1
            ByteVector vectorNum = ByteVector.createRandom(num, secureRandom);
            vectorNum.reduce(num - 1);
            Assert.assertEquals(num - 1, vectorNum.getNum());
            // reduce half
            ByteVector vectorHalf = ByteVector.createRandom(num, secureRandom);
            vectorHalf.reduce(num / 2);
            Assert.assertEquals(num / 2, vectorHalf.getNum());
        }
    }

    @Test
    public void testAllEmptyMerge() {
        ByteVector vector = ByteVector.createEmpty();
        ByteVector mergeVector = ByteVector.createEmpty();
        vector.merge(mergeVector);
        Assert.assertEquals(0, vector.getNum());
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        ByteVector vector = ByteVector.createEmpty();
        ByteVector mergeVector = ByteVector.createRandom(num, secureRandom);
        vector.merge(mergeVector);
        Assert.assertEquals(num, vector.getNum());
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        ByteVector vector = ByteVector.createRandom(num, secureRandom);
        ByteVector mergeVector = ByteVector.createEmpty();
        vector.merge(mergeVector);
        Assert.assertEquals(num, vector.getNum());
    }

    @Test
    public void testMerge() {
        for (int num1 = MIN_NUM; num1 < MAX_NUM; num1++) {
            for (int num2 = MIN_NUM; num2 < MAX_NUM; num2++) {
                testMerge(num1, num2);
            }
        }
    }

    private void testMerge(int num1, int num2) {
        ByteVector vector = ByteVector.createRandom(num1, secureRandom);
        ByteVector mergeVector = ByteVector.createRandom(num2, secureRandom);
        vector.merge(mergeVector);
        Assert.assertEquals(num1 + num2, vector.getNum());
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        // split 1
        ByteVector vector1 = ByteVector.createRandom(num, secureRandom);
        ByteVector splitVector1 = vector1.split(1);
        Assert.assertEquals(num - 1, vector1.getNum());
        Assert.assertEquals(1, splitVector1.getNum());
        // split all
        ByteVector vectorAll = ByteVector.createRandom(num, secureRandom);
        ByteVector splitVectorAll = vectorAll.split(num);
        Assert.assertEquals(0, vectorAll.getNum());
        Assert.assertEquals(num, splitVectorAll.getNum());
        if (num > 1) {
            // split num - 1
            ByteVector vectorNum = ByteVector.createRandom(num, secureRandom);
            ByteVector splitVectorNum = vectorNum.split(num - 1);
            Assert.assertEquals(1, vectorNum.getNum());
            Assert.assertEquals(num - 1, splitVectorNum.getNum());
            // split half
            ByteVector vectorHalf = ByteVector.createRandom(num, secureRandom);
            ByteVector splitVectorHalf = vectorHalf.split(num / 2);
            Assert.assertEquals(num - num / 2, vectorHalf.getNum());
            Assert.assertEquals(num / 2, splitVectorHalf.getNum());
        }
    }

    @Test
    public void testSplitMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplitMerge(num);
        }
    }

    private void testSplitMerge(int num) {
        // split and merge 1
        ByteVector vector1 = ByteVector.createRandom(num, secureRandom);
        ByteVector copyVector1 = vector1.copy();
        ByteVector splitVector1 = vector1.split(1);
        vector1.merge(splitVector1);
        Assert.assertEquals(copyVector1, vector1);
        // split and merge all
        ByteVector vectorAll = ByteVector.createRandom(num, secureRandom);
        ByteVector copyVectorAll = vectorAll.copy();
        ByteVector splitVectorAll = vectorAll.split(num);
        vectorAll.merge(splitVectorAll);
        Assert.assertEquals(copyVectorAll, vectorAll);
        if (num > 1) {
            // split and merge num - 1
            ByteVector vectorNum = ByteVector.createRandom(num, secureRandom);
            ByteVector copyVectorNum = vectorNum.copy();
            ByteVector splitVectorNum = vectorNum.split(num - 1);
            vectorNum.merge(splitVectorNum);
            Assert.assertEquals(copyVectorNum, vectorNum);
            // split half
            ByteVector vectorHalf = ByteVector.createRandom(num, secureRandom);
            ByteVector copyVectorHalf = vectorHalf.copy();
            ByteVector splitVectorHalf = vectorHalf.split(num / 2);
            vectorHalf.merge(splitVectorHalf);
            Assert.assertEquals(copyVectorHalf, vectorHalf);
        }
    }

    @Test
    public void testAddSub() {
        ByteVector expect, operand, actual, expect1, actual1;
        for (int i = 0; i < 10; i++) {
            // operations
            expect = ByteVector.createRandom(MAX_NUM, secureRandom);
            operand = ByteVector.createRandom(MAX_NUM, secureRandom);
            // add and sub
            actual = expect.add(operand);
            actual = actual.sub(operand);
            Assert.assertEquals(expect, actual);
            // sub and add
            actual = expect.sub(operand);
            actual = actual.add(operand);
            Assert.assertEquals(expect, actual);
            // neg
            actual = expect.neg();
            actual = actual.neg();
            Assert.assertEquals(expect, actual);
            // add and sub neg
            expect1 = expect.add(operand);
            actual1 = expect.sub(operand.neg());
            Assert.assertEquals(expect1, actual1);
            // sub and add neg
            expect1 = expect.sub(operand);
            actual1 = expect.add(operand.neg());
            Assert.assertEquals(expect1, actual1);

            // in-place operations
            expect = ByteVector.createRandom(MAX_NUM, secureRandom);
            operand = ByteVector.createRandom(MAX_NUM, secureRandom);
            // addi and subi
            actual = expect.copy();
            actual.addi(operand);
            actual.subi(operand);
            Assert.assertEquals(expect, actual);
            // subi and addi
            actual = expect.copy();
            actual.subi(operand);
            actual.addi(operand);
            Assert.assertEquals(expect, actual);
            // negi
            actual = expect.copy();
            actual.negi();
            actual.negi();
            Assert.assertEquals(expect, actual);
            // addi and subi neg
            expect1 = expect.copy();
            expect1.addi(operand);
            actual1 = expect.copy();
            actual1.subi(operand.neg());
            Assert.assertEquals(expect1, actual1);
            // subi and addi neg
            expect1 = expect.copy();
            expect1.subi(operand);
            actual1 = expect.copy();
            actual1.addi(operand.neg());
            Assert.assertEquals(expect1, actual1);
        }
    }
}
