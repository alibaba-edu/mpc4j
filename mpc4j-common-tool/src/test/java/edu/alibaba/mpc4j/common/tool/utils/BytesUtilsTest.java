package edu.alibaba.mpc4j.common.tool.utils;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * BytesUtils test.
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public class BytesUtilsTest {

    @Test
    public void testReverseByte() {
        Assert.assertEquals((byte) 0b10000000, BytesUtils.reverseBit((byte) 0b00000001));
        Assert.assertEquals((byte) 0b00000010, BytesUtils.reverseBit((byte) 0b01000000));
        Assert.assertEquals((byte) 0b00000100, BytesUtils.reverseBit((byte) 0b00100000));
        Assert.assertEquals((byte) 0b00001000, BytesUtils.reverseBit((byte) 0b00010000));
        Assert.assertEquals((byte) 0b00010000, BytesUtils.reverseBit((byte) 0b00001000));
        Assert.assertEquals((byte) 0b00100000, BytesUtils.reverseBit((byte) 0b00000100));
        Assert.assertEquals((byte) 0b01000000, BytesUtils.reverseBit((byte) 0b00000010));
        Assert.assertEquals((byte) 0b10000000, BytesUtils.reverseBit((byte) 0b00000001));
    }

    @Test
    public void testBitCount() {
        Assert.assertEquals(0, BytesUtils.bitCount(new byte[0]));
        Assert.assertEquals(1, BytesUtils.bitCount(new byte[]{(byte) 0b00000001,}));
        Assert.assertEquals(1, BytesUtils.bitCount(new byte[]{(byte) 0b00000010,}));
        Assert.assertEquals(2, BytesUtils.bitCount(new byte[]{(byte) 0b00000101,}));
        Assert.assertEquals(2, BytesUtils.bitCount(new byte[]{(byte) 0b00001001,}));
        Assert.assertEquals(3, BytesUtils.bitCount(new byte[]{(byte) 0b01100001,}));
        Assert.assertEquals(3, BytesUtils.bitCount(new byte[]{(byte) 0b11000001,}));
        Assert.assertEquals(5, BytesUtils.bitCount(new byte[]{(byte) 0b01101110,}));
        Assert.assertEquals(8, BytesUtils.bitCount(new byte[]{(byte) 0b11111111,}));
    }

    @Test
    public void testIsReducedByteArray() {
        // bitLength = 0, byteLength = 1
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000001}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00001111}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b11111111}, 0));
        // bitLength = 1, byteLength = 1
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000}, 1));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000001}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00001111}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b11111111}, 1));
        // bitLength = 8, byteLength = 1
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000001}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00001111}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b11111111}, 8));
        // bitLength = 0, byteLength = 2
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000000}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000001}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00001111}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b11111111}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000001, (byte) 0b00000000}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00001111, (byte) 0b00000000}, 0));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b11111111, (byte) 0b00000000}, 0));
        // bitLength = 1, byteLength = 2
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000000}, 1));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000001}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00001111}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b11111111}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000001, (byte) 0b00000000}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00001111, (byte) 0b00000000}, 1));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b11111111, (byte) 0b00000000}, 1));
        // bitLength = 8, byteLength = 2
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000000}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000001}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00001111}, 8));
        Assert.assertTrue(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b11111111}, 8));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00000001, (byte) 0b00000000}, 8));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b00001111, (byte) 0b00000000}, 8));
        Assert.assertFalse(BytesUtils.isReduceByteArray(new byte[]{(byte) 0b11111111, (byte) 0b00000000}, 8));
    }

    @Test
    public void testIsFixedReducedByteArray() {
        // bitLength = 0, expected byteLength = 0, byteLength = 1
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000}, 0, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000001}, 0, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00001111}, 0, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b11111111}, 0, 0));
        // bitLength = 0, expected byteLength = 1, byteLength = 1
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000001}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00001111}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b11111111}, 1, 0));
        // bitLength = 1, expected byteLength = 1, byteLength = 1
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000}, 1, 1));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000001}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00001111}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b11111111}, 1, 1));
        // bitLength = 8, expected byteLength = 1, byteLength = 1
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000}, 1, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000001}, 1, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00001111}, 1, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b11111111}, 1, 8));
        // bitLength = 0, expected byteLength = 1, byteLength = 2
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000000}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000001}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00001111}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b11111111}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000001, (byte) 0b00000000}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00001111, (byte) 0b00000000}, 1, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b11111111, (byte) 0b00000000}, 1, 0));
        // bitLength = 0, expected byteLength = 2, byteLength = 2
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000000}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000001}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00001111}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b11111111}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000001, (byte) 0b00000000}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00001111, (byte) 0b00000000}, 2, 0));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b11111111, (byte) 0b00000000}, 2, 0));
        // bitLength = 1, expected byteLength = 1, byteLength = 2
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000000}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000001}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00001111}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b11111111}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000001, (byte) 0b00000000}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00001111, (byte) 0b00000000}, 1, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b11111111, (byte) 0b00000000}, 1, 1));
        // bitLength = 1, expected byteLength = 2, byteLength = 2
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000000}, 2, 1));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000001}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00001111}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b11111111}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000001, (byte) 0b00000000}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00001111, (byte) 0b00000000}, 2, 1));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b11111111, (byte) 0b00000000}, 2, 1));
        // bitLength = 8, expected byteLength = 2, byteLength = 2
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000000}, 2, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00000001}, 2, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b00001111}, 2, 8));
        Assert.assertTrue(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000000, (byte) 0b11111111}, 2, 8));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00000001, (byte) 0b00000000}, 2, 8));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b00001111, (byte) 0b00000000}, 2, 8));
        Assert.assertFalse(BytesUtils.isFixedReduceByteArray(new byte[]{(byte) 0b11111111, (byte) 0b00000000}, 2, 8));
    }

    @Test
    public void testShiftLeft() {
        byte[] data = new byte[]{(byte) 0b00000000, (byte) 0b00001011};
        BigInteger and = BigInteger.ONE.shiftLeft(data.length * Byte.SIZE).subtract(BigInteger.ONE);
        BigInteger dataBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(data);
        for (int x = 0; x < data.length * Byte.SIZE + 1; x++) {
            byte[] actual = BytesUtils.shiftLeft(data, x);
            byte[] expect = BigIntegerUtils.nonNegBigIntegerToByteArray(dataBigInteger.shiftLeft(x).and(and), data.length);
            Assert.assertArrayEquals(expect, actual);
        }
    }

    @Test
    public void testShiftLefti() {
        byte[] data = new byte[]{(byte) 0b00000000, (byte) 0b00001011};
        BigInteger and = BigInteger.ONE.shiftLeft(data.length * Byte.SIZE).subtract(BigInteger.ONE);
        BigInteger dataBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(data);
        for (int x = 0; x < data.length * Byte.SIZE + 1; x++) {
            byte[] actual = BytesUtils.clone(data);
            BytesUtils.shiftLefti(actual, x);
            byte[] expect = BigIntegerUtils.nonNegBigIntegerToByteArray(dataBigInteger.shiftLeft(x).and(and), data.length);
            Assert.assertArrayEquals(expect, actual);
        }
    }

    @Test
    public void testShiftRight() {
        byte[] data = new byte[]{(byte) 0b11010000, (byte) 0b00000000};
        BigInteger and = BigInteger.ONE.shiftLeft(data.length * Byte.SIZE).subtract(BigInteger.ONE);
        BigInteger dataBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(data);
        for (int x = 0; x < data.length * Byte.SIZE + 1; x++) {
            byte[] actual = BytesUtils.shiftRight(data, x);
            byte[] expect = BigIntegerUtils.nonNegBigIntegerToByteArray(dataBigInteger.shiftRight(x).and(and), data.length);
            Assert.assertArrayEquals(expect, actual);
        }
    }

    @Test
    public void testShiftRighti() {
        byte[] data = new byte[]{(byte) 0b11010000, (byte) 0b00000000};
        BigInteger and = BigInteger.ONE.shiftLeft(data.length * Byte.SIZE).subtract(BigInteger.ONE);
        BigInteger dataBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(data);
        for (int x = 0; x < data.length * Byte.SIZE + 1; x++) {
            byte[] actual = BytesUtils.clone(data);
            BytesUtils.shiftRighti(actual, x);
            byte[] expect = BigIntegerUtils.nonNegBigIntegerToByteArray(dataBigInteger.shiftRight(x).and(and), data.length);
            Assert.assertArrayEquals(expect, actual);
        }
    }

    @Test
    public void testCreateReduceByteArray() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] data = new byte[10];
        secureRandom.nextBytes(data);
        BigInteger v = BigIntegerUtils.byteArrayToNonNegBigInteger(data);
        for (int i = (data.length * Byte.SIZE) - 1; i > 0; i--) {
            byte[] result = BytesUtils.createReduceByteArray(data, i);
            Assert.assertEquals(CommonUtils.getByteLength(i), result.length);
            BigInteger actual = BigIntegerUtils.byteArrayToNonNegBigInteger(result);
            BigInteger expect = v.and(BigInteger.ONE.shiftLeft(i).subtract(BigInteger.ONE));
            Assert.assertEquals(expect, actual);
        }
    }

    @Test
    public void testCopyByteArray() {
        byte[] data = new byte[10];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(data);
        for (int length = 1; length < (data.length << 1); length++) {
            byte[] result = BytesUtils.copyByteArray(data, length);
            Assert.assertEquals(length, result.length);
            int minLength = Math.min(length, data.length);
            Assert.assertArrayEquals(
                Arrays.copyOfRange(result, result.length - minLength, result.length),
                Arrays.copyOfRange(data, data.length - minLength, data.length)
            );
        }
    }
}
