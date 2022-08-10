package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

/**
 * 转置布尔矩阵抽象类。
 *
 * @author Weiran Liu
 * @date 2021/11/30
 */
abstract class AbstractTransBitMatrix implements TransBitMatrix {
    /**
     * 行数
     */
    protected final int rows;
    /**
     * 列数
     */
    protected final int columns;

    AbstractTransBitMatrix(int rows, int columns) {
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
