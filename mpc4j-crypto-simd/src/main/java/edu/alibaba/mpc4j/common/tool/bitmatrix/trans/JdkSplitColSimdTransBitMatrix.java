package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.SimdTransBitMatrixFactory.SimdTransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;

/**
 * JDK SIMD transpose matrix with column splitting.
 *
 * @author Weiran Liu
 * @date 2024/3/9
 */
class JdkSplitColSimdTransBitMatrix extends AbstractDirectSimdTransBitMatrix {

    JdkSplitColSimdTransBitMatrix(int rows, int columns) {
        super(SimdTransBitMatrixType.JDK_SPLIT_COL, TransBitMatrixType.JDK_SPLIT_COL, rows, columns);
    }

    private JdkSplitColSimdTransBitMatrix(TransBitMatrix transBitMatrix) {
        super(SimdTransBitMatrixType.JDK_SPLIT_COL, transBitMatrix);
    }

    @Override
    public SimdTransBitMatrix transpose() {
        TransBitMatrix transposed = transBitMatrix.transpose();
        return new JdkSplitColSimdTransBitMatrix(transposed);
    }
}
