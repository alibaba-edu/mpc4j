package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.SimdTransBitMatrixFactory.SimdTransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;

/**
 * abstract direct SIMD transpose bit matrix.
 *
 * @author Weiran Liu
 * @date 2024/3/9
 */
abstract class AbstractDirectSimdTransBitMatrix implements SimdTransBitMatrix {
    /**
     * SIMD transpose bit matrix type.
     */
    private final SimdTransBitMatrixType simdTransBitMatrixType;
    /**
     * inner transpose bit matrix
     */
    protected final TransBitMatrix transBitMatrix;

    AbstractDirectSimdTransBitMatrix(SimdTransBitMatrixType simdTransBitMatrixType,
                                     TransBitMatrixType transBitMatrixType,
                                     int rows, int columns) {
        this.simdTransBitMatrixType = simdTransBitMatrixType;
        transBitMatrix = TransBitMatrixFactory.createInstance(transBitMatrixType, rows, columns);
    }

    protected AbstractDirectSimdTransBitMatrix(SimdTransBitMatrixType simdTransBitMatrixType,
                                               TransBitMatrix transBitMatrix) {
        this.simdTransBitMatrixType = simdTransBitMatrixType;
        this.transBitMatrix = transBitMatrix;
    }

    @Override
    public boolean get(int x, int y) {
        return transBitMatrix.get(x, y);
    }

    @Override
    public byte[] getColumn(int y) {
        return transBitMatrix.getColumn(y);
    }

    @Override
    public void setColumn(int y, byte[] byteArray) {
        transBitMatrix.setColumn(y, byteArray);
    }

    @Override
    public int getRows() {
        return transBitMatrix.getRows();
    }

    @Override
    public int getColumns() {
        return transBitMatrix.getColumns();
    }

    @Override
    public SimdTransBitMatrixType getType() {
        return simdTransBitMatrixType;
    }

    @Override
    public String toString() {
        return transBitMatrix.toString();
    }
}
