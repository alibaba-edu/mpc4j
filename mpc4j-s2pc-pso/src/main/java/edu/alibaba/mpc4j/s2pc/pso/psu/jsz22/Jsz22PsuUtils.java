package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * JSZ22-PSU协议工具类。
 *
 * @author Weiran Liu
 * @date 2022/03/14
 */
public class Jsz22PsuUtils {
    /**
     * 私有构造函数。
     */
    private Jsz22PsuUtils() {
        // empty
    }

    /**
     * 计算PEQT协议对比字节长度σ + 2 * log_2(binNum)，转换为字节长度。
     *
     * @param binNum 桶数量（β）。
     * @return PEQT协议对比长度。
     */
    static int getOprfByteLength(int binNum) {
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(2 * (int) (DoubleUtils.log2(binNum)));
    }

    /**
     * 计算PEQT协议对比字节长度σ + log_2(maxBinSize^2 * binNum)，转换为字节长度。
     *
     * @param binNum     桶数量（β）。
     * @param maxBinSize 最大桶大小（m）。
     * @return PEQT协议对比长度
     */
    static int getOprfByteLength(int binNum, int maxBinSize) {
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(
            (int) (DoubleUtils.log2(Math.pow(maxBinSize, 2) * binNum))
        );
    }
}
