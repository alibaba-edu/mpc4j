package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.matrix.MatrixUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zl vector test.
 *
 * @author Weiran Liu
 * @date 2023/5/9
 */
public class ZlVectorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlVectorTest.class);
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
    /**
     * default Zl instance
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, 40);
    /**
     * large Zl instance
     */
    private static final Zl LARGE_ZL = ZlFactory.createInstance(EnvType.STANDARD, 128);

    @Test
    public void testIllegalInputs() {
        // create a vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> ZlVector.create(DEFAULT_ZL, new BigInteger[0]));
        // create a random vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> ZlVector.createRandom(DEFAULT_ZL, 0, SECURE_RANDOM));
        // create a vector with invalid data
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] data = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> DEFAULT_ZL.createZero().subtract(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            ZlVector.create(DEFAULT_ZL, data);
        });
        ZlVector vector = ZlVector.createRandom(DEFAULT_ZL, DEFAULT_NUM, SECURE_RANDOM);
        // split vector with split num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.split(0));
        // split vector with split num > num
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.split(DEFAULT_NUM + 1));
        // reduce vector with reduce num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.reduce(0));
        // reduce vector with reduce num > num
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.reduce(DEFAULT_NUM + 1));
        // merge two vector with different l
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlVector mergeVector = ZlVector.createRandom(LARGE_ZL, DEFAULT_NUM, SECURE_RANDOM);
            vector.merge(mergeVector);
        });
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        // reduce 1
        ZlVector vector1 = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
        vector1.reduce(1);
        Assert.assertEquals(1, vector1.getNum());
        // reduce all
        ZlVector vectorAll = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
        vectorAll.reduce(num);
        Assert.assertEquals(num, vectorAll.getNum());
        if (num > 1) {
            // reduce num - 1
            ZlVector vectorNum = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
            vectorNum.reduce(num - 1);
            Assert.assertEquals(num - 1, vectorNum.getNum());
            // reduce half
            ZlVector vectorHalf = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
            vectorHalf.reduce(num / 2);
            Assert.assertEquals(num / 2, vectorHalf.getNum());
        }
    }

    @Test
    public void testAllEmptyMerge() {
        ZlVector vector = ZlVector.createEmpty(DEFAULT_ZL);
        ZlVector mergeVector = ZlVector.createEmpty(DEFAULT_ZL);
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
        ZlVector vector = ZlVector.createEmpty(DEFAULT_ZL);
        ZlVector mergeVector = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
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
        ZlVector vector = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
        ZlVector mergeVector = ZlVector.createEmpty(DEFAULT_ZL);
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
        ZlVector vector = ZlVector.createRandom(DEFAULT_ZL, num1, SECURE_RANDOM);
        ZlVector mergeVector = ZlVector.createRandom(DEFAULT_ZL, num2, SECURE_RANDOM);
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
        ZlVector vector1 = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
        ZlVector splitVector1 = vector1.split(1);
        Assert.assertEquals(num - 1, vector1.getNum());
        Assert.assertEquals(1, splitVector1.getNum());
        // split all
        ZlVector vectorAll = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
        ZlVector splitVectorAll = vectorAll.split(num);
        Assert.assertEquals(0, vectorAll.getNum());
        Assert.assertEquals(num, splitVectorAll.getNum());
        if (num > 1) {
            // split num - 1
            ZlVector vectorNum = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
            ZlVector splitVectorNum = vectorNum.split(num - 1);
            Assert.assertEquals(1, vectorNum.getNum());
            Assert.assertEquals(num - 1, splitVectorNum.getNum());
            // split half
            ZlVector vectorHalf = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
            ZlVector splitVectorHalf = vectorHalf.split(num / 2);
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
        ZlVector vector1 = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
        ZlVector copyVector1 = vector1.copy();
        ZlVector splitVector1 = vector1.split(1);
        splitVector1.merge(vector1);
        Assert.assertEquals(copyVector1, splitVector1);
        // split and merge all
        ZlVector vectorAll = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
        ZlVector copyVectorAll = vectorAll.copy();
        ZlVector splitVectorAll = vectorAll.split(num);
        splitVectorAll.merge(vectorAll);
        Assert.assertEquals(copyVectorAll, splitVectorAll);
        if (num > 1) {
            // split and merge num - 1
            ZlVector vectorNum = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
            ZlVector copyVectorNum = vectorNum.copy();
            ZlVector splitVectorNum = vectorNum.split(num - 1);
            splitVectorNum.merge(vectorNum);
            Assert.assertEquals(copyVectorNum, splitVectorNum);
            // split half
            ZlVector vectorHalf = ZlVector.createRandom(DEFAULT_ZL, num, SECURE_RANDOM);
            ZlVector copyVectorHalf = vectorHalf.copy();
            ZlVector splitVectorHalf = vectorHalf.split(num / 2);
            splitVectorHalf.merge(vectorHalf);
            Assert.assertEquals(copyVectorHalf, splitVectorHalf);
        }
    }

    @Test
    public void testDisplay() {
        // empty
        ZlVector vectorEmpty = ZlVector.createEmpty(DEFAULT_ZL);
        LOGGER.info(vectorEmpty.toString());
        // num = 1
        ZlVector vector1 = ZlVector.createRandom(DEFAULT_ZL, 1, SECURE_RANDOM);
        LOGGER.info(vector1.toString());
        // num = DISPLAY_NUM
        ZlVector vectorDisplayNum = ZlVector.createRandom(DEFAULT_ZL, MatrixUtils.DISPLAY_NUM, SECURE_RANDOM);
        LOGGER.info(vectorDisplayNum.toString());
        // num = DISPLAY_NUM + 1
        ZlVector vectorDisplayNum1 = ZlVector.createRandom(DEFAULT_ZL, MatrixUtils.DISPLAY_NUM + 1, SECURE_RANDOM);
        LOGGER.info(vectorDisplayNum1.toString());
    }
}
