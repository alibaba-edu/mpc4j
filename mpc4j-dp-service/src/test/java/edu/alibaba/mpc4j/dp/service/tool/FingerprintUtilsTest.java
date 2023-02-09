package edu.alibaba.mpc4j.dp.service.tool;

import org.junit.Assert;
import org.junit.Test;

/**
 * 指纹工具类测试。
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
        // 0个元素放置到1个桶中，一定不发生碰撞，l = 0
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(0, 1));
        // 0个元素放置到2个桶中，一定不发生碰撞
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(0, 2));
        // 0个元素放置到2^20个桶中，一定不发生碰撞
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(0, 1 << 20));
    }

    @Test
    public void testOneM() {
        // 1个元素放置到1个桶中，一定不发生碰撞，l = 1
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(1, 1));
        // 1个元素放置到2个桶中，一定不发生碰撞
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(1, 2));
        // 1个元素放置到2^20个桶中，一定不发生碰撞
        Assert.assertEquals(1, FingerprintUtils.fingerprintBitLength(1, 1 << 20));
    }

    @Test
    public void testOneW() {
        // 2个元素放置到1个桶中，41比特
        Assert.assertEquals(41, FingerprintUtils.fingerprintBitLength(2, 1));
        // 4个元素放置到1个桶中，42比特
        Assert.assertEquals(42, FingerprintUtils.fingerprintBitLength(4, 1));
        // 512个元素放置到1个桶中，512比特
        Assert.assertEquals(512, FingerprintUtils.fingerprintBitLength(1 << 9, 1));
    }
}
