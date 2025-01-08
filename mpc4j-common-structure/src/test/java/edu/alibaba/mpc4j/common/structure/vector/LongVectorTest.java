package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.StructureUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * long vector test.
 *
 * @author Feng Han
 * @date 2024/02/29
 */
public class LongVectorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LongVectorTest.class);
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 100;
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

    public LongVectorTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        LongVector vector = LongVector.createRandom(DEFAULT_NUM, secureRandom);
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
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        // reduce 1
        LongVector vector1 = LongVector.createRandom(num, secureRandom);
        vector1.reduce(1);
        Assert.assertEquals(1, vector1.getNum());
        // reduce all
        LongVector vectorAll = LongVector.createRandom(num, secureRandom);
        vectorAll.reduce(num);
        Assert.assertEquals(num, vectorAll.getNum());
        if (num > 1) {
            // reduce num - 1
            LongVector vectorNum = LongVector.createRandom(num, secureRandom);
            vectorNum.reduce(num - 1);
            Assert.assertEquals(num - 1, vectorNum.getNum());
            // reduce half
            LongVector vectorHalf = LongVector.createRandom(num, secureRandom);
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
        LongVector vector1 = LongVector.createRandom(num, secureRandom);
        LongVector splitVector1 = vector1.split(1);
        Assert.assertEquals(num - 1, vector1.getNum());
        Assert.assertEquals(1, splitVector1.getNum());
        // split all
        LongVector vectorAll = LongVector.createRandom(num, secureRandom);
        LongVector splitVectorAll = vectorAll.split(num);
        Assert.assertEquals(0, vectorAll.getNum());
        Assert.assertEquals(num, splitVectorAll.getNum());
        if (num > 1) {
            // split num - 1
            LongVector vectorNum = LongVector.createRandom(num, secureRandom);
            LongVector splitVectorNum = vectorNum.split(num - 1);
            Assert.assertEquals(1, vectorNum.getNum());
            Assert.assertEquals(num - 1, splitVectorNum.getNum());
            // split half
            LongVector vectorHalf = LongVector.createRandom(num, secureRandom);
            LongVector splitVectorHalf = vectorHalf.split(num / 2);
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
        LongVector vector = LongVector.createRandom(num1, secureRandom);
        LongVector mergeVector = LongVector.createRandom(num2, secureRandom);
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
        LongVector vector1 = LongVector.createRandom(num, secureRandom);
        LongVector copyVector1 = vector1.copy();
        LongVector splitVector1 = vector1.split(1);
        vector1.merge(splitVector1);
        Assert.assertEquals(copyVector1, vector1);
        // split and merge all
        LongVector vectorAll = LongVector.createRandom(num, secureRandom);
        LongVector copyVectorAll = vectorAll.copy();
        LongVector splitVectorAll = vectorAll.split(num);
        vectorAll.merge(splitVectorAll);
        Assert.assertEquals(copyVectorAll, vectorAll);
        if (num > 1) {
            // split and merge num - 1
            LongVector vectorNum = LongVector.createRandom(num, secureRandom);
            LongVector copyVectorNum = vectorNum.copy();
            LongVector splitVectorNum = vectorNum.split(num - 1);
            vectorNum.merge(splitVectorNum);
            Assert.assertEquals(copyVectorNum, vectorNum);
            // split half
            LongVector vectorHalf = LongVector.createRandom(num, secureRandom);
            LongVector copyVectorHalf = vectorHalf.copy();
            LongVector splitVectorHalf = vectorHalf.split(num / 2);
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
        LongVector[] expectVectors = IntStream.range(0, num)
            .mapToObj(i -> LongVector.createRandom(splits[i], secureRandom))
            .toArray(LongVector[]::new);
        LongVector vector = LongVector.merge(expectVectors);
        LongVector[] actualVectors = LongVector.split(vector, splits);
        MathPreconditions.checkEqual("expectVectors.length", "actualVectors.length", expectVectors.length, actualVectors.length);
        for (int i = 0; i < expectVectors.length; i++) {
            Assert.assertEquals(actualVectors[i], expectVectors[i]);
        }
    }

    @Test
    public void testElementsByInterval() {
        LongVector vector = LongVector.createRandom(MAX_NUM, secureRandom);
        for (int i = 0; i < 10; i++) {
            int interval = secureRandom.nextInt(MAX_NUM - 1) + 1;
            int pos = secureRandom.nextInt(MAX_NUM - interval);
            int num = (vector.getNum() - pos) / interval;
            LongVector intervalVector = vector.getElementsByInterval(pos, num, interval);
            // verify
            for (int j = 0; j < num; j++) {
                Assert.assertEquals(intervalVector.getElement(j), vector.getElement(pos + j * interval));
            }
            LongVector copyVector = vector.copy();
            vector.setElementsByInterval(intervalVector, pos, num, interval);
            Assert.assertEquals(copyVector, vector);
        }
    }

    @Test
    public void testDecompose() {
        LongVector vector = LongVector.createRandom(MAX_NUM, secureRandom);
        // p > 1
        Assert.assertThrows(IllegalArgumentException.class, () -> LongVector.decompose(vector, 0));
        Assert.assertThrows(IllegalArgumentException.class, () -> LongVector.decompose(vector, 1));

        for (int i = 0; i < 10; i++) {
            // p ∈ [2, 12)
            int p = secureRandom.nextInt(10) + 2;
            // decompose
            LongVector[] decomposedVectors = LongVector.decompose(vector, p);
            // verify one result
            long value = 0;
            for (LongVector decomposedVector : decomposedVectors) {
                value = value * p + decomposedVector.getElement(0);
            }
            Assert.assertEquals(vector.getElement(0), value);
            // compose
            LongVector composedVector = LongVector.compose(decomposedVectors, p);
            Assert.assertEquals(vector, composedVector);
        }
    }

    @Test
    public void testOperations() {
        // addition
        LongVector actual = LongVector.createRandom(MAX_NUM, secureRandom);
        long[] expect = Arrays.copyOf(actual.getElements(), MAX_NUM);
        for (int i = 0; i < RANDOM_ROUND; i++) {
            LongVector random = LongVector.createRandom(MAX_NUM, secureRandom);
            actual = actual.add(random);
            IntStream.range(0, MAX_NUM).forEach(j -> expect[j] += random.getElement(j));
            Assert.assertArrayEquals(expect, actual.getElements());
        }
        // negation
        for (int i = 0; i < RANDOM_ROUND; i++) {
            actual = actual.neg();
            IntStream.range(0, MAX_NUM).forEach(j -> expect[j] = - expect[j]);
            Assert.assertArrayEquals(expect, actual.getElements());
        }
        // subtraction
        for (int i = 0; i < RANDOM_ROUND; i++) {
            LongVector random = LongVector.createRandom(MAX_NUM, secureRandom);
            actual = actual.sub(random);
            IntStream.range(0, MAX_NUM).forEach(j -> expect[j] -= random.getElement(j));
            Assert.assertArrayEquals(expect, actual.getElements());
        }
        // multiplication
        for (int i = 0; i < RANDOM_ROUND; i++) {
            LongVector random = LongVector.createRandom(MAX_NUM, secureRandom);
            actual = actual.mul(random);
            IntStream.range(0, MAX_NUM).forEach(j -> expect[j] *= random.getElement(j));
            Assert.assertArrayEquals(expect, actual.getElements());
        }

        // in-place addition
        for (int i = 0; i < RANDOM_ROUND; i++) {
            LongVector random = LongVector.createRandom(MAX_NUM, secureRandom);
            actual.addi(random);
            IntStream.range(0, MAX_NUM).forEach(j -> expect[j] += random.getElement(j));
            Assert.assertArrayEquals(expect, actual.getElements());
        }
        // in-place negation
        for (int i = 0; i < RANDOM_ROUND; i++) {
            actual.negi();
            IntStream.range(0, MAX_NUM).forEach(j -> expect[j] = - expect[j]);
            Assert.assertArrayEquals(expect, actual.getElements());
        }
        // in-place subtraction
        for (int i = 0; i < RANDOM_ROUND; i++) {
            LongVector random = LongVector.createRandom(MAX_NUM, secureRandom);
            actual.subi(random);
            IntStream.range(0, MAX_NUM).forEach(j -> expect[j] -= random.getElement(j));
            Assert.assertArrayEquals(expect, actual.getElements());
        }
        // in-place multiplication
        for (int i = 0; i < RANDOM_ROUND; i++) {
            LongVector random = LongVector.createRandom(MAX_NUM, secureRandom);
            actual.muli(random);
            IntStream.range(0, MAX_NUM).forEach(j -> expect[j] *= random.getElement(j));
            Assert.assertArrayEquals(expect, actual.getElements());
        }
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
}
