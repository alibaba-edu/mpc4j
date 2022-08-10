package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * Eklundh高效缓存矩阵转置算法实现的转置布尔矩阵。论文来源：
 * <p>
 * J. O. Eklundh. A fast computer method for matrix transposing. IEEE Transactions on Computers, C-21(7):801–803, 1972.
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/01/25
 */
class EklundhTransBitMatrix extends AbstractTransBitMatrix {
    /**
     * 用二维字节数组表示的矩阵
     */
    private final byte[][] data;
    /**
     * 字节行数
     */
    private final int rowBytes;
    /**
     * 行偏移量
     */
    private final int rowOffset;

    EklundhTransBitMatrix(int rows, int columns) {
        super(rows, columns);
        rowBytes = CommonUtils.getByteLength(rows);
        int roundByteRow = rowBytes * Byte.SIZE;
        rowOffset = roundByteRow - rows;
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
        return this.data[y];
    }

    @Override
    public void setColumn(int y, byte[] byteArray) {
        assert (y >= 0 && y < columns);
        assert (byteArray.length == rowBytes);
        assert BytesUtils.isReduceByteArray(byteArray, rows);
        this.data[y] = byteArray;
    }

    @Override
    public TransBitMatrix transpose() {
        // 创建一个新的转置矩阵，新矩阵的行数为原始矩阵的列数，新矩阵的列数为原始矩阵的行数
        EklundhTransBitMatrix b = new EklundhTransBitMatrix(columns, rows);
        // 迭代调用转置函数，完成转置
        eklundhTranspose(this, b, 0, 0, rows, columns);

        return b;
    }

    @Override
    public TransBitMatrixType getTransBitMatrixType() {
        return TransBitMatrixType.EKLUNDH;
    }

    /**
     * 高效比特矩阵迭代转置。
     *
     * @param a             原始比特矩阵。
     * @param b             转置比特矩阵。
     * @param startAbscissa 横坐标开始值。
     * @param startOrdinate 纵坐标开始值。
     * @param endAbscissa   横坐标结束值。
     * @param endOrdinate   纵坐标结束值。
     */
    private void eklundhTranspose(EklundhTransBitMatrix a, EklundhTransBitMatrix b,
                                  int startAbscissa, int startOrdinate, int endAbscissa, int endOrdinate) {
        if (endOrdinate - startOrdinate == 1 && endAbscissa - startAbscissa == 1) {
            // 此时被转置的是2 * 2矩阵，只需要交换对角的元素位置
            if (BinaryUtils.getBoolean(a.data[startOrdinate], startAbscissa + a.rowOffset)) {
                // 如果a[startX, startY] == 1，则b[startY, startX] == 1
                BinaryUtils.setBoolean(b.data[startAbscissa], startOrdinate + b.rowOffset, true);
            }
        } else if (endOrdinate - startOrdinate < endAbscissa - startAbscissa) {
            // 如果不是2 * 2的矩阵，且横坐标差比纵坐标差大，则需要拆分横坐标
            // 注意，即使是2 * 1的矩阵也会被拆分成1 * 1的矩阵，而1 * 1的矩阵不需要转置
            int midAbscissa = (startAbscissa + endAbscissa) / 2;
            eklundhTranspose(a, b, startAbscissa, startOrdinate, midAbscissa, endOrdinate);
            eklundhTranspose(a, b, midAbscissa, startOrdinate, endAbscissa, endOrdinate);
        } else {
            // 如果不是2 * 2的矩阵，且纵坐标差比横坐标大，则需要拆分纵坐标
            // 注意，即使是1 * 2的矩阵也会被拆分成1 * 1的矩阵，而1 * 1的矩阵不需要转置
            int midOrdinate = (startOrdinate + endOrdinate) / 2;
            eklundhTranspose(a, b, startAbscissa, startOrdinate, endAbscissa, midOrdinate);
            eklundhTranspose(a, b, startAbscissa, midOrdinate, endAbscissa, endOrdinate);
        }
    }
}
