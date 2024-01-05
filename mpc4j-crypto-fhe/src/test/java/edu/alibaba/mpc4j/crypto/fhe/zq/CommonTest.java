package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.junit.Assert;
import org.junit.Test;

/**
 * Common unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/common.cpp
 * </p>
 *
 * @author Anony_Trent, Liqiang Peng
 * @date 2023/8/9
 */
public class CommonTest {

    @Test
    public void testCommonConstants() {
        Assert.assertEquals(4, Common.BITS_PER_NIBBLE);
        Assert.assertEquals(8, Common.BITS_PER_BYTE);
        Assert.assertEquals(8, Common.BYTES_PER_UINT64);
        Assert.assertEquals(64, Common.BITS_PER_UINT64);
        Assert.assertEquals(2, Common.NIBBLES_PER_BYTE);
        Assert.assertEquals(16, Common.NIBBLES_PER_UINT64);
    }

    @Test
    public void testCommonUnsignedComparisons() {
        int posI = 5;
        int negI = -5;
        int posU = 6;
        int posS = 6;
        long posUll = 1;
        long posUllMax = 0xFFFF_FFFF_FFFF_FFFFL;
        long negUll = -1;

        Assert.assertTrue(Common.unsignedEq(posI, posI));
        Assert.assertFalse(Common.unsignedEq(posI, negI));
        Assert.assertTrue(Common.unsignedGt(posU, posI));
        Assert.assertTrue(Common.unsignedLt(posI, negI));
        Assert.assertTrue(Common.unsignedGeq(posU, posS));
        Assert.assertTrue(Common.unsignedEq(negUll, posUllMax));
        Assert.assertFalse(Common.unsignedLt(negUll, posUllMax));
        Assert.assertTrue(Common.unsignedLt(posUll, posUllMax));
    }

    @Test
    public void testCommonSafeArithmetic() {
        int posI = 5;
        int negI = -5;
        int posU = 6;
        long posUllMax = 0xFFFF_FFFF_FFFF_FFFFL;
        long negUll = -1;

        Assert.assertEquals(25, Common.mulSafe(posI, posI, false));
        Assert.assertEquals(25, Common.mulSafe(negI, negI, false));
        Assert.assertEquals(10, Common.addSafe(posI, posI, false));
        Assert.assertEquals(-10, Common.addSafe(negI, negI, false));
        Assert.assertEquals(0, Common.addSafe(posI, negI, false));
        Assert.assertEquals(0, Common.addSafe(negI, posI, false));
        Assert.assertEquals(10, Common.subSafe(posI, negI, false));
        Assert.assertEquals(-10, Common.subSafe(negI, posI, false));
        Assert.assertEquals(0, Common.subSafe(posU, posU, true));
        Assert.assertThrows(ArithmeticException.class, () -> Common.subSafe(0, posU, true));
        Assert.assertThrows(ArithmeticException.class, () -> Common.subSafe(4, posU, true));
        Assert.assertThrows(ArithmeticException.class, () -> Common.mulSafe(posUllMax, posUllMax, true));
        Assert.assertEquals(0L, Common.mulSafe(0L, posUllMax, false));
        Assert.assertEquals(1L, Common.mulSafe(negUll, negUll, false));
        Assert.assertEquals(15, Common.addSafe(posI, -posI, false, posI, posI, posI));
        Assert.assertEquals(6, Common.addSafe(0, -posI, false, posI, 1, posI));
        Assert.assertEquals(0, Common.mulSafe(posI, posI, false, posI, 0, posI));
        Assert.assertEquals(625, Common.mulSafe(posI, posI, false, posI, posI));
        Assert.assertThrows(
            ArithmeticException.class, () -> Common.mulSafe(
                posI, posI, false, posI, posI, posI, posI, posI, posI, posI, posI, posI, posI, posI, posI
            )
        );
    }

    @Test
    public void testCommonFitsIn() {
        int pos_s = 6;
        float f = 1.234f;
        double d = -1234;

        Assert.assertTrue(Common.productFitsIn(true, pos_s));
        Assert.assertTrue(Common.productFitsIn(false, (int) d));
        Assert.assertTrue(Common.productFitsIn(true, (int) f));
    }

    @Test
    public void testCommonDivideRoundUp() {
        Assert.assertEquals(0, Common.divideRoundUp(0, 4));
        Assert.assertEquals(1, Common.divideRoundUp(1, 4));
        Assert.assertEquals(1, Common.divideRoundUp(2, 4));
        Assert.assertEquals(1, Common.divideRoundUp(3, 4));
        Assert.assertEquals(1, Common.divideRoundUp(4, 4));
        Assert.assertEquals(2, Common.divideRoundUp(5, 4));
        Assert.assertEquals(2, Common.divideRoundUp(6, 4));
        Assert.assertEquals(2, Common.divideRoundUp(7, 4));
        Assert.assertEquals(2, Common.divideRoundUp(8, 4));
        Assert.assertEquals(3, Common.divideRoundUp(9, 4));
        Assert.assertEquals(3, Common.divideRoundUp(12, 4));
        Assert.assertEquals(4, Common.divideRoundUp(13, 4));
    }

    @Test
    public void hammingWeight() {
        Assert.assertEquals(0, Common.hammingWeight((byte) 0x00));
        Assert.assertEquals(8, Common.hammingWeight((byte) 0xFF));
        Assert.assertEquals(4, Common.hammingWeight((byte) 0xF0));
        Assert.assertEquals(4, Common.hammingWeight((byte) 0x0F));
        Assert.assertEquals(2, Common.hammingWeight((byte) 0xC0));
        Assert.assertEquals(2, Common.hammingWeight((byte) 0x0C));
        Assert.assertEquals(2, Common.hammingWeight((byte) 0x03));
        Assert.assertEquals(2, Common.hammingWeight((byte) 0x30));
        Assert.assertEquals(4, Common.hammingWeight((byte) 0xAA));
        Assert.assertEquals(4, Common.hammingWeight((byte) 0x55));
        Assert.assertEquals(5, Common.hammingWeight((byte) 0xD6));
        Assert.assertEquals(5, Common.hammingWeight((byte) 0x6D));
        Assert.assertEquals(7, Common.hammingWeight((byte) 0xBF));
        Assert.assertEquals(7, Common.hammingWeight((byte) 0xFB));
    }

    @Test
    public void testCommonReversedBits32() {
        Assert.assertEquals((0), Common.reverseBits((0)));
        Assert.assertEquals((0x80000000), Common.reverseBits((1)));
        Assert.assertEquals((0x40000000), Common.reverseBits((2)));
        Assert.assertEquals((0xC0000000), Common.reverseBits((3)));
        Assert.assertEquals((0x00010000), Common.reverseBits((0x00008000)));
        Assert.assertEquals((0xFFFF0000), Common.reverseBits((0x0000FFFF)));
        Assert.assertEquals((0x0000FFFF), Common.reverseBits((0xFFFF0000)));
        Assert.assertEquals((0x00008000), Common.reverseBits((0x00010000)));
        Assert.assertEquals((3), Common.reverseBits((0xC0000000)));
        Assert.assertEquals((2), Common.reverseBits((0x40000000)));
        Assert.assertEquals((1), Common.reverseBits((0x80000000)));
        Assert.assertEquals((0xFFFFFFFF), Common.reverseBits((0xFFFFFFFF)));

        // Reversing a 0-bit item should return 0
        Assert.assertEquals((0), Common.reverseBits((0xFFFFFFFF), 0));

        // Reversing a 32-bit item returns is same as normal reverse
        Assert.assertEquals((0), Common.reverseBits((0), 32));
        Assert.assertEquals((0x80000000), Common.reverseBits((1), 32));
        Assert.assertEquals((0x40000000), Common.reverseBits((2), 32));
        Assert.assertEquals((0xC0000000), Common.reverseBits((3), 32));
        Assert.assertEquals((0x00010000), Common.reverseBits((0x00008000), 32));
        Assert.assertEquals((0xFFFF0000), Common.reverseBits((0x0000FFFF), 32));
        Assert.assertEquals((0x0000FFFF), Common.reverseBits((0xFFFF0000), 32));
        Assert.assertEquals((0x00008000), Common.reverseBits((0x00010000), 32));
        Assert.assertEquals((3), Common.reverseBits((0xC0000000), 32));
        Assert.assertEquals((2), Common.reverseBits((0x40000000), 32));
        Assert.assertEquals((1), Common.reverseBits((0x80000000), 32));
        Assert.assertEquals((0xFFFFFFFF), Common.reverseBits((0xFFFFFFFF), 32));

        // 16-bit reversal
        Assert.assertEquals((0), Common.reverseBits((0), 16));
        Assert.assertEquals((0x00008000), Common.reverseBits((1), 16));
        Assert.assertEquals((0x00004000), Common.reverseBits((2), 16));
        Assert.assertEquals((0x0000C000), Common.reverseBits((3), 16));
        Assert.assertEquals((0x00000001), Common.reverseBits((0x00008000), 16));
        Assert.assertEquals((0x0000FFFF), Common.reverseBits((0x0000FFFF), 16));
        Assert.assertEquals((0x00000000), Common.reverseBits((0xFFFF0000), 16));
        Assert.assertEquals((0x00000000), Common.reverseBits((0x00010000), 16));
        Assert.assertEquals((3), Common.reverseBits((0x0000C000), 16));
        Assert.assertEquals((2), Common.reverseBits((0x00004000), 16));
        Assert.assertEquals((1), Common.reverseBits((0x00008000), 16));
        Assert.assertEquals((0x0000FFFF), Common.reverseBits((0xFFFFFFFF), 16));
    }

    @Test
    public void testCommonReversedBits64() {
        Assert.assertEquals(0L, Common.reverseBits(0L));
        Assert.assertEquals(1L << 63, Common.reverseBits(1L));
        Assert.assertEquals(1L << 32, Common.reverseBits(1L << 31));
        Assert.assertEquals(0xFFFFL << 32, Common.reverseBits(0xFFFFL << 16));
        Assert.assertEquals(0x0000FFFFFFFF0000L, Common.reverseBits(0x0000FFFFFFFF0000L));
        Assert.assertEquals(0x0000FFFF0000FFFFL, Common.reverseBits(0xFFFF0000FFFF0000L));

        Assert.assertEquals(0L, Common.reverseBits(0L, 0));
        Assert.assertEquals(0L, Common.reverseBits(0L, 1));
        Assert.assertEquals(0L, Common.reverseBits(0L, 32));
        Assert.assertEquals(0L, Common.reverseBits(0L, 64));

        Assert.assertEquals(0L, Common.reverseBits(1L, 0));
        Assert.assertEquals(1L, Common.reverseBits(1L, 1));
        Assert.assertEquals(1L << 31, Common.reverseBits(1L, 32));
        Assert.assertEquals(1L << 63, Common.reverseBits(1L, 64));

        Assert.assertEquals(0L, Common.reverseBits(1L << 31, 0));
        Assert.assertEquals(0L, Common.reverseBits(1L << 31, 1));
        Assert.assertEquals(1L, Common.reverseBits(1L << 31, 32));
        Assert.assertEquals(1L << 32, Common.reverseBits(1L << 31, 64));

        Assert.assertEquals(0L, Common.reverseBits(0xFFFFL << 16, 0));
        Assert.assertEquals(0L, Common.reverseBits(0xFFFFL << 16, 1));
        Assert.assertEquals(0xFFFFL, Common.reverseBits(0xFFFFL << 16, 32));
        Assert.assertEquals(0xFFFFL << 32, Common.reverseBits(0xFFFFL << 16, 64));

        Assert.assertEquals(0L, Common.reverseBits(0x0000FFFFFFFF0000L, 0));
        Assert.assertEquals(0L, Common.reverseBits(0x0000FFFFFFFF0000L, 1));
        Assert.assertEquals(0xFFFFL, Common.reverseBits(0x0000FFFFFFFF0000L, 32));
        Assert.assertEquals(0x0000FFFFFFFF0000L, Common.reverseBits(0x0000FFFFFFFF0000L, 64));

        Assert.assertEquals(0L, Common.reverseBits(0xFFFF0000FFFF0000L, 0));
        Assert.assertEquals(0L, Common.reverseBits(0xFFFF0000FFFF0000L, 1));
        Assert.assertEquals(0xFFFFL, Common.reverseBits(0xFFFF0000FFFF0000L, 32));
        Assert.assertEquals(0x0000FFFF0000FFFFL, Common.reverseBits(0xFFFF0000FFFF0000L, 64));
    }

    @Test
    public void getMsbIndexTest() {
        long result;
        result = Common.getMsbIndex(1);
        Assert.assertEquals(0, result);
        result = Common.getMsbIndex(2);
        Assert.assertEquals(1, result);
        result = Common.getMsbIndex(3);
        Assert.assertEquals(1, result);
        result = Common.getMsbIndex(4);
        Assert.assertEquals(2, result);
        result = Common.getMsbIndex(16);
        Assert.assertEquals(4, result);
        result = Common.getMsbIndex(0xFFFFFFFFL);
        Assert.assertEquals(31, result);
        result = Common.getMsbIndex(0x100000000L);
        Assert.assertEquals(32, result);
        result = Common.getMsbIndex(0xFFFFFFFFFFFFFFFFL);
        Assert.assertEquals(63, result);
    }
}
