package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.SimdTransBitMatrixFactory.SimdTransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;

/**
 * JDK SIMD transpose matrix with row splitting.
 *
 * @author Weiran Liu
 * @date 2024/3/9
 */
class JdkSplitRowSimdTransBitMatrix extends AbstractDirectSimdTransBitMatrix {

    JdkSplitRowSimdTransBitMatrix(int rows, int columns) {
        super(SimdTransBitMatrixType.JDK_SPLIT_ROW, TransBitMatrixType.JDK_SPLIT_ROW, rows, columns);
    }

    private JdkSplitRowSimdTransBitMatrix(TransBitMatrix transBitMatrix) {
        super(SimdTransBitMatrixType.JDK_SPLIT_ROW, transBitMatrix);
    }

    @Override
    public SimdTransBitMatrix transpose() {
        TransBitMatrix transposed = transBitMatrix.transpose();
        return new JdkSplitRowSimdTransBitMatrix(transposed);
    }
}
