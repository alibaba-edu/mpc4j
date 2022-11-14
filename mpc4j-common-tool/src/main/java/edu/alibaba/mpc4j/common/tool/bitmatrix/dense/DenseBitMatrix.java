package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 密集布尔矩阵接口。
 *
 * @author Weiran Liu
 * @date 2022/8/1
 */
public interface DenseBitMatrix {
    /**
     * 布尔矩阵相加。
     *
     * @param that 另一个布尔矩阵。
     * @return 相加结果。
     */
    DenseBitMatrix add(DenseBitMatrix that);

    /**
     * 布尔矩阵相加，结果更新至当前布尔矩阵中。
     *
     * @param that 另一个布尔矩阵。
     */
    void addi(DenseBitMatrix that);

    /**
     * 当前布尔矩阵右乘给定布尔矩阵。
     *
     * @param that 另一个布尔矩阵。
     * @return 右乘结果。
     */
    DenseBitMatrix multiply(DenseBitMatrix that);

    /**
     * 当前布尔矩阵左乘v，即计算v·M。
     *
     * @param v 向量v。
     * @return v·M。
     */
    byte[] lmul(final byte[] v);

    /**
     * 当前布尔矩阵左乘布尔向量v，即计算v·M。
     *
     * @param v 向量v。
     * @return v·M。
     */
    boolean[] lmul(final boolean[] v);

    /**
     * 当前布尔矩阵左乘扩域GF2E的向量v，即计算v·M
     * @param v 扩域GF2E上的向量v。
     * @return v·M。
     */
    byte[][] lExtMul(final byte[][] v);

    /**
     * 计算v·M + t，结果更新至t中。
     *
     * @param v 向量v。
     * @param t 向量t。
     */
    void lmulAddi(final byte[] v, byte[] t);

    /**
     * 计算v·M + t，结果更新至t中。
     *
     * @param v 向量v。
     * @param t 向量t。
     */
    void lmulAddi(final boolean[] v, boolean[] t);

    /**
     * 计算v·M + t，结果更新至t中。
     *
     * @param v 扩域GF2E上的向量v。
     * @param t 扩域GF2E上的向量t。
     */
    void lExtMulAddi(final byte[][] v, byte[][] t);

    /**
     * 布尔矩阵转置。
     *
     * @param envType  环境类型。
     * @param parallel 是否并发。
     * @return 转置结果。
     */
    DenseBitMatrix transpose(EnvType envType, boolean parallel);

    /**
     * 返回行数量。
     *
     * @return 行数量。
     */
    int getRows();

    /**
     * 得到第{@code x}列。
     *
     * @param x 行索引值。
     * @return 第{@code x}列。
     */
    byte[] getRow(int x);

    /**
     * 返回列数量。
     *
     * @return 列数量。
     */
    int getColumns();

    /**
     * 得到(x, y)坐标对应的布尔值。
     *
     * @param x 行坐标。
     * @param y 列坐标。
     * @return (x, y)坐标对应的布尔值。
     */
    boolean get(int x, int y);

    /**
     * 返回表示矩阵的字节数组。
     *
     * @return 表示矩阵的字节数组。
     */
    byte[][] toByteArrays();
}
