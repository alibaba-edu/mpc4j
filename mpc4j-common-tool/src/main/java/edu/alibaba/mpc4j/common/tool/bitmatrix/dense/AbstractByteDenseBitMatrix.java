package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 用byte[]表示的布尔矩阵抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/2
 */
public abstract class AbstractByteDenseBitMatrix implements DenseBitMatrix {
    /**
     * 行数
     */
    protected int rows;
    /**
     * 字节行数
     */
    protected int byteRows;
    /**
     * 行数偏移量
     */
    protected int rowOffset;
    /**
     * 列数
     */
    protected int columns;
    /**
     * 字节列数
     */
    protected int byteColumns;
    /**
     * 列数偏移量
     */
    protected int columnOffset;
    /**
     * 布尔方阵
     */
    protected byte[][] byteBitMatrix;

    /**
     * 从稀疏点位置初始化成员变量。
     * @param columns 列数。
     * @param positions 位置矩阵。
     * @throws ArithmeticException 抛出异常。
     */
    protected void initFromSparse(final int columns, int[][] positions) throws ArithmeticException {
        assert positions.length > 0 : "rows must be greater than 0: " + positions.length;
        rows = positions.length;
        byteRows = CommonUtils.getByteLength(rows);
        rowOffset = byteRows * Byte.SIZE - rows;
        assert columns > 0 : "columns must be greater than 0: " + columns;
        this.columns = columns;
        byteColumns = CommonUtils.getByteLength(columns);
        columnOffset = byteColumns * Byte.SIZE - columns;
        byteBitMatrix = Arrays.stream(positions)
            .map(rowPositions -> {
                byte[] row = new byte[byteColumns];
                for (int position : rowPositions) {
                    assert position >= 0 && position < columns
                        : "position must be in range [0, " + columns + "): " + position;
                    // 将每个所需的位置设置为1
                    BinaryUtils.setBoolean(row, position + columnOffset, true);
                }
                return row;
            })
            .toArray(byte[][]::new);
    }

    protected void initFromDense(final int columns, byte[][] byteArrays) {
        assert byteArrays.length > 0 : "rows must be greater than 0: " + byteArrays.length;
        rows = byteArrays.length;
        byteRows = CommonUtils.getByteLength(rows);
        rowOffset = byteRows * Byte.SIZE - rows;
        assert columns > 0 : "columns must be greater than 0: " + columns;
        this.columns = columns;
        byteColumns = CommonUtils.getByteLength(columns);
        columnOffset = byteColumns * Byte.SIZE - columns;
        byteBitMatrix = Arrays.stream(byteArrays)
            .peek(row -> {
                assert row.length == byteColumns
                    : "row byte length must be " + byteColumns + ": " + row.length;
                assert BytesUtils.isReduceByteArray(row, columns)
                    : "row must contain " + columns + " valid bits, current row: " + Hex.toHexString(row);
            })
            .toArray(byte[][]::new);
    }

    protected byte[][] addToBytes(DenseBitMatrix that) {
        assert rows == that.getRows() : "input matrix must have " + rows + " rows: " + that.getRows();
        assert columns == that.getColumns() : "input matrix must have " + columns + " columns: " + that.getColumns();
        return IntStream.range(0, rows)
            .mapToObj(x -> BytesUtils.xor(byteBitMatrix[x], that.getRow(x)))
            .toArray(byte[][]::new);
    }

    protected byte[][] multiplyToBytes(DenseBitMatrix that) {
        assert columns == that.getRows() : "input matrix must have " + rows + " rows: " + that.getRows();
        int mulColumns = that.getColumns();
        int mulByteColumns = CommonUtils.getByteLength(mulColumns);
        byte[][] mulByteBitMatrix = new byte[rows][mulByteColumns];
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            byte[] input = byteBitMatrix[rowIndex];
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                if (BinaryUtils.getBoolean(input, columnIndex + columnOffset)) {
                    BytesUtils.xori(mulByteBitMatrix[rowIndex], that.getRow(columnIndex));
                }
            }
        }
        return mulByteBitMatrix;
    }

    protected byte[][] transposeToBytes(EnvType envType, boolean parallel) {
        // 调用TransBitMatrix实现转置，TransBitMatrix是按列表示的，所以设置时候要反过来
        TransBitMatrix originTransBitMatrix = TransBitMatrixFactory.createInstance(envType, columns, rows, parallel);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            originTransBitMatrix.setColumn(rowIndex, byteBitMatrix[rowIndex]);
        }
        TransBitMatrix transposedTransBitMatrix = originTransBitMatrix.transpose();
        byte[][] transByteBitMatrix = new byte[columns][byteRows];
        for (int transRowIndex = 0; transRowIndex < columns; transRowIndex++) {
            transByteBitMatrix[transRowIndex] = transposedTransBitMatrix.getColumn(transRowIndex);
        }
        return transByteBitMatrix;
    }

    @Override
    public void addi(DenseBitMatrix that) {
        assert rows == that.getRows() : "input matrix must have " + rows + " rows: " + that.getRows();
        assert columns == that.getColumns() : "input matrix must have " + columns + " columns: " + that.getColumns();
        IntStream.range(0, rows).forEach(x -> BytesUtils.xori(byteBitMatrix[x], that.getRow(x)));
    }

    @Override
    public byte[] lmul(byte[] v) {
        assert v.length == byteRows : "byte length of v must be " + byteRows + ": " + v.length;
        assert BytesUtils.isReduceByteArray(v, rows)
            : "v must contain " + rows + " valid bits, current v: " + Hex.toHexString(v);
        byte[] output = new byte[byteColumns];
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            if (BinaryUtils.getBoolean(v, rowIndex + rowOffset)) {
                BytesUtils.xori(output, byteBitMatrix[rowIndex]);
            }
        }
        return output;
    }

    @Override
    public boolean[] lmul(boolean[] v) {
        assert v.length == rows : "length of v must be " + rows + ": " + v.length;
        byte[] output = new byte[byteColumns];
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            if (v[rowIndex]) {
                BytesUtils.xori(output, byteBitMatrix[rowIndex]);
            }
        }
        return BinaryUtils.byteArrayToBinary(output, columns);
    }

    @Override
    public byte[][] lExtMul(byte[][] v) {
        assert v.length == rows : "length of v must be " + rows + ": " + v.length;
        byte[][] output = new byte[columns][v[0].length];
        lExtMulAddi(v, output);
        return output;
    }

    @Override
    public void lmulAddi(byte[] v, byte[] t) {
        assert v.length == byteRows : "byte length of v must be " + byteRows + ": " + v.length;
        assert BytesUtils.isReduceByteArray(v, rows)
            : "v must contain " + rows + " valid bits, current v: " + Hex.toHexString(v);
        assert t.length == byteColumns : "byte length of t must be " + byteColumns + ": " + t.length;
        assert BytesUtils.isReduceByteArray(t, columns)
            : "t must contain " + columns + " valid bits, current t: " + Hex.toHexString(v);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            if (BinaryUtils.getBoolean(v, rowIndex + rowOffset)) {
                BytesUtils.xori(t, byteBitMatrix[rowIndex]);
            }
        }
    }

    @Override
    public void lmulAddi(boolean[] v, boolean[] t) {
        assert v.length == rows : "length of v must be " + rows + ": " + v.length;
        assert t.length == columns : "length of t must be " + columns + ": " + t.length;
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            if (v[rowIndex]) {
                for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                    t[columnIndex] ^= BinaryUtils.getBoolean(byteBitMatrix[rowIndex], columnIndex + columnOffset);
                }
            }
        }
    }

    @Override
    public void lExtMulAddi(byte[][] v, byte[][] t) {
        assert v.length == rows : "length of v must be " + rows + ": " + v.length;
        assert t.length == columns : "length of t must be " + columns + ": " + t.length;
        assert v[0].length == t[0].length : "length of t[i] must be " + v[0].length + ": " + t[0].length;
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                if (BinaryUtils.getBoolean(byteBitMatrix[rowIndex], columnIndex + columnOffset)) {
                    BytesUtils.xori(t[columnIndex], v[rowIndex]);
                }
            }
        }
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public byte[] getRow(int x) {
        return byteBitMatrix[x];
    }

    @Override
    public int getColumns() {
        return columns;
    }

    @Override
    public boolean get(int x, int y) {
        assert y >= 0 && y < columns : "y must be in range [0, " + columns + "): " + y;
        return BinaryUtils.getBoolean(byteBitMatrix[x], y + columnOffset);
    }

    @Override
    public byte[][] toByteArrays() {
        return byteBitMatrix;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(byteBitMatrix).forEach(row -> {
            BigInteger rowBigInteger = new BigInteger(1, row);
            StringBuilder rowStringBuilder = new StringBuilder(rowBigInteger.toString(2));
            while (rowStringBuilder.length() < columns) {
                rowStringBuilder.insert(0, "0");
            }
            stringBuilder.append(rowStringBuilder).append("\n");
        });
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractByteDenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        AbstractByteDenseBitMatrix that = (AbstractByteDenseBitMatrix) obj;
        return new EqualsBuilder().append(this.byteBitMatrix, that.byteBitMatrix).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(byteBitMatrix).toHashCode();
    }
}
