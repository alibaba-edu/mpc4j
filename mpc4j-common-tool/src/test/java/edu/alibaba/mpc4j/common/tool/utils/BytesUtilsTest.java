package edu.alibaba.mpc4j.common.tool.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * 字节数组工具类测试。
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
}
