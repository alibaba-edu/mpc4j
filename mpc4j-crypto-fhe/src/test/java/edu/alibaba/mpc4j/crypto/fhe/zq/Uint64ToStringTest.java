package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.rq.PolyCore;
import org.junit.Assert;
import org.junit.Test;

/**
 * String to Uint64 unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/uint64tostring.cpp
 *
 * @author Anony_Trent
 * @date 2023/9/25
 */
public class Uint64ToStringTest {

    @Test
    public void testNibbleToUpperHex() {
        Assert.assertEquals('0', Common.nibbleToUpperHex(0));
        Assert.assertEquals('1', Common.nibbleToUpperHex(1));
        Assert.assertEquals('2', Common.nibbleToUpperHex(2));
        Assert.assertEquals('3', Common.nibbleToUpperHex(3));
        Assert.assertEquals('4', Common.nibbleToUpperHex(4));
        Assert.assertEquals('5', Common.nibbleToUpperHex(5));
        Assert.assertEquals('6', Common.nibbleToUpperHex(6));
        Assert.assertEquals('7', Common.nibbleToUpperHex(7));
        Assert.assertEquals('8', Common.nibbleToUpperHex(8));
        Assert.assertEquals('9', Common.nibbleToUpperHex(9));
        Assert.assertEquals('A', Common.nibbleToUpperHex(10));
        Assert.assertEquals('B', Common.nibbleToUpperHex(11));
        Assert.assertEquals('C', Common.nibbleToUpperHex(12));
        Assert.assertEquals('D', Common.nibbleToUpperHex(13));
        Assert.assertEquals('E', Common.nibbleToUpperHex(14));
        Assert.assertEquals('F', Common.nibbleToUpperHex(15));
    }

    @Test
    public void testUint64ToHexString() {
        long[] number = new long[] { 0, 0, 0 };
        String correct = "0";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 1));
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 0));
        Assert.assertEquals(correct, UintCore.uintToHexString(null, 0));

        number[0] = 1;
        correct = "1";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 1));

        number[0] = 0xF;
        correct = "F";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));

        number[0] = 0x10;
        correct = "10";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));

        number[0] = 0x100;
        correct = "100";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));

        number[0] = 0x123;
        correct = "123";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));

        number[0] = 0;
        number[1] = 1;
        correct = "10000000000000000";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));

        number[0] = 0x1123456789ABCDEFL;
        number[1] = 0x1;
        correct = "11123456789ABCDEF";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));

        number[0] = 0x3456789ABCDEF123L;
        number[1] = 0x23456789ABCDEF12L;
        number[2] = 0x123456789ABCDEF1L;
        correct = "123456789ABCDEF123456789ABCDEF123456789ABCDEF123";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));

        number[0] = 0xFFFFFFFFFFFFFFFFL;
        number[1] = 0xFFFFFFFFFFFFFFFFL;
        number[2] = 0xFFFFFFFFFFFFFFFFL;
        correct = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
        Assert.assertEquals(correct, UintCore.uintToHexString(number, 3));
    }

    @Test
    public void testUint64ToDecString() {
        long[] number = new long[]{ 0, 0, 0 };
        String correct = "0";
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 3));
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 1));
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 0));
        Assert.assertEquals(correct, UintCore.uintToDecimalString(null, 0));

        number[0] = 1;
        correct = "1";
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 3));
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 1));

        number[0] = 9;
        correct = "9";
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 3));

        number[0] = 10;
        correct = "10";
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 3));

        number[0] = 123;
        correct = "123";
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 3));

        number[0] = 987654321;
        correct = "987654321";
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 3));

        number[0] = 0;
        number[1] = 1;
        correct = "18446744073709551616";
        Assert.assertEquals(correct, UintCore.uintToDecimalString(number, 3));
    }

    @Test
    public void testPolyToHexString() {
        long[] number = { 0, 0, 0, 0 };
        String correct = "0";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 0, 1));
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 4, 0));
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 1, 1));
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 4, 1));
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 2, 2));
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 1, 4));

        number[0] = 1;
        correct = "1";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 4, 1));
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 2, 2));
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 1, 4));

        number[0] = 0;
        number[1] = 1;
        correct = "1x^1";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 4, 1));
        correct = "10000000000000000";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 2, 2));
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 1, 4));

        number[0] = 1;
        number[1] = 0;
        number[2] = 0;
        number[3] = 1;
        correct = "1x^3 + 1";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 4, 1));
        correct = "10000000000000000x^1 + 1";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 2, 2));
        correct = "1000000000000000000000000000000000000000000000001";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 1, 4));

        number[0] = 0xF00000000000000FL;
        number[1] = 0xF0F0F0F0F0F0F0F0L;
        number[2] = 0;
        number[3] = 0;
        correct = "F0F0F0F0F0F0F0F0x^1 + F00000000000000F";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 4, 1));

        number[2] = 0xF0FF0F0FF0F0FF0FL;
        number[3] = 0xBABABABABABABABAL;
        correct = "BABABABABABABABAF0FF0F0FF0F0FF0Fx^1 + F0F0F0F0F0F0F0F0F00000000000000F";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 2, 2));
        correct = "BABABABABABABABAx^3 + F0FF0F0FF0F0FF0Fx^2 + F0F0F0F0F0F0F0F0x^1 + F00000000000000F";
        Assert.assertEquals(correct, PolyCore.polyToHexString(number, 4, 1));
    }
}