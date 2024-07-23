package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.StructureUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * int vector test.
 *
 * @author Weiran Liu
 * @date 2024/7/5
 */
public class IntVectorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntVectorTest.class);
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
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public IntVectorTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        IntVector vector = IntVector.createRandom(DEFAULT_NUM, secureRandom);
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
        IntVector vectorEmpty = IntVector.createEmpty();
        LOGGER.info(vectorEmpty.toString());
        // num = 1
        IntVector vector1 = IntVector.createRandom(1, secureRandom);
        LOGGER.info(vector1.toString());
        // num = DISPLAY_NUM
        IntVector vectorDisplayNum = IntVector.createRandom(StructureUtils.DISPLAY_NUM, secureRandom);
        LOGGER.info(vectorDisplayNum.toString());
        // num = DISPLAY_NUM + 1
        IntVector vectorDisplayNum1 = IntVector.createRandom(StructureUtils.DISPLAY_NUM + 1, secureRandom);
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
        IntVector vector1 = IntVector.createRandom(num, secureRandom);
        vector1.reduce(1);
        Assert.assertEquals(1, vector1.getNum());
        // reduce all
        IntVector vectorAll = IntVector.createRandom(num, secureRandom);
        vectorAll.reduce(num);
        Assert.assertEquals(num, vectorAll.getNum());
        if (num > 1) {
            // reduce num - 1
            IntVector vectorNum = IntVector.createRandom(num, secureRandom);
            vectorNum.reduce(num - 1);
            Assert.assertEquals(num - 1, vectorNum.getNum());
            // reduce half
            IntVector vectorHalf = IntVector.createRandom(num, secureRandom);
            vectorHalf.reduce(num / 2);
            Assert.assertEquals(num / 2, vectorHalf.getNum());
        }
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        // split 1
        IntVector vector1 = IntVector.createRandom(num, secureRandom);
        IntVector splitVector1 = vector1.split(1);
        Assert.assertEquals(num - 1, vector1.getNum());
        Assert.assertEquals(1, splitVector1.getNum());
        // split all
        IntVector vectorAll = IntVector.createRandom(num, secureRandom);
        IntVector splitVectorAll = vectorAll.split(num);
        Assert.assertEquals(0, vectorAll.getNum());
        Assert.assertEquals(num, splitVectorAll.getNum());
        if (num > 1) {
            // split num - 1
            IntVector vectorNum = IntVector.createRandom(num, secureRandom);
            IntVector splitVectorNum = vectorNum.split(num - 1);
            Assert.assertEquals(1, vectorNum.getNum());
            Assert.assertEquals(num - 1, splitVectorNum.getNum());
            // split half
            IntVector vectorHalf = IntVector.createRandom(num, secureRandom);
            IntVector splitVectorHalf = vectorHalf.split(num / 2);
            Assert.assertEquals(num - num / 2, vectorHalf.getNum());
            Assert.assertEquals(num / 2, splitVectorHalf.getNum());
        }
    }

    @Test
    public void testMerge() {
        for (int num1 = 0; num1 < MAX_NUM; num1++) {
            for (int num2 = 0; num2 < MAX_NUM; num2++) {
                testMerge(num1, num2);
            }
        }
    }

    private void testMerge(int num1, int num2) {
        IntVector vector = IntVector.createRandom(num1, secureRandom);
        IntVector mergeVector = IntVector.createRandom(num2, secureRandom);
        vector.merge(mergeVector);
        Assert.assertEquals(num1 + num2, vector.getNum());
    }

    @Test
    public void testSplitMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplitMerge(num);
        }
    }

    private void testSplitMerge(int num) {
        // split and merge 1
        IntVector vector1 = IntVector.createRandom(num, secureRandom);
        IntVector copyVector1 = vector1.copy();
        IntVector splitVector1 = vector1.split(1);
        vector1.merge(splitVector1);
        Assert.assertEquals(copyVector1, vector1);
        // split and merge all
        IntVector vectorAll = IntVector.createRandom(num, secureRandom);
        IntVector copyVectorAll = vectorAll.copy();
        IntVector splitVectorAll = vectorAll.split(num);
        vectorAll.merge(splitVectorAll);
        Assert.assertEquals(copyVectorAll, vectorAll);
        if (num > 1) {
            // split and merge num - 1
            IntVector vectorNum = IntVector.createRandom(num, secureRandom);
            IntVector copyVectorNum = vectorNum.copy();
            IntVector splitVectorNum = vectorNum.split(num - 1);
            vectorNum.merge(splitVectorNum);
            Assert.assertEquals(copyVectorNum, vectorNum);
            // split half
            IntVector vectorHalf = IntVector.createRandom(num, secureRandom);
            IntVector copyVectorHalf = vectorHalf.copy();
            IntVector splitVectorHalf = vectorHalf.split(num / 2);
            vectorHalf.merge(splitVectorHalf);
            Assert.assertEquals(copyVectorHalf, vectorHalf);
        }
    }

    @Test
    public void testSplitMergeVector() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplitMergeVector(num);
        }
    }

    public void testSplitMergeVector(int num) {
        int[] splits = IntStream.range(0, num).map(i -> secureRandom.nextInt(MAX_NUM) + 1).toArray();
        IntVector[] expectVectors = IntStream.range(0, num)
            .mapToObj(i -> IntVector.createRandom(splits[i], secureRandom))
            .toArray(IntVector[]::new);
        IntVector vector = IntVector.merge(expectVectors);
        IntVector[] actualVectors = IntVector.split(vector, splits);
        MathPreconditions.checkEqual("expectVectors.length", "actualVectors.length", expectVectors.length, actualVectors.length);
        for (int i = 0; i < expectVectors.length; i++) {
            Assert.assertEquals(actualVectors[i], expectVectors[i]);
        }
    }

    @Test
    public void testElementsByInterval() {
        IntVector vector = IntVector.createRandom(MAX_NUM, secureRandom);
        for (int i = 0; i < 10; i++) {
            int interval = secureRandom.nextInt(MAX_NUM - 1) + 1;
            int pos = secureRandom.nextInt(MAX_NUM - interval);
            int num = (vector.getNum() - pos) / interval;
            IntVector intervalVector = vector.getElementsByInterval(pos, num, interval);
            // verify
            for (int j = 0; j < num; j++) {
                Assert.assertEquals(intervalVector.getElement(j), vector.getElement(pos + j * interval));
            }
            IntVector copyVector = vector.copy();
            vector.setElementsByInterval(intervalVector, pos, num, interval);
            Assert.assertEquals(copyVector, vector);
        }
    }

    @Test
    public void testDecompose() {
        IntVector vector = IntVector.createRandom(MAX_NUM, secureRandom);
        // p > 1
        Assert.assertThrows(IllegalArgumentException.class, () -> IntVector.decompose(vector, 0));
        Assert.assertThrows(IllegalArgumentException.class, () -> IntVector.decompose(vector, 1));

        for (int i = 0; i < 10; i++) {
            // p ∈ [2, 12)
            int p = secureRandom.nextInt(10) + 2;
            // decompose
            IntVector[] decomposedVectors = IntVector.decompose(vector, p);
            // verify one result
            int value = 0;
            for (IntVector decomposedVector : decomposedVectors) {
                value = value * p + decomposedVector.getElement(0);
            }
            Assert.assertEquals(vector.getElement(0), value);
            // compose
            IntVector composedVector = IntVector.compose(decomposedVectors, p);
            Assert.assertEquals(vector, composedVector);
        }
    }

    @Test
    public void testDecomposeToByteVector() {
        IntVector vector = IntVector.createRandom(MAX_NUM, secureRandom);
        // decompose
        IntVector[] decomposedVectors = IntVector.decomposeToByteVector(vector);
        // verify one result
        int value = 0;
        for (IntVector decomposedVector : decomposedVectors) {
            value = (value << Byte.SIZE) + (decomposedVector.getElement(0));
        }
        Assert.assertEquals(vector.getElement(0), value);
        // compose
        IntVector composedVector = IntVector.composeByteVector(decomposedVectors);
        Assert.assertEquals(vector, composedVector);
    }

    @Test
    public void testAddSub() {
        IntVector expect, operand, actual, expect1, actual1;
        for (int i = 0; i < 10; i++) {
            // operations
            expect = IntVector.createRandom(MAX_NUM, secureRandom);
            operand = IntVector.createRandom(MAX_NUM, secureRandom);
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
            expect = IntVector.createRandom(MAX_NUM, secureRandom);
            operand = IntVector.createRandom(MAX_NUM, secureRandom);
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
