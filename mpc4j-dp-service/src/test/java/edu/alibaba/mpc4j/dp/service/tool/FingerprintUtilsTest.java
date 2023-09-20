package edu.alibaba.mpc4j.dp.service.tool;

import org.junit.Assert;
import org.junit.Test;

/**
 * fingerprint utility test.
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class FingerprintUtilsTest {

    @Test(expected = AssertionError.class)
    public void testInvalidNegM() {
        FingerprintUtils.fingerprintBitLength(-1, 1 << 20);
    }

    @Test(expected = AssertionError.class)
    public void testInvalidNegW() {
        FingerprintUtils.fingerprintBitLength(10000, -1);
    }

    @Test(expected = AssertionError.class)
    public void testInvalidZeroW() {
        FingerprintUtils.fingerprintBitLength(10000, 0);
    }

    @Test
    public void testZeroM() {
        // 0 element in 1 bin, l = 0
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(0, 1));
        // 0 element in 2 bins, l = 0
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(0, 2));
        // 0 element in 2^20 bins, l = 0
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(0, 1 << 20));
    }

    @Test
    public void testOneM() {
        // 1 element in 1 bin, l = 1
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(1, 1));
        // 1 element in 2 bins, l = 1
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(1, 2));
        // 1 element in 2^20 bins, l = 1
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(1, 1 << 20));
    }

    @Test
    public void testOneW() {
        // 2 elements in 1 bin, l = 41
        Assert.assertEquals(41, FingerprintUtils.fingerprintBitLength(2, 1));
        // 4 elements in 1 bin, l = 42
        Assert.assertEquals(42, FingerprintUtils.fingerprintBitLength(4, 1));
        // 2^9 elements in 1 bin, l = 2^9
        Assert.assertEquals(512, FingerprintUtils.fingerprintBitLength(1 << 9, 1));
    }
}
