package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.StructureUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zl64 vector test.
 *
 * @author Weiran Liu
 * @date 2024/5/25
 */
public class Zl64VectorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zp64VectorTest.class);
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 100;
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 64;
    /**
     * Zl64 instance
     */
    private final Zl64 zl64;
    /**
     * large Zp64 instance
     */
    private final Zl64 largeZl64;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Zl64VectorTest() {
        zl64 = Zl64Factory.createInstance(EnvType.STANDARD, 32);
        largeZl64 = Zl64Factory.createInstance(EnvType.STANDARD, 40);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        // create a vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> Zl64Vector.create(zl64, new long[0]));
        int num = 12;
        // create a random vector with num = 0
        Assert.assertThrows(IllegalArgumentException.class, () -> Zl64Vector.createRandom(zl64, 0, secureRandom));
        // create a vector with invalid data
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] data = IntStream.range(0, num).mapToLong(index -> -1).toArray();
            Zl64Vector.create(zl64, data);
        });
    }

    @Test
    public void testIllegalUpdate() {
        int num = 4;
        Zl64Vector vector = Zl64Vector.createRandom(zl64, num, secureRandom);
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
            Zl64Vector mergeVector = Zl64Vector.createRandom(largeZl64, num, secureRandom);
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
        Zl64Vector vector1 = Zl64Vector.createRandom(zl64, num, secureRandom);
        vector1.reduce(1);
        Assert.assertEquals(1, vector1.getNum());
        // reduce all
        Zl64Vector vectorAll = Zl64Vector.createRandom(zl64, num, secureRandom);
        vectorAll.reduce(num);
        Assert.assertEquals(num, vectorAll.getNum());
        if (num > 1) {
            // reduce num - 1
            Zl64Vector vectorNum = Zl64Vector.createRandom(zl64, num, secureRandom);
            vectorNum.reduce(num - 1);
            Assert.assertEquals(num - 1, vectorNum.getNum());
            // reduce half
            Zl64Vector vectorHalf = Zl64Vector.createRandom(zl64, num, secureRandom);
            vectorHalf.reduce(num / 2);
            Assert.assertEquals(num / 2, vectorHalf.getNum());
        }
    }

    @Test
    public void testAllEmptyMerge() {
        Zl64Vector vector = Zl64Vector.createEmpty(zl64);
        Zl64Vector mergeVector = Zl64Vector.createEmpty(zl64);
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
        Zl64Vector vector = Zl64Vector.createEmpty(zl64);
        Zl64Vector mergeVector = Zl64Vector.createRandom(zl64, num, secureRandom);
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
        Zl64Vector vector = Zl64Vector.createRandom(zl64, num, secureRandom);
        Zl64Vector mergeVector = Zl64Vector.createEmpty(zl64);
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
        Zl64Vector vector = Zl64Vector.createRandom(zl64, num1, secureRandom);
        Zl64Vector mergeVector = Zl64Vector.createRandom(zl64, num2, secureRandom);
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
        Zl64Vector vector1 = Zl64Vector.createRandom(zl64, num, secureRandom);
        Zl64Vector splitVector1 = vector1.split(1);
        Assert.assertEquals(num - 1, vector1.getNum());
        Assert.assertEquals(1, splitVector1.getNum());
        // split all
        Zl64Vector vectorAll = Zl64Vector.createRandom(zl64, num, secureRandom);
        Zl64Vector splitVectorAll = vectorAll.split(num);
        Assert.assertEquals(0, vectorAll.getNum());
        Assert.assertEquals(num, splitVectorAll.getNum());
        if (num > 1) {
            // split num - 1
            Zl64Vector vectorNum = Zl64Vector.createRandom(zl64, num, secureRandom);
            Zl64Vector splitVectorNum = vectorNum.split(num - 1);
            Assert.assertEquals(1, vectorNum.getNum());
            Assert.assertEquals(num - 1, splitVectorNum.getNum());
            // split half
            Zl64Vector vectorHalf = Zl64Vector.createRandom(zl64, num, secureRandom);
            Zl64Vector splitVectorHalf = vectorHalf.split(num / 2);
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
        Zl64Vector vector1 = Zl64Vector.createRandom(zl64, num, secureRandom);
        Zl64Vector copyVector1 = vector1.copy();
        Zl64Vector splitVector1 = vector1.split(1);
        vector1.merge(splitVector1);
        Assert.assertEquals(copyVector1, vector1);
        // split and merge all
        Zl64Vector vectorAll = Zl64Vector.createRandom(zl64, num, secureRandom);
        Zl64Vector copyVectorAll = vectorAll.copy();
        Zl64Vector splitVectorAll = vectorAll.split(num);
        vectorAll.merge(splitVectorAll);
        Assert.assertEquals(copyVectorAll, vectorAll);
        if (num > 1) {
            // split and merge num - 1
            Zl64Vector vectorNum = Zl64Vector.createRandom(zl64, num, secureRandom);
            Zl64Vector copyVectorNum = vectorNum.copy();
            Zl64Vector splitVectorNum = vectorNum.split(num - 1);
            vectorNum.merge(splitVectorNum);
            Assert.assertEquals(copyVectorNum, vectorNum);
            // split half
            Zl64Vector vectorHalf = Zl64Vector.createRandom(zl64, num, secureRandom);
            Zl64Vector copyVectorHalf = vectorHalf.copy();
            Zl64Vector splitVectorHalf = vectorHalf.split(num / 2);
            vectorHalf.merge(splitVectorHalf);
            Assert.assertEquals(copyVectorHalf, vectorHalf);
        }
    }

    @Test
    public void testLazyOperations() {
        Zl64Vector expect, actual;
        // addition
        expect = Zl64Vector.createZeros(zl64, MAX_NUM);
        actual = Zl64Vector.createZeros(zl64, MAX_NUM);
        for (int i = 0; i < RANDOM_ROUND; i++) {
            Zl64Vector random = Zl64Vector.createRandom(zl64, MAX_NUM, secureRandom);
            expect = expect.add(random);
            actual = actual.lazyAdd(random);
        }
        actual.module();
        Assert.assertEquals(expect, actual);
        // negation
        expect = Zl64Vector.createRandom(zl64, MAX_NUM, secureRandom);
        actual = expect.copy();
        expect = expect.neg();
        actual = actual.lazyNeg();
        actual.module();
        Assert.assertEquals(expect, actual);
        // subtraction
        expect = Zl64Vector.createZeros(zl64, MAX_NUM);
        actual = Zl64Vector.createZeros(zl64, MAX_NUM);
        for (int i = 0; i < RANDOM_ROUND; i++) {
            Zl64Vector random = Zl64Vector.createRandom(zl64, MAX_NUM, secureRandom);
            expect = expect.sub(random);
            actual = actual.lazySub(random);
        }
        actual.module();
        Assert.assertEquals(expect, actual);
        // multiplication
        expect = Zl64Vector.createZeros(zl64, MAX_NUM);
        actual = Zl64Vector.createZeros(zl64, MAX_NUM);
        for (int i = 0; i < RANDOM_ROUND; i++) {
            Zl64Vector random = Zl64Vector.createRandom(zl64, MAX_NUM, secureRandom);
            expect = expect.mul(random);
            actual = actual.lazyMul(random);
        }
        actual.module();
        Assert.assertEquals(expect, actual);

        // in-place addition
        expect = Zl64Vector.createZeros(zl64, MAX_NUM);
        actual = Zl64Vector.createZeros(zl64, MAX_NUM);
        for (int i = 0; i < RANDOM_ROUND; i++) {
            Zl64Vector random = Zl64Vector.createRandom(zl64, MAX_NUM, secureRandom);
            expect.addi(random);
            actual.lazyAddi(random);
        }
        actual.module();
        Assert.assertEquals(expect, actual);
        // in-place negation
        expect = Zl64Vector.createRandom(zl64, MAX_NUM, secureRandom);
        actual = expect.copy();
        expect.negi();
        actual.lazyNegi();
        actual.module();
        Assert.assertEquals(expect, actual);
        // in-place subtraction
        expect = Zl64Vector.createZeros(zl64, MAX_NUM);
        actual = Zl64Vector.createZeros(zl64, MAX_NUM);
        for (int i = 0; i < RANDOM_ROUND; i++) {
            Zl64Vector random = Zl64Vector.createRandom(zl64, MAX_NUM, secureRandom);
            expect.subi(random);
            actual.lazySubi(random);
        }
        actual.module();
        Assert.assertEquals(expect, actual);
        // in-place multiplication
        expect = Zl64Vector.createZeros(zl64, MAX_NUM);
        actual = Zl64Vector.createZeros(zl64, MAX_NUM);
        for (int i = 0; i < RANDOM_ROUND; i++) {
            Zl64Vector random = Zl64Vector.createRandom(zl64, MAX_NUM, secureRandom);
            expect.muli(random);
            actual.lazyMuli(random);
        }
        actual.module();
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void testDisplay() {
        // empty
        Zl64Vector vectorEmpty = Zl64Vector.createEmpty(zl64);
        LOGGER.info(vectorEmpty.toString());
        // num = 1
        Zl64Vector vector1 = Zl64Vector.createRandom(zl64, 1, secureRandom);
        LOGGER.info(vector1.toString());
        // num = DISPLAY_NUM
        Zl64Vector vectorDisplayNum = Zl64Vector.createRandom(zl64, StructureUtils.DISPLAY_NUM, secureRandom);
        LOGGER.info(vectorDisplayNum.toString());
        // num = DISPLAY_NUM + 1
        Zl64Vector vectorDisplayNum1 = Zl64Vector.createRandom(zl64, StructureUtils.DISPLAY_NUM + 1, secureRandom);
        LOGGER.info(vectorDisplayNum1.toString());
    }
}
