package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * dense bit matrix using byte arrays.
 *
 * @author Weiran Liu
 * @date 2023/6/19
 */
public class ByteDenseBitMatrix implements DenseBitMatrix {
    /**
     * Creates an all-zero matrix.
     *
     * @param rows number of rows.
     * @param columns number of columns.
     * @return a matrix.
     */
    public static ByteDenseBitMatrix createAllZero(final int rows, final int columns) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        ByteDenseBitMatrix bitMatrix = new ByteDenseBitMatrix(rows, columns);
        bitMatrix.data = IntStream.range(0, rows)
            .mapToObj(iRow -> new byte[bitMatrix.byteColumns])
            .toArray(byte[][]::new);
        return bitMatrix;
    }

    /**
     * Creates an all-one matrix.
     *
     * @param rows number of rows.
     * @param columns number of columns.
     * @return a matrix.
     */
    public static ByteDenseBitMatrix createAllOne(final int rows, final int columns) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        ByteDenseBitMatrix bitMatrix = new ByteDenseBitMatrix(rows, columns);
        bitMatrix.data = IntStream.range(0, rows)
            .mapToObj(iRow -> BytesUtils.allOneByteArray(columns))
            .toArray(byte[][]::new);
        return bitMatrix;
    }

    /**
     * Creates a random matrix.
     *
     * @param rows number of rows.
     * @param columns number of columns.
     * @return a matrix.
     */
    public static ByteDenseBitMatrix createRandom(final int rows, final int columns, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        ByteDenseBitMatrix bitMatrix = new ByteDenseBitMatrix(rows, columns);
        bitMatrix.data = IntStream.range(0, rows)
            .mapToObj(iRow -> BytesUtils.randomByteArray(bitMatrix.byteColumns, columns, secureRandom))
            .toArray(byte[][]::new);
        return bitMatrix;
    }

    /**
     * Creates from the dense data.
     *
     * @param columns number of columns.
     * @param data    dense data.
     * @return the matrix.
     */
    public static ByteDenseBitMatrix createFromDense(final int columns, byte[][] data) {
        MathPreconditions.checkPositive("rows", data.length);
        int rows = data.length;
        MathPreconditions.checkPositive("columns", columns);
        ByteDenseBitMatrix bitMatrix = new ByteDenseBitMatrix(rows, columns);
        bitMatrix.data = Arrays.stream(data)
            .peek(row ->
                Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(row, bitMatrix.byteColumns, bitMatrix.columns))
            )
            .toArray(byte[][]::new);
        return bitMatrix;
    }

    /**
     * Creates from the dense data without validate check.
     *
     * @param columns number of columns.
     * @param data    dense data.
     * @return the matrix.
     */
    private static ByteDenseBitMatrix createFromDenseUncheck(final int columns, byte[][] data) {
        int rows = data.length;
        ByteDenseBitMatrix bitMatrix = new ByteDenseBitMatrix(rows, columns);
        bitMatrix.data = data;
        return bitMatrix;
    }

    /**
     * Creates from the sparse data.
     *
     * @param columns   number of columns.
     * @param positions sparse data.
     * @return the matrix.
     */
    public static ByteDenseBitMatrix createFromSparse(final int columns, int[][] positions) {
        MathPreconditions.checkPositive("rows", positions.length);
        int rows = positions.length;
        MathPreconditions.checkPositive("columns", columns);
        ByteDenseBitMatrix bitMatrix = new ByteDenseBitMatrix(rows, columns);
        bitMatrix.data = new byte[rows][bitMatrix.byteColumns];
        for (int iRow = 0; iRow < rows; iRow++) {
            for (int position : positions[iRow]) {
                MathPreconditions.checkNonNegativeInRange("position", position, columns);
                BinaryUtils.setBoolean(bitMatrix.data[iRow], position + bitMatrix.byteColumnsOffset, true);
            }
        }
        return bitMatrix;
    }

    /**
     * number of rows
     */
    private final int rows;
    /**
     * number of rows in byte
     */
    private final int byteRows;
    /**
     * offset number of rows in byte
     */
    private final int byteRowsOffset;
    /**
     * number of columns
     */
    private final int columns;
    /**
     * number of columns in byte
     */
    private final int byteColumns;
    /**
     * offset number of columns in byte
     */
    private final int byteColumnsOffset;
    /**
     * data
     */
    private byte[][] data;

    /**
     * private constructor.
     */
    private ByteDenseBitMatrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        byteRows = CommonUtils.getByteLength(rows);
        byteRowsOffset = byteRows * Byte.SIZE - rows;
        byteColumns = CommonUtils.getByteLength(columns);
        byteColumnsOffset = byteColumns * Byte.SIZE - columns;
    }

    @Override
    public DenseBitMatrix xor(DenseBitMatrix that) {
        MathPreconditions.checkEqual("this.rows", "that.rows", this.rows, that.getRows());
        MathPreconditions.checkEqual("this.columns", "that.columns", this.columns, that.getColumns());
        byte[][] addData = IntStream.range(0, rows)
            .mapToObj(iRow -> BytesUtils.xor(this.data[iRow], that.getByteArrayRow(iRow)))
            .toArray(byte[][]::new);
        return createFromDenseUncheck(columns, addData);
    }

    @Override
    public void xori(DenseBitMatrix that) {
        MathPreconditions.checkEqual("this.rows", "that.rows", this.rows, that.getRows());
        MathPreconditions.checkEqual("this.columns", "that.columns", this.columns, that.getColumns());
        IntStream.range(0, rows).forEach(iRow -> BytesUtils.xori(this.data[iRow], that.getByteArrayRow(iRow)));
    }

    @Override
    public DenseBitMatrix multiply(DenseBitMatrix that) {
        int thatColumns = that.getColumns();
        MathPreconditions.checkEqual("this.columns", "that.rows", this.columns, that.getRows());
        int thatByteColumns = CommonUtils.getByteLength(thatColumns);
        byte[][] mulData = new byte[rows][thatByteColumns];
        for (int iRow = 0; iRow < rows; iRow++) {
            byte[] thisRow = data[iRow];
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                if (BinaryUtils.getBoolean(thisRow, iColumn + byteColumnsOffset)) {
                    BytesUtils.xori(mulData[iRow], that.getByteArrayRow(iColumn));
                }
            }
        }
        return createFromDenseUncheck(thatColumns, mulData);
    }

    @Override
    public DenseBitMatrix transpose(EnvType envType, boolean parallel) {
        TransBitMatrix cTransBitMatrix = TransBitMatrixFactory.createInstance(envType, columns, rows, parallel);
        for (int iRow = 0; iRow < rows; iRow++) {
            cTransBitMatrix.setColumn(iRow, data[iRow]);
        }
        TransBitMatrix tTransBitMatrix = cTransBitMatrix.transpose();
        byte[][] tData = new byte[columns][byteRows];
        for (int itRow = 0; itRow < columns; itRow++) {
            tData[itRow] = tTransBitMatrix.getColumn(itRow);
        }
        return createFromDenseUncheck(rows, tData);
    }

    @Override
    public ByteDenseBitMatrix inverse() {
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        int size = rows;
        //noinspection UnnecessaryLocalVariable
        int byteSize = byteRows;
        int byteOffset = byteRowsOffset;
        byte[][] cData = BytesUtils.clone(data);
        byte[][] iData = new byte[size][byteSize];
        IntStream.range(0, size).forEach(iRow -> BinaryUtils.setBoolean(iData[iRow], iRow + byteOffset, true));
        // 利用初等变换计算逆矩阵。首先将左矩阵转换为上三角矩阵
        for (int p = 0; p < size; p++) {
            if (!BinaryUtils.getBoolean(cData[p], p + byteOffset)) {
                // 找到一个不为0的行
                int other = p + 1;
                while (other < size && !BinaryUtils.getBoolean(cData[other], p + byteOffset)) {
                    other++;
                }
                if (other >= size) {
                    throw new ArithmeticException("Cannot invert bit matrix");
                } else {
                    // 左侧矩阵行swap
                    byte[] matrixRowTemp = cData[p];
                    cData[p] = cData[other];
                    cData[other] = matrixRowTemp;
                    // 右侧矩阵行swap
                    byte[] inverseMatrixRowTemp = iData[p];
                    iData[p] = iData[other];
                    iData[other] = inverseMatrixRowTemp;
                }
            }
            // 左右侧矩阵高斯消元
            for (int i = p + 1; i < size; i++) {
                if (BinaryUtils.getBoolean(cData[i], p + byteOffset)) {
                    BytesUtils.xori(cData[i], cData[p]);
                    BytesUtils.xori(iData[i], iData[p]);
                }
            }
        }
        // 将左侧矩阵转为单位矩阵，此时右侧得到的矩阵就是左侧矩阵的逆矩阵
        for (int p = size - 1; p >= 0; p--) {
            // from the top to the bottom
            for (int r = 0; r < p; r++) {
                if (BinaryUtils.getBoolean(cData[r], p + byteOffset)) {
                    // if matrix[r][p] = 1, then eliminate
                    BytesUtils.xori(cData[r], cData[p]);
                    BytesUtils.xori(iData[r], iData[p]);
                }
            }
        }
        // 返回逆矩阵
        return ByteDenseBitMatrix.createFromDenseUncheck(size, iData);
    }

    @Override
    public byte[] leftMultiply(byte[] v) {
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(v, byteRows, rows));
        byte[] output = new byte[byteColumns];
        for (int iRow = 0; iRow < rows; iRow++) {
            if (BinaryUtils.getBoolean(v, iRow + byteRowsOffset)) {
                BytesUtils.xori(output, data[iRow]);
            }
        }
        return output;
    }

    @Override
    public void leftMultiplyXori(byte[] v, byte[] t) {
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(v, byteRows, rows));
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(t, byteColumns, columns));
        for (int iRow = 0; iRow < rows; iRow++) {
            if (BinaryUtils.getBoolean(v, iRow + byteRowsOffset)) {
                BytesUtils.xori(t, data[iRow]);
            }
        }
    }

    @Override
    public boolean[] leftMultiply(boolean[] v) {
        MathPreconditions.checkEqual("v.length", "rows", v.length, rows);
        byte[] output = new byte[byteColumns];
        for (int iRow = 0; iRow < rows; iRow++) {
            if (v[iRow]) {
                BytesUtils.xori(output, data[iRow]);
            }
        }
        return BinaryUtils.byteArrayToBinary(output, columns);
    }

    @Override
    public void leftMultiplyXori(boolean[] v, boolean[] t) {
        MathPreconditions.checkEqual("v.length", "rows", v.length, rows);
        MathPreconditions.checkEqual("t.length", "columns", t.length, columns);
        for (int iRow = 0; iRow < rows; iRow++) {
            if (v[iRow]) {
                for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                    t[columnIndex] ^= BinaryUtils.getBoolean(data[iRow], columnIndex + byteColumnsOffset);
                }
            }
        }
    }

    @Override
    public byte[][] leftGf2lMultiply(byte[][] v) {
        MathPreconditions.checkEqual("v.length", "rows", v.length, rows);
        byte[][] output = new byte[columns][v[0].length];
        leftGf2lMultiplyXori(v, output);
        return output;
    }

    @Override
    public void leftGf2lMultiplyXori(byte[][] v, byte[][] t) {
        MathPreconditions.checkEqual("v.length", "rows", v.length, rows);
        MathPreconditions.checkEqual("t.length", "columns", t.length, columns);
        MathPreconditions.checkEqual("vi.length", "t.length", v[0].length, t[0].length);
        for (int iColumn = 0; iColumn < columns; iColumn++) {
            for (int iRow = 0; iRow < rows; iRow++) {
                if (BinaryUtils.getBoolean(data[iRow], iColumn + byteColumnsOffset)) {
                    BytesUtils.xori(t[iColumn], v[iRow]);
                }
            }
        }
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public byte[] getByteArrayRow(int iRow) {
        return data[iRow];
    }

    @Override
    public long[] getLongArrayRow(int iRow) {
        return LongUtils.byteArrayToRoundLongArray(data[iRow]);
    }

    @Override
    public int getColumns() {
        return columns;
    }

    @Override
    public int getSize() {
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        return rows;
    }

    @Override
    public int getByteSize() {
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        return byteRows;
    }

    @Override
    public boolean get(int iRow, int iColumn) {
        MathPreconditions.checkNonNegativeInRange("iColumn", iColumn, columns);
        return BinaryUtils.getBoolean(data[iRow], iColumn + byteColumnsOffset);
    }

    @Override
    public byte[][] getByteArrayData() {
        return data;
    }

    @Override
    public long[][] getLongArrayData() {
        return IntStream.range(0, rows)
            .mapToObj(iRow -> LongUtils.byteArrayToRoundLongArray(data[iRow]))
            .toArray(long[][]::new);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(data).forEach(row -> {
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
    public int hashCode() {
        return new HashCodeBuilder().append(data).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        DenseBitMatrix that = (DenseBitMatrix) obj;
        return new EqualsBuilder().append(this.data, that.getByteArrayData()).isEquals();
    }
}
