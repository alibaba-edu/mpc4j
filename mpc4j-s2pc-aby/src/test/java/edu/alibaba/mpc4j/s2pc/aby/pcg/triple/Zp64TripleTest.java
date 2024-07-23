package edu.alibaba.mpc4j.s2pc.aby.pcg.triple;

import edu.alibaba.mpc4j.common.structure.vector.Zp64Vector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zp64 triple tests.
 *
 * @author Weiran Liu
 * @date 2022/11/5
 */
public class Zp64TripleTest {
    /**
     * min num
     */
    private static final int MIN_NUM = 1;
    /**
     * max num
     */
    private static final int MAX_NUM = 32;
    /**
     * Zp64
     */
    private final Zp64 zp64;
    /**
     * large Zp64
     */
    private final Zp64 largeZp64;
    /**
     * Zp64 array
     */
    private final Zp64[] zp64Array;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Zp64TripleTest() {
        zp64 = Zp64Factory.createInstance(EnvType.STANDARD, 32);
        largeZp64 = Zp64Factory.createInstance(EnvType.STANDARD, 40);
        zp64Array = IntStream.range(1, LongUtils.MAX_L_FOR_MODULE_N)
            .mapToObj(l -> Zp64Factory.createInstance(EnvType.STANDARD, l))
            .toArray(Zp64[]::new);
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        int num = 12;
        // create triples with mis-matched num
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] as = Zp64Vector.createRandom(zp64, num - 1, secureRandom).getElements();
            long[] bs = Zp64Vector.createRandom(zp64, num, secureRandom).getElements();
            long[] cs = Zp64Vector.createRandom(zp64, num + 1, secureRandom).getElements();
            Zp64Triple.create(zp64, as, bs, cs);
        });
        // create triples with large element
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] as = Zp64Vector.createRandom(zp64, num, secureRandom).getElements();
            as[0] = zp64.getPrime();
            long[] bs = Zp64Vector.createRandom(zp64, num, secureRandom).getElements();
            long[] cs = Zp64Vector.createRandom(zp64, num, secureRandom).getElements();
            Zp64Triple.create(zp64, as, bs, cs);
        });
        // create triples with negative element
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            long[] as = Zp64Vector.createRandom(zp64, num, secureRandom).getElements();
            as[0] = -1;
            long[] bs = Zp64Vector.createRandom(zp64, num, secureRandom).getElements();
            long[] cs = Zp64Vector.createRandom(zp64, num, secureRandom).getElements();
            Zp64Triple.create(zp64, as, bs, cs);
        });
    }

    @Test
    public void testIllegalUpdate() {
        // split with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64Triple triple = Zp64Triple.createRandom(zp64, 4, secureRandom);
            triple.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64Triple triple = Zp64Triple.createRandom(zp64, 4, secureRandom);
            triple.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64Triple triple = Zp64Triple.createRandom(zp64, 4, secureRandom);
            triple.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64Triple triple = Zp64Triple.createRandom(zp64, 4, secureRandom);
            triple.reduce(5);
        });
        // merge two vector with different p
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Zp64Triple triple = Zp64Triple.createRandom(zp64, 4, secureRandom);
            Zp64Triple mergeTriple = Zp64Triple.createRandom(largeZp64, 4, secureRandom);
            triple.merge(mergeTriple);
        });
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        Zp64Triple senderTriple = Zp64Triple.createRandom(zp64, num, secureRandom);
        Zp64Triple receiverTriple = Zp64Triple.createRandom(senderTriple, secureRandom);
        TripleTestUtils.assertOutput(zp64, num, senderTriple, receiverTriple);
    }

    @Test
    public void testReduce() {
        for (Zp64 zp64 : zp64Array) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testReduce(zp64, num);
            }
        }
    }

    private void testReduce(Zp64 zp64, int num) {
        // reduce 1
        Zp64Triple triple1 = Zp64Triple.createRandom(zp64, num, secureRandom);
        triple1.reduce(1);
        TripleTestUtils.assertOutput(zp64, 1, triple1, Zp64Triple.createRandom(triple1, secureRandom));
        // reduce all
        Zp64Triple tripleAll = Zp64Triple.createRandom(zp64, num, secureRandom);
        tripleAll.reduce(num);
        TripleTestUtils.assertOutput(zp64, num, tripleAll, Zp64Triple.createRandom(tripleAll, secureRandom));
        if (num > 1) {
            // reduce num - 1
            Zp64Triple tripleNum = Zp64Triple.createRandom(zp64, num, secureRandom);
            tripleNum.reduce(num - 1);
            TripleTestUtils.assertOutput(zp64, num - 1, tripleNum, Zp64Triple.createRandom(tripleNum, secureRandom));
            // reduce half
            Zp64Triple tripleHalf = Zp64Triple.createRandom(zp64, num, secureRandom);
            tripleHalf.reduce(num / 2);
            TripleTestUtils.assertOutput(zp64, num / 2, tripleHalf, Zp64Triple.createRandom(tripleHalf, secureRandom));
        }
    }

    @Test
    public void testMerge() {
        for (Zp64 zp64 : zp64Array) {
            for (int num1 = 0; num1 < MAX_NUM; num1++) {
                for (int num2 = 0; num2 < MAX_NUM; num2++) {
                    testMerge(zp64, num1, num2);
                }
            }
        }
    }

    private void testMerge(Zp64 zp64, int num1, int num2) {
        Zp64Triple triple = Zp64Triple.createRandom(zp64, num1, secureRandom);
        Zp64Triple mergerTriple = Zp64Triple.createRandom(zp64, num2, secureRandom);
        triple.merge(mergerTriple);
        TripleTestUtils.assertOutput(zp64, num1 + num2, triple, Zp64Triple.createRandom(triple, secureRandom));
    }

    @Test
    public void testSplit() {
        for (Zp64 zp64 : zp64Array) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testSplit(zp64, num);
            }
        }
    }

    private void testSplit(Zp64 zp64, int num) {
        // split 1
        Zp64Triple triple1 = Zp64Triple.createRandom(zp64, num, secureRandom);
        Zp64Triple splitTriple1 = triple1.split(1);
        TripleTestUtils.assertOutput(zp64, num - 1, triple1, Zp64Triple.createRandom(triple1, secureRandom));
        TripleTestUtils.assertOutput(zp64, 1, splitTriple1, Zp64Triple.createRandom(splitTriple1, secureRandom));
        // split all
        Zp64Triple tripleAll = Zp64Triple.createRandom(zp64, num, secureRandom);
        Zp64Triple splitTripleAll = tripleAll.split(num);
        TripleTestUtils.assertOutput(zp64, 0, tripleAll, Zp64Triple.createRandom(tripleAll, secureRandom));
        TripleTestUtils.assertOutput(zp64, num, splitTripleAll, Zp64Triple.createRandom(splitTripleAll, secureRandom));
        if (num > 1) {
            // split num - 1
            Zp64Triple tripleNum = Zp64Triple.createRandom(zp64, num, secureRandom);
            Zp64Triple splitTripleNum = tripleNum.split(num - 1);
            TripleTestUtils.assertOutput(zp64, 1, tripleNum, Zp64Triple.createRandom(tripleNum, secureRandom));
            TripleTestUtils.assertOutput(zp64, num - 1, splitTripleNum, Zp64Triple.createRandom(splitTripleNum, secureRandom));
            // split half
            Zp64Triple tripleHalf = Zp64Triple.createRandom(zp64, num, secureRandom);
            Zp64Triple splitTripleHalf = tripleHalf.split(num / 2);
            TripleTestUtils.assertOutput(zp64, num - num / 2, tripleHalf, Zp64Triple.createRandom(tripleHalf, secureRandom));
            TripleTestUtils.assertOutput(zp64, num / 2, splitTripleHalf, Zp64Triple.createRandom(splitTripleHalf, secureRandom));
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
        Zp64Triple triple1 = Zp64Triple.createRandom(zp64, num, secureRandom);
        Zp64Triple copyTriple1 = triple1.copy();
        Zp64Triple splitTriple1 = triple1.split(1);
        triple1.merge(splitTriple1);
        Assert.assertEquals(copyTriple1, triple1);
        // split and merge all
        Zp64Triple tripleAll = Zp64Triple.createRandom(zp64, num, secureRandom);
        Zp64Triple copyTripleAll = tripleAll.copy();
        Zp64Triple splitTripleAll = tripleAll.split(num);
        tripleAll.merge(splitTripleAll);
        Assert.assertEquals(copyTripleAll, tripleAll);
        if (num > 1) {
            // split and merge num - 1
            Zp64Triple tripleNum = Zp64Triple.createRandom(zp64, num, secureRandom);
            Zp64Triple copyTripleNum = tripleNum.copy();
            Zp64Triple splitTripleNum = tripleNum.split(num - 1);
            tripleNum.merge(splitTripleNum);
            Assert.assertEquals(copyTripleNum, tripleNum);
            // split half
            Zp64Triple tripleHalf = Zp64Triple.createRandom(zp64, num, secureRandom);
            Zp64Triple copyTripleHalf = tripleHalf.copy();
            Zp64Triple splitTripleHalf = tripleHalf.split(num / 2);
            tripleHalf.merge(splitTripleHalf);
            Assert.assertEquals(copyTripleHalf, tripleHalf);
        }
    }
}
