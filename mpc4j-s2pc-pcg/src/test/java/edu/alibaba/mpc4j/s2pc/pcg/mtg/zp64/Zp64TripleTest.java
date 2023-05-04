package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
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
    private static final int MAX_NUM = 64;
    /**
     * default Zp64 instance
     */
    private static final Zp64 DEFAULT_ZP64 = Zp64Factory.createInstance(EnvType.STANDARD, 32);
    /**
     * Zp64 array
     */
    private static final Zp64[] ZP64S = IntStream.range(1, LongUtils.MAX_L)
        .mapToObj(l -> Zp64Factory.createInstance(EnvType.STANDARD, l))
        .toArray(Zp64[]::new);

    @Test
    public void testIllegalInputs() {
        // create triple with num = 0
        Assert.assertThrows(AssertionError.class, () ->
            Zp64Triple.create(DEFAULT_ZP64, 0, new long[0], new long[0], new long[0])
        );
        int num = 12;
        long element = DEFAULT_ZP64.createNonZeroRandom(new SecureRandom());
        // create triples with less num
        Assert.assertThrows(AssertionError.class, () -> {
            long[] as = new long[num - 1];
            Arrays.fill(as, element);
            long[] bs = new long[num - 1];
            Arrays.fill(bs, element);
            long[] cs = new long[num - 1];
            Arrays.fill(cs, element);
            Zp64Triple.create(DEFAULT_ZP64, num, as, bs, cs);
        });
        // create triples with large num
        Assert.assertThrows(AssertionError.class, () -> {
            long[] as = new long[num + 1];
            Arrays.fill(as, element);
            long[] bs = new long[num + 1];
            Arrays.fill(bs, element);
            long[] cs = new long[num + 1];
            Arrays.fill(cs, element);
            Zp64Triple.create(DEFAULT_ZP64, num, as, bs, cs);
        });
        // create triples with mis-matched num
        Assert.assertThrows(AssertionError.class, () -> {
            long[] as = new long[num - 1];
            Arrays.fill(as, element);
            long[] bs = new long[num];
            Arrays.fill(bs, element);
            long[] cs = new long[num + 1];
            Arrays.fill(cs, element);
            Zp64Triple.create(DEFAULT_ZP64, num, as, bs, cs);
        });
        // create triples with large element
        Assert.assertThrows(AssertionError.class, () -> {
            long p = DEFAULT_ZP64.getPrime();
            long[] as = new long[num];
            Arrays.fill(as, p);
            long[] bs = new long[num];
            Arrays.fill(bs, element);
            long[] cs = new long[num];
            Arrays.fill(cs, element);
            Zp64Triple.create(DEFAULT_ZP64, num, as, bs, cs);
        });
        // create triples with negative element
        Assert.assertThrows(AssertionError.class, () -> {
            long negativeElement = -1;
            long[] as = new long[num];
            Arrays.fill(as, negativeElement);
            long[] bs = new long[num];
            Arrays.fill(bs, element);
            long[] cs = new long[num];
            Arrays.fill(cs, element);
            Zp64Triple.create(DEFAULT_ZP64, num, as, bs, cs);
        });
    }

    @Test
    public void testReduce() {
        for (Zp64 zp64 : ZP64S) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testReduce(zp64, num);
            }
        }
    }

    private void testReduce(Zp64 zp64, int num) {
        long element = 1L << zp64.getL();
        // create triples
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);
        // reduce 1
        Zp64Triple triple1 = Zp64Triple.create(zp64, num, as, bs, cs);
        triple1.reduce(1);
        assertCorrectness(triple1, 1);
        // reduce all
        Zp64Triple tripleAll = Zp64Triple.create(zp64, num, as, bs, cs);
        tripleAll.reduce(num);
        assertCorrectness(tripleAll, num);
        if (num > 1) {
            // reduce num - 1
            Zp64Triple tripleN = Zp64Triple.create(zp64, num, as, bs, cs);
            tripleN.reduce(num - 1);
            assertCorrectness(tripleN, num - 1);
            // reduce half
            Zp64Triple tripleHalf = Zp64Triple.create(zp64, num, as, bs, cs);
            tripleHalf.reduce(num / 2);
            assertCorrectness(tripleHalf, num / 2);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        for (Zp64 zp64 : ZP64S) {
            Zp64Triple triple = Zp64Triple.createEmpty(zp64);
            Zp64Triple mergeTriple = Zp64Triple.createEmpty(zp64);
            triple.merge(mergeTriple);
            assertCorrectness(triple, 0);
        }
    }

    @Test
    public void testLeftEmptyMerge() {
        for (Zp64 zp64 : ZP64S) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testLeftEmptyMerge(zp64, num);
            }
        }
    }

    private void testLeftEmptyMerge(Zp64 zp64, int num) {
        long element = 1L << zp64.getL();
        // create triples
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);

        Zp64Triple triple = Zp64Triple.createEmpty(zp64);
        Zp64Triple mergeTriple = Zp64Triple.create(zp64, num, as, bs, cs);
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testRightEmptyMerge() {
        for (Zp64 zp64 : ZP64S) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testRightEmptyMerge(zp64, num);
            }
        }
    }

    private void testRightEmptyMerge(Zp64 zp64, int num) {
        long element = 1L << zp64.getL();
        // 创建三元组
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);

        Zp64Triple triple = Zp64Triple.create(zp64, num, as, bs, cs);
        Zp64Triple mergeTriple = Zp64Triple.createEmpty(zp64);
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testMerge() {
        for (Zp64 zp64 : ZP64S) {
            for (int num1 = MIN_NUM; num1 < MAX_NUM; num1++) {
                for (int num2 = MIN_NUM; num2 < MAX_NUM; num2++) {
                    testMerge(zp64, num1, num2);
                }
            }
        }
    }

    private void testMerge(Zp64 zp64, int num1, int num2) {
        long element = 1L << zp64.getL();
        // create the 1st triple
        long[] a1s = new long[num1];
        Arrays.fill(a1s, element);
        long[] b1s = new long[num1];
        Arrays.fill(b1s, element);
        long[] c1s = new long[num1];
        Arrays.fill(c1s, element);
        // create the 2nd triple
        long[] a2s = new long[num2];
        Arrays.fill(a2s, element);
        long[] b2s = new long[num2];
        Arrays.fill(b2s, element);
        long[] c2s = new long[num2];
        Arrays.fill(c2s, element);
        // merge and verify
        Zp64Triple triple = Zp64Triple.create(zp64, num1, a1s, b1s, c1s);
        Zp64Triple mergerTriple = Zp64Triple.create(zp64, num2, a2s, b2s, c2s);
        triple.merge(mergerTriple);
        assertCorrectness(triple, num1 + num2);
    }

    @Test
    public void testSplit() {
        for (Zp64 zp64 : ZP64S) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testSplit(zp64, num);
            }
        }
    }

    private void testSplit(Zp64 zp64, int num) {
        long element = 1L << zp64.getL();
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);
        // split 1
        Zp64Triple triple1 = Zp64Triple.create(zp64, num, as, bs, cs);
        Zp64Triple splitTriple1 = triple1.split(1);
        assertCorrectness(triple1, num - 1);
        assertCorrectness(splitTriple1, 1);
        // split all
        Zp64Triple tripleAll = Zp64Triple.create(zp64, num, as, bs, cs);
        Zp64Triple splitTripleAll = tripleAll.split(num);
        assertCorrectness(tripleAll, 0);
        assertCorrectness(splitTripleAll, num);
        if (num > 1) {
            // split num - 1
            Zp64Triple tripleN = Zp64Triple.create(zp64, num, as, bs, cs);
            Zp64Triple splitTripleN = tripleN.split(num - 1);
            assertCorrectness(tripleN, 1);
            assertCorrectness(splitTripleN, num - 1);
            // split half
            Zp64Triple tripleHalf = Zp64Triple.create(zp64, num, as, bs, cs);
            Zp64Triple splitTripleHalf = tripleHalf.split(num / 2);
            assertCorrectness(tripleHalf, num - num / 2);
            assertCorrectness(splitTripleHalf, num / 2);
        }
    }

    private void assertCorrectness(Zp64Triple triple, int num) {
        if (num == 0) {
            Assert.assertEquals(0, triple.getNum());
            Assert.assertEquals(0, triple.getA().length);
            Assert.assertEquals(0, triple.getB().length);
            Assert.assertEquals(0, triple.getC().length);
        } else {
            Assert.assertEquals(num, triple.getNum());
            long element = 1L << triple.getZp64().getL();
            for (int index = 0; index < num; index++) {
                Assert.assertEquals(element, triple.getA(index));
                Assert.assertEquals(element, triple.getB(index));
                Assert.assertEquals(element, triple.getC(index));
            }
        }
    }
}
