package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.SimdTransBitMatrixFactory.SimdTransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;

/**
 * Vector API SIMD transpose bit matrix using 128-bit Species.
 *
 * @author Feng Han
 * @date 2024/3/11
 */
public class Vector128SimdTransBitMatrix extends AbstractVectorSimdTransBitMatrix {
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
    public Vector128SimdTransBitMatrix(final int rows, final int columns) {
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
        Vector128SimdTransBitMatrix b = new Vector128SimdTransBitMatrix(columns, rows);
        int rr, cc;
        int rrByte, ccByte;
        for (cc = 0, ccByte = 0; cc <= roundByteColumns - 16; cc += 16, ccByte += 2) {
            int ccByte1 = ccByte + 1;
            for (rr = 0, rrByte = 0; rr < roundByteRows; rr += Byte.SIZE, rrByte++) {
                // using Vector API to set ByteVector, set in reverse order
                ByteVector byteVector = ByteVector.fromArray(
                    ByteVector.SPECIES_128,
                    new byte[] {
                        data[cc + 15][rrByte], data[cc + 14][rrByte], data[cc + 13][rrByte], data[cc + 12][rrByte],
                        data[cc + 11][rrByte], data[cc + 10][rrByte], data[cc + 9][rrByte], data[cc + 8][rrByte],
                        data[cc + 7][rrByte], data[cc + 6][rrByte], data[cc + 5][rrByte], data[cc + 4][rrByte],
                        data[cc + 3][rrByte], data[cc + 2][rrByte], data[cc + 1][rrByte], data[cc][rrByte],
                    },
                    0
                );
                for (int i = 0; i < Byte.SIZE; i++) {
                    // _mm_movemask_epi8(vec)
                    long movemask = byteVector
                        .compare(VectorOperators.LT, 0)
                        .toLong();
                    int rwoTarget = rr + i;
                    b.data[rwoTarget][ccByte] = (byte) ((movemask >> Byte.SIZE) & 0xFF);
                    b.data[rwoTarget][ccByte1] = (byte) (movemask & 0xFF);
                    // _mm_slli_epi64(vec, 1)
                    byteVector = byteVector.lanewise(VectorOperators.LSHL, 1);
                }
            }
        }
        if(cc == roundByteColumns){
            return b;
        }else{
            // The remainder is a block of 8x(16n+8) bits (n may be 0).
            for(rr = 0, rrByte = 0; rr <= roundByteRows - 16; rr += 16, rrByte += 2){
                int rrByte1 = rrByte + 1;
                ByteVector byteVector = ByteVector.fromArray(
                    ByteVector.SPECIES_128,
                    new byte[] {
                        data[cc + 7][rrByte], data[cc + 6][rrByte], data[cc + 5][rrByte], data[cc + 4][rrByte],
                        data[cc + 3][rrByte], data[cc + 2][rrByte], data[cc + 1][rrByte], data[cc][rrByte],
                        data[cc + 7][rrByte1], data[cc + 6][rrByte1], data[cc + 5][rrByte1], data[cc + 4][rrByte1],
                        data[cc + 3][rrByte1], data[cc + 2][rrByte1], data[cc + 1][rrByte1], data[cc][rrByte1],
                    },
                    0
                );
                int tmp = rr + 8;
                for(int i = 0; i < Byte.SIZE; i++){
                    // _mm_movemask_epi8(vec)
                    long movemask = byteVector
                        .compare(VectorOperators.LT, 0)
                        .toLong();
                    b.data[tmp + i][ccByte] = (byte) ((movemask >> Byte.SIZE) & 0xFF);
                    b.data[rr + i][ccByte] = (byte) (movemask & 0xFF);
                    // _mm_slli_epi64(vec, 1)
                    byteVector = byteVector.lanewise(VectorOperators.LSHL, 1);
                }
            }
        }
        if (rr == roundByteRows) {
            return b;
        }

        //  Do the remaining 8x8 block:
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
                .toLong();
            b.data[rr + i][ccByte] = (byte) (movemask & 0xFF);
            // _mm_slli_epi64(vec, 1)
            byteVector = byteVector.lanewise(VectorOperators.LSHL, 1);
        }
        return b;
    }

    @Override
    public SimdTransBitMatrixType getType() {
        return SimdTransBitMatrixType.VECTOR_128;
    }
}
