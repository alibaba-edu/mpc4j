package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.ArrayList;


/**
 * 下三角稀疏方阵，对角线全为1。支持高效的逆矩阵乘法运算。
 *
 * @author Hanwen Feng
 * @date 2022/9/20
 */
public class LowerTriangularSparseBitMatrix extends AbstractSparseBitMatrix {

    /**
     * 构建器。
     *
     * @param colsList 列向量组。
     * @return 下三角稀疏方阵。
     */
    public static LowerTriangularSparseBitMatrix create(ArrayList<SparseBitVector> colsList) {
        if (!isLowerTriangular(colsList)) {
            throw new IllegalArgumentException("The input colsList does not form a triangular matrix!");
        }
        LowerTriangularSparseBitMatrix lowerTriangularSparseBitMatrix = new LowerTriangularSparseBitMatrix();
        lowerTriangularSparseBitMatrix.initFromColList(colsList);
        return lowerTriangularSparseBitMatrix;
    }

    /**
     * 构建器，不检测是否构成下三角稀疏方阵。
     *
     * @param colsList 列向量组。
     * @return 下三角稀疏方阵。
     */
    public static LowerTriangularSparseBitMatrix createUnCheck(ArrayList<SparseBitVector> colsList) {
        LowerTriangularSparseBitMatrix lowerTriangularSparseBitMatrix = new LowerTriangularSparseBitMatrix();
        lowerTriangularSparseBitMatrix.initFromColList(colsList);
        return lowerTriangularSparseBitMatrix;
    }

    /**
     * 计算 v*C^{-1} =x. 等价于求解方程组 xC = v （或者 C^T*x =v）。
     *
     * @param v 输入向量v
     * @return 输出向量 x
     */
    public boolean[] invLmul(boolean[] v) {
        assert v.length == cols;

        boolean[] outputs = new boolean[rows];
        for (int i = cols - 1; i >= 0; --i) {
            outputs[i] = v[i];
            for (int j = 1; j < getCol(i).getSize(); j++) {
                int index = getCol(i).getValue(j);
                outputs[i] ^= outputs[index];
            }
        }
        return outputs;
    }

    /**
     * 计算 v*C^{-1} + t, 结果输出到t。
     *
     * @param v 输入向量v。
     * @param t 输入向量t。
     */
    public void invLmulAddi(final boolean[] v, boolean[] t) {
        assert t.length == rows;
        boolean[] outputs = invLmul(v);
        for (int i = 0; i < t.length; i++) {
            t[i] ^= outputs[i];
        }
    }

    /**
     * 计算 v*C^{-1} =x. 等价于求解方程组 xC = v （或者 C^T*x =v）。v视为GF2K的向量。
     *
     * @param v 输入向量v
     * @return 输出向量 x
     */
    public byte[][] invLextMul(final byte[][] v) {
        assert v.length == cols;

        byte[][] outputs = new byte[getRows()][];

        for (int i = getCols() - 1; i >= 0; --i) {
            outputs[i] = BytesUtils.clone(v[i]);
            for (int j = 1; j < getCol(i).getSize(); j++) {
                int index = getCol(i).getValue(j);
                BytesUtils.xori(outputs[i], outputs[index]);
            }
        }
        return outputs;
    }

    /**
     * 计算 v*C^{-1} + t, 结果输出到t。
     *
     * @param v 输入向量v。
     * @param t 输入向量t。
     */
    public void invLextMulAddi(final byte[][] v, byte[][] t) {
        assert t.length == rows;
        byte[][] outputs = invLextMul(v);
        for (int i = 0; i < t.length; i++) {
            BytesUtils.xori(t[i], outputs[i]);
        }
    }

    /**
     * 稀疏布尔矩阵加法。
     *
     * @param that 另一个稀疏布尔矩阵。
     * @return 加和。
     */
    public SparseBitMatrix add(AbstractSparseBitMatrix that) {
        return SparseBitMatrix.creatFromColsList(addToColsList(that));
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
        return SparseBitMatrix.creatFromColsList(getSubColsList(startColIndex, endColIndex, startRowIndex, endRowIndex));
    }

    /**
     * 获取转置矩阵。
     *
     * @return 转置矩阵。
     */
    public UpperTriangularSparseBitMatrix transpose() {
        return UpperTriangularSparseBitMatrix.createUnCheck(getRowsList());
    }

    /**
     * 私有构造函数。
     */
    private LowerTriangularSparseBitMatrix() {
        // empty
    }

    private static boolean isLowerTriangular(ArrayList<SparseBitVector> colList) {
        for (int colIndex = 0; colIndex < colList.size(); colIndex++) {
            if (colList.get(colIndex).getValue(0) != colIndex) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LowerTriangularSparseBitMatrix)) {
            return false;
        }
        return super.equals(obj);
    }

}
