package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * String to Uint64 unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/stringtouint64.cpp
 *
 * @author Anony_Trent
 * @date 2023/9/25
 */
public class StringToUint64Test {

    @Test
    public void testIsHexChar() {
        Assert.assertTrue(Common.isHexChar('0'));
        Assert.assertTrue(Common.isHexChar('1'));
        Assert.assertTrue(Common.isHexChar('2'));
        Assert.assertTrue(Common.isHexChar('3'));
        Assert.assertTrue(Common.isHexChar('4'));
        Assert.assertTrue(Common.isHexChar('5'));
        Assert.assertTrue(Common.isHexChar('6'));
        Assert.assertTrue(Common.isHexChar('7'));
        Assert.assertTrue(Common.isHexChar('8'));
        Assert.assertTrue(Common.isHexChar('9'));
        Assert.assertTrue(Common.isHexChar('A'));
        Assert.assertTrue(Common.isHexChar('B'));
        Assert.assertTrue(Common.isHexChar('C'));
        Assert.assertTrue(Common.isHexChar('D'));
        Assert.assertTrue(Common.isHexChar('E'));
        Assert.assertTrue(Common.isHexChar('F'));
        Assert.assertTrue(Common.isHexChar('a'));
        Assert.assertTrue(Common.isHexChar('b'));
        Assert.assertTrue(Common.isHexChar('c'));
        Assert.assertTrue(Common.isHexChar('d'));
        Assert.assertTrue(Common.isHexChar('e'));
        Assert.assertTrue(Common.isHexChar('f'));

        Assert.assertFalse(Common.isHexChar('/'));
        Assert.assertFalse(Common.isHexChar(' '));
        Assert.assertFalse(Common.isHexChar('+'));
        Assert.assertFalse(Common.isHexChar('\\'));
        Assert.assertFalse(Common.isHexChar('G'));
        Assert.assertFalse(Common.isHexChar('g'));
        Assert.assertFalse(Common.isHexChar('Z'));
        Assert.assertFalse(Common.isHexChar('Z'));
    }

    @Test
    public void testHexToNibble() {
        Assert.assertEquals(0, Common.hexToNibble('0'));
        Assert.assertEquals(1, Common.hexToNibble('1'));
        Assert.assertEquals(2, Common.hexToNibble('2'));
        Assert.assertEquals(3, Common.hexToNibble('3'));
        Assert.assertEquals(4, Common.hexToNibble('4'));
        Assert.assertEquals(5, Common.hexToNibble('5'));
        Assert.assertEquals(6, Common.hexToNibble('6'));
        Assert.assertEquals(7, Common.hexToNibble('7'));
        Assert.assertEquals(8, Common.hexToNibble('8'));
        Assert.assertEquals(9, Common.hexToNibble('9'));
        Assert.assertEquals(10, Common.hexToNibble('A'));
        Assert.assertEquals(11, Common.hexToNibble('B'));
        Assert.assertEquals(12, Common.hexToNibble('C'));
        Assert.assertEquals(13, Common.hexToNibble('D'));
        Assert.assertEquals(14, Common.hexToNibble('E'));
        Assert.assertEquals(15, Common.hexToNibble('F'));
        Assert.assertEquals(10, Common.hexToNibble('a'));
        Assert.assertEquals(11, Common.hexToNibble('b'));
        Assert.assertEquals(12, Common.hexToNibble('c'));
        Assert.assertEquals(13, Common.hexToNibble('d'));
        Assert.assertEquals(14, Common.hexToNibble('e'));
        Assert.assertEquals(15, Common.hexToNibble('f'));
    }

    @Test
    public void testGetHexStringBitCount() {
        Assert.assertEquals(0, Common.getHexStringBitCount(null, 0));
        Assert.assertEquals(0, Common.getHexStringBitCount("0", 1));
        Assert.assertEquals(0, Common.getHexStringBitCount("000000000", 9));
        Assert.assertEquals(1, Common.getHexStringBitCount("1", 1));
        Assert.assertEquals(1, Common.getHexStringBitCount("00001", 5));
        Assert.assertEquals(2, Common.getHexStringBitCount("2", 1));
        Assert.assertEquals(2, Common.getHexStringBitCount("00002", 5));
        Assert.assertEquals(2, Common.getHexStringBitCount("3", 1));
        Assert.assertEquals(2, Common.getHexStringBitCount("0003", 4));
        Assert.assertEquals(3, Common.getHexStringBitCount("4", 1));
        Assert.assertEquals(3, Common.getHexStringBitCount("5", 1));
        Assert.assertEquals(3, Common.getHexStringBitCount("6", 1));
        Assert.assertEquals(3, Common.getHexStringBitCount("7", 1));
        Assert.assertEquals(4, Common.getHexStringBitCount("8", 1));
        Assert.assertEquals(4, Common.getHexStringBitCount("9", 1));
        Assert.assertEquals(4, Common.getHexStringBitCount("A", 1));
        Assert.assertEquals(4, Common.getHexStringBitCount("B", 1));
        Assert.assertEquals(4, Common.getHexStringBitCount("C", 1));
        Assert.assertEquals(4, Common.getHexStringBitCount("D", 1));
        Assert.assertEquals(4, Common.getHexStringBitCount("E", 1));
        Assert.assertEquals(4, Common.getHexStringBitCount("F", 1));
        Assert.assertEquals(5, Common.getHexStringBitCount("10", 2));
        Assert.assertEquals(5, Common.getHexStringBitCount("00010", 5));
        Assert.assertEquals(5, Common.getHexStringBitCount("11", 2));
        Assert.assertEquals(5, Common.getHexStringBitCount("1F", 2));
        Assert.assertEquals(6, Common.getHexStringBitCount("20", 2));
        Assert.assertEquals(6, Common.getHexStringBitCount("2F", 2));
        Assert.assertEquals(7, Common.getHexStringBitCount("7F", 2));
        Assert.assertEquals(7, Common.getHexStringBitCount("0007F", 5));
        Assert.assertEquals(8, Common.getHexStringBitCount("80", 2));
        Assert.assertEquals(8, Common.getHexStringBitCount("FF", 2));
        Assert.assertEquals(8, Common.getHexStringBitCount("00FF", 4));
        Assert.assertEquals(9, Common.getHexStringBitCount("100", 3));
        Assert.assertEquals(9, Common.getHexStringBitCount("000100", 6));
        Assert.assertEquals(22, Common.getHexStringBitCount("200000", 6));
        Assert.assertEquals(35, Common.getHexStringBitCount("7FFF30001", 9));

        Assert.assertEquals(15, Common.getHexStringBitCount("7FFF30001", 4));
        Assert.assertEquals(3, Common.getHexStringBitCount("7FFF30001", 1));
        Assert.assertEquals(0, Common.getHexStringBitCount("7FFF30001", 0));
    }

    @Test
    public void testHexStringToUint64() {
        long[] correct = new long[3];
        long[] parsed = new long[3];

        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("0", 1, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("0", 1, 1, parsed);
        Assert.assertArrayEquals(Arrays.copyOf(correct, 1), Arrays.copyOf(parsed, 1));
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint(null, 0, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 1;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("1", 1, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("01", 2, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("001", 3, 1, parsed);
        Assert.assertArrayEquals(Arrays.copyOf(correct, 1), Arrays.copyOf(parsed, 1));

        correct[0] = 0xF;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("F", 1, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0x10;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("10", 2, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("010", 3, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0x100;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("100", 3, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0x123;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("123", 3, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("00000123", 8, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0;
        correct[1] = 1;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("10000000000000000", 17, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0x1123456789ABCDEFL;
        correct[1] = 0x1;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("11123456789ABCDEF", 17, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("000011123456789ABCDEF", 21, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0x3456789ABCDEF123L;
        correct[1] = 0x23456789ABCDEF12L;
        correct[2] = 0x123456789ABCDEF1L;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("123456789ABCDEF123456789ABCDEF123456789ABCDEF123", 48, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0xFFFFFFFFFFFFFFFFL;
        correct[1] = 0xFFFFFFFFFFFFFFFFL;
        correct[2] = 0xFFFFFFFFFFFFFFFFL;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 48, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0x100;
        correct[1] = 0;
        correct[2] = 0;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("100", 3, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0x10;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("100", 2, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0x1;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("100", 1, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);

        correct[0] = 0;
        parsed[0] = 0x123;
        parsed[1] = 0x123;
        parsed[2] = 0x123;
        UintCore.hexStringToUint("100", 0, 3, parsed);
        Assert.assertArrayEquals(correct, parsed);
    }
}