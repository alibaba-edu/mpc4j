package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * l比特三元组测试。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlTripleTest {
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
    private static final int SMALL_L = 1;
    /**
     * 较大的l
     */
    private static final int LARGE_L = CommonConstants.BLOCK_BIT_LENGTH;

    @Test
    public void testIllegalInputs() {
        try {
            // 创建长度为0的三元组
            ZlTriple.create(1, 0, new BigInteger[0], new BigInteger[0], new BigInteger[0]);
            throw new IllegalStateException("ERROR: successfully create " + ZlTriple.class.getSimpleName() + " with num = 0");
        } catch (AssertionError ignored) {

        }
        int num = 12;
        try {
            // 创建l = 0的三元组
            BigInteger[] as = new BigInteger[num];
            Arrays.fill(as, BigInteger.ZERO);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, BigInteger.ZERO);
            BigInteger[] cs = new BigInteger[num];
            Arrays.fill(cs, BigInteger.ZERO);
            ZlTriple.create(0, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + ZlTriple.class.getSimpleName() + " with l = 0");
        } catch (AssertionError ignored) {

        }
        int l = CommonConstants.BLOCK_BIT_LENGTH;
        BigInteger element = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        try {
            // 创建数量小的三元组
            BigInteger[] as = new BigInteger[num - 1];
            Arrays.fill(as, element);
            BigInteger[] bs = new BigInteger[num - 1];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num - 1];
            Arrays.fill(cs, element);
            ZlTriple.create(l, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + ZlTriple.class.getSimpleName() + " with less num");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建数量大的三元组
            BigInteger[] as = new BigInteger[num + 1];
            Arrays.fill(as, element);
            BigInteger[] bs = new BigInteger[num + 1];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num + 1];
            Arrays.fill(cs, element);
            ZlTriple.create(l, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + ZlTriple.class.getSimpleName() + " with large num");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建数量不一致的三元组
            BigInteger[] as = new BigInteger[num - 1];
            Arrays.fill(as, element);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num + 1];
            Arrays.fill(cs, element);
            ZlTriple.create(l, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + ZlTriple.class.getSimpleName() + " with distinct num");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建元素比特长度大于指定长度的三元组
            BigInteger largeElement = BigInteger.ONE.shiftLeft(l);
            BigInteger[] as = new BigInteger[num];
            Arrays.fill(as, largeElement);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num];
            Arrays.fill(cs, element);
            ZlTriple.create(l, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + ZlTriple.class.getSimpleName() + " with large bit length");
        } catch (AssertionError ignored) {

        }
        try {
            // 创建元素为负数的三元组
            BigInteger negativeElement = BigInteger.ONE.negate();
            BigInteger[] as = new BigInteger[num];
            Arrays.fill(as, negativeElement);
            BigInteger[] bs = new BigInteger[num];
            Arrays.fill(bs, element);
            BigInteger[] cs = new BigInteger[num];
            Arrays.fill(cs, element);
            ZlTriple.create(l, num, as, bs, cs);
            throw new IllegalStateException("ERROR: successfully create " + ZlTriple.class.getSimpleName() + " with negative element");
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
        BigInteger element = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        // 创建三元组
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);
        // 减小到1
        ZlTriple triple1 = ZlTriple.create(l, num, as, bs, cs);
        triple1.reduce(1);
        assertCorrectness(triple1, 1);
        // 减小到相同长度
        ZlTriple tripleAll = ZlTriple.create(l, num, as, bs, cs);
        tripleAll.reduce(num);
        assertCorrectness(tripleAll, num);
        if (num > 1) {
            // 减小n - 1
            ZlTriple tripleN = ZlTriple.create(l, num, as, bs, cs);
            tripleN.reduce(num - 1);
            assertCorrectness(tripleN, num - 1);
            // 减小到一半
            ZlTriple tripleHalf = ZlTriple.create(l, num, as, bs, cs);
            tripleHalf.reduce(num / 2);
            assertCorrectness(tripleHalf, num / 2);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        for (int l = SMALL_L; l < LARGE_L; l++) {
            ZlTriple triple = ZlTriple.createEmpty(l);
            ZlTriple mergeTriple = ZlTriple.createEmpty(l);
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
        BigInteger element = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        // 创建三元组
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);

        ZlTriple triple = ZlTriple.createEmpty(l);
        ZlTriple mergeTriple = ZlTriple.create(l, num, as, bs, cs);
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
        BigInteger element = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        // 创建三元组
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);

        ZlTriple triple = ZlTriple.create(l, num, as, bs, cs);
        ZlTriple mergeTriple = ZlTriple.createEmpty(l);
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
        BigInteger element = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        // 创建第1个三元组
        BigInteger[] a1s = new BigInteger[num1];
        Arrays.fill(a1s, element);
        BigInteger[] b1s = new BigInteger[num1];
        Arrays.fill(b1s, element);
        BigInteger[] c1s = new BigInteger[num1];
        Arrays.fill(c1s, element);
        // 创建第2个三元组
        BigInteger[] a2s = new BigInteger[num2];
        Arrays.fill(a2s, element);
        BigInteger[] b2s = new BigInteger[num2];
        Arrays.fill(b2s, element);
        BigInteger[] c2s = new BigInteger[num2];
        Arrays.fill(c2s, element);
        // 合并三元组并验证结果
        ZlTriple triple = ZlTriple.create(l, num1, a1s, b1s, c1s);
        ZlTriple mergerTriple = ZlTriple.create(l, num2, a2s, b2s, c2s);
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
        // 创建三元组
        BigInteger element = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
        // 创建三元组
        BigInteger[] as = new BigInteger[num];
        Arrays.fill(as, element);
        BigInteger[] bs = new BigInteger[num];
        Arrays.fill(bs, element);
        BigInteger[] cs = new BigInteger[num];
        Arrays.fill(cs, element);
        // 切分1比特
        ZlTriple triple1 = ZlTriple.create(l, num, as, bs, cs);
        ZlTriple splitTriple1 = triple1.split(1);
        assertCorrectness(triple1, num - 1);
        assertCorrectness(splitTriple1, 1);
        // 切分全部比特
        ZlTriple tripleAll = ZlTriple.create(l, num, as, bs, cs);
        ZlTriple splitTripleAll = tripleAll.split(num);
        assertCorrectness(tripleAll, 0);
        assertCorrectness(splitTripleAll, num);
        if (num > 1) {
            // 切分num - 1比特
            ZlTriple tripleN = ZlTriple.create(l, num, as, bs, cs);
            ZlTriple splitTripleN = tripleN.split(num - 1);
            assertCorrectness(tripleN, 1);
            assertCorrectness(splitTripleN, num - 1);
            // 切分一半比特
            ZlTriple tripleHalf = ZlTriple.create(l, num, as, bs, cs);
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
            int l = zlTriple.getL();
            BigInteger element = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.ONE);
            for (int index = 0; index < num; index++) {
                Assert.assertEquals(element, zlTriple.getA(index));
                Assert.assertEquals(element, zlTriple.getB(index));
                Assert.assertEquals(element, zlTriple.getC(index));
            }
        }
    }
}
