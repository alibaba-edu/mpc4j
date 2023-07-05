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
 * 用long[]维护的布尔方阵。
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
public class LongDenseBitMatrix implements DenseBitMatrix {
    /**
     * Creates an all-zero matrix.
     *
     * @param rows    number of rows.
     * @param columns number of columns.
     * @return a matrix.
     */
    public static LongDenseBitMatrix createAllZero(final int rows, final int columns) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        LongDenseBitMatrix bitMatrix = new LongDenseBitMatrix(rows, columns);
        bitMatrix.data = IntStream.range(0, rows)
            .mapToObj(iRow -> new long[bitMatrix.longColumns])
            .toArray(long[][]::new);
        return bitMatrix;
    }

    /**
     * Creates an all-one matrix.
     *
     * @param rows    number of rows.
     * @param columns number of columns.
     * @return a matrix.
     */
    public static LongDenseBitMatrix createAllOne(final int rows, final int columns) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        LongDenseBitMatrix bitMatrix = new LongDenseBitMatrix(rows, columns);
        bitMatrix.data = IntStream.range(0, rows)
            .mapToObj(iRow -> LongUtils.allOneLongArray(columns))
            .toArray(long[][]::new);
        return bitMatrix;
    }
    /**
     * Creates a random matrix.
     *
     * @param rows number of rows.
     * @param columns number of columns.
     * @return a matrix.
     */
    public static LongDenseBitMatrix createRandom(final int rows, final int columns, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        LongDenseBitMatrix bitMatrix = new LongDenseBitMatrix(rows, columns);
        bitMatrix.data = IntStream.range(0, rows)
            .mapToObj(iRow -> LongUtils.randomLongArray(bitMatrix.longColumns, columns, secureRandom))
            .toArray(long[][]::new);
        return bitMatrix;
    }


    /**
     * Creates from the dense data.
     *
     * @param columns number of columns.
     * @param data    dense data.
     * @return the matrix.
     */
    public static LongDenseBitMatrix createFromDense(int columns, byte[][] data) {
        MathPreconditions.checkPositive("rows", data.length);
        int rows = data.length;
        MathPreconditions.checkPositive("columns", columns);
        LongDenseBitMatrix bitMatrix = new LongDenseBitMatrix(rows, columns);
        bitMatrix.data = Arrays.stream(data)
            .peek(row ->
                Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(row, bitMatrix.byteColumns, bitMatrix.columns))
            )
            .map(LongUtils::byteArrayToRoundLongArray)
            .toArray(long[][]::new);
        return bitMatrix;
    }

    /**
     * Creates from the dense data without validate check.
     *
     * @param columns number of columns.
     * @param data    dense data.
     * @return the matrix.
     */
    private static LongDenseBitMatrix createFromDenseUncheck(int columns, long[][] data) {
        int rows = data.length;
        LongDenseBitMatrix bitMatrix = new LongDenseBitMatrix(rows, columns);
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
    public static LongDenseBitMatrix createFromSparse(int columns, int[][] positions) {
        MathPreconditions.checkPositive("rows", positions.length);
        int rows = positions.length;
        MathPreconditions.checkPositive("columns", columns);
        LongDenseBitMatrix bitMatrix = new LongDenseBitMatrix(rows, columns);
        bitMatrix.data = new long[rows][bitMatrix.longColumns];
        for (int iRow = 0; iRow < rows; iRow++) {
            for (int position : positions[iRow]) {
                MathPreconditions.checkNonNegativeInRange("position", position, columns);
                BinaryUtils.setBoolean(bitMatrix.data[iRow], position + bitMatrix.longColumnsOffset, true);
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
     * number of rows in long
     */
    private final int longRows;
    /**
     * offset number of rows in long
     */
    private final int longRowsOffset;
    /**
     * number of columns
     */
    private final int columns;
    /**
     * number of columns in byte
     */
    private final int byteColumns;
    /**
     * number of columns in long
     */
    private final int longColumns;
    /**
     * offset number of columns in long
     */
    private final int longColumnsOffset;
    /**
     * data
     */
    private long[][] data;

    /**
     * private constructor.
     */
    private LongDenseBitMatrix(int rows, int columns) {
        this.rows = rows;
        byteRows = CommonUtils.getByteLength(rows);
        byteRowsOffset = byteRows * Byte.SIZE - rows;
        longRows = CommonUtils.getLongLength(rows);
        longRowsOffset = longRows * Long.SIZE - rows;
        this.columns = columns;
        byteColumns = CommonUtils.getByteLength(columns);
        longColumns = CommonUtils.getLongLength(columns);
        longColumnsOffset = longColumns * Long.SIZE - columns;
    }

    @Override
    public LongDenseBitMatrix xor(DenseBitMatrix that) {
        MathPreconditions.checkEqual("this.rows", "that.rows", this.rows, that.getRows());
        MathPreconditions.checkEqual("this.columns", "that.columns", this.columns, that.getColumns());
        long[][] addData = IntStream.range(0, rows)
            .mapToObj(iRow -> LongUtils.xor(data[iRow], that.getLongArrayRow(iRow)))
            .toArray(long[][]::new);
        return LongDenseBitMatrix.createFromDenseUncheck(columns, addData);
    }

    @Override
    public void xori(DenseBitMatrix that) {
        MathPreconditions.checkEqual("this.rows", "that.rows", this.rows, that.getRows());
        MathPreconditions.checkEqual("this.columns", "that.columns", this.columns, that.getColumns());
        IntStream.range(0, rows).forEach(iRow -> LongUtils.xori(data[iRow], that.getLongArrayRow(iRow)));
    }

    @Override
    public DenseBitMatrix multiply(DenseBitMatrix that) {
        int thatColumns = that.getColumns();
        MathPreconditions.checkEqual("this.columns", "that.rows", this.columns, that.getRows());
        int thatLongColumns = CommonUtils.getLongLength(thatColumns);
        long[][] mulData = new long[rows][thatLongColumns];
        for (int iRow = 0; iRow < rows; iRow++) {
            long[] thisRow = data[iRow];
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                if (BinaryUtils.getBoolean(thisRow, iColumn + longColumnsOffset)) {
                    LongUtils.xori(mulData[iRow], that.getLongArrayRow(iColumn));
                }
            }
        }
        return LongDenseBitMatrix.createFromDenseUncheck(thatColumns, mulData);
    }

    @Override
    public byte[] leftMultiply(final byte[] v) {
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(v, byteRows, rows));
        long[] longOutput = new long[longColumns];
        for (int iRow = 0; iRow < rows; iRow++) {
            if (BinaryUtils.getBoolean(v, iRow + byteRowsOffset)) {
                LongUtils.xori(longOutput, data[iRow]);
            }
        }
        return LongUtils.longArrayToByteArray(longOutput, byteColumns);
    }

    @Override
    public void leftMultiplyXori(byte[] v, byte[] t) {
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(v, byteRows, rows));
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(t, byteColumns, columns));
        for (int iRow = 0; iRow < rows; iRow++) {
            if (BinaryUtils.getBoolean(v, iRow + byteRowsOffset)) {
                BytesUtils.xori(t, getByteArrayRow(iRow));
            }
        }
    }

    /**
     * Left-multiplies a vector v (encoded as a long array), i.e., computes v·M.
     *
     * @param v the vector v (encoded as a long array).
     * @return v·M (encoded as a byte array).
     */
    public long[] leftMultiply(final long[] v) {
        Preconditions.checkArgument(LongUtils.isFixedReduceLongArray(v, longRows, rows));
        long[] output = new long[longColumns];
        for (int iRow = 0; iRow < rows; iRow++) {
            if (BinaryUtils.getBoolean(v, iRow + longRowsOffset)) {
                LongUtils.xori(output, data[iRow]);
            }
        }
        return output;
    }

    @Override
    public boolean[] leftMultiply(boolean[] v) {
        MathPreconditions.checkEqual("v.length", "rows", v.length, rows);
        long[] output = new long[longColumns];
        for (int iRow = 0; iRow < rows; iRow++) {
            if (v[iRow]) {
                LongUtils.xori(output, data[iRow]);
            }
        }
        return BinaryUtils.longArrayToBinary(output, columns);
    }

    @Override
    public void leftMultiplyXori(boolean[] v, boolean[] t) {
        MathPreconditions.checkEqual("v.length", "rows", v.length, rows);
        MathPreconditions.checkEqual("t.length", "columns", t.length, columns);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            if (v[rowIndex]) {
                for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                    t[columnIndex] ^= BinaryUtils.getBoolean(data[rowIndex], columnIndex + longColumnsOffset);
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
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                if (BinaryUtils.getBoolean(data[rowIndex], columnIndex + longColumnsOffset)) {
                    BytesUtils.xori(t[columnIndex], v[rowIndex]);
                }
            }
        }
    }

    @Override
    public DenseBitMatrix transpose(EnvType envType, boolean parallel) {
        byte[][] cByteData = getByteArrayData();
        // 调用TransBitMatrix实现转置，TransBitMatrix是按列表示的，所以设置时候要反过来
        TransBitMatrix cTransBitMatrix = TransBitMatrixFactory.createInstance(envType, columns, rows, parallel);
        for (int iRow = 0; iRow < rows; iRow++) {
            cTransBitMatrix.setColumn(iRow, cByteData[iRow]);
        }
        TransBitMatrix tTransBitMatrix = cTransBitMatrix.transpose();
        long[][] tData = new long[columns][longRows];
        for (int itRow = 0; itRow < columns; itRow++) {
            tData[itRow] = LongUtils.byteArrayToRoundLongArray(tTransBitMatrix.getColumn(itRow));
        }
        return LongDenseBitMatrix.createFromDenseUncheck(rows, tData);
    }

    @Override
    public DenseBitMatrix inverse() {
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        int size = rows;
        //noinspection UnnecessaryLocalVariable
        int longSize = longRows;
        int longSizeOffset = longRowsOffset;
        long[][] cData = LongUtils.clone(data);
        long[][] iData = new long[size][longSize];
        IntStream.range(0, size).forEach(i -> BinaryUtils.setBoolean(iData[i], i + longSizeOffset, true));
        // 利用初等变换计算逆矩阵。首先将左矩阵转换为上三角矩阵
        for (int p = 0; p < size; p++) {
            if (!BinaryUtils.getBoolean(cData[p], p + longSizeOffset)) {
                // 找到一个不为0的行
                int other = p + 1;
                while (other < size && !BinaryUtils.getBoolean(cData[other], p + longSizeOffset)) {
                    other++;
                }
                if (other >= size) {
                    throw new ArithmeticException("Cannot invert bit matrix");
                } else {
                    // 左侧矩阵行swap
                    long[] matrixRowTemp = cData[p];
                    cData[p] = cData[other];
                    cData[other] = matrixRowTemp;
                    // 右侧矩阵行swap
                    long[] inverseMatrixRowTemp = iData[p];
                    iData[p] = iData[other];
                    iData[other] = inverseMatrixRowTemp;
                }
            }
            // 左右侧矩阵高斯消元
            for (int i = p + 1; i < size; i++) {
                if (BinaryUtils.getBoolean(cData[i], p + longSizeOffset)) {
                    LongUtils.xori(cData[i], cData[p]);
                    LongUtils.xori(iData[i], iData[p]);
                }
            }
        }
        // 将左侧矩阵转为单位矩阵，此时右侧得到的矩阵就是左侧矩阵的逆矩阵
        for (int p = size - 1; p >= 0; p--) {
            for (int r = 0; r < p; r++) {
                if (BinaryUtils.getBoolean(cData[r], p + longSizeOffset)) {
                    // 如果有1的，则进行相加
                    LongUtils.xori(cData[r], cData[p]);
                    LongUtils.xori(iData[r], iData[p]);
                }
            }
        }
        return LongDenseBitMatrix.createFromDenseUncheck(size, iData);
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public byte[] getByteArrayRow(int iRow) {
        return LongUtils.longArrayToByteArray(data[iRow], byteColumns);
    }

    @Override
    public long[] getLongArrayRow(int iRow) {
        return data[iRow];
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
        return BinaryUtils.getBoolean(data[iRow], iColumn + longColumnsOffset);
    }

    @Override
    public byte[][] getByteArrayData() {
        return IntStream.range(0, rows)
            .mapToObj(this::getByteArrayRow)
            .toArray(byte[][]::new);
    }

    @Override
    public long[][] getLongArrayData() {
        return data;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        IntStream.range(0, rows)
            .forEach(iRow -> {
                BigInteger rowBigInteger = new BigInteger(1, getByteArrayRow(iRow));
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
        if (!(obj instanceof DenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        DenseBitMatrix that = (DenseBitMatrix) obj;
        return new EqualsBuilder().append(this.data, that.getLongArrayData()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(data).toHashCode();
    }
}
