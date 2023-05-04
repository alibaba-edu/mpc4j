package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
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
    private static final int MAX_NUM = 64;
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, 32);
    /**
     * Zl array
     */
    private static final Zl[] ZLS = IntStream.range(1, 128)
        .mapToObj(l -> ZlFactory.createInstance(EnvType.STANDARD, l))
        .toArray(Zl[]::new);

    @Test
    public void testIllegalInputs() {
        // create triple with num = 0
        Assert.assertThrows(AssertionError.class, () ->
            ZlTriple.create(DEFAULT_ZL, 0, new BigInteger[0], new BigInteger[0], new BigInteger[0])
        );
        int num = 12;
        BigInteger element = DEFAULT_ZL.createNonZeroRandom(new SecureRandom());
        // create triples with less num
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] as = new BigInteger[num - 1];
            Arrays.fill(as, element);
            BigInteger[] bs = new BigInteger[num - 1];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num - 1];
            Arrays.fill(cs, element);
            ZlTriple.create(DEFAULT_ZL, num, as, bs, cs);
        });
        // create triples with large num
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] as = new BigInteger[num + 1];
            Arrays.fill(as, element);
            BigInteger[] bs = new BigInteger[num + 1];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num + 1];
            Arrays.fill(cs, element);
            ZlTriple.create(DEFAULT_ZL, num, as, bs, cs);
        });
        // create triples with mis-matched num
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger[] as = new BigInteger[num - 1];
            Arrays.fill(as, element);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num + 1];
            Arrays.fill(cs, element);
            ZlTriple.create(DEFAULT_ZL, num, as, bs, cs);
        });
        // create triples with large element
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger largeElement = DEFAULT_ZL.getRangeBound().add(BigInteger.ONE);
            BigInteger[] as = new BigInteger[num];
            Arrays.fill(as, largeElement);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num];
            Arrays.fill(cs, element);
            ZlTriple.create(DEFAULT_ZL, num, as, bs, cs);
        });
        // create triples with negative element
        Assert.assertThrows(AssertionError.class, () -> {
            BigInteger negativeElement = BigInteger.ONE.negate();
            BigInteger[] as = new BigInteger[num];
            Arrays.fill(as, negativeElement);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num];
            Arrays.fill(cs, element);
            ZlTriple.create(DEFAULT_ZL, num, as, bs, cs);
        });
    }

    @Test
    public void testReduce() {
        for (Zl zl : ZLS) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testReduce(zl, num);
            }
        }
    }

    private void testReduce(Zl zl, int num) {
        BigInteger element = largestValidElement(zl);
        // create triples
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);
        // reduce 1
        ZlTriple triple1 = ZlTriple.create(zl, num, as, bs, cs);
        triple1.reduce(1);
        assertCorrectness(triple1, 1);
        // reduce all
        ZlTriple tripleAll = ZlTriple.create(zl, num, as, bs, cs);
        tripleAll.reduce(num);
        assertCorrectness(tripleAll, num);
        if (num > 1) {
            // reduce num - 1
            ZlTriple tripleN = ZlTriple.create(zl, num, as, bs, cs);
            tripleN.reduce(num - 1);
            assertCorrectness(tripleN, num - 1);
            // reduce half
            ZlTriple tripleHalf = ZlTriple.create(zl, num, as, bs, cs);
            tripleHalf.reduce(num / 2);
            assertCorrectness(tripleHalf, num / 2);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        for (Zl zl : ZLS) {
            ZlTriple triple = ZlTriple.createEmpty(zl);
            ZlTriple mergeTriple = ZlTriple.createEmpty(zl);
            triple.merge(mergeTriple);
            assertCorrectness(triple, 0);
        }
    }

    @Test
    public void testLeftEmptyMerge() {
        for (Zl zl : ZLS) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testLeftEmptyMerge(zl, num);
            }
        }
    }

    private void testLeftEmptyMerge(Zl zl, int num) {
        BigInteger element = largestValidElement(zl);
        // create triples
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);

        ZlTriple triple = ZlTriple.createEmpty(zl);
        ZlTriple mergeTriple = ZlTriple.create(zl, num, as, bs, cs);
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testRightEmptyMerge() {
        for (Zl zl : ZLS) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testRightEmptyMerge(zl, num);
            }
        }
    }

    private void testRightEmptyMerge(Zl zl, int num) {
        BigInteger element = largestValidElement(zl);
        // create triples
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);

        ZlTriple triple = ZlTriple.create(zl, num, as, bs, cs);
        ZlTriple mergeTriple = ZlTriple.createEmpty(zl);
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testMerge() {
        for (Zl zl : ZLS) {
            for (int num1 = MIN_NUM; num1 < MAX_NUM; num1++) {
                for (int num2 = MIN_NUM; num2 < MAX_NUM; num2++) {
                    testMerge(zl, num1, num2);
                }
            }
        }
    }

    private void testMerge(Zl zl, int num1, int num2) {
        BigInteger element = largestValidElement(zl);
        // create the 1st triple
        BigInteger[] a1s = new BigInteger[num1];
        Arrays.fill(a1s, element);
        BigInteger[] b1s = new BigInteger[num1];
        Arrays.fill(b1s, element);
        BigInteger[] c1s = new BigInteger[num1];
        Arrays.fill(c1s, element);
        // create the 2nd triple
        BigInteger[] a2s = new BigInteger[num2];
        Arrays.fill(a2s, element);
        BigInteger[] b2s = new BigInteger[num2];
        Arrays.fill(b2s, element);
        BigInteger[] c2s = new BigInteger[num2];
        Arrays.fill(c2s, element);
        // merge and verify
        ZlTriple triple = ZlTriple.create(zl, num1, a1s, b1s, c1s);
        ZlTriple mergerTriple = ZlTriple.create(zl, num2, a2s, b2s, c2s);
        triple.merge(mergerTriple);
        assertCorrectness(triple, num1 + num2);
    }

    @Test
    public void testSplit() {
        for (Zl zl : ZLS) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testSplit(zl, num);
            }
        }
    }

    private void testSplit(Zl zl, int num) {
        BigInteger element = largestValidElement(zl);
        // create triple
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);
        // split 1
        ZlTriple triple1 = ZlTriple.create(zl, num, as, bs, cs);
        ZlTriple splitTriple1 = triple1.split(1);
        assertCorrectness(triple1, num - 1);
        assertCorrectness(splitTriple1, 1);
        // split all
        ZlTriple tripleAll = ZlTriple.create(zl, num, as, bs, cs);
        ZlTriple splitTripleAll = tripleAll.split(num);
        assertCorrectness(tripleAll, 0);
        assertCorrectness(splitTripleAll, num);
        if (num > 1) {
            // split num - 1
            ZlTriple tripleN = ZlTriple.create(zl, num, as, bs, cs);
            ZlTriple splitTripleN = tripleN.split(num - 1);
            assertCorrectness(tripleN, 1);
            assertCorrectness(splitTripleN, num - 1);
            // split half
            ZlTriple tripleHalf = ZlTriple.create(zl, num, as, bs, cs);
            ZlTriple splitTripleHalf = tripleHalf.split(num / 2);
            assertCorrectness(tripleHalf, num - num / 2);
            assertCorrectness(splitTripleHalf, num / 2);
        }
    }

    private void assertCorrectness(ZlTriple zlTriple, int num) {
        if (num == 0) {
            Assert.assertEquals(0, zlTriple.getNum());
            Assert.assertEquals(0, zlTriple.getA().length);
            Assert.assertEquals(0, zlTriple.getB().length);
            Assert.assertEquals(0, zlTriple.getC().length);
        } else {
            Assert.assertEquals(num, zlTriple.getNum());
            Zl zl = zlTriple.getZl();
            BigInteger element = largestValidElement(zl);
            for (int index = 0; index < num; index++) {
                Assert.assertEquals(element, zlTriple.getA(index));
                Assert.assertEquals(element, zlTriple.getB(index));
                Assert.assertEquals(element, zlTriple.getC(index));
            }
        }
    }

    private static BigInteger largestValidElement(Zl zl) {
        return zl.getRangeBound().subtract(BigInteger.ONE);
    }
}
