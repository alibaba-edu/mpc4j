package edu.alibaba.mpc4j.common.tool.coder.random;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 伪随机编码工具类。
 *
 * @author Weiran Liu
 * @date 2021/12/19
 */
public class RandomCoderUtils {
    /**
     * 输入最小值对数，输入最大值对数为Long.SIZE = 64
     */
    private static final int MIN_INPUT_LOG = 8;
    /**
     * 私有构造函数
     */
    private RandomCoderUtils() {
        // empty
    }

    /**
     * 调用次数 -> 编码字节长度映射表
     */
    private static final Map<Integer, Integer> CODEWORD_BYTE_LENGTH_TABLE = new HashMap<>();

    static {
        CODEWORD_BYTE_LENGTH_TABLE.put(8, 52);
        CODEWORD_BYTE_LENGTH_TABLE.put(9, 52);
        CODEWORD_BYTE_LENGTH_TABLE.put(10, 52);
        CODEWORD_BYTE_LENGTH_TABLE.put(11, 53);
        CODEWORD_BYTE_LENGTH_TABLE.put(12, 53);
        CODEWORD_BYTE_LENGTH_TABLE.put(13, 53);
        CODEWORD_BYTE_LENGTH_TABLE.put(14, 53);
        CODEWORD_BYTE_LENGTH_TABLE.put(15, 54);
        CODEWORD_BYTE_LENGTH_TABLE.put(16, 54);
        CODEWORD_BYTE_LENGTH_TABLE.put(17, 54);
        CODEWORD_BYTE_LENGTH_TABLE.put(18, 54);
        CODEWORD_BYTE_LENGTH_TABLE.put(19, 55);
        CODEWORD_BYTE_LENGTH_TABLE.put(20, 55);
        CODEWORD_BYTE_LENGTH_TABLE.put(21, 55);
        CODEWORD_BYTE_LENGTH_TABLE.put(22, 55);
        CODEWORD_BYTE_LENGTH_TABLE.put(23, 56);
        CODEWORD_BYTE_LENGTH_TABLE.put(24, 56);
        CODEWORD_BYTE_LENGTH_TABLE.put(25, 56);
        CODEWORD_BYTE_LENGTH_TABLE.put(26, 56);
        CODEWORD_BYTE_LENGTH_TABLE.put(27, 57);
        CODEWORD_BYTE_LENGTH_TABLE.put(28, 57);
        CODEWORD_BYTE_LENGTH_TABLE.put(29, 57);
        CODEWORD_BYTE_LENGTH_TABLE.put(30, 57);
        CODEWORD_BYTE_LENGTH_TABLE.put(31, 58);
        CODEWORD_BYTE_LENGTH_TABLE.put(32, 58);
        CODEWORD_BYTE_LENGTH_TABLE.put(33, 58);
        CODEWORD_BYTE_LENGTH_TABLE.put(34, 58);
        CODEWORD_BYTE_LENGTH_TABLE.put(35, 58);
        CODEWORD_BYTE_LENGTH_TABLE.put(36, 59);
        CODEWORD_BYTE_LENGTH_TABLE.put(37, 59);
        CODEWORD_BYTE_LENGTH_TABLE.put(38, 59);
        CODEWORD_BYTE_LENGTH_TABLE.put(39, 59);
        CODEWORD_BYTE_LENGTH_TABLE.put(40, 60);
        CODEWORD_BYTE_LENGTH_TABLE.put(41, 60);
        CODEWORD_BYTE_LENGTH_TABLE.put(42, 60);
        CODEWORD_BYTE_LENGTH_TABLE.put(43, 60);
        CODEWORD_BYTE_LENGTH_TABLE.put(44, 61);
        CODEWORD_BYTE_LENGTH_TABLE.put(45, 61);
        CODEWORD_BYTE_LENGTH_TABLE.put(46, 61);
        CODEWORD_BYTE_LENGTH_TABLE.put(47, 61);
        CODEWORD_BYTE_LENGTH_TABLE.put(48, 61);
        CODEWORD_BYTE_LENGTH_TABLE.put(49, 62);
        CODEWORD_BYTE_LENGTH_TABLE.put(50, 62);
        CODEWORD_BYTE_LENGTH_TABLE.put(51, 62);
        CODEWORD_BYTE_LENGTH_TABLE.put(52, 62);
        CODEWORD_BYTE_LENGTH_TABLE.put(53, 63);
        CODEWORD_BYTE_LENGTH_TABLE.put(54, 63);
        CODEWORD_BYTE_LENGTH_TABLE.put(55, 63);
        CODEWORD_BYTE_LENGTH_TABLE.put(56, 63);
        CODEWORD_BYTE_LENGTH_TABLE.put(58, 63);
        CODEWORD_BYTE_LENGTH_TABLE.put(59, 64);
        CODEWORD_BYTE_LENGTH_TABLE.put(60, 64);
        CODEWORD_BYTE_LENGTH_TABLE.put(61, 64);
        CODEWORD_BYTE_LENGTH_TABLE.put(62, 64);
        // 超过Long.SIZE的编码长度为65字节，但恰好超过了4倍分组长度
        CODEWORD_BYTE_LENGTH_TABLE.put(63, 65);
        CODEWORD_BYTE_LENGTH_TABLE.put(64, 65);
    }

    /**
     * 返回伪随机编码的码字字节长度。
     *
     * @param maxN 总调用次数。
     * @return 伪随机编码的码字字节长度。
     */
    public static int getCodewordByteLength(long maxN) {
        assert maxN > 0;
        int ceilLogMaxN = LongUtils.ceilLog2(maxN);
        assert ceilLogMaxN > 0 && ceilLogMaxN <= Long.SIZE
            : "log(maxN) must be in range (0, " + Long.SIZE + ": " + ceilLogMaxN;
        if (ceilLogMaxN < MIN_INPUT_LOG) {
            // we have a minimal input log, so that the codeword byte length has a min value.
            return CODEWORD_BYTE_LENGTH_TABLE.get(MIN_INPUT_LOG);
        } else {
            // We thank Qixian Zhou for pointing out that
            // here we should call get(ceilLogMaxN) instead of get(MAX_INPUT_LOG)
            return CODEWORD_BYTE_LENGTH_TABLE.get(ceilLogMaxN);
        }
    }

    /**
     * 返回伪随机编码最大字节长度。
     *
     * @return 伪随机编码最大字节长度。
     */
    public static int getMaxCodewordByteLength() {
        return 65;
    }

    /**
     * 返回伪随机编码最小字节长度。
     *
     * @return 伪随机编码最小字节长度。
     */
    public static int getMinCodewordByteLength() {
        return 52;
    }

    /**
     * 计算伪随机编码最大调用次数的对数。
     *
     * @param codewordByteLength 码字字节长度。
     * @return 最大调用次数的对数。
     */
    public static int getLogMaxCallTime(int codewordByteLength) {
        int codewordBitLength = codewordByteLength * Byte.SIZE;
        // 码字至少要大于128比特
        assert codewordBitLength >= CommonConstants.BLOCK_BIT_LENGTH;
        double probability = 0.0;
        // Σ_{i = 0}^{d - 1} {C(n, i)}
        for (int i = 0; i < CommonConstants.BLOCK_BIT_LENGTH; i++) {
            double combinatorial = DoubleUtils.estimateCombinatorial(codewordBitLength, i);
            probability += combinatorial;
        }
        // 2^{-n} * Σ_{i = 0}^{d - 1} {C(n, i)}
        probability *= Math.pow(0.5, codewordBitLength);
        // 2^{-n} * Σ_{i = 0}^{d - 1} {C(n, i)} + 2^{-128}
        probability += DoubleUtils.COMP_NEG_PROBABILITY;
        // 对结果去log和负数
        return (int)Math.floor(-1 * DoubleUtils.log2(probability)) - CommonConstants.STATS_BIT_LENGTH;
    }
}
