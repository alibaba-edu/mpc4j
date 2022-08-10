package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;

/**
 * 转置布尔矩阵接口。
 *
 * @author Weiran Liu
 * @date 2019/10/17
 */
public interface TransBitMatrix {
    /**
     * 得到(x, y)坐标对应的布尔值。
     *
     * @param x 行坐标。
     * @param y 列坐标。
     * @return (x, y)坐标对应的布尔值。
     */
    boolean get(int x, int y);

    /**
     * 得到第{@code y}列。
     *
     * @param y 列索引值。
     * @return 第{@code y}列。
     */
    byte[] getColumn(int y);

    /**
     * 将第{@code y}列设置为{@code byteArray}。
     *
     * @param y         列索引值。
     * @param byteArray 列值。
     */
    void setColumn(int y, byte[] byteArray);

    /**
     * 返回行数量。
     *
     * @return 行数量。
     */
    int getRows();

    /**
     * 返回列数量。
     *
     * @return 列数量。
     */
    int getColumns();

    /**
     * 矩阵转置。
     *
     * @return 转置结果。
     */
    TransBitMatrix transpose();

    /**
     * 返回类型。
     *
     * @return 类型。
     */
    TransBitMatrixType getTransBitMatrixType();
}
