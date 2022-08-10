package edu.alibaba.mpc4j.s2pc.pso.psu.krtw19;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * KRWT19-PSU协议工具类。
 * <p>
 * 最大桶大小数据来自：https://github.com/osu-crypto/PSU/blob/master/libPSU/Tools/SimpleIndex.cpp
 * 桶数量数据来自：https://github.com/osu-crypto/PSU/blob/master/libPSU/Tools/SimpleIndex.cpp
 *
 * @author Weiran Liu
 * @date 2022/02/19
 */
class Krtw19PsuUtils {
    /**
     * 私有构造函数。
     */
    private Krtw19PsuUtils() {
        // empty
    }

    /**
     * 桶数量（β）取值查找表
     */
    private static final Map<Integer, Integer> BIN_NUM_MATRIX = new HashMap<>();

    static {
        // n = 2^8, beta / n = 0.043
        BIN_NUM_MATRIX.put(8, 11);
        // n = 2^10, beta / n = 0.055
        BIN_NUM_MATRIX.put(10, 56);
        // n = 2^12, beta / n = 0.05
        BIN_NUM_MATRIX.put(12, 204);
        // n = 2^14, beta / n = 0.053
        BIN_NUM_MATRIX.put(14, 868);
        // n = 2^16, beta / n = 0.058
        BIN_NUM_MATRIX.put(16, 3801);
        // n = 2^18, beta / n = 0.052
        BIN_NUM_MATRIX.put(18, 13631);
        // n = 2^20, beta / n = 0.06
        BIN_NUM_MATRIX.put(20, 62914);
        // n = 2^22, beta / n = 0.051
        BIN_NUM_MATRIX.put(22, 213909);
        // n = 2^24, beta / n = 0.051
        BIN_NUM_MATRIX.put(24, 855638);
    }

    /**
     * 计算桶数量（β）的值，见原始论文表2。
     *
     * @param n 服务端和客户端的最大元素数量。
     * @return 桶数量（β）。
     */
    static int getBinNum(int n) {
        assert n > 0;
        // n的最小值为2^8，2^2为一个单位，所以log(n)和log(n + 1)都要查一遍
        int nLogValue = LongUtils.ceilLog2(Math.max(n, 1 << 8));
        if (BIN_NUM_MATRIX.containsKey(nLogValue)) {
            return BIN_NUM_MATRIX.get(nLogValue);
        } else if (BIN_NUM_MATRIX.containsKey(nLogValue + 1)) {
            return BIN_NUM_MATRIX.get(nLogValue + 1);
        }
        throw new IllegalArgumentException("Max element size = " + n + " exceeds supported size = " + (1 << 22));
    }

    /**
     * 返回最大桶数量（β）。
     */
    static int MAX_BIN_NUM = BIN_NUM_MATRIX.values().stream().mapToInt(value -> value).max().orElse(0);

    /**
     * 最大桶大小（m）取值查找表
     */
    private static final Map<Integer, Integer> MAX_BIN_SIZE_MATRIX = new HashMap<>();

    static {
        // n = 2^8, m = 63
        MAX_BIN_SIZE_MATRIX.put(8, 63);
        // n = 2^10, m = 58
        MAX_BIN_SIZE_MATRIX.put(10, 58);
        // n = 2^12, m = 63
        MAX_BIN_SIZE_MATRIX.put(12, 63);
        // n = 2^14, m = 62
        MAX_BIN_SIZE_MATRIX.put(14, 62);
        // n = 2^16, m = 60
        MAX_BIN_SIZE_MATRIX.put(16, 60);
        // n = 2^18, m = 65
        MAX_BIN_SIZE_MATRIX.put(18, 65);
        // n = 2^20, m = 61
        MAX_BIN_SIZE_MATRIX.put(20, 61);
        // n = 2^22, m = 68
        MAX_BIN_SIZE_MATRIX.put(22, 68);
        // n = 2^24, m = 69
        MAX_BIN_SIZE_MATRIX.put(24, 69);
    }

    /**
     * 计算最大桶大小（m）的值，见原始论文表2。
     *
     * @param n 服务端和客户端的最大元素数量。
     * @return 最大桶大小（m）。
     */
    static int getMaxBinSize(int n) {
        assert n > 0;
        // n的最小值为2^8，2^2为一个单位，所以log(n)和log(n + 1)都要查一遍
        int nLogValue = LongUtils.ceilLog2(Math.max(n, 1 << 8));
        if (MAX_BIN_SIZE_MATRIX.containsKey(nLogValue)) {
            return MAX_BIN_SIZE_MATRIX.get(nLogValue);
        } else if (MAX_BIN_SIZE_MATRIX.containsKey(nLogValue + 1)) {
            return MAX_BIN_SIZE_MATRIX.get(nLogValue + 1);
        }
        throw new IllegalArgumentException("Max element size = " + n + " exceeds supported size = " + (1 << 22));
    }

    /**
     * 计算有限域比特长度σ = λ + log(β * (m + 1)^2)，向上取整为Byte.SIZE的整数倍。
     *
     * @param binNum     桶数量（β）。
     * @param maxBinSize 最大桶大小（m）。
     * @return 有限域比特长度σ。
     */
    static int getFiniteFieldBitLength(int binNum, int maxBinSize) {
        assert binNum > 0;
        assert maxBinSize > 0;
        return CommonUtils.getByteLength(CommonConstants.STATS_BIT_LENGTH
            + (int) Math.ceil(Math.log(binNum * (maxBinSize + 1) * (maxBinSize + 1)) / Math.log(2.0))
        ) * Byte.SIZE;
    }

    /**
     * 计算PEQT协议对比字节长度σ + log_2(maxBinSize^2 * binNum)，转换为字节长度。
     *
     * @param binNum     桶数量（β）。
     * @param maxBinSize 最大桶大小（m）。
     * @return PEQT协议对比长度
     */
    static int getPeqtByteLength(int binNum, int maxBinSize) {
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(
            (int) (DoubleUtils.log2(Math.pow(maxBinSize, 2) * binNum))
        );
    }
}
