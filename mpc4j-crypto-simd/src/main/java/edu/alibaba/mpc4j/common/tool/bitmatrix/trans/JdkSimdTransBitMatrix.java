package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.SimdTransBitMatrixFactory.SimdTransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;

/**
 * JDK SIMD transpose bit matrix
 *
 * @author Weiran Liu
 * @date 2024/3/9
 */
class JdkSimdTransBitMatrix extends AbstractDirectSimdTransBitMatrix {

    JdkSimdTransBitMatrix(int rows, int columns) {
        super(SimdTransBitMatrixType.JDK, TransBitMatrixType.JDK, rows, columns);
    }

    private JdkSimdTransBitMatrix(TransBitMatrix transBitMatrix) {
        super(SimdTransBitMatrixType.JDK, transBitMatrix);
    }

    @Override
    public SimdTransBitMatrix transpose() {
        TransBitMatrix transposed = transBitMatrix.transpose();
        return new JdkSimdTransBitMatrix(transposed);
    }
}
