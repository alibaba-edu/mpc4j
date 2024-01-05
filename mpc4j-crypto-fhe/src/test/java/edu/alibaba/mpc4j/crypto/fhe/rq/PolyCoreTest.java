package edu.alibaba.mpc4j.crypto.fhe.rq;

import org.junit.Assert;
import org.junit.Test;

/**
 * Polynomial core unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/polycore.cpp
 * </p>
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/29
 */
public class PolyCoreTest {

    @Test
    public void testSetZeroPoly() {
        long[] ptr = PolyCore.allocateZeroPoly(1, 1);
        ptr[0] = 0x1234567812345678L;
        PolyCore.setZeroPoly(1, 1, ptr);
        Assert.assertEquals(0, ptr[0]);

        ptr = PolyCore.allocateZeroPoly(2, 3);
        for (int i = 0; i < 6; i++) {
            ptr[i] = 0x1234567812345678L;
        }

        PolyCore.setZeroPoly(2, 3, ptr);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(0, ptr[i]);
        }
    }

    @Test
    public void testAllocateZeroPoly() {
        long[] ptr = PolyCore.allocateZeroPoly(1, 1);
        Assert.assertEquals(0, ptr[0]);

        ptr = PolyCore.allocateZeroPoly(2, 3);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(0, ptr[i]);
        }
    }

    @Test
    public void testSetZeroPolyArray() {
        long[] ptr = PolyCore.allocateZeroPolyArray(1, 1, 1);
        ptr[0] = 0x1234567812345678L;
        PolyCore.setZeroPolyArray(1, 1, 1, ptr);
        Assert.assertEquals(0, ptr[0]);

        ptr = PolyCore.allocateZeroPolyArray(2, 3, 4);
        for (int i = 0; i < 24; i++) {
            ptr[i] = 0x1234567812345678L;
        }
        PolyCore.setZeroPolyArray(2, 3, 4, ptr);
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(0, ptr[i]);
        }
    }

    @Test
    public void testAllocateZeroPolyArray() {
        long[] ptr = PolyCore.allocateZeroPolyArray(1, 1, 1);
        Assert.assertEquals(0, ptr[0]);

        ptr = PolyCore.allocateZeroPolyArray(2, 1, 1);
        Assert.assertEquals(0, ptr[0]);
        Assert.assertEquals(0, ptr[1]);
    }

    @Test
    public void testSetPoly() {
        long[] ptr1 = PolyCore.allocateZeroPoly(2, 3);
        long[] ptr2 = PolyCore.allocateZeroPoly(2, 3);
        for (int i = 0; i < 6; i++) {
            ptr1[i] = (i + 1L);
        }
        PolyCore.setPoly(ptr1, 2, 3, ptr2);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(ptr2[i], i + 1L);
        }

        PolyCore.setPoly(ptr1, 2, 3, ptr1);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(ptr2[i], i + 1L);
        }
    }

    @Test
    public void testSetPolyArray() {
        long[] ptr1 = PolyCore.allocateZeroPolyArray(1, 2, 3);
        long[] ptr2 = PolyCore.allocateZeroPolyArray(1, 2, 3);
        for (int i = 0; i < 6; i++) {
            ptr1[i] = (i + 1L);
        }
        PolyCore.setPolyArray(ptr1, 1, 2, 3, ptr2);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(ptr2[i], i + 1L);
        }
    }
}
