package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 布尔方阵接口。
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
public interface SquareDenseBitMatrix extends DenseBitMatrix {

    /**
     * 布尔矩阵相加。
     *
     * @param denseBitMatrix 另一个布尔矩阵。
     * @return 相加结果。
     */
    @Override
    SquareDenseBitMatrix add(DenseBitMatrix denseBitMatrix);

    /**
     * 返回布尔方阵的大小。
     *
     * @return 布尔方阵的大小。
     */
    int getSize();

    /**
     * 返回布尔仿真的字节大小。
     *
     * @return 布尔仿真的字节大小。
     */
    int getByteSize();

    /**
     * 计算布尔方阵的转置。
     *
     * @param envType  环境类型。
     * @param parallel 是否并行处理。
     * @return 转置布尔方阵。
     */
    @Override
    SquareDenseBitMatrix transpose(EnvType envType, boolean parallel);

    /**
     * 计算布尔方阵的逆方阵。
     *
     * @return 布尔方阵的逆方阵。
     * @throws ArithmeticException 如果布尔方阵不可逆。
     */
    SquareDenseBitMatrix inverse() throws ArithmeticException;
}
