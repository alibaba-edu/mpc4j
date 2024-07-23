package edu.alibaba.mpc4j.s2pc.aby.pcg.triple;

import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zl triple tests.
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlTripleTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 32;
    /**
     * Zl
     */
    private final Zl zl;
    /**
     * large Zl
     */
    private final Zl largeZl;
    /**
     * Zl array
     */
    private final Zl[] zlArray;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public ZlTripleTest() {
        zl = ZlFactory.createInstance(EnvType.STANDARD, 32);
        largeZl = ZlFactory.createInstance(EnvType.STANDARD, 40);
        zlArray = IntStream.range(1, 128)
            .mapToObj(l -> ZlFactory.createInstance(EnvType.STANDARD, l))
            .toArray(Zl[]::new);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        int num = 12;
        // create triples with mis-matched num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] as = ZlVector.createRandom(zl, num - 1, secureRandom).getElements();
            BigInteger[] bs = ZlVector.createRandom(zl, num, secureRandom).getElements();
            BigInteger[] cs = ZlVector.createRandom(zl, num + 1, secureRandom).getElements();
            ZlTriple.create(zl, as, bs, cs);
        });
        // create triples with large element
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] as = ZlVector.createRandom(zl, num, secureRandom).getElements();
            as[0] = zl.getRangeBound().add(BigInteger.ONE);
            BigInteger[] bs = ZlVector.createRandom(zl, num, secureRandom).getElements();
            BigInteger[] cs = ZlVector.createRandom(zl, num, secureRandom).getElements();
            ZlTriple.create(zl, as, bs, cs);
        });
        // create triples with negative element
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            BigInteger[] as = ZlVector.createRandom(zl, num, secureRandom).getElements();
            as[0] = BigInteger.ONE.negate();
            BigInteger[] bs = ZlVector.createRandom(zl, num, secureRandom).getElements();
            BigInteger[] cs = ZlVector.createRandom(zl, num, secureRandom).getElements();
            ZlTriple.create(zl, as, bs, cs);
        });
    }

    @Test
    public void testIllegalUpdate() {
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlTriple triple = ZlTriple.createRandom(zl, 4, secureRandom);
            triple.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlTriple triple = ZlTriple.createRandom(zl, 4, secureRandom);
            triple.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlTriple triple = ZlTriple.createRandom(zl, 4, secureRandom);
            triple.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlTriple triple = ZlTriple.createRandom(zl, 4, secureRandom);
            triple.reduce(5);
        });
        // merge two vector with different l
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            ZlTriple triple = ZlTriple.createRandom(zl, 4, secureRandom);
            ZlTriple mergeTriple = ZlTriple.createRandom(largeZl, 4, secureRandom);
            triple.merge(mergeTriple);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        ZlTriple senderTriple = ZlTriple.createRandom(zl, num, secureRandom);
        ZlTriple receiverTriple = ZlTriple.createRandom(senderTriple, secureRandom);
        TripleTestUtils.assertOutput(zl, num, senderTriple, receiverTriple);
    }

    @Test
    public void testReduce() {
        for (Zl zl : zlArray) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testReduce(zl, num);
            }
        }
    }

    private void testReduce(Zl zl, int num) {
        // reduce 1
        ZlTriple triple1 = ZlTriple.createRandom(zl, num, secureRandom);
        triple1.reduce(1);
        TripleTestUtils.assertOutput(zl, 1, triple1, ZlTriple.createRandom(triple1, secureRandom));
        // reduce all
        ZlTriple tripleAll = ZlTriple.createRandom(zl, num, secureRandom);
        tripleAll.reduce(num);
        TripleTestUtils.assertOutput(zl, num, tripleAll, ZlTriple.createRandom(tripleAll, secureRandom));
        if (num > 1) {
            // reduce num - 1
            ZlTriple tripleNum = ZlTriple.createRandom(zl, num, secureRandom);
            tripleNum.reduce(num - 1);
            TripleTestUtils.assertOutput(zl, num - 1, tripleNum, ZlTriple.createRandom(tripleNum, secureRandom));
            // reduce half
            ZlTriple tripleHalf = ZlTriple.createRandom(zl, num, secureRandom);
            tripleHalf.reduce(num / 2);
            TripleTestUtils.assertOutput(zl, num / 2, tripleHalf, ZlTriple.createRandom(tripleHalf, secureRandom));
        }
    }

    @Test
    public void testMerge() {
        for (Zl zl : zlArray) {
            for (int num1 = 0; num1 < MAX_NUM; num1++) {
                for (int num2 = 0; num2 < MAX_NUM; num2++) {
                    testMerge(zl, num1, num2);
                }
            }
        }
    }

    private void testMerge(Zl zl, int num1, int num2) {
        ZlTriple triple = ZlTriple.createRandom(zl, num1, secureRandom);
        ZlTriple mergerTriple = ZlTriple.createRandom(zl, num2, secureRandom);
        triple.merge(mergerTriple);
        TripleTestUtils.assertOutput(zl, num1 + num2, triple, ZlTriple.createRandom(triple, secureRandom));
    }

    @Test
    public void testSplit() {
        for (Zl zl : zlArray) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testSplit(zl, num);
            }
        }
    }

    private void testSplit(Zl zl, int num) {
        // split 1
        ZlTriple triple1 = ZlTriple.createRandom(zl, num, secureRandom);
        ZlTriple splitTriple1 = triple1.split(1);
        TripleTestUtils.assertOutput(zl, num - 1, triple1, ZlTriple.createRandom(triple1, secureRandom));
        TripleTestUtils.assertOutput(zl, 1, splitTriple1, ZlTriple.createRandom(splitTriple1, secureRandom));
        // split all
        ZlTriple tripleAll = ZlTriple.createRandom(zl, num, secureRandom);
        ZlTriple splitTripleAll = tripleAll.split(num);
        TripleTestUtils.assertOutput(zl, 0, tripleAll, ZlTriple.createRandom(tripleAll, secureRandom));
        TripleTestUtils.assertOutput(zl, num, splitTripleAll, ZlTriple.createRandom(splitTripleAll, secureRandom));
        if (num > 1) {
            // split num - 1
            ZlTriple tripleNum = ZlTriple.createRandom(zl, num, secureRandom);
            ZlTriple splitTripleNum = tripleNum.split(num - 1);
            TripleTestUtils.assertOutput(zl, 1, tripleNum, ZlTriple.createRandom(tripleNum, secureRandom));
            TripleTestUtils.assertOutput(zl, num - 1, splitTripleNum, ZlTriple.createRandom(splitTripleNum, secureRandom));
            // split half
            ZlTriple tripleHalf = ZlTriple.createRandom(zl, num, secureRandom);
            ZlTriple splitTripleHalf = tripleHalf.split(num / 2);
            TripleTestUtils.assertOutput(zl, num - num / 2, tripleHalf, ZlTriple.createRandom(tripleHalf, secureRandom));
            TripleTestUtils.assertOutput(zl, num / 2, splitTripleHalf, ZlTriple.createRandom(splitTripleHalf, secureRandom));
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
        ZlTriple triple1 = ZlTriple.createRandom(zl, num, secureRandom);
        ZlTriple copyTriple1 = triple1.copy();
        ZlTriple splitTriple1 = triple1.split(1);
        triple1.merge(splitTriple1);
        Assert.assertEquals(copyTriple1, triple1);
        // split and merge all
        ZlTriple tripleAll = ZlTriple.createRandom(zl, num, secureRandom);
        ZlTriple copyTripleAll = tripleAll.copy();
        ZlTriple splitTripleAll = tripleAll.split(num);
        tripleAll.merge(splitTripleAll);
        Assert.assertEquals(copyTripleAll, tripleAll);
        if (num > 1) {
            // split and merge num - 1
            ZlTriple tripleNum = ZlTriple.createRandom(zl, num, secureRandom);
            ZlTriple copyTripleNum = tripleNum.copy();
            ZlTriple splitTripleNum = tripleNum.split(num - 1);
            tripleNum.merge(splitTripleNum);
            Assert.assertEquals(copyTripleNum, tripleNum);
            // split half
            ZlTriple tripleHalf = ZlTriple.createRandom(zl, num, secureRandom);
            ZlTriple copyTripleHalf = tripleHalf.copy();
            ZlTriple splitTripleHalf = tripleHalf.split(num / 2);
            tripleHalf.merge(splitTripleHalf);
            Assert.assertEquals(copyTripleHalf, tripleHalf);
        }
    }
}
