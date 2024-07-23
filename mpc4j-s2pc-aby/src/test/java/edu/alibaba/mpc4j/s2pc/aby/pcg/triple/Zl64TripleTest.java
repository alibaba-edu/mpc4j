package edu.alibaba.mpc4j.s2pc.aby.pcg.triple;

import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zl64 triple tests.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
public class Zl64TripleTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 32;
    /**
     * Zl64
     */
    private final Zl64 zl64;
    /**
     * large Zl64
     */
    private final Zl64 largeZl64;
    /**
     * Zl64 array
     */
    private final Zl64[] zl64Array;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Zl64TripleTest() {
        zl64 = Zl64Factory.createInstance(EnvType.STANDARD, 32);
        largeZl64 = Zl64Factory.createInstance(EnvType.STANDARD, 40);
        zl64Array = IntStream.range(1, LongUtils.MAX_L_FOR_MODULE_N)
            .mapToObj(l -> Zl64Factory.createInstance(EnvType.STANDARD, l))
            .toArray(Zl64[]::new);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        int num = 12;
        // create triples with mis-matched num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] as = Zl64Vector.createRandom(zl64, num - 1, secureRandom).getElements();
            long[] bs = Zl64Vector.createRandom(zl64, num, secureRandom).getElements();
            long[] cs = Zl64Vector.createRandom(zl64, num + 1, secureRandom).getElements();
            Zl64Triple.create(zl64, as, bs, cs);
        });
        // create triples with large element
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] as = Zl64Vector.createRandom(zl64, num, secureRandom).getElements();
            as[0] = zl64.getRangeBound() + 1L;
            long[] bs = Zl64Vector.createRandom(zl64, num, secureRandom).getElements();
            long[] cs = Zl64Vector.createRandom(zl64, num, secureRandom).getElements();
            Zl64Triple.create(zl64, as, bs, cs);
        });
        // create triples with negative element
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] as = Zl64Vector.createRandom(zl64, num, secureRandom).getElements();
            as[0] = -1L;
            long[] bs = Zl64Vector.createRandom(zl64, num, secureRandom).getElements();
            long[] cs = Zl64Vector.createRandom(zl64, num, secureRandom).getElements();
            Zl64Triple.create(zl64, as, bs, cs);
        });
    }

    @Test
    public void testIllegalUpdate() {
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zl64Triple triple = Zl64Triple.createRandom(zl64, 4, secureRandom);
            triple.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zl64Triple triple = Zl64Triple.createRandom(zl64, 4, secureRandom);
            triple.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zl64Triple triple = Zl64Triple.createRandom(zl64, 4, secureRandom);
            triple.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zl64Triple triple = Zl64Triple.createRandom(zl64, 4, secureRandom);
            triple.reduce(5);
        });
        // merge two vector with different p
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zl64Triple triple = Zl64Triple.createRandom(zl64, 4, secureRandom);
            Zl64Triple mergeTriple = Zl64Triple.createRandom(largeZl64, 4, secureRandom);
            triple.merge(mergeTriple);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        Zl64Triple senderTriple = Zl64Triple.createRandom(zl64, num, secureRandom);
        Zl64Triple receiverTriple = Zl64Triple.createRandom(senderTriple, secureRandom);
        TripleTestUtils.assertOutput(zl64, num, senderTriple, receiverTriple);
    }

    @Test
    public void testReduce() {
        for (Zl64 zl64 : zl64Array) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testReduce(zl64, num);
            }
        }
    }

    private void testReduce(Zl64 zl64, int num) {
        // reduce 1
        Zl64Triple triple1 = Zl64Triple.createRandom(zl64, num, secureRandom);
        triple1.reduce(1);
        TripleTestUtils.assertOutput(zl64, 1, triple1, Zl64Triple.createRandom(triple1, secureRandom));
        // reduce the same num
        Zl64Triple tripleAll = Zl64Triple.createRandom(zl64, num, secureRandom);
        tripleAll.reduce(num);
        TripleTestUtils.assertOutput(zl64, num, tripleAll, Zl64Triple.createRandom(tripleAll, secureRandom));
        if (num > 1) {
            // reduce num - 1
            Zl64Triple tripleNum = Zl64Triple.createRandom(zl64, num, secureRandom);
            tripleNum.reduce(num - 1);
            TripleTestUtils.assertOutput(zl64, num - 1, tripleNum, Zl64Triple.createRandom(tripleNum, secureRandom));
            // reduce half
            Zl64Triple tripleHalf = Zl64Triple.createRandom(zl64, num, secureRandom);
            tripleHalf.reduce(num / 2);
            TripleTestUtils.assertOutput(zl64, num / 2, tripleHalf, Zl64Triple.createRandom(tripleHalf, secureRandom));
        }
    }

    @Test
    public void testMerge() {
        for (Zl64 zl64 : zl64Array) {
            for (int num1 = 0; num1 < MAX_NUM; num1++) {
                for (int num2 = 0; num2 < MAX_NUM; num2++) {
                    testMerge(zl64, num1, num2);
                }
            }
        }
    }

    private void testMerge(Zl64 zl64, int num1, int num2) {
        Zl64Triple triple = Zl64Triple.createRandom(zl64, num1, secureRandom);
        Zl64Triple mergerTriple = Zl64Triple.createRandom(zl64, num2, secureRandom);
        triple.merge(mergerTriple);
        TripleTestUtils.assertOutput(zl64, num1 + num2, triple, Zl64Triple.createRandom(triple, secureRandom));
    }

    @Test
    public void testSplit() {
        for (Zl64 zl64 : zl64Array) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testSplit(zl64, num);
            }
        }
    }

    private void testSplit(Zl64 zl64, int num) {
        // split 1
        Zl64Triple triple1 = Zl64Triple.createRandom(zl64, num, secureRandom);
        Zl64Triple splitTriple1 = triple1.split(1);
        TripleTestUtils.assertOutput(zl64, num - 1, triple1, Zl64Triple.createRandom(triple1, secureRandom));
        TripleTestUtils.assertOutput(zl64, 1, splitTriple1, Zl64Triple.createRandom(splitTriple1, secureRandom));
        // split num
        Zl64Triple tripleAll = Zl64Triple.createRandom(zl64, num, secureRandom);
        Zl64Triple splitTripleAll = tripleAll.split(num);
        TripleTestUtils.assertOutput(zl64, 0, tripleAll, Zl64Triple.createRandom(tripleAll, secureRandom));
        TripleTestUtils.assertOutput(zl64, num, splitTripleAll, Zl64Triple.createRandom(splitTripleAll, secureRandom));
        if (num > 1) {
            // split num - 1
            Zl64Triple tripleNum = Zl64Triple.createRandom(zl64, num, secureRandom);
            Zl64Triple splitTripleNum = tripleNum.split(num - 1);
            TripleTestUtils.assertOutput(zl64, 1, tripleNum, Zl64Triple.createRandom(tripleNum, secureRandom));
            TripleTestUtils.assertOutput(zl64, num - 1, splitTripleNum, Zl64Triple.createRandom(splitTripleNum, secureRandom));
            // split half
            Zl64Triple tripleHalf = Zl64Triple.createRandom(zl64, num, secureRandom);
            Zl64Triple splitTripleHalf = tripleHalf.split(num / 2);
            TripleTestUtils.assertOutput(zl64, num - num / 2, tripleHalf, Zl64Triple.createRandom(tripleHalf, secureRandom));
            TripleTestUtils.assertOutput(zl64, num / 2, splitTripleHalf, Zl64Triple.createRandom(splitTripleHalf, secureRandom));
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
        Zl64Triple triple1 = Zl64Triple.createRandom(zl64, num, secureRandom);
        Zl64Triple copyTriple1 = triple1.copy();
        Zl64Triple splitTriple1 = triple1.split(1);
        triple1.merge(splitTriple1);
        Assert.assertEquals(copyTriple1, triple1);
        // split and merge all
        Zl64Triple tripleAll = Zl64Triple.createRandom(zl64, num, secureRandom);
        Zl64Triple copyTripleAll = tripleAll.copy();
        Zl64Triple splitTripleAll = tripleAll.split(num);
        tripleAll.merge(splitTripleAll);
        Assert.assertEquals(copyTripleAll, tripleAll);
        if (num > 1) {
            // split and merge num - 1
            Zl64Triple tripleNum = Zl64Triple.createRandom(zl64, num, secureRandom);
            Zl64Triple copyTripleNum = tripleNum.copy();
            Zl64Triple splitTripleNum = tripleNum.split(num - 1);
            tripleNum.merge(splitTripleNum);
            Assert.assertEquals(copyTripleNum, tripleNum);
            // split half
            Zl64Triple tripleHalf = Zl64Triple.createRandom(zl64, num, secureRandom);
            Zl64Triple copyTripleHalf = tripleHalf.copy();
            Zl64Triple splitTripleHalf = tripleHalf.split(num / 2);
            tripleHalf.merge(splitTripleHalf);
            Assert.assertEquals(copyTripleHalf, tripleHalf);
        }
    }
}
