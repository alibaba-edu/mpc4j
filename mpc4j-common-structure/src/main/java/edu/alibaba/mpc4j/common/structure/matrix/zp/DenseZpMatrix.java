package edu.alibaba.mpc4j.common.structure.matrix.zp;

import cc.redberry.rings.util.ArraysUtil;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * BigInteger dense Zp matrix.
 *
 * @author Weiran Liu
 * @date 2023/6/19
 */
public class DenseZpMatrix implements ZpMatrix {
    /**
     * Returns if the given matrix is an identity matrix.
     *
     * @param matrix matrix.
     * @return true if it is an identity matrix.
     */
    public static boolean isIdentity(DenseZpMatrix matrix) {
        if (matrix.rows != matrix.columns) {
            return false;
        }
        int size = matrix.rows;
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (row == column) {
                    if (!matrix.zp.isOne(matrix.data[row][column])) {
                        return false;
                    }
                } else {
                    if (!matrix.zp.isZero(matrix.data[row][column])) {
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
     * @param zp           Zp instance.
     * @param rows         rows.
     * @param columns      columns.
     * @param secureRandom random state.
     * @return a random matrix.
     */
    public static DenseZpMatrix createRandom(Zp zp, int rows, int columns, SecureRandom secureRandom) {
        DenseZpMatrix matrix = new DenseZpMatrix(zp, rows, columns);
        matrix.data = IntStream.range(0, rows)
            .mapToObj(iRow ->
                IntStream.range(0, columns)
                    .mapToObj(iColumn -> zp.createRandom(secureRandom))
                    .toArray(BigInteger[]::new)
            )
            .toArray(BigInteger[][]::new);
        return matrix;
    }

    /**
     * Creates an identity matrix.
     *
     * @param zp   Zp instance.
     * @param size size.
     * @return an identity matrix.
     */
    public static DenseZpMatrix createIdentity(Zp zp, int size) {
        DenseZpMatrix matrix = new DenseZpMatrix(zp, size, size);
        matrix.data = createIdentityData(zp, size);
        return matrix;
    }

    private static BigInteger[][] createIdentityData(Zp zp, int size) {
        MathPreconditions.checkPositive("size", size);
        BigInteger[][] matrix = new BigInteger[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (row == column) {
                    matrix[row][column] = zp.createOne();
                } else {
                    matrix[row][column] = zp.createZero();
                }
            }
        }
        return matrix;
    }

    /**
     * Creates a matrix.
     *
     * @param zp   Zp instance.
     * @param data the matrix data.
     * @return a matrix.
     */
    public static DenseZpMatrix fromDense(Zp zp, BigInteger[][] data) {
        MathPreconditions.checkPositive("rows", data.length);
        int rows = data.length;
        MathPreconditions.checkPositive("columns", data[0].length);
        int columns = data[0].length;
        DenseZpMatrix matrix = new DenseZpMatrix(zp, rows, columns);
        // verify square and valid elements
        for (int iRow = 0; iRow < rows; iRow++) {
            MathPreconditions.checkEqual(iRow + "-th row length", "size", data[iRow].length, columns);
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                Preconditions.checkArgument(zp.validateElement(data[iRow][iColumn]));
            }
        }
        matrix.data = data;
        return matrix;
    }

    /**
     * Creates a matrix without validate check.
     *
     * @param zp   Zp instance.
     * @param data the matrix data.
     * @return a matrix.
     */
    private static DenseZpMatrix fromDenseUncheck(Zp zp, BigInteger[][] data) {
        int rows = data.length;
        int columns = data[0].length;
        DenseZpMatrix matrix = new DenseZpMatrix(zp, rows, columns);
        matrix.data = data;
        return matrix;
    }

    /**
     * Zp instance
     */
    protected final Zp zp;
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
    private BigInteger[][] data;
    /**
     * parallel
     */
    private boolean parallel;

    protected DenseZpMatrix(Zp zp, int rows, int columns) {
        this.zp = zp;
        this.rows = rows;
        this.columns = columns;
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public DenseZpMatrix copy() {
        return DenseZpMatrix.fromDenseUncheck(zp, BigIntegerUtils.clone(data));
    }

    /**
     * Adds a matrix.
     *
     * @param that that matrix.
     * @return result.
     */
    public DenseZpMatrix add(ZpMatrix that) {
        Preconditions.checkArgument(this.zp.equals(that.getZp()));
        MathPreconditions.checkEqual("this.rows", "that.rows", this.rows, that.getRows());
        MathPreconditions.checkEqual("this.columns", "that.columns", this.columns, that.getColumns());
        BigInteger[][] addData = new BigInteger[rows][columns];
        IntStream rowIntStream = IntStream.range(0, rows);
        rowIntStream = parallel ? rowIntStream.parallel() : rowIntStream;
        rowIntStream.forEach(iRow -> {
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                addData[iRow][iColumn] = zp.add(data[iRow][iColumn], that.getEntry(iRow, iColumn));
            }
        });
        return DenseZpMatrix.fromDenseUncheck(zp, addData);
    }

    /**
     * Multiplies a matrix.
     *
     * @param that that matrix.
     * @return result.
     */
    public DenseZpMatrix multiply(ZpMatrix that) {
        int thatColumns = that.getColumns();
        MathPreconditions.checkEqual("this.columns", "that.rows", this.columns, that.getRows());
        BigInteger[][] mulData = new BigInteger[rows][thatColumns];
        IntStream rowIntStream = IntStream.range(0, rows);
        rowIntStream = parallel ? rowIntStream.parallel() : rowIntStream;
        rowIntStream.forEach(iRow -> {
            for (int iColumn = 0; iColumn < thatColumns; iColumn++) {
                mulData[iRow][iColumn] = zp.createZero();
                for (int index = 0; index < columns; index++) {
                    mulData[iRow][iColumn] = zp.add(mulData[iRow][iColumn], zp.mul(data[iRow][index], that.getEntry(index, iColumn)));
                }
            }
        });
        return DenseZpMatrix.fromDenseUncheck(zp, mulData);
    }

    /**
     * Transposes the matrix.
     *
     * @return result.
     */
    public DenseZpMatrix transpose() {
        BigInteger[][] tData = new BigInteger[columns][rows];
        IntStream rowIntStream = IntStream.range(0, rows);
        rowIntStream = parallel ? rowIntStream.parallel() : rowIntStream;
        rowIntStream.forEach(iRow -> {
            for (int iColumn = 0; iColumn < columns; iColumn++) {
                tData[iColumn][iRow] = data[iRow][iColumn];
            }
        });
        return DenseZpMatrix.fromDenseUncheck(zp, tData);
    }

    /**
     * Inverses the matrix.
     *
     * @return the inverse matrix.
     * @throws IllegalArgumentException if the matrix is not square.
     */
    public DenseZpMatrix inverse() {
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        int size = rows;
        // copy the matrix, since the inverse procedure modifies the matrix.
        BigInteger[][] cData = BigIntegerUtils.clone(data);
        // init the inverse matrix as the identity matrix.
        BigInteger[][] iData = createIdentityData(zp, size);
        // transform the left matrix to the upper-triangular matrix
        for (int column = 0; column < size; column++) {
            //noinspection UnnecessaryLocalVariable
            int row = column;
            if (zp.isZero(cData[row][column])) {
                // find a non-zero row
                int max = row;
                for (int iRow = row + 1; iRow < size; ++iRow) {
                    if (!zp.isZero(cData[iRow][column])) {
                        max = iRow;
                        break;
                    }
                }
                // left swap
                ArraysUtil.swap(cData, row, max);
                // right swap
                ArraysUtil.swap(iData, row, max);
                // singular
                if (zp.isZero(cData[row][column])) {
                    throw new ArithmeticException("Cannot inverse the matrix");
                }
            }
            // Gaussian elimination
            for (int iRow = row + 1; iRow < size; ++iRow) {
                BigInteger alpha = zp.div(cData[iRow][column], cData[row][column]);
                if (!zp.isZero(alpha)) {
                    // left elimination
                    for (int iColumn = column; iColumn < size; ++iColumn) {
                        cData[iRow][iColumn] = zp.sub(cData[iRow][iColumn], zp.mul(cData[row][iColumn], alpha));
                    }
                    // right elimination
                    for (int iColumn = 0; iColumn < size; iColumn++) {
                        iData[iRow][iColumn] = zp.sub(iData[iRow][iColumn], zp.mul(iData[row][iColumn], alpha));
                    }
                }
            }
        }
        // back substitution
        for (int column = size - 1; column >= 0; column--) {
            //noinspection UnnecessaryLocalVariable
            int row = column;
            // row normalization, i.e., set matrix[row][column] = 1
            BigInteger val = cData[row][column];
            assert !zp.isZero(val);
            if (!zp.isOne(val)) {
                BigInteger valInv = zp.inv(val);
                // left normalization
                for (int iColumn = column; iColumn < size; iColumn++) {
                    cData[row][iColumn] = zp.mul(cData[row][iColumn], valInv);
                }
                // right normalization
                for (int iColumn = 0; iColumn < size; iColumn++) {
                    iData[row][iColumn] = zp.mul(iData[row][iColumn], valInv);
                }
            }
            // substitution
            for (int iRow = 0; iRow < row; iRow++) {
                if (!zp.isZero(cData[iRow][column])) {
                    BigInteger alpha = cData[iRow][column];
                    // left substitution
                    for (int iColumn = column; iColumn < size; iColumn++) {
                        cData[iRow][iColumn] = zp.sub(cData[iRow][iColumn], zp.mul(cData[row][iColumn], alpha));
                    }
                    // right substitution
                    for (int iColumn = 0; iColumn < size; iColumn++) {
                        iData[iRow][iColumn] = zp.sub(iData[iRow][iColumn], zp.mul(iData[row][iColumn], alpha));
                    }
                }
            }
        }
        return DenseZpMatrix.fromDenseUncheck(zp, iData);
    }

    @Override
    public Zp getZp() {
        return zp;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public BigInteger[] getRow(int iRow) {
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
    public BigInteger getEntry(int iRow, int iColumn) {
        return data[iRow][iColumn];
    }

    @Override
    public BigInteger[][] getData() {
        return data;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(zp).append(data).hashCode();
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
            .append(this.zp, that.getZp())
            .append(this.data, that.getData())
            .isEquals();
    }
}
