package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.matrix.MatrixUtils;
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
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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

    @Test
    public void testIllegalInputs() {
        // create a vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> LongVector.create(new long[0]));
        // create a random vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> LongVector.createRandom(0, SECURE_RANDOM));
        LongVector vector = LongVector.createRandom(DEFAULT_NUM, SECURE_RANDOM);
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
        LongVector vector1 = LongVector.createRandom(num, SECURE_RANDOM);
        vector1.reduce(1);
        Assert.assertEquals(1, vector1.getNum());
        // reduce all
        LongVector vectorAll = LongVector.createRandom(num, SECURE_RANDOM);
        vectorAll.reduce(num);
        Assert.assertEquals(num, vectorAll.getNum());
        if (num > 1) {
            // reduce num - 1
            LongVector vectorNum = LongVector.createRandom(num, SECURE_RANDOM);
            vectorNum.reduce(num - 1);
            Assert.assertEquals(num - 1, vectorNum.getNum());
            // reduce half
            LongVector vectorHalf = LongVector.createRandom(num, SECURE_RANDOM);
            vectorHalf.reduce(num / 2);
            Assert.assertEquals(num / 2, vectorHalf.getNum());
        }
    }

    @Test
    public void testAllEmptyMerge() {
        LongVector vector = LongVector.createEmpty();
        LongVector mergeVector = LongVector.createEmpty();
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
        LongVector vector = LongVector.createEmpty();
        LongVector mergeVector = LongVector.createRandom(num, SECURE_RANDOM);
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
        LongVector vector = LongVector.createRandom(num, SECURE_RANDOM);
        LongVector mergeVector = LongVector.createEmpty();
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
        LongVector vector = LongVector.createRandom(num1, SECURE_RANDOM);
        LongVector mergeVector = LongVector.createRandom(num2, SECURE_RANDOM);
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
        LongVector vector1 = LongVector.createRandom(num, SECURE_RANDOM);
        LongVector splitVector1 = vector1.split(1);
        Assert.assertEquals(num - 1, vector1.getNum());
        Assert.assertEquals(1, splitVector1.getNum());
        // split all
        LongVector vectorAll = LongVector.createRandom(num, SECURE_RANDOM);
        LongVector splitVectorAll = vectorAll.split(num);
        Assert.assertEquals(0, vectorAll.getNum());
        Assert.assertEquals(num, splitVectorAll.getNum());
        if (num > 1) {
            // split num - 1
            LongVector vectorNum = LongVector.createRandom(num, SECURE_RANDOM);
            LongVector splitVectorNum = vectorNum.split(num - 1);
            Assert.assertEquals(1, vectorNum.getNum());
            Assert.assertEquals(num - 1, splitVectorNum.getNum());
            // split half
            LongVector vectorHalf = LongVector.createRandom(num, SECURE_RANDOM);
            LongVector splitVectorHalf = vectorHalf.split(num / 2);
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
        LongVector vector1 = LongVector.createRandom(num, SECURE_RANDOM);
        LongVector copyVector1 = vector1.copy();
        LongVector splitVector1 = vector1.split(1);
        splitVector1.merge(vector1);
        Assert.assertEquals(copyVector1, splitVector1);
        // split and merge all
        LongVector vectorAll = LongVector.createRandom(num, SECURE_RANDOM);
        LongVector copyVectorAll = vectorAll.copy();
        LongVector splitVectorAll = vectorAll.split(num);
        splitVectorAll.merge(vectorAll);
        Assert.assertEquals(copyVectorAll, splitVectorAll);
        if (num > 1) {
            // split and merge num - 1
            LongVector vectorNum = LongVector.createRandom(num, SECURE_RANDOM);
            LongVector copyVectorNum = vectorNum.copy();
            LongVector splitVectorNum = vectorNum.split(num - 1);
            splitVectorNum.merge(vectorNum);
            Assert.assertEquals(copyVectorNum, splitVectorNum);
            // split half
            LongVector vectorHalf = LongVector.createRandom(num, SECURE_RANDOM);
            LongVector copyVectorHalf = vectorHalf.copy();
            LongVector splitVectorHalf = vectorHalf.split(num / 2);
            splitVectorHalf.merge(vectorHalf);
            Assert.assertEquals(copyVectorHalf, splitVectorHalf);
        }
    }

    @Test
    public void testSplitMergeList() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplitMergeList(num);
        }
    }

    public void testSplitMergeList(int num){
        LongVector[] tmp = IntStream.range(0, num).mapToObj(i ->
            LongVector.createRandom(SECURE_RANDOM.nextInt(MAX_NUM) + 1, SECURE_RANDOM)).toArray(LongVector[]::new);
        LongVector mergeRes = LongVector.merge(tmp);
        LongVector[] splitRes = mergeRes.split(Arrays.stream(tmp).mapToInt(LongVector::getNum).toArray());
        MathPreconditions.checkEqual("splitRes.length", "tmp.length", splitRes.length, tmp.length);
        for(int i = 0; i < tmp.length; i++){
            Assert.assertEquals(splitRes[i], tmp[i]);
        }
    }

    @Test
    public void testOperateElementsByInterval() {
        LongVector origin = LongVector.createRandom(MAX_NUM, SECURE_RANDOM);
        Assert.assertThrows(IllegalArgumentException.class, () -> origin.getElementsByInterval(0, 0, 0));
        for(int i = 0; i < 10; i++){
            int tmpSep = SECURE_RANDOM.nextInt(MAX_NUM - 1) + 1;
            int startPos = SECURE_RANDOM.nextInt(MAX_NUM - tmpSep);
            int num = (origin.getNum() - startPos) / tmpSep;
            LongVector tmp = origin.getElementsByInterval(startPos, num, tmpSep);
            LongVector originCopy = origin.copy();
            origin.setElementsByInterval(tmp, startPos, num, tmpSep);
            Assert.assertEquals(originCopy, origin);
        }
    }

    @Test
    public void testDisplay() {
        // empty
        LongVector vectorEmpty = LongVector.createEmpty();
        LOGGER.info(vectorEmpty.toString());
        // num = 1
        LongVector vector1 = LongVector.createRandom(1, SECURE_RANDOM);
        LOGGER.info(vector1.toString());
        // num = DISPLAY_NUM
        LongVector vectorDisplayNum = LongVector.createRandom(MatrixUtils.DISPLAY_NUM, SECURE_RANDOM);
        LOGGER.info(vectorDisplayNum.toString());
        // num = DISPLAY_NUM + 1
        LongVector vectorDisplayNum1 = LongVector.createRandom(MatrixUtils.DISPLAY_NUM + 1, SECURE_RANDOM);
        LOGGER.info(vectorDisplayNum1.toString());
    }
}
