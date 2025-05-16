package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

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
     * Returns unit num if we split length into unit length. Here length can be 0 (in which the returned unit num is 0).
     *
     * @param length     length.
     * @param unitLength unit length.
     * @return unit num.
     */
    public static int getUnitNum(int length, int unitLength) {
        assert length >= 0 : "length must be non-negative: " + length;
        assert unitLength > 0 : "unit length must be positive: " + unitLength;
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
     * Creates a secureRandom that allows to set seed.
     * <p></p>
     * Recall that if we directly create a new <code>SecureRandom</code>, <code>setSeed(byte[] seed)</code> cannot make
     * the output deterministic. Instead, here we create and return a SecureRandom that allows to
     * <code>setSeed(byte[] seed)</code>.
     * <p></p>
     * Note that we must call <code>setSeed(byte[] seed)</code> to make the output deterministic. Calling
     * <code>setSeed(long seed)</code> would still return random output.
     *
     * @return a secureRandom that allows to call <code>setSeed(byte[] seed)</code> to make the output deterministic.
     */
    public static SecureRandom createSeedSecureRandom() {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossible if create an JDK Secure instance with invalid algorithm name.");
        }
    }
}
