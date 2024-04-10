package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

/**
 * abstract vector API SIMD transpose bit matrix
 *
 * @author Weiran Liu
 * @date 2024/3/9
 */
abstract class AbstractVectorSimdTransBitMatrix implements SimdTransBitMatrix {
    /**
     * number of rows.
     */
    protected final int rows;
    /**
     * number of columns.
     */
    protected final int columns;

    AbstractVectorSimdTransBitMatrix(int rows, int columns) {
        assert rows > 0;
        assert columns > 0;
        this.rows = rows;
        this.columns = columns;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public int getColumns() {
        return columns;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        for (int x = 0; x < getRows(); x++) {
            StringBuilder innerStringBuilder = new StringBuilder();
            for (int y = 0; y < getColumns(); y++) {
                innerStringBuilder.append((this.get(x, y) ? '1' : '0'));
            }
            stringBuilder.append(innerStringBuilder);
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
