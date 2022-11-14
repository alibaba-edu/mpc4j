package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import java.util.ArrayList;

/**
 * 稀疏布尔矩阵类。
 *
 * @author Hanwen Feng
 * @date 2022/09/20
 */
public class SparseBitMatrix extends AbstractSparseBitMatrix {
    /**
     * 私有构造函数。
     */
    private SparseBitMatrix() {
        // empty
    }

    /**
     * 构建器。
     *
     * @param colsList 列向量组。
     * @return 稀疏布尔矩阵。
     */
    public static SparseBitMatrix creatFromColsList(ArrayList<SparseBitVector> colsList) {
        SparseBitMatrix sparseBitMatrix = new SparseBitMatrix();
        sparseBitMatrix.initFromColList(colsList);
        return sparseBitMatrix;
    }

    /**
     * 构建器，按照循环矩阵初始化。
     *
     * @param rows      行数。
     * @param cols      列数。
     * @param initArray 初始向量。
     * @return 稀疏布尔矩阵。
     */
    public static SparseBitMatrix createCyclicMatrix(int rows, int cols, int[] initArray) {
        SparseBitMatrix sparseBitMatrix = new SparseBitMatrix();
        sparseBitMatrix.initAsCyclicMatrix(rows, cols, initArray);
        return sparseBitMatrix;
    }

    /**
     * 稀疏布尔矩阵加法。
     *
     * @param that 另一个稀疏布尔矩阵。
     * @return 加和。
     */
    public SparseBitMatrix add(AbstractSparseBitMatrix that) {
        return creatFromColsList(addToColsList(that));
    }

    /**
     * 截取子矩阵。
     *
     * @param startColIndex 开始截取的列位置。
     * @param endColIndex   结束截取的列位置。
     * @param startRowIndex 开始截取的行位置。
     * @param endRowIndex   结束截取的行位置。
     * @return 子矩阵。
     */
    public SparseBitMatrix getSubMatrix(int startColIndex, int endColIndex, int startRowIndex, int endRowIndex) {
        return creatFromColsList(getSubColsList(startColIndex, endColIndex, startRowIndex, endRowIndex));
    }

    /**
     * 获取转置矩阵。
     *
     * @return 转置矩阵。
     */
    public SparseBitMatrix transpose() {
        return creatFromColsList(getRowsList());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SparseBitMatrix)) {
            return false;
        }
        return super.equals(obj);
    }
}
