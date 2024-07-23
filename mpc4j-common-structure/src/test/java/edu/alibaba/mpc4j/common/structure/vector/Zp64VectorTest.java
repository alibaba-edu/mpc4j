package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.StructureUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zp64 vector test.
 *
 * @author Weiran Liu
 * @date 2024/5/25
 */
public class Zp64VectorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zp64VectorTest.class);
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 64;
    /**
     * Zp64 instance
     */
    private final Zp64 zp64;
    /**
     * large Zp64 instance
     */
    private final Zp64 largeZp64;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Zp64VectorTest() {
        zp64 = Zp64Factory.createInstance(EnvType.STANDARD, 32);
        largeZp64 = Zp64Factory.createInstance(EnvType.STANDARD, 40);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        // create a vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> Zp64Vector.create(zp64, new long[0]));
        int num = 12;
        // create a random vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> Zp64Vector.createRandom(zp64, 0, secureRandom));
        // create a vector with invalid data
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] data = IntStream.range(0, num).mapToLong(index -> -1).toArray();
            Zp64Vector.create(zp64, data);
        });
    }

    @Test
    public void testIllegalUpdate() {
        int num = 4;
        Zp64Vector vector = Zp64Vector.createRandom(zp64, num, secureRandom);
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
            Zp64Vector mergeVector = Zp64Vector.createRandom(largeZp64, num, secureRandom);
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
        Zp64Vector vector1 = Zp64Vector.createRandom(zp64, num, secureRandom);
        vector1.reduce(1);
        Assert.assertEquals(1, vector1.getNum());
        // reduce all
        Zp64Vector vectorAll = Zp64Vector.createRandom(zp64, num, secureRandom);
        vectorAll.reduce(num);
        Assert.assertEquals(num, vectorAll.getNum());
        if (num > 1) {
            // reduce num - 1
            Zp64Vector vectorNum = Zp64Vector.createRandom(zp64, num, secureRandom);
            vectorNum.reduce(num - 1);
            Assert.assertEquals(num - 1, vectorNum.getNum());
            // reduce half
            Zp64Vector vectorHalf = Zp64Vector.createRandom(zp64, num, secureRandom);
            vectorHalf.reduce(num / 2);
            Assert.assertEquals(num / 2, vectorHalf.getNum());
        }
    }

    @Test
    public void testAllEmptyMerge() {
        Zp64Vector vector = Zp64Vector.createEmpty(zp64);
        Zp64Vector mergeVector = Zp64Vector.createEmpty(zp64);
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
        Zp64Vector vector = Zp64Vector.createEmpty(zp64);
        Zp64Vector mergeVector = Zp64Vector.createRandom(zp64, num, secureRandom);
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
        Zp64Vector vector = Zp64Vector.createRandom(zp64, num, secureRandom);
        Zp64Vector mergeVector = Zp64Vector.createEmpty(zp64);
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
        Zp64Vector vector = Zp64Vector.createRandom(zp64, num1, secureRandom);
        Zp64Vector mergeVector = Zp64Vector.createRandom(zp64, num2, secureRandom);
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
        Zp64Vector vector1 = Zp64Vector.createRandom(zp64, num, secureRandom);
        Zp64Vector splitVector1 = vector1.split(1);
        Assert.assertEquals(num - 1, vector1.getNum());
        Assert.assertEquals(1, splitVector1.getNum());
        // split all
        Zp64Vector vectorAll = Zp64Vector.createRandom(zp64, num, secureRandom);
        Zp64Vector splitVectorAll = vectorAll.split(num);
        Assert.assertEquals(0, vectorAll.getNum());
        Assert.assertEquals(num, splitVectorAll.getNum());
        if (num > 1) {
            // split num - 1
            Zp64Vector vectorNum = Zp64Vector.createRandom(zp64, num, secureRandom);
            Zp64Vector splitVectorNum = vectorNum.split(num - 1);
            Assert.assertEquals(1, vectorNum.getNum());
            Assert.assertEquals(num - 1, splitVectorNum.getNum());
            // split half
            Zp64Vector vectorHalf = Zp64Vector.createRandom(zp64, num, secureRandom);
            Zp64Vector splitVectorHalf = vectorHalf.split(num / 2);
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
        Zp64Vector vector1 = Zp64Vector.createRandom(zp64, num, secureRandom);
        Zp64Vector copyVector1 = vector1.copy();
        Zp64Vector splitVector1 = vector1.split(1);
        vector1.merge(splitVector1);
        Assert.assertEquals(copyVector1, vector1);
        // split and merge all
        Zp64Vector vectorAll = Zp64Vector.createRandom(zp64, num, secureRandom);
        Zp64Vector copyVectorAll = vectorAll.copy();
        Zp64Vector splitVectorAll = vectorAll.split(num);
        vectorAll.merge(splitVectorAll);
        Assert.assertEquals(copyVectorAll, vectorAll);
        if (num > 1) {
            // split and merge num - 1
            Zp64Vector vectorNum = Zp64Vector.createRandom(zp64, num, secureRandom);
            Zp64Vector copyVectorNum = vectorNum.copy();
            Zp64Vector splitVectorNum = vectorNum.split(num - 1);
            vectorNum.merge(splitVectorNum);
            Assert.assertEquals(copyVectorNum, vectorNum);
            // split half
            Zp64Vector vectorHalf = Zp64Vector.createRandom(zp64, num, secureRandom);
            Zp64Vector copyVectorHalf = vectorHalf.copy();
            Zp64Vector splitVectorHalf = vectorHalf.split(num / 2);
            vectorHalf.merge(splitVectorHalf);
            Assert.assertEquals(copyVectorHalf, vectorHalf);
        }
    }

    @Test
    public void testDisplay() {
        // empty
        Zp64Vector vectorEmpty = Zp64Vector.createEmpty(zp64);
        LOGGER.info(vectorEmpty.toString());
        // num = 1
        Zp64Vector vector1 = Zp64Vector.createRandom(zp64, 1, secureRandom);
        LOGGER.info(vector1.toString());
        // num = DISPLAY_NUM
        Zp64Vector vectorDisplayNum = Zp64Vector.createRandom(zp64, StructureUtils.DISPLAY_NUM, secureRandom);
        LOGGER.info(vectorDisplayNum.toString());
        // num = DISPLAY_NUM + 1
        Zp64Vector vectorDisplayNum1 = Zp64Vector.createRandom(zp64, StructureUtils.DISPLAY_NUM + 1, secureRandom);
        LOGGER.info(vectorDisplayNum1.toString());
    }
}
