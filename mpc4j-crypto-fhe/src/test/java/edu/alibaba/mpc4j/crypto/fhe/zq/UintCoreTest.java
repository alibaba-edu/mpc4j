package edu.alibaba.mpc4j.crypto.fhe.zq;

import org.junit.Assert;
import org.junit.Test;

/**
 * Uint Core unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/uintcore.cpp
 *
 * @author Anony_Trent
 * @date 2023/8/4
 */
public class UintCoreTest {

    @Test
    public void testSetZeroUint() {
        long[] ptr = new long[1];
        ptr[0] = 0x1234567812345678L;
        UintCore.setZeroUint(1, ptr);
        Assert.assertEquals(0, ptr[0]);

        ptr = new long[2];
        ptr[0] = 0x1234567812345678L;
        ptr[1] = 0x1234567812345678L;
        UintCore.setZeroUint(2, ptr);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
    }

    @Test
    public void testSetUint() {
        long[] ptr = new long[1];
        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(1 , 1, ptr);
        Assert.assertEquals(1, ptr[0]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(0x1234567812345678L, 1, ptr);
        Assert.assertEquals(0x1234567812345678L, ptr[0]);

        ptr = new long[2];
        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(1 , 2, ptr);
        Assert.assertEquals(1, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(0x1234567812345678L, 2, ptr);
        Assert.assertEquals(0x1234567812345678L, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
    }

    @Test
    public void testSetUint2() {
        long[] ptr1 = new long[1];
        ptr1[0] = 0x1234567887654321L;
        long[] ptr2 = new long[1];
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(ptr1 , 1, ptr2);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);

        ptr1[0] = 0x1231231231231231L;
        UintCore.setUint(ptr1 , 1, ptr1);
        Assert.assertEquals(0x1231231231231231L, ptr1[0]);

        ptr1 = new long[2];
        ptr2 = new long[2];
        ptr1[0] = 0x1234567887654321L;
        ptr1[1] = 0x8765432112345678L;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(ptr1, 2, ptr2);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);
        Assert.assertEquals(0x8765432112345678L, ptr2[1]);

        ptr1[0] = 0x1231231231231321L;
        ptr1[1] = 0x3213213213213211L;
        UintCore.setUint(ptr1, 2, ptr2);
        Assert.assertEquals(0x1231231231231321L, ptr2[0]);
        Assert.assertEquals(0x3213213213213211L, ptr2[1]);
    }

    @Test
    public void testSetUint3() {
        long[] ptr1 = new long[1];
        ptr1[0] = 0x1234567887654321L;
        long[] ptr2 = new long[1];
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(ptr1, 1, ptr2, 1);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);

        ptr1[0] = 0x1231231231231231L;
        UintCore.setUint(ptr1, 1, ptr2, 1);
        Assert.assertEquals(0x1231231231231231L, ptr2[0]);

        ptr1 = new long[2];
        ptr2 = new long[2];
        ptr1[0] = 0x1234567887654321L;
        ptr1[1] = 0x8765432112345678L;
        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(ptr1, 1, ptr2,  2);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
        UintCore.setUint(ptr1, 2, ptr2,  2);
        Assert.assertEquals(0x1234567887654321L, ptr2[0]);
        Assert.assertEquals(0x8765432112345678L, ptr2[1]);

        ptr1[0] = 0x1231231231231321L;
        ptr1[1] = 0x3213213213213211L;
        UintCore.setUint(ptr1, 2, ptr2,  2);
        Assert.assertEquals(0x1231231231231321L, ptr2[0]);
        Assert.assertEquals(0x3213213213213211L, ptr2[1]);

        UintCore.setUint(ptr1, 1, ptr2,  2);
        Assert.assertEquals(0x1231231231231321L, ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
    }

    @Test
    public void testIsZeroUint() {
        long[] ptr = new long[1];
        ptr[0] = 1;
        Assert.assertFalse(UintCore.isZeroUint(ptr, 1));
        ptr[0] = 0;
        Assert.assertTrue(UintCore.isZeroUint(ptr, 1));

        ptr = new long[2];
        ptr[0] = 0x8000000000000000L;
        ptr[1] = 0x8000000000000000L;
        Assert.assertFalse(UintCore.isZeroUint(ptr, 2));
        ptr[0] = 0;
        Assert.assertFalse(UintCore.isZeroUint(ptr, 2));
        ptr[0] = 0x8000000000000000L;
        ptr[1] = 0;
        Assert.assertFalse(UintCore.isZeroUint(ptr, 2));
        ptr[0] = 0;
        Assert.assertTrue(UintCore.isZeroUint(ptr, 2));
    }

    @Test
    public void testIsEqualUint() {
        long[] ptr = new long[1];
        ptr[0] = 1;
        Assert.assertTrue(UintCore.isEqualUint(ptr, 1, 1));
        Assert.assertFalse(UintCore.isEqualUint(ptr, 1, 0));
        Assert.assertFalse(UintCore.isEqualUint(ptr, 1, 2));

        ptr = new long[2];
        ptr[0] = 1;
        ptr[1] = 1;
        Assert.assertFalse(UintCore.isEqualUint(ptr, 2, 1));
        ptr[0] = 1;
        ptr[1] = 0;
        Assert.assertTrue(UintCore.isEqualUint(ptr, 2, 1));
        ptr[0] = 0x1234567887654321L;
        ptr[1] = 0;
        Assert.assertTrue(UintCore.isEqualUint(ptr, 2, 0x1234567887654321L));
        Assert.assertFalse(UintCore.isEqualUint(ptr, 2, 0x2234567887654321L));
    }

    @Test
    public void testIsBitSetUint() {
        long[] ptr = new long[2];
        for (int i = 0; i < 128; i++) {
            Assert.assertFalse(UintCore.isBitSetUint(ptr, 2, i));
        }
        ptr[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr[1] = 0xFFFFFFFFFFFFFFFFL;
        for (int i = 0; i < 128; i++) {
            Assert.assertTrue(UintCore.isBitSetUint(ptr, 2, i));
        }

        ptr[0] = 0x0000000000000001L;
        ptr[1] = 0x8000000000000000L;
        for (int i = 0; i < 128; i++) {
            if (i == 0 || i == 127) {
                Assert.assertTrue(UintCore.isBitSetUint(ptr,2, i));
            } else {
                Assert.assertFalse(UintCore.isBitSetUint(ptr, 2, i));
            }
        }
    }

    @Test
    public void testSetBitUint() {
        long[] ptr = new long[2];
        UintCore.setBitUint(ptr, 2, 0);
        Assert.assertEquals(1, ptr[0]);
        Assert.assertEquals(0, ptr[1]);

        UintCore.setBitUint(ptr, 2, 127);
        Assert.assertEquals(1, ptr[0]);
        Assert.assertEquals(0x8000000000000000L, ptr[1]);

        UintCore.setBitUint(ptr, 2, 63);
        Assert.assertEquals(0x8000000000000001L, ptr[0]);
        Assert.assertEquals(0x8000000000000000L, ptr[1]);

        UintCore.setBitUint(ptr, 2, 64);
        Assert.assertEquals(0x8000000000000001L, ptr[0]);
        Assert.assertEquals(0x8000000000000001L, ptr[1]);

        UintCore.setBitUint(ptr, 2, 3);
        Assert.assertEquals(0x8000000000000009L, ptr[0]);
        Assert.assertEquals(0x8000000000000001L, ptr[1]);
    }

    @Test
    public void testGetSignificantBitCountUint() {
        long[] values = new long[]{0, 0};
        Assert.assertEquals(0, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{1, 0};
        Assert.assertEquals(1, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{2, 0};
        Assert.assertEquals(2, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{3, 0};
        Assert.assertEquals(2, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{29, 0};
        Assert.assertEquals(5, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{4, 0};
        Assert.assertEquals(3, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0xFFFFFFFFFFFFFFFFL, 0};
        Assert.assertEquals(64, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0, 1};
        Assert.assertEquals(65, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0xFFFFFFFFFFFFFFFFL, 1};
        Assert.assertEquals(65, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0xFFFFFFFFFFFFFFFFL, 0x7000000000000000L};
        Assert.assertEquals(127, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L};
        Assert.assertEquals(128, UintCore.getSignificantBitCountUint(values, 2));

        values = new long[]{0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL};
        Assert.assertEquals(128, UintCore.getSignificantBitCountUint(values, 2));
    }

    @Test
    public void testGetSignificantUint64CountUint() {
        long[] ptr = new long[2];
        Assert.assertEquals(0, UintCore.getSignificantUint64CountUint(ptr, 2));

        ptr = new long[] {1, 0};
        Assert.assertEquals(1, UintCore.getSignificantUint64CountUint(ptr, 2));

        ptr = new long[] {2, 0};
        Assert.assertEquals(1, UintCore.getSignificantUint64CountUint(ptr, 2));

        ptr = new long[] {0xFFFFFFFFFFFFFFFFL, 0};
        Assert.assertEquals(1, UintCore.getSignificantUint64CountUint(ptr, 2));

        ptr = new long[] {0, 1};
        Assert.assertEquals(2, UintCore.getSignificantUint64CountUint(ptr, 2));

        ptr = new long[] {0xFFFFFFFFFFFFFFFFL, 1};
        Assert.assertEquals(2, UintCore.getSignificantUint64CountUint(ptr, 2));

        ptr = new long[] {0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L};
        Assert.assertEquals(2, UintCore.getSignificantUint64CountUint(ptr, 2));

        ptr = new long[] {0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL};
        Assert.assertEquals(2, UintCore.getSignificantUint64CountUint(ptr, 2));
    }

    @Test
    public void testGetNonzeroUint64CountUint() {
        long[] ptr = new long[2];
        Assert.assertEquals(0, UintCore.getNonZeroUint64CountUint(ptr, 2));

        ptr = new long[] {1, 0};
        Assert.assertEquals(1, UintCore.getNonZeroUint64CountUint(ptr, 2));

        ptr = new long[] {2, 0};
        Assert.assertEquals(1, UintCore.getNonZeroUint64CountUint(ptr, 2));

        ptr = new long[] {0xFFFFFFFFFFFFFFFFL, 0};
        Assert.assertEquals(1, UintCore.getNonZeroUint64CountUint(ptr, 2));

        ptr = new long[] {0, 1};
        Assert.assertEquals(1, UintCore.getNonZeroUint64CountUint(ptr, 2));

        ptr = new long[] {0xFFFFFFFFFFFFFFFFL, 1};
        Assert.assertEquals(2, UintCore.getNonZeroUint64CountUint(ptr, 2));

        ptr = new long[] {0xFFFFFFFFFFFFFFFFL, 0x8000000000000000L};
        Assert.assertEquals(2, UintCore.getNonZeroUint64CountUint(ptr, 2));

        ptr = new long[] {0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL};
        Assert.assertEquals(2, UintCore.getNonZeroUint64CountUint(ptr, 2));
    }

    @Test
    public void testCompareUint() {
        long[] ptr1 = new long[2];
        long[] ptr2 = new long[2];
        Assert.assertEquals(0, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 0x1234567887654321L;
        ptr1[1] = 0x8765432112345678L;
        ptr2[0] = 0x1234567887654321L;
        ptr2[1] = 0x8765432112345678L;
        Assert.assertEquals(0, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 1;
        ptr1[1] = 0;
        ptr2[0] = 2;
        ptr2[1] = 0;
        Assert.assertEquals(-1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1 = new long[] {1, 0xFFFFFFFFFFFFFFFFL};
        ptr2 = new long[] {2, 0xFFFFFFFFFFFFFFFFL};
        Assert.assertEquals(-1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 2;
        ptr1[1] = 0;
        ptr2[0] = 1;
        ptr2[1] = 0;
        Assert.assertEquals(1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1 = new long[] {2, 0xFFFFFFFFFFFFFFFFL};
        ptr2 = new long[] {1, 0xFFFFFFFFFFFFFFFFL};
        Assert.assertEquals(1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));

        ptr1[0] = 0xFFFFFFFFFFFFFFFFL;
        ptr1[1] = 0x0000000000000003L;
        ptr2[0] = 0x0000000000000000;
        ptr2[1] = 0x0000000000000002L;
        Assert.assertEquals(1, UintCore.compareUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isEqualUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanUint(ptr1, ptr2, 2));
        Assert.assertTrue(UintCore.isGreaterThanOrEqualUint(ptr1, ptr2, 2));
        Assert.assertFalse(UintCore.isLessThanOrEqualUint(ptr1, ptr2, 2));
    }

    @Test
    public void testGetPowerOfTwo() {
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(0));
        Assert.assertEquals(0, UintCore.getPowerOfTwo(1));
        Assert.assertEquals(1, UintCore.getPowerOfTwo(2));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(3));
        Assert.assertEquals(2, UintCore.getPowerOfTwo(4));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(5));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(6));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(7));
        Assert.assertEquals(3, UintCore.getPowerOfTwo(8));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(15));
        Assert.assertEquals(4, UintCore.getPowerOfTwo(16));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(17));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(255));
        Assert.assertEquals(8, UintCore.getPowerOfTwo(256));
        Assert.assertEquals(-1, UintCore.getPowerOfTwo(257));
        Assert.assertEquals(10, UintCore.getPowerOfTwo(1 << 10));
        Assert.assertEquals(30, UintCore.getPowerOfTwo(1 << 30));
        Assert.assertEquals(32, UintCore.getPowerOfTwo(1L << 32));
        Assert.assertEquals(62, UintCore.getPowerOfTwo(1L << 62));
        Assert.assertEquals(63, UintCore.getPowerOfTwo(1L << 63));
    }

    @Test
    public void testDuplicateUintIfNeeded() {
        long[] ptr = new long[2];
        ptr[0] = 0xF0F0F0F0F0L;
        ptr[1] = 0xABABABABABL;
        long[] ptr2 = UintCore.duplicateUintIfNeeded(ptr, 0, 0, false);
        // No forcing and sizes are same (although zero) so just alias
        Assert.assertArrayEquals(ptr2, ptr);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 0, 0, true);
        // Forcing and size is zero so return size is zero
        Assert.assertEquals(ptr2.length, 0);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 1, 0, false);
        Assert.assertArrayEquals(ptr2, ptr);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 1, 0, true);
        Assert.assertEquals(ptr2.length, 0);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 1, 1, false);
        Assert.assertArrayEquals(ptr2, ptr);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 1, 1, true);
        Assert.assertNotEquals(ptr2, ptr);
        Assert.assertEquals(ptr[0], ptr2[0]);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 2, 2, true);
        Assert.assertNotEquals(ptr2, ptr);
        Assert.assertEquals(ptr[0], ptr2[0]);
        Assert.assertEquals(ptr[1], ptr2[1]);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 2, 2, false);
        Assert.assertEquals(ptr2, ptr);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 2, 1, false);
        Assert.assertEquals(ptr2, ptr);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 1, 2, false);
        Assert.assertNotEquals(ptr2, ptr);
        Assert.assertEquals(ptr[0], ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);

        ptr2 = UintCore.duplicateUintIfNeeded(ptr, 1, 2, true);
        Assert.assertNotEquals(ptr2, ptr);
        Assert.assertEquals(ptr[0], ptr2[0]);
        Assert.assertEquals(0, ptr2[1]);
    }
}