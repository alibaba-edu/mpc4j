package edu.alibaba.mpc4j.common.tool.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * 公共工具类测试。
 *
 * @author Weiran Liu
 * @date 2021/12/05
 */
public class CommonUtilsTest {

    @Test
    public void testGetByteLength() {
        Assert.assertEquals(1, CommonUtils.getByteLength(1));
        Assert.assertEquals(1, CommonUtils.getByteLength(7));
        Assert.assertEquals(1, CommonUtils.getByteLength(8));
        Assert.assertEquals(2, CommonUtils.getByteLength(9));
    }

    @Test
    public void testGetBlockLength() {
        Assert.assertEquals(1, CommonUtils.getBlockLength(1));
        Assert.assertEquals(1, CommonUtils.getBlockLength(127));
        Assert.assertEquals(1, CommonUtils.getBlockLength(128));
        Assert.assertEquals(2, CommonUtils.getBlockLength(129));
    }
}
