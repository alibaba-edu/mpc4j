package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

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

    @Test
    public void testSeedSecureRandom() {
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        byte[] seedSecureRandomBytes0 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        byte[] seedSecureRandomBytes1 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        // create standard SecureRandom
        SecureRandom stdSecureRandom0 = new SecureRandom();
        stdSecureRandom0.setSeed(seed);
        SecureRandom stdSecureRandom1 = new SecureRandom();
        stdSecureRandom1.setSeed(seed);
        // generate corresponding randomness
        stdSecureRandom0.nextBytes(seedSecureRandomBytes0);
        stdSecureRandom1.nextBytes(seedSecureRandomBytes1);
        Assert.assertFalse(Arrays.equals(seedSecureRandomBytes0, seedSecureRandomBytes1));

        // create seed SecureRandom
        SecureRandom seedSecureRandom0 = CommonUtils.createSeedSecureRandom();
        seedSecureRandom0.setSeed(seed);
        SecureRandom seedSecureRandom1 = CommonUtils.createSeedSecureRandom();
        seedSecureRandom1.setSeed(seed);
        // generate corresponding randomness
        seedSecureRandom0.nextBytes(seedSecureRandomBytes0);
        seedSecureRandom1.nextBytes(seedSecureRandomBytes1);
        Assert.assertArrayEquals(seedSecureRandomBytes0, seedSecureRandomBytes1);

    }
}
