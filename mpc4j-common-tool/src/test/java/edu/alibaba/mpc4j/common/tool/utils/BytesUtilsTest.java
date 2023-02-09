package edu.alibaba.mpc4j.common.tool.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * BytesUtils test.
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public class BytesUtilsTest {

    @Test
    public void testReverseByte() {
        Assert.assertEquals((byte)0b10000000, BytesUtils.reverseBit((byte)0b00000001));
        Assert.assertEquals((byte)0b00000010, BytesUtils.reverseBit((byte)0b01000000));
        Assert.assertEquals((byte)0b00000100, BytesUtils.reverseBit((byte)0b00100000));
        Assert.assertEquals((byte)0b00001000, BytesUtils.reverseBit((byte)0b00010000));
        Assert.assertEquals((byte)0b00010000, BytesUtils.reverseBit((byte)0b00001000));
        Assert.assertEquals((byte)0b00100000, BytesUtils.reverseBit((byte)0b00000100));
        Assert.assertEquals((byte)0b01000000, BytesUtils.reverseBit((byte)0b00000010));
        Assert.assertEquals((byte)0b10000000, BytesUtils.reverseBit((byte)0b00000001));
    }

    @Test
    public void testBitCount() {
        Assert.assertEquals(0, BytesUtils.bitCount(new byte[0]));
        Assert.assertEquals(1, BytesUtils.bitCount(new byte[] {(byte)0b00000001, }));
        Assert.assertEquals(1, BytesUtils.bitCount(new byte[] {(byte)0b00000010, }));
        Assert.assertEquals(2, BytesUtils.bitCount(new byte[] {(byte)0b00000101, }));
        Assert.assertEquals(2, BytesUtils.bitCount(new byte[] {(byte)0b00001001, }));
        Assert.assertEquals(3, BytesUtils.bitCount(new byte[] {(byte)0b01100001, }));
        Assert.assertEquals(3, BytesUtils.bitCount(new byte[] {(byte)0b11000001, }));
        Assert.assertEquals(5, BytesUtils.bitCount(new byte[] {(byte)0b01101110, }));
        Assert.assertEquals(8, BytesUtils.bitCount(new byte[] {(byte)0b11111111, }));
    }

    @Test
    public void testIsReducedByteArray() {
        // bitLength = 0, byteLength = 1
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x01}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x0F}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0xFF}, 0));
        // bitLength = 1, byteLength = 1
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00}, 1));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x01}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x0F}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0xFF}, 1));
        // bitLength = 8, byteLength = 1
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x01}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x0F}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0xFF}, 8));
        // bitLength = 0, byteLength = 2
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0x00}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0x01}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0x0F}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0xFF}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x01, (byte)0x00}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x0F, (byte)0x00}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0xFF, (byte)0x00}, 0));
        // bitLength = 1, byteLength = 2
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0x00}, 1));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0x01}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0x0F}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0xFF}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x01, (byte)0x00}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x0F, (byte)0x00}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0xFF, (byte)0x00}, 1));
        // bitLength = 8, byteLength = 2
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0x00}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0x01}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0x0F}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte)0x00, (byte)0xFF}, 8));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x01, (byte)0x00}, 8));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0x0F, (byte)0x00}, 8));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte)0xFF, (byte)0x00}, 8));
    }

    @Test
    public void testIsFixedReducedByteArray() {
        // bitLength = 0, expected byteLength = 0, byteLength = 1
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00}, 0, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x01}, 0, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x0F}, 0, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0xFF}, 0, 0));
        // bitLength = 0, expected byteLength = 1, byteLength = 1
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x01}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x0F}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0xFF}, 1, 0));
        // bitLength = 1, expected byteLength = 1, byteLength = 1
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00}, 1, 1));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x01}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x0F}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0xFF}, 1, 1));
        // bitLength = 8, expected byteLength = 1, byteLength = 1
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00}, 1, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x01}, 1, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x0F}, 1, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0xFF}, 1, 8));
        // bitLength = 0, expected byteLength = 1, byteLength = 2
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x00}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x01}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x0F}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0xFF}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x01, (byte)0x00}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x0F, (byte)0x00}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0xFF, (byte)0x00}, 1, 0));
        // bitLength = 0, expected byteLength = 2, byteLength = 2
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x00}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x01}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x0F}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0xFF}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x01, (byte)0x00}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x0F, (byte)0x00}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0xFF, (byte)0x00}, 2, 0));
        // bitLength = 1, expected byteLength = 1, byteLength = 2
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x00}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x01}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x0F}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0xFF}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x01, (byte)0x00}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x0F, (byte)0x00}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0xFF, (byte)0x00}, 1, 1));
        // bitLength = 1, expected byteLength = 2, byteLength = 2
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x00}, 2, 1));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x01}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x0F}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0xFF}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x01, (byte)0x00}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x0F, (byte)0x00}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0xFF, (byte)0x00}, 2, 1));
        // bitLength = 8, expected byteLength = 2, byteLength = 2
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x00}, 2, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x01}, 2, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0x0F}, 2, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x00, (byte)0xFF}, 2, 8));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x01, (byte)0x00}, 2, 8));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0x0F, (byte)0x00}, 2, 8));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte)0xFF, (byte)0x00}, 2, 8));
    }
}
