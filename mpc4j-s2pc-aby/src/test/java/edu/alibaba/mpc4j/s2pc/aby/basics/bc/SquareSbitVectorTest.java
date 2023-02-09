package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 布尔方括向量测试。
 *
 * @author Weiran Liu
 * @date 2022/12/13
 */
public class SquareSbitVectorTest {
    /**
     * 最小比特长度
     */
    private static final int MIN_BIT_LENGTH = 1;
    /**
     * 最大比特长度
     */
    private static final int MAX_BIT_LENGTH = 128;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testIllegalInputs() {
        // create vector with 0 length
        Assert.assertThrows(AssertionError.class, () -> SquareSbitVector.create(0, new byte[0], true));
        Assert.assertThrows(AssertionError.class, () -> SquareSbitVector.create(0, new byte[0], false));
        // create vector with mismatch bit num
        Assert.assertThrows(AssertionError.class, () -> SquareSbitVector.create(1, new byte[] { (byte)0x0F, }, true));
        Assert.assertThrows(AssertionError.class, () -> SquareSbitVector.create(1, new byte[] { (byte)0x0F, }, false));
        // create vector with mismatch byte num
        Assert.assertThrows(AssertionError.class, () -> SquareSbitVector.create(1, new byte[2], true));
        Assert.assertThrows(AssertionError.class, () -> SquareSbitVector.create(1, new byte[2], false));
        Assert.assertThrows(AssertionError.class, () -> SquareSbitVector.create(9, new byte[1], true));
        Assert.assertThrows(AssertionError.class, () -> SquareSbitVector.create(9, new byte[1], false));
        // merge vector with mismatch byte plain state
        Assert.assertThrows(AssertionError.class, () -> {
            SquareSbitVector vector0 = SquareSbitVector.create(4, new byte[] {(byte) 0x0F, }, true);
            SquareSbitVector vector1 = SquareSbitVector.create(4, new byte[] {(byte) 0x0F, }, false);
            vector0.merge(vector1);
        });
    }

    @Test
    public void testOnesMerge() {
        for (int num1 = MIN_BIT_LENGTH; num1 < MAX_BIT_LENGTH; num1++) {
            for (int num2 = MIN_BIT_LENGTH; num2 < MAX_BIT_LENGTH; num2++) {
                testOnesMerge(num1, num2);
            }
        }
    }

    private void testOnesMerge(int num1, int num2) {
        SquareSbitVector vector1 = SquareSbitVector.createOnes(num1);
        SquareSbitVector vector2 = SquareSbitVector.createOnes(num2);
        // manually merge
        int expectMergeByteLength = CommonUtils.getByteLength(num1 + num2);
        byte[] expectMerge = new byte[expectMergeByteLength];
        Arrays.fill(expectMerge, (byte) 0xFF);
        BytesUtils.reduceByteArray(expectMerge, num1 + num2);
        // merge and verify
        vector1.merge(vector2);
        Assert.assertTrue(vector1.isPlain());
        Assert.assertEquals(num1 + num2, vector1.bitNum());
        Assert.assertArrayEquals(expectMerge, vector1.getBytes());
    }

    @Test
    public void testRandomMerge() {
        for (int num1 = MIN_BIT_LENGTH; num1 < MAX_BIT_LENGTH; num1++) {
            for (int num2 = MIN_BIT_LENGTH; num2 < MAX_BIT_LENGTH; num2++) {
                testRandomMerge(num1, num2);
            }
        }
    }

    private void testRandomMerge(int num1, int num2) {
        int num1Offset = CommonUtils.getByteLength(num1) * Byte.SIZE - num1;
        int num2Offset = CommonUtils.getByteLength(num2) * Byte.SIZE - num2;
        SquareSbitVector vector1 = SquareSbitVector.createRandom(num1, SECURE_RANDOM);
        SquareSbitVector vector2 = SquareSbitVector.createRandom(num2, SECURE_RANDOM);
        // manually merge
        byte[] vector1Bytes = vector1.getBytes();
        byte[] vector2Bytes = vector2.getBytes();
        int expectMergeByteLength = CommonUtils.getByteLength(num1 + num2);
        int expectMergeOffset = expectMergeByteLength * Byte.SIZE - (num1 + num2);
        byte[] expectMerge = new byte[expectMergeByteLength];
        for (int index = 0; index < num1; index++) {
            if (BinaryUtils.getBoolean(vector1Bytes, num1Offset + index)) {
                BinaryUtils.setBoolean(expectMerge, expectMergeOffset + index, true);
            }
        }
        for (int index = 0; index < num2; index++) {
            if (BinaryUtils.getBoolean(vector2Bytes, num2Offset + index)) {
                BinaryUtils.setBoolean(expectMerge, expectMergeOffset + num1 + index, true);
            }
        }
        // merge and verify
        vector1.merge(vector2);
        Assert.assertEquals(num1 + num2, vector1.bitNum());
        Assert.assertArrayEquals(expectMerge, vector1.getBytes());
    }
}
