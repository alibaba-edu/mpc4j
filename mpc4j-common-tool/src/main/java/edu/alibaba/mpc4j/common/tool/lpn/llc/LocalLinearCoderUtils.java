package edu.alibaba.mpc4j.common.tool.lpn.llc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

/**
 * 本地线性编码（Linear Linear Coder）工具类。
 *
 * @author Weiran Liu
 * @date 2022/6/11
 */
class LocalLinearCoderUtils {
    /**
     * 私有构造函数
     */
    private LocalLinearCoderUtils() {
        // empty
    }

    /**
     * 编码矩阵有d个位置为1
     */
    static final int D = 10;
    /**
     * 生成d个随机整数所需要的分组数量
     */
    static final int RANDOM_BLOCK_NUM = (int)Math.ceil((double)D * Integer.BYTES / CommonConstants.BLOCK_BYTE_LENGTH);
    /**
     * 随机分组对应的整数数量
     */
    static final int RANDOM_INT_NUM = RANDOM_BLOCK_NUM * CommonConstants.BLOCK_BYTE_LENGTH / Integer.BYTES;
}
