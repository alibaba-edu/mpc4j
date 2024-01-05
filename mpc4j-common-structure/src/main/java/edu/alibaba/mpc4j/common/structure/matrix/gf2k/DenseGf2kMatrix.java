package edu.alibaba.mpc4j.common.structure.matrix.gf2k;

import cc.redberry.rings.util.ArraysUtil;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.structure.matrix.zp.ZpMatrix;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * dense GF(2^κ) matrix.
 *
 * @author Weiran Liu
 * @date 2023/7/4
 */
public class DenseGf2kMatrix implements Gf2kMatrix {
    /**
     * Returns if the given matrix is an identity matrix.
     *
     * @param matrix matrix.
     * @return true if it is an identity matrix.
     */
    public static boolean isIdentity(DenseGf2kMatrix matrix) {
        if (matrix.rows != matrix.columns) {
            return false;
        }
        int size = matrix.rows;
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (row == column) {
                    if (!matrix.gf2k.isOne(matrix.data[row][column])) {
                        return false;
                    }
                } else {
                    if (!matrix.gf2k.isZero(matrix.data[row][column])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Creates a random matrix.
     *
     * @param gf2k         GF2K instance.
     * @param rows         rows.
     * @param columns      columns.
     * @param secureRandom random state.
     * @return a random matrix.
     */
    public static DenseGf2kMatrix createRandom(Gf2k gf2k, int rows, int columns, SecureRandom secureRandom) {
        DenseGf2kMatrix matrix = new DenseGf2kMatrix(gf2k, rows, columns);
        matrix.data = IntStream.range(0, rows)
            .mapToObj(iRow ->
                IntStream.range(0, columns)
                    .mapToObj(iColumn -> gf2k.createRandom(secureRandom))
                    .toArray(byte[][]::new)
            )
            .toArray(byte[][][]::new);
        return matrix;
    }

    /**
     * Creates an identity matrix.
     *
     * @param gf2k GF2K instance.
     * @param size size.
     * @return an identity matrix.
     */
    public static DenseGf2kMatrix createIdentity(Gf2k gf2k, int size) {
        DenseGf2kMatrix matrix = new DenseGf2kMatrix(gf2k, size, size);
        matrix.data = createIdentityData(gf2k, size);
        return matrix;
    }

    private static byte[][][] createIdentityData(Gf2k gf2k, int size) {
        MathPreconditions.checkPositive("size", size);
        byte[][][] matrix = new byte[size][size][];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (row == column) {
                    matrix[row][column] = gf2k.createOne();
                } else {
                    matrix[row][column] = gf2k.createZero();
                }
            }
        }
        return matrix;
    }

    /**
     * Creates a matrix.
     *
     * @param gf2k GF2K instance.
     * @param data the matrix data.
     * @return a matrix.
     */
    public static DenseGf2kMatrix fromDense(Gf2k gf2k, byte[][][] data) {
        MathPreconditions.checkPositive("rows", data.length);
        int rows = data.length;
        MathPreconditions.checkPositive("columns", data[0].length);
        int columns = data[0].length;
        DenseGf2kMatrix matrix = new DenseGf2kMatrix(gf2k, rows, columns);
        // verify square and valid elements
        for (int iRow = 0; iRow < rows; iRow++) {
            MathPreconditions.checkEqual(iRow + "-th row length", "size", data[iRow].length, columns);
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                Preconditions.checkArgument(gf2k.validateElement(data[iRow][iColumn]));
            }
        }
        matrix.data = data;
        return matrix;
    }

    /**
     * Creates a matrix without validate check.
     *
     * @param gf2k GF2K instance.
     * @param data the matrix data.
     * @return a matrix.
     */
    private static DenseGf2kMatrix fromDenseUncheck(Gf2k gf2k, byte[][][] data) {
        int rows = data.length;
        int columns = data[0].length;
        DenseGf2kMatrix matrix = new DenseGf2kMatrix(gf2k, rows, columns);
        matrix.data = data;
        return matrix;
    }

    /**
     * GF2K instance
     */
    protected final Gf2k gf2k;
    /**
     * number of rows
     */
    private final int rows;
    /**
     * number of columns.
     */
    private final int columns;
    /**
     * data
     */
    private byte[][][] data;
    /**
     * parallel
     */
    private boolean parallel;

    protected DenseGf2kMatrix(Gf2k gf2k, int rows, int columns) {
        this.gf2k = gf2k;
        this.rows = rows;
        this.columns = columns;
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public DenseGf2kMatrix copy() {
        return DenseGf2kMatrix.fromDenseUncheck(gf2k, BytesUtils.clone(data));
    }

    /**
     * Adds a matrix.
     *
     * @param that that matrix.
     * @return result.
     */
    public DenseGf2kMatrix add(Gf2kMatrix that) {
        Preconditions.checkArgument(this.gf2k.equals(that.getGf2k()));
        MathPreconditions.checkEqual("this.rows", "that.rows", this.rows, that.getRows());
        MathPreconditions.checkEqual("this.columns", "that.columns", this.columns, that.getColumns());
        byte[][][] addData = new byte[rows][columns][];
        IntStream rowIntStream = IntStream.range(0, rows);
        rowIntStream = parallel ? rowIntStream.parallel() : rowIntStream;
        rowIntStream.forEach(iRow -> {
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                addData[iRow][iColumn] = gf2k.add(data[iRow][iColumn], that.getEntry(iRow, iColumn));
            }
        });
        return DenseGf2kMatrix.fromDenseUncheck(gf2k, addData);
    }

    /**
     * Multiplies a matrix.
     *
     * @param that that matrix.
     * @return result.
     */
    public DenseGf2kMatrix multiply(Gf2kMatrix that) {
        int thatColumns = that.getColumns();
        MathPreconditions.checkEqual("this.columns", "that.rows", this.columns, that.getRows());
        byte[][][] mulData = new byte[rows][thatColumns][];
        IntStream rowIntStream = IntStream.range(0, rows);
        rowIntStream = parallel ? rowIntStream.parallel() : rowIntStream;
        rowIntStream.forEach(iRow -> {
            for (int iColumn = 0; iColumn < thatColumns; iColumn++) {
                mulData[iRow][iColumn] = gf2k.createZero();
                for (int index = 0; index < columns; index++) {
                   gf2k.addi(mulData[iRow][iColumn], gf2k.mul(data[iRow][index], that.getEntry(index, iColumn)));
                }
            }
        });
        return DenseGf2kMatrix.fromDenseUncheck(gf2k, mulData);
    }

    /**
     * Transposes the matrix.
     *
     * @return result.
     */
    public DenseGf2kMatrix transpose() {
        byte[][][] tData = new byte[columns][rows][];
        IntStream rowIntStream = IntStream.range(0, rows);
        rowIntStream = parallel ? rowIntStream.parallel() : rowIntStream;
        rowIntStream.forEach(iRow -> {
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                tData[iColumn][iRow] = BytesUtils.clone(data[iRow][iColumn]);
            }
        });
        return DenseGf2kMatrix.fromDenseUncheck(gf2k, tData);
    }

    /**
     * Inverses the matrix.
     *
     * @return the inverse matrix.
     * @throws IllegalArgumentException if the matrix is not square.
     */
    public DenseGf2kMatrix inverse() {
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        int size = rows;
        // copy the matrix, since the inverse procedure modifies the matrix.
        byte[][][] cData = BytesUtils.clone(data);
        // init the inverse matrix as the identity matrix.
        byte[][][] iData = createIdentityData(gf2k, size);
        // transform the left matrix to the upper-triangular matrix
        for (int column = 0; column < size; column++) {
            //noinspection UnnecessaryLocalVariable
            int row = column;
            if (gf2k.isZero(cData[row][column])) {
                // find a non-zero row
                int max = row;
                for (int iRow = row + 1; iRow < size; ++iRow) {
                    if (!gf2k.isZero(cData[iRow][column])) {
                        max = iRow;
                        break;
                    }
                }
                // left swap
                ArraysUtil.swap(cData, row, max);
                // right swap
                ArraysUtil.swap(iData, row, max);
                // singular
                if (gf2k.isZero(cData[row][column])) {
                    throw new ArithmeticException("Cannot inverse the matrix");
                }
            }
            // Gaussian elimination
            for (int iRow = row + 1; iRow < size; ++iRow) {
                byte[] alpha = gf2k.div(cData[iRow][column], cData[row][column]);
                if (!gf2k.isZero(alpha)) {
                    // left elimination
                    for (int iColumn = column; iColumn < size; ++iColumn) {
                        gf2k.subi(cData[iRow][iColumn], gf2k.mul(cData[row][iColumn], alpha));
                    }
                    // right elimination
                    for (int iColumn = 0; iColumn < size; iColumn++) {
                        gf2k.subi(iData[iRow][iColumn], gf2k.mul(iData[row][iColumn], alpha));
                    }
                }
            }
        }
        // back substitution
        for (int column = size - 1; column >= 0; column--) {
            //noinspection UnnecessaryLocalVariable
            int row = column;
            // row normalization, i.e., set matrix[row][column] = 1
            byte[] val = BytesUtils.clone(cData[row][column]);
            assert !gf2k.isZero(val);
            if (!gf2k.isOne(val)) {
                byte[] valInv = gf2k.inv(val);
                // left normalization
                for (int iColumn = column; iColumn < size; iColumn++) {
                    gf2k.muli(cData[row][iColumn], valInv);
                }
                // right normalization
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    gf2k.muli(iData[row][iColumn], valInv);
                }
            }
            // substitution
            for (int iRow = 0; iRow < row; iRow++) {
                if (!gf2k.isZero(cData[iRow][column])) {
                    byte[] alpha = BytesUtils.clone(cData[iRow][column]);
                    // left substitution
                    for (int iColumn = column; iColumn < size; iColumn++) {
                        gf2k.subi(cData[iRow][iColumn], gf2k.mul(cData[row][iColumn], alpha));
                    }
                    // right substitution
                    for (int iColumn = 0; iColumn < size; iColumn++) {
                        gf2k.subi(iData[iRow][iColumn], gf2k.mul(iData[row][iColumn], alpha));
                    }
                }
            }
        }
        return DenseGf2kMatrix.fromDenseUncheck(gf2k, iData);
    }

    @Override
    public Gf2k getGf2k() {
        return gf2k;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public byte[][] getRow(int iRow) {
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
    public byte[] getEntry(int iRow, int iColumn) {
        return data[iRow][iColumn];
    }

    @Override
    public byte[][][] getData() {
        return data;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(gf2k).append(data).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ZpMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ZpMatrix that = (ZpMatrix) obj;
        return new EqualsBuilder()
            .append(this.gf2k, that.getZp())
            .append(this.data, that.getData())
            .isEquals();
    }
}
