package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.junit.Assert;
import org.junit.Test;

/**
 * Uint Arithmetic Mod unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/uintarithmod.cpp
 *
 * @author Anony_Trent
 * @date 2023/8/9
 */
public class UintArithmeticModTest {

    @Test
    public void testIncrementUintMod() {
        long[] value = new long[2];
        long[] modulus = new long[2];

        modulus[0] = 3;
        UintArithmeticMod.incrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(1, value[0]);
        Assert.assertEquals(0, value[1]);
        UintArithmeticMod.incrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(2, value[0]);
        Assert.assertEquals(0, value[1]);
        UintArithmeticMod.incrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(0, value[0]);
        Assert.assertEquals(0, value[1]);

        value[0] = 0xFFFFFFFFFFFFFFFDL;
        value[1] = 0xFFFFFFFFFFFFFFFFL;
        modulus[0] = 0xFFFFFFFFFFFFFFFFL;
        modulus[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmeticMod.incrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, value[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, value[1]);
        UintArithmeticMod.incrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(0, value[0]);
        Assert.assertEquals(0, value[1]);
        UintArithmeticMod.incrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(1, value[0]);
        Assert.assertEquals(0, value[1]);
    }

    @Test
    public void testDecrementUintMod() {
        long[] value = new long[2];
        long[] modulus = new long[2];

        value[0] = 2;
        modulus[0] = 3;
        UintArithmeticMod.decrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(1, value[0]);
        Assert.assertEquals(0, value[1]);
        UintArithmeticMod.decrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(0, value[0]);
        Assert.assertEquals(0, value[1]);
        UintArithmeticMod.decrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(2, value[0]);
        Assert.assertEquals(0, value[1]);

        value[0] = 1;
        value[1] = 0;
        modulus[0] = 0xFFFFFFFFFFFFFFFFL;
        modulus[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmeticMod.decrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(0, value[0]);
        Assert.assertEquals(0, value[1]);
        UintArithmeticMod.decrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFEL, value[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, value[1]);
        UintArithmeticMod.decrementUintMod(value, modulus, 2, value);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFDL, value[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, value[1]);
    }

    @Test
    public void testNegateUintMod() {
        long[] value = new long[2];
        long[] modulus = new long[2];

        modulus[0] = 3;
        UintArithmeticMod.negateUintMod(value, modulus, 2, value);
        Assert.assertEquals(0, value[0]);
        Assert.assertEquals(0, value[1]);

        value[0] = 1;
        value[1] = 0;
        modulus[0] = 3;
        modulus[1] = 0;
        UintArithmeticMod.negateUintMod(value, modulus, 2, value);
        Assert.assertEquals(2, value[0]);
        Assert.assertEquals(0, value[1]);
        UintArithmeticMod.negateUintMod(value, modulus, 2, value);
        Assert.assertEquals(1, value[0]);
        Assert.assertEquals(0, value[1]);

        value[0] = 2;
        value[1] = 0;
        modulus[0] = 0xFFFFFFFFFFFFFFFFL;
        modulus[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmeticMod.negateUintMod(value, modulus, 2, value);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFDL, value[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, value[1]);
        UintArithmeticMod.negateUintMod(value, modulus, 2, value);
        Assert.assertEquals(2, value[0]);
        Assert.assertEquals(0, value[1]);
    }

    @Test
    public void testDiv2UintMod() {
        long[] value = new long[2];
        long[] modulus = new long[2];

        modulus[0] = 3;
        UintArithmeticMod.div2UintMod(value, modulus, 2, value);
        Assert.assertEquals(0, value[0]);
        Assert.assertEquals(0, value[1]);

        value[0] = 1;
        value[1] = 0;
        modulus[0] = 3;
        modulus[1] = 0;
        UintArithmeticMod.div2UintMod(value, modulus, 2, value);
        Assert.assertEquals(2, value[0]);
        Assert.assertEquals(0, value[1]);

        value[0] = 8;
        value[1] = 0;
        modulus[0] = 17;
        modulus[1] = 0;
        UintArithmeticMod.div2UintMod(value, modulus, 2, value);
        Assert.assertEquals(4, value[0]);
        Assert.assertEquals(0, value[1]);

        value[0] = 5;
        value[1] = 0;
        modulus[0] = 17;
        modulus[1] = 0;
        UintArithmeticMod.div2UintMod(value, modulus, 2, value);
        Assert.assertEquals(11, value[0]);
        Assert.assertEquals(0, value[1]);

        value[0] = 1;
        value[1] = 0;
        modulus[0] = 0xFFFFFFFFFFFFFFFFL;
        modulus[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmeticMod.div2UintMod(value, modulus, 2, value);
        Assert.assertEquals(0, value[0]);
        Assert.assertEquals(0x8000000000000000L, value[1]);

        value[0] = 3;
        value[1] = 0;
        modulus[0] = 0xFFFFFFFFFFFFFFFFL;
        modulus[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmeticMod.div2UintMod(value, modulus, 2, value);
        Assert.assertEquals(1, value[0]);
        Assert.assertEquals(0x8000000000000000L, value[1]);
    }

    @Test
    public void testAddUintMod() {
        long[] value1 = new long[2];
        long[] value2 = new long[2];
        long[] modulus = new long[2];

        modulus[0] = 3;
        UintArithmeticMod.addUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(0, value1[0]);
        Assert.assertEquals(0, value1[1]);

        value1[0] = 1;
        value1[1] = 0;
        value2[0] = 1;
        value2[1] = 0;
        modulus[0] = 3;
        modulus[1] = 0;
        UintArithmeticMod.addUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(2, value1[0]);
        Assert.assertEquals(0, value1[1]);

        value1[0] = 1;
        value1[1] = 0;
        value2[0] = 2;
        value2[1] = 0;
        modulus[0] = 3;
        modulus[1] = 0;
        UintArithmeticMod.addUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(0, value1[0]);
        Assert.assertEquals(0, value1[1]);

        value1[0] = 2;
        value1[1] = 0;
        value2[0] = 2;
        value2[1] = 0;
        modulus[0] = 3;
        modulus[1] = 0;
        UintArithmeticMod.addUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(1, value1[0]);
        Assert.assertEquals(0, value1[1]);

        value1[0] = 0xFFFFFFFFFFFFFFFEL;
        value1[1] = 0xFFFFFFFFFFFFFFFFL;
        value2[0] = 0xFFFFFFFFFFFFFFFEL;
        value2[1] = 0xFFFFFFFFFFFFFFFFL;
        modulus[0] = 0xFFFFFFFFFFFFFFFFL;
        modulus[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmeticMod.addUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFDL, value1[0]);
        Assert.assertEquals(0xFFFFFFFFFFFFFFFFL, value1[1]);
    }

    @Test
    public void testSubUintMod() {
        long[] value1 = new long[2];
        long[] value2 = new long[2];
        long[] modulus = new long[2];

        modulus[0] = 3;
        UintArithmeticMod.subUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(0, value1[0]);
        Assert.assertEquals(0, value1[1]);

        value1[0] = 2;
        value1[1] = 0;
        value2[0] = 1;
        value2[1] = 0;
        modulus[0] = 3;
        modulus[1] = 0;
        UintArithmeticMod.subUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(1, value1[0]);
        Assert.assertEquals(0, value1[1]);

        value1[0] = 1;
        value1[1] = 0;
        value2[0] = 2;
        value2[1] = 0;
        modulus[0] = 3;
        modulus[1] = 0;
        UintArithmeticMod.subUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(2, value1[0]);
        Assert.assertEquals(0, value1[1]);

        value1[0] = 2;
        value1[1] = 0;
        value2[0] = 2;
        value2[1] = 0;
        modulus[0] = 3;
        modulus[1] = 0;
        UintArithmeticMod.subUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(0, value1[0]);
        Assert.assertEquals(0, value1[1]);

        value1[0] = 1;
        value1[1] = 0;
        value2[0] = 0xFFFFFFFFFFFFFFFEL;
        value2[1] = 0xFFFFFFFFFFFFFFFFL;
        modulus[0] = 0xFFFFFFFFFFFFFFFFL;
        modulus[1] = 0xFFFFFFFFFFFFFFFFL;
        UintArithmeticMod.subUintUintMod(value1, value2, modulus, 2, value1);
        Assert.assertEquals(2, value1[0]);
        Assert.assertEquals(0, value1[1]);
    }

    @Test
    public void testTryInvertUintMod() {
        long[] values = new long[2];
        long[] modulus = new long[2];

        modulus[0] = 5;
        Assert.assertFalse(UintArithmeticMod.tryInvertUintMod(values, modulus, 2, values));

        values[0] = 1;
        values[1] = 0;
        modulus[0] = 5;
        modulus[1] = 0;
        Assert.assertTrue(UintArithmeticMod.tryInvertUintMod(values, modulus, 2, values));
        Assert.assertEquals(1, values[0]);
        Assert.assertEquals(0, values[1]);

        values[0] = 2;
        values[1] = 0;
        modulus[0] = 5;
        modulus[1] = 0;
        Assert.assertTrue(UintArithmeticMod.tryInvertUintMod(values, modulus, 2, values));
        Assert.assertEquals(3, values[0]);
        Assert.assertEquals(0, values[1]);

        values[0] = 3;
        values[1] = 0;
        modulus[0] = 5;
        modulus[1] = 0;
        Assert.assertTrue(UintArithmeticMod.tryInvertUintMod(values, modulus, 2, values));
        Assert.assertEquals(2, values[0]);
        Assert.assertEquals(0, values[1]);

        values[0] = 4;
        values[1] = 0;
        modulus[0] = 5;
        modulus[1] = 0;
        Assert.assertTrue(UintArithmeticMod.tryInvertUintMod(values, modulus, 2, values));
        Assert.assertEquals(4, values[0]);
        Assert.assertEquals(0, values[1]);

        values[0] = 2;
        values[1] = 0;
        modulus[0] = 6;
        modulus[1] = 0;
        Assert.assertFalse(UintArithmeticMod.tryInvertUintMod(values, modulus, 2, values));

        values[0] = 3;
        values[1] = 0;
        modulus[0] = 6;
        modulus[1] = 0;
        Assert.assertFalse(UintArithmeticMod.tryInvertUintMod(values, modulus, 2, values));

        values[0] = 331975426;
        values[1] = 0;
        modulus[0] = 1351315121;
        modulus[1] = 0;
        Assert.assertTrue(UintArithmeticMod.tryInvertUintMod(values, modulus, 2, values));
        Assert.assertEquals(1052541512, values[0]);
        Assert.assertEquals(0, values[1]);
    }
}