package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * 公共工具类。
 *
 * @author Weiran Liu
 * @date 2021/12/05
 */
public class CommonUtils {
    /**
     * 私有构造函数
     */
    private CommonUtils() {
        // empty
    }

    /**
     * 返回在给定的单位长度下，至少需要多少单位长度才能容纳输入的长度。
     *
     * @param length 输入长度。
     * @param unitLength 单位长度。
     * @return 为容纳输入长度所需的单位长度数量。
     */
    public static int getUnitNum(int length, int unitLength) {
        assert length > 0 : "length must be greater than 0: " + length;
        assert unitLength > 0 : "unit length must be greater than 0: " + unitLength;
        return (length + unitLength - 1) / unitLength;
    }

    /**
     * 根据比特长度计算字节长度。
     *
     * @param bitLength 比特长度。
     * @return 字节长度。
     */
    public static int getByteLength(int bitLength) {
        return CommonUtils.getUnitNum(bitLength, Byte.SIZE);
    }

    /**
     * 根据比特长度计算长整数长度。
     *
     * @param bitLength 比特长度。
     * @return 长整数长度。
     */
    public static int getLongLength(int bitLength) {
        return CommonUtils.getUnitNum(bitLength, Long.SIZE);
    }

    /**
     * 根据比特长度计算分组长度。
     *
     * @param bitLength 比特长度。
     * @return 分组长度。
     */
    public static int getBlockLength(int bitLength) {
        return CommonUtils.getUnitNum(bitLength, CommonConstants.BLOCK_BIT_LENGTH);
    }

    /**
     * 生成随机密钥。
     *
     * @param secureRandom 随机状态。
     * @return 随机密钥。
     */
    public static byte[] generateRandomKey(SecureRandom secureRandom) {
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        return key;
    }

    /**
     * 生成随机密钥数组。
     *
     * @param keyNum 密钥数量。
     * @param secureRandom 随机状态。
     * @return 随机密钥数组。
     */
    public static byte[][] generateRandomKeys(int keyNum, SecureRandom secureRandom) {
        return IntStream.range(0, keyNum)
            .mapToObj(index -> generateRandomKey(secureRandom))
            .toArray(byte[][]::new);
    }

    /**
     * Creates a secureRandom that allows to set seed. Note that if we directly create a new SecureRandom(), we cannot
     * handle the output even though we call setSeed().
     *
     * @return a secureRandom that allows to set seed.
     */
    public static SecureRandom createSeedSecureRandom() {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossible if create an JDK Secure instance with invalid algorithm name.");
        }
    }
}
