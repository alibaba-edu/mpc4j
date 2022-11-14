package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Zp64三元组测试。
 *
 * @author Weiran Liu
 * @date 2022/11/5
 */
public class Zp64TripleTest {
    /**
     * 较小数量
     */
    private static final int MIN_NUM = 1;
    /**
     * 较大数量
     */
    private static final int MAX_NUM = 128;
    /**
     * 较小的l
     */
    private static final int SMALL_L = 2;
    /**
     * 较大的l
     */
    private static final int LARGE_L = CommonConstants.STATS_BIT_LENGTH;

    @Test
    public void testIllegalInputs() {
        try {
            // 创建长度为0的三元组
            Zp64Triple.create(2, 0, new long[0], new long[0], new long[0]);
            throw new IllegalStateException("ERROR: successfully create " + Zp64Triple.class.getSimpleName() + " with num = 0");
        } catch (AssertionError ignored) {

        }
        int num = 12;
        try {
            // 创建p = 0的三元组
            long[] as = new long[num];
            long[] bs = new long[num];
            long[] cs = new long[num];
            Zp64Triple.create(0, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + Zp64Triple.class.getSimpleName() + " with p = 0");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建p = 1的三元组
            long[] as = new long[num];
            long[] bs = new long[num];
            long[] cs = new long[num];
            Zp64Triple.create(1, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + Zp64Triple.class.getSimpleName() + " with p = 1");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建p为非质数的三元组
            long[] as = new long[num];
            long[] bs = new long[num];
            long[] cs = new long[num];
            Zp64Triple.create(4, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + Zp64Triple.class.getSimpleName() + " with p = 4");
        } catch (AssertionError ignored) {

        }
        long p = ZpManager.getPrime(CommonConstants.STATS_BIT_LENGTH).longValue();
        long element = p - 1;
        try {
            // 创建数量小的三元组
            long[] as = new long[num - 1];
            Arrays.fill(as, element);
            long[] bs = new long[num - 1];
            Arrays.fill(bs, element);
            long[] cs = new long[num - 1];
            Arrays.fill(cs, element);
            Zp64Triple.create(p, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + Zp64Triple.class.getSimpleName() + " with less num");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建数量大的三元组
            long[] as = new long[num + 1];
            Arrays.fill(as, element);
            long[] bs = new long[num + 1];
            Arrays.fill(bs, element);
            long[] cs = new long[num + 1];
            Arrays.fill(cs, element);
            Zp64Triple.create(p, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + Zp64Triple.class.getSimpleName() + " with large num");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建数量不一致的三元组
            long[] as = new long[num - 1];
            Arrays.fill(as, element);
            long[] bs = new long[num];
            Arrays.fill(bs, element);
            long[] cs = new long[num + 1];
            Arrays.fill(cs, element);
            Zp64Triple.create(p, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + Zp64Triple.class.getSimpleName() + " with distinct num");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建元素比特长度大于指定长度的三元组
            long[] as = new long[num];
            Arrays.fill(as, p);
            long[] bs = new long[num];
            Arrays.fill(bs, element);
            long[] cs = new long[num];
            Arrays.fill(cs, element);
            Zp64Triple.create(p, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + Zp64Triple.class.getSimpleName() + " with large element");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建元素为负数的三元组
            long negativeElement = -1;
            long[] as = new long[num];
            Arrays.fill(as, negativeElement);
            long[] bs = new long[num];
            Arrays.fill(bs, element);
            long[] cs = new long[num];
            Arrays.fill(cs, element);
            Zp64Triple.create(p, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + Zp64Triple.class.getSimpleName() + " with negative element");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testReduce() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testReduce(l, num);
            }
        }
    }

    private void testReduce(int l, int num) {
        long p = ZpManager.getPrime(l).longValue();
        long element = 1L << l;
        // 创建三元组
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);
        // 减小到1
        Zp64Triple triple1 = Zp64Triple.create(p, num, as, bs, cs);
        triple1.reduce(1);
        assertCorrectness(triple1, 1);
        // 减小到相同长度
        Zp64Triple tripleAll = Zp64Triple.create(p, num, as, bs, cs);
        tripleAll.reduce(num);
        assertCorrectness(tripleAll, num);
        if (num > 1) {
            // 减小n - 1
            Zp64Triple tripleN = Zp64Triple.create(p, num, as, bs, cs);
            tripleN.reduce(num - 1);
            assertCorrectness(tripleN, num - 1);
            // 减小到一半
            Zp64Triple tripleHalf = Zp64Triple.create(p, num, as, bs, cs);
            tripleHalf.reduce(num / 2);
            assertCorrectness(tripleHalf, num / 2);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            long p = ZpManager.getPrime(l).longValue();
            Zp64Triple triple = Zp64Triple.createEmpty(p);
            Zp64Triple mergeTriple = Zp64Triple.createEmpty(p);
            triple.merge(mergeTriple);
            assertCorrectness(triple, 0);
        }
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testLeftEmptyMerge(l, num);
            }
        }
    }

    private void testLeftEmptyMerge(int l, int num) {
        long p = ZpManager.getPrime(l).longValue();
        long element = 1L << l;
        // 创建三元组
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);

        Zp64Triple triple = Zp64Triple.createEmpty(p);
        Zp64Triple mergeTriple = Zp64Triple.create(p, num, as, bs, cs);
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testRightEmptyMerge(l, num);
            }
        }
    }

    private void testRightEmptyMerge(int l, int num) {
        long p = ZpManager.getPrime(l).longValue();
        long element = 1L << l;
        // 创建三元组
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);

        Zp64Triple triple = Zp64Triple.create(p, num, as, bs, cs);
        Zp64Triple mergeTriple = Zp64Triple.createEmpty(p);
        triple.merge(mergeTriple);
        assertCorrectness(triple, num);
    }

    @Test
    public void testMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num1 = MIN_NUM; num1 < MAX_NUM; num1++) {
                for (int num2 = MIN_NUM; num2 < MAX_NUM; num2++) {
                    testMerge(l, num1, num2);
                }
            }
        }
    }

    private void testMerge(int l, int num1, int num2) {
        long p = ZpManager.getPrime(l).longValue();
        long element = 1L << l;
        // 创建第1个三元组
        long[] a1s = new long[num1];
        Arrays.fill(a1s, element);
        long[] b1s = new long[num1];
        Arrays.fill(b1s, element);
        long[] c1s = new long[num1];
        Arrays.fill(c1s, element);
        // 创建第2个三元组
        long[] a2s = new long[num2];
        Arrays.fill(a2s, element);
        long[] b2s = new long[num2];
        Arrays.fill(b2s, element);
        long[] c2s = new long[num2];
        Arrays.fill(c2s, element);
        // 合并三元组并验证结果
        Zp64Triple triple = Zp64Triple.create(p, num1, a1s, b1s, c1s);
        Zp64Triple mergerTriple = Zp64Triple.create(p, num2, a2s, b2s, c2s);
        triple.merge(mergerTriple);
        assertCorrectness(triple, num1 + num2);
    }

    @Test
    public void testSplit() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            for (int num = MIN_NUM; num < MAX_NUM; num++) {
                testSplit(l, num);
            }
        }
    }

    private void testSplit(int l, int num) {
        long p = ZpManager.getPrime(l).longValue();
        long element = 1L << l;
        long[] as = new long[num];
        Arrays.fill(as, element);
        long[] bs = new long[num];
        Arrays.fill(bs, element);
        long[] cs = new long[num];
        Arrays.fill(cs, element);
        // 切分1比特
        Zp64Triple triple1 = Zp64Triple.create(p, num, as, bs, cs);
        Zp64Triple splitTriple1 = triple1.split(1);
        assertCorrectness(triple1, num - 1);
        assertCorrectness(splitTriple1, 1);
        // 切分全部比特
        Zp64Triple tripleAll = Zp64Triple.create(p, num, as, bs, cs);
        Zp64Triple splitTripleAll = tripleAll.split(num);
        assertCorrectness(tripleAll, 0);
        assertCorrectness(splitTripleAll, num);
        if (num > 1) {
            // 切分num - 1比特
            Zp64Triple tripleN = Zp64Triple.create(p, num, as, bs, cs);
            Zp64Triple splitTripleN = tripleN.split(num - 1);
            assertCorrectness(tripleN, 1);
            assertCorrectness(splitTripleN, num - 1);
            // 切分一半比特
            Zp64Triple tripleHalf = Zp64Triple.create(p, num, as, bs, cs);
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
            int l = LongUtils.ceilLog2(triple.getP()) - 1;
            long element = 1L << l;
            for (int index = 0; index < num; index++) {
                Assert.assertEquals(element, triple.getA(index));
                Assert.assertEquals(element, triple.getB(index));
                Assert.assertEquals(element, triple.getC(index));
            }
        }
    }
}
