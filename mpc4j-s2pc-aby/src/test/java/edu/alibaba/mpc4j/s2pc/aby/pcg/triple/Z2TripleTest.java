package edu.alibaba.mpc4j.s2pc.aby.pcg.triple;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.*;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

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
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Z2TripleTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
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
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Z2Triple triple = Z2Triple.createRandom(4, secureRandom);
            triple.split(0);
        });
        // split with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Z2Triple triple = Z2Triple.createRandom(4, secureRandom);
            triple.split(5);
        });
        // reduce triple with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Z2Triple triple = Z2Triple.createRandom(4, secureRandom);
            triple.reduce(0);
        });
        // reduce triple with large length
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Z2Triple triple = Z2Triple.createRandom(4, secureRandom);
            triple.reduce(5);
        });
    }

    @Test
    public void testCreateFromCotCorrelation() {
        int num = MAX_NUM;
        byte[] senderDelta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        byte[] receiverDelta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        CotSenderOutput firstCotSenderOutput = CotSenderOutput.createRandom(num, senderDelta, secureRandom);
        CotReceiverOutput firstCotReceiverOutput = CotReceiverOutput.createRandom(firstCotSenderOutput, secureRandom);
        CotSenderOutput secondCotSenderOutput = CotSenderOutput.createRandom(num, receiverDelta, secureRandom);
        CotReceiverOutput secondCotReceiverOutput = CotReceiverOutput.createRandom(secondCotSenderOutput, secureRandom);
        Z2Triple senderTriple = Z2Triple.create(EnvType.STANDARD, firstCotSenderOutput, secondCotReceiverOutput);
        Z2Triple receiverTriple = Z2Triple.create(EnvType.STANDARD, secondCotSenderOutput, firstCotReceiverOutput);
        TripleTestUtils.assertOutput(num, senderTriple, receiverTriple);
    }

    @Test
    public void testCreateRandomCorrelation() {
        int num = MAX_NUM;
        Z2Triple senderTriple = Z2Triple.createRandom(num, secureRandom);
        Z2Triple receiverTriple = Z2Triple.createRandom(senderTriple, secureRandom);
        TripleTestUtils.assertOutput(num, senderTriple, receiverTriple);
    }

    @Test
    public void testReduce() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testReduce(num);
        }
    }

    private void testReduce(int num) {
        // reduce 1
        Z2Triple triple1 = Z2Triple.createRandom(num, secureRandom);
        triple1.reduce(1);
        TripleTestUtils.assertOutput( 1, triple1, Z2Triple.createRandom(triple1, secureRandom));
        // reduce all
        Z2Triple tripleAll = Z2Triple.createRandom(num, secureRandom);
        tripleAll.reduce(num);
        TripleTestUtils.assertOutput(num, tripleAll, Z2Triple.createRandom(tripleAll, secureRandom));
        if (num > 1) {
            // reduce n - 1
            Z2Triple tripleNum = Z2Triple.createRandom(num, secureRandom);
            tripleNum.reduce(num - 1);
            TripleTestUtils.assertOutput(num - 1, tripleNum, Z2Triple.createRandom(tripleNum, secureRandom));
            // reduce half
            Z2Triple tripleHalf = Z2Triple.createRandom(num, secureRandom);
            tripleHalf.reduce(num / 2);
            TripleTestUtils.assertOutput(num / 2, tripleHalf, Z2Triple.createRandom(tripleHalf, secureRandom));
        }
    }

    @Test
    public void testMerge() {
        for (int num1 = 0; num1 < MAX_NUM; num1++) {
            for (int num2 = 0; num2 < MAX_NUM; num2++) {
                testMerge(num1, num2);
            }
        }
    }

    private void testMerge(int num1, int num2) {
        Z2Triple triple = Z2Triple.createRandom(num1, secureRandom);
        Z2Triple mergeTriple = Z2Triple.createRandom(num2, secureRandom);
        triple.merge(mergeTriple);
        TripleTestUtils.assertOutput(num1 + num2, triple, Z2Triple.createRandom(triple, secureRandom));
    }

    @Test
    public void testSplit() {
        for (int num = MIN_NUM; num < MAX_NUM; num++) {
            testSplit(num);
        }
    }

    private void testSplit(int num) {
        // split 1
        Z2Triple triple1 = Z2Triple.createRandom(num, secureRandom);
        Z2Triple splitTriple1 = triple1.split(1);
        TripleTestUtils.assertOutput(num - 1, triple1, Z2Triple.createRandom(triple1, secureRandom));
        TripleTestUtils.assertOutput(1, splitTriple1, Z2Triple.createRandom(splitTriple1, secureRandom));
        // split all
        Z2Triple tripleAll = Z2Triple.createRandom(num, secureRandom);
        Z2Triple splitTripleAll = tripleAll.split(num);
        TripleTestUtils.assertOutput(0, tripleAll, Z2Triple.createRandom(tripleAll, secureRandom));
        TripleTestUtils.assertOutput(num, splitTripleAll, Z2Triple.createRandom(splitTripleAll, secureRandom));
        if (num > 1) {
            // split num - 1
            Z2Triple tripleNum = Z2Triple.createRandom(num, secureRandom);
            Z2Triple splitTripleNum = tripleNum.split(num - 1);
            TripleTestUtils.assertOutput(1, tripleNum, Z2Triple.createRandom(tripleNum, secureRandom));
            TripleTestUtils.assertOutput(num - 1, splitTripleNum, Z2Triple.createRandom(splitTripleNum, secureRandom));
            // split half
            Z2Triple tripleHalf = Z2Triple.createRandom(num, secureRandom);
            Z2Triple splitTripleHalf = tripleHalf.split(num / 2);
            TripleTestUtils.assertOutput( num - num / 2, tripleHalf, Z2Triple.createRandom(tripleHalf, secureRandom));
            TripleTestUtils.assertOutput(num / 2, splitTripleHalf, Z2Triple.createRandom(splitTripleHalf, secureRandom));
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
        Z2Triple triple1 = Z2Triple.createRandom(num, secureRandom);
        Z2Triple copyTriple1 = triple1.copy();
        Z2Triple splitTriple1 = triple1.split(1);
        triple1.merge(splitTriple1);
        Assert.assertEquals(copyTriple1, triple1);
        // split and merge all
        Z2Triple tripleAll = Z2Triple.createRandom(num, secureRandom);
        Z2Triple copyTripleAll = tripleAll.copy();
        Z2Triple splitTripleAll = tripleAll.split(num);
        tripleAll.merge(splitTripleAll);
        Assert.assertEquals(copyTripleAll, tripleAll);
        if (num > 1) {
            // split and merge num - 1
            Z2Triple tripleNum = Z2Triple.createRandom(num, secureRandom);
            Z2Triple copyTripleNum = tripleNum.copy();
            Z2Triple splitTripleNum = tripleNum.split(num - 1);
            tripleNum.merge(splitTripleNum);
            Assert.assertEquals(copyTripleNum, tripleNum);
            // split half
            Z2Triple tripleHalf = Z2Triple.createRandom(num, secureRandom);
            Z2Triple copyTripleHalf = tripleHalf.copy();
            Z2Triple splitTripleHalf = tripleHalf.split(num / 2);
            tripleHalf.merge(splitTripleHalf);
            Assert.assertEquals(copyTripleHalf, tripleHalf);
        }
    }
}
