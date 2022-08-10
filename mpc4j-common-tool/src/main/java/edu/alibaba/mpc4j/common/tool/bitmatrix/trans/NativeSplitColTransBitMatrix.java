package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;

/**
 * 本地列切分转置布尔矩阵。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
class NativeSplitColTransBitMatrix extends AbstractSplitColTransBitMatrix {

    public NativeSplitColTransBitMatrix(int rows, int columns) {
        super(TransBitMatrixType.NATIVE, rows, columns);
    }

    @Override
    public TransBitMatrixType getTransBitMatrixType() {
        return TransBitMatrixType.NATIVE_SPLIT_COL;
    }
}
