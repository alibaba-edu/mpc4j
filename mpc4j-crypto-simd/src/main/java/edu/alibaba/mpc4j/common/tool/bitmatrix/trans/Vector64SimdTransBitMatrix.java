package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.SimdTransBitMatrixFactory.SimdTransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;

/**
 * Vector API SIMD transpose bit matrix using 64-bit Species.
 *
 * @author Weiran Liu
 * @date 2024/3/9
 */
class Vector64SimdTransBitMatrix extends AbstractVectorSimdTransBitMatrix {
    /**
     * data is represented using an 2D-array
     */
    private final byte[][] data;
    /**
     * row in short
     */
    private final int rowBytes;
    /**
     * row offset
     */
    private final int rowOffset;
    /**
     * row rounded to divide Byte.SIZE
     */
    private final int roundByteRows;
    /**
     * column offset
     */
    private final int columnOffset;
    /**
     * column rounded to divide Byte.SIZE
     */
    private final int roundByteColumns;

    public Vector64SimdTransBitMatrix(final int rows, final int columns) {
        super(rows, columns);
        rowBytes = CommonUtils.getByteLength(rows);
        roundByteRows = rowBytes * Byte.SIZE;
        rowOffset = roundByteRows - rows;
        int columnBytes = CommonUtils.getByteLength(columns);
        roundByteColumns = columnBytes * Byte.SIZE;
        columnOffset = roundByteColumns - columns;
        data = new byte[roundByteColumns][rowBytes];
    }


    @Override
    public boolean get(int x, int y) {
        assert (x >= 0 && x < rows);
        assert (y >= 0 && y < columns);
        // do not forget to add offset in the column index
        return BinaryUtils.getBoolean(data[y + columnOffset], x + rowOffset);
    }

    @Override
    public byte[] getColumn(int y) {
        assert (y >= 0 && y < columns);
        return data[y + columnOffset];
    }

    @Override
    public void setColumn(int y, byte[] byteArray) {
        assert (y >= 0 && y < columns);
        assert BytesUtils.isFixedReduceByteArray(byteArray, rowBytes, rows);
        data[y + columnOffset] = byteArray;
    }

    @Override
    public SimdTransBitMatrix transpose() {
        Vector64SimdTransBitMatrix b = new Vector64SimdTransBitMatrix(columns, rows);
        for (int cc = 0; cc < roundByteColumns; cc += Byte.SIZE) {
            for (int rr = 0; rr < roundByteRows; rr += Byte.SIZE) {
                int ccByte = cc / Byte.SIZE;
                int rrByte = rr / Byte.SIZE;
                // using Vector API to set ByteVector, set in reverse order
                ByteVector byteVector = ByteVector.fromArray(
                    ByteVector.SPECIES_64,
                    new byte[] {
                        data[cc + 7][rrByte], data[cc + 6][rrByte], data[cc + 5][rrByte], data[cc + 4][rrByte],
                        data[cc + 3][rrByte], data[cc + 2][rrByte], data[cc + 1][rrByte], data[cc][rrByte],
                    },
                    0
                );
                for (int i = 0; i < Byte.SIZE; i++) {
                    // _mm_movemask_epi8(vec)
                    long movemask = byteVector
                        .compare(VectorOperators.LT, 0)
                        // toLong() treats the first boolean to be the last flag;
                        // that is the reason why we set ByteVector in reverse order.
                        .toLong();
                    b.data[rr + i][ccByte] = (byte) (movemask & 0xFF);
                    // _mm_slli_epi64(vec, 1)
                    byteVector = byteVector.lanewise(VectorOperators.LSHL, 1);
                }
            }
        }
        return b;
    }

    @Override
    public SimdTransBitMatrixType getType() {
        return SimdTransBitMatrixType.VECTOR_64;
    }
}
