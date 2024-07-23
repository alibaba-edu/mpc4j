package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.StructureUtils;
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
    private final Zl zl;
    /**
     * large Zl instance
     */
    private final Zl largeZl;
    /**
     * the random state
     */
    private final SecureRandom secureRandom;

    public ZlVectorTest() {
        zl = ZlFactory.createInstance(EnvType.STANDARD, 40);
        largeZl = ZlFactory.createInstance(EnvType.STANDARD, 128);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        // create a vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> ZlVector.create(zl, new BigInteger[0]));
        int num = 12;
        // create a random vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> ZlVector.createRandom(zl, 0, secureRandom));
        // create a vector with invalid data
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] data = IntStream.range(0, num)
                .mapToObj(index -> zl.createZero().subtract(BigInteger.ONE))
                .toArray(BigInteger[]::new);
            ZlVector.create(zl, data);
        });
    }

    @Test
    public void testIllegalUpdate() {
        int num = 4;
        ZlVector vector = ZlVector.createRandom(zl, num, secureRandom);
        // split vector with split num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.split(0));
        // split vector with split num > num
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.split(num + 1));
        // reduce vector with reduce num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.reduce(0));
        // reduce vector with reduce num > num
        Assert.assertThrows(IllegalArgumentException.class, () -> vector.reduce(num + 1));
        // merge two vector with different l
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlVector mergeVector = ZlVector.createRandom(largeZl, num, secureRandom);
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
        ZlVector vector1 = ZlVector.createRandom(zl, num, secureRandom);
        vector1.reduce(1);
        Assert.assertEquals(1, vector1.getNum());
        // reduce all
        ZlVector vectorAll = ZlVector.createRandom(zl, num, secureRandom);
        vectorAll.reduce(num);
        Assert.assertEquals(num, vectorAll.getNum());
        if (num > 1) {
            // reduce num - 1
            ZlVector vectorNum = ZlVector.createRandom(zl, num, secureRandom);
            vectorNum.reduce(num - 1);
            Assert.assertEquals(num - 1, vectorNum.getNum());
            // reduce half
            ZlVector vectorHalf = ZlVector.createRandom(zl, num, secureRandom);
            vectorHalf.reduce(num / 2);
            Assert.assertEquals(num / 2, vectorHalf.getNum());
        }
    }

    @Test
    public void testAllEmptyMerge() {
        ZlVector vector = ZlVector.createEmpty(zl);
        ZlVector mergeVector = ZlVector.createEmpty(zl);
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
        ZlVector vector = ZlVector.createEmpty(zl);
        ZlVector mergeVector = ZlVector.createRandom(zl, num, secureRandom);
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
        ZlVector vector = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector mergeVector = ZlVector.createEmpty(zl);
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
        ZlVector vector = ZlVector.createRandom(zl, num1, secureRandom);
        ZlVector mergeVector = ZlVector.createRandom(zl, num2, secureRandom);
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
        ZlVector vector1 = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector splitVector1 = vector1.split(1);
        Assert.assertEquals(num - 1, vector1.getNum());
        Assert.assertEquals(1, splitVector1.getNum());
        // split all
        ZlVector vectorAll = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector splitVectorAll = vectorAll.split(num);
        Assert.assertEquals(0, vectorAll.getNum());
        Assert.assertEquals(num, splitVectorAll.getNum());
        if (num > 1) {
            // split num - 1
            ZlVector vectorNum = ZlVector.createRandom(zl, num, secureRandom);
            ZlVector splitVectorNum = vectorNum.split(num - 1);
            Assert.assertEquals(1, vectorNum.getNum());
            Assert.assertEquals(num - 1, splitVectorNum.getNum());
            // split half
            ZlVector vectorHalf = ZlVector.createRandom(zl, num, secureRandom);
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
        ZlVector vector1 = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector copyVector1 = vector1.copy();
        ZlVector splitVector1 = vector1.split(1);
        vector1.merge(splitVector1);
        Assert.assertEquals(copyVector1, vector1);
        // split and merge all
        ZlVector vectorAll = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector copyVectorAll = vectorAll.copy();
        ZlVector splitVectorAll = vectorAll.split(num);
        vectorAll.merge(splitVectorAll);
        Assert.assertEquals(copyVectorAll, vectorAll);
        if (num > 1) {
            // split and merge num - 1
            ZlVector vectorNum = ZlVector.createRandom(zl, num, secureRandom);
            ZlVector copyVectorNum = vectorNum.copy();
            ZlVector splitVectorNum = vectorNum.split(num - 1);
            vectorNum.merge(splitVectorNum);
            Assert.assertEquals(copyVectorNum, vectorNum);
            // split half
            ZlVector vectorHalf = ZlVector.createRandom(zl, num, secureRandom);
            ZlVector copyVectorHalf = vectorHalf.copy();
            ZlVector splitVectorHalf = vectorHalf.split(num / 2);
            vectorHalf.merge(splitVectorHalf);
            Assert.assertEquals(copyVectorHalf, vectorHalf);
        }
    }

    @Test
    public void testDisplay() {
        // empty
        ZlVector vectorEmpty = ZlVector.createEmpty(zl);
        LOGGER.info(vectorEmpty.toString());
        // num = 1
        ZlVector vector1 = ZlVector.createRandom(zl, 1, secureRandom);
        LOGGER.info(vector1.toString());
        // num = DISPLAY_NUM
        ZlVector vectorDisplayNum = ZlVector.createRandom(zl, StructureUtils.DISPLAY_NUM, secureRandom);
        LOGGER.info(vectorDisplayNum.toString());
        // num = DISPLAY_NUM + 1
        ZlVector vectorDisplayNum1 = ZlVector.createRandom(zl, StructureUtils.DISPLAY_NUM + 1, secureRandom);
        LOGGER.info(vectorDisplayNum1.toString());
    }
}
