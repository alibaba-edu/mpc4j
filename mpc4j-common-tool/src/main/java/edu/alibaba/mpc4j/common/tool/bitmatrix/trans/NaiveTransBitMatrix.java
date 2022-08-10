package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * 朴素转置布尔矩阵。
 *
 * @author Weiran Liu
 * @date 2020/09/08
 */
class NaiveTransBitMatrix extends AbstractTransBitMatrix {
    /**
     * 用二维字节数组表示的矩阵
     */
    private final byte[][] data;
    /**
     * 字节行数
     */
    private final int rowBytes;
    /**
     * 字节行数偏移量
     */
    private final int rowOffset;

    NaiveTransBitMatrix(final int rows, final int columns) {
        super(rows, columns);
        rowBytes = CommonUtils.getByteLength(rows);
        int roundByteRows = rowBytes * Byte.SIZE;
        rowOffset = roundByteRows - rows;
        data = new byte[columns][rowBytes];
    }

    @Override
    public boolean get(int x, int y) {
        assert (x >= 0 && x < rows);
        assert (y >= 0 && y < columns);
        return BinaryUtils.getBoolean(data[y], x + rowOffset);
    }

    @Override
    public byte[] getColumn(int y) {
        assert (y >= 0 && y < columns);
        return data[y];
    }

    @Override
    public void setColumn(int y, byte[] byteArray) {
        assert (y >= 0 && y < columns);
        assert (byteArray.length == rowBytes);
        assert BytesUtils.isReduceByteArray(byteArray, rows);
        this.data[y] = byteArray;
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
    public TransBitMatrix transpose() {
        // 创建一个新的转置矩阵，新矩阵的行数为原始矩阵的列数，新矩阵的列数为原始矩阵的行数
        NaiveTransBitMatrix b = new NaiveTransBitMatrix(columns, rows);
        // 朴素转置方法
        for (int bColumnIndex = 0; bColumnIndex < b.columns; bColumnIndex++) {
            for (int bRowIndex = 0; bRowIndex < b.rows; bRowIndex++) {
                if (this.get(bColumnIndex, bRowIndex)) {
                    BinaryUtils.setBoolean(b.data[bColumnIndex], bRowIndex + b.rowOffset, true);
                }
            }
        }
        return b;
    }

    @Override
    public TransBitMatrixType getTransBitMatrixType() {
        return TransBitMatrixType.NAIVE;
    }
}
