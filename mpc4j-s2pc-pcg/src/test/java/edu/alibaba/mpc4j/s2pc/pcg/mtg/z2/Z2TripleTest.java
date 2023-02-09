package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * Z2Triple tests.
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
public class Z2TripleTest {
    /**
     * the minimum triple num
     */
    private static final int MIN_NUM = 1;
    /**
     * the maximum triple num
     */
    private static final int MAX_NUM = 128;

    @Test
    public void testIllegalInputs() {
        // create triple with 0 num
        Assert.assertThrows(AssertionError.class, () ->
            Z2Triple.create(0, new byte[0], new byte[0], new byte[0])
        );
        int num = 12;
        int byteNum = CommonUtils.getByteLength(num);
        // create triple with small bytes.length
        Assert.assertThrows(AssertionError.class, () ->
            Z2Triple.create(num, new byte[byteNum - 1], new byte[byteNum - 1], new byte[byteNum - 1])
        );
        // create triple with large bytes.length
        Assert.assertThrows(AssertionError.class, () ->
            Z2Triple.create(num, new byte[byteNum + 1], new byte[byteNum + 1], new byte[byteNum + 1])
        );
        // create triple with mismatch bytes.length
        Assert.assertThrows(AssertionError.class, () ->
            Z2Triple.create(num, new byte[byteNum], new byte[byteNum - 1], new byte[byteNum + 1])
        );
        // create valid bytes
        byte[] aBytes = new byte[]{0x0F, (byte) 0xFF};
        byte[] bBytes = new byte[]{0x0F, (byte) 0xFF};
        byte[] cBytes = new byte[]{0x0F, (byte) 0xFF};
        // create triple with small num
        Assert.assertThrows(AssertionError.class, () -> Z2Triple.create(num - 1, aBytes, bBytes, cBytes));
    }

    @Test
    public void testIllegalUpdate() {
        // split with 0 length
        Assert.assertThrows(AssertionError.class, () -> {
            Z2Triple triple = Z2Triple.createOnes(4);
            triple.split(0);
        });
        // split with large length
        Assert.assertThrows(AssertionError.class, () -> {
            Z2Triple triple = Z2Triple.createOnes(4);
            triple.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(AssertionError.class, () -> {
            Z2Triple triple = Z2Triple.createOnes(4);
            triple.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(AssertionError.class, () -> {
            Z2Triple triple = Z2Triple.createOnes(4);
            triple.reduce(5);
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
        Z2Triple triple1 = Z2Triple.createOnes(num);
        triple1.reduce(1);
        assertOnesCorrectness(triple1, 1);
        // reduce all
        Z2Triple tripleAll = Z2Triple.createOnes(num);
        tripleAll.reduce(num);
        assertOnesCorrectness(tripleAll, num);
        if (num > 1) {
            // reduce n - 1
            Z2Triple tripleNum = Z2Triple.createOnes(num);
            tripleNum.reduce(num - 1);
            assertOnesCorrectness(tripleNum, num - 1);
            // reduce half
            Z2Triple tripleHalf = Z2Triple.createOnes(num);
            tripleHalf.reduce(num / 2);
            assertOnesCorrectness(tripleHalf, num / 2);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        Z2Triple triple = Z2Triple.createEmpty();
        Z2Triple mergeTriple = Z2Triple.createEmpty();
        triple.merge(mergeTriple);
        assertEmptyCorrectness(triple);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testLeftEmptyMerge(num);
        }
    }

    private void testLeftEmptyMerge(int num) {
        Z2Triple triple = Z2Triple.createEmpty();
        Z2Triple mergeTriple = Z2Triple.createOnes(num);
        triple.merge(mergeTriple);
        assertOnesCorrectness(triple, num);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testRightEmptyMerge(num);
        }
    }

    private void testRightEmptyMerge(int num) {
        Z2Triple triple = Z2Triple.createOnes(num);
        Z2Triple mergeTriple = Z2Triple.createEmpty();
        triple.merge(mergeTriple);
        assertOnesCorrectness(triple, num);
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
        Z2Triple triple = Z2Triple.createOnes(num1);
        Z2Triple mergerTriple = Z2Triple.createOnes(num2);
        triple.merge(mergerTriple);
        assertOnesCorrectness(triple, num1 + num2);
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        // split 1
        Z2Triple triple1 = Z2Triple.createOnes(num);
        Z2Triple splitTriple1 = triple1.split(1);
        assertOnesCorrectness(triple1, num - 1);
        assertOnesCorrectness(splitTriple1, 1);
        // 切分全部比特
        Z2Triple tripleAll = Z2Triple.createOnes(num);
        Z2Triple splitTripleAll = tripleAll.split(num);
        assertEmptyCorrectness(tripleAll);
        assertOnesCorrectness(splitTripleAll, num);
        if (num > 1) {
            // 切分num - 1比特
            Z2Triple tripleNum = Z2Triple.createOnes(num);
            Z2Triple splitTripleNum = tripleNum.split(num - 1);
            assertOnesCorrectness(tripleNum, 1);
            assertOnesCorrectness(splitTripleNum, num - 1);
            // 切分一半比特
            Z2Triple tripleHalf = Z2Triple.createOnes(num);
            Z2Triple splitTripleHalf = tripleHalf.split(num / 2);
            assertOnesCorrectness(tripleHalf, num - num / 2);
            assertOnesCorrectness(splitTripleHalf, num / 2);
        }
    }

    private void assertEmptyCorrectness(Z2Triple triple) {
        Assert.assertEquals(0, triple.getNum());
        Assert.assertEquals(0, triple.getByteNum());
        Assert.assertEquals(0, triple.getA().length);
        Assert.assertEquals(0, triple.getB().length);
        Assert.assertEquals(0, triple.getC().length);
        Assert.assertEquals("", triple.getStringA());
        Assert.assertEquals("", triple.getStringB());
        Assert.assertEquals("", triple.getStringC());
    }

    private void assertOnesCorrectness(Z2Triple triple, int num) {
        if (num == 0) {
            assertEmptyCorrectness(triple);
        } else {
            Assert.assertEquals(num, triple.getNum());
            Assert.assertEquals(CommonUtils.getByteLength(num), triple.getByteNum());
            String fullOneString = BigInteger.ONE.shiftLeft(num).subtract(BigInteger.ONE).toString(2);
            Assert.assertEquals(fullOneString, triple.getStringA());
            Assert.assertEquals(fullOneString, triple.getStringB());
            Assert.assertEquals(fullOneString, triple.getStringC());
        }
    }
}
