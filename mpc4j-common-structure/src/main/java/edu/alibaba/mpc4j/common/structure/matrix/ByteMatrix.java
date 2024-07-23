package edu.alibaba.mpc4j.common.structure.matrix;

import edu.alibaba.mpc4j.common.structure.vector.ByteVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Byte Matrix.
 *
 * @author Weiran Liu
 * @date 2024/5/22
 */
public class ByteMatrix implements Matrix {
    /**
     * Creates a matrix.
     *
     * @param elements elements.
     * @return a matrix.
     */
    public static ByteMatrix create(byte[][] elements) {
        int rows = elements.length;
        MathPreconditions.checkPositive("rows", rows);
        int columns = elements[0].length;
        MathPreconditions.checkPositive("columns", columns);
        for (byte[] row : elements) {
            MathPreconditions.checkEqual("rows", "row.length", rows, row.length);
        }
        ByteMatrix matrix = new ByteMatrix();
        matrix.rowVectors = Arrays.stream(elements)
            .map(ByteVector::create)
            .toArray(ByteVector[]::new);
        return matrix;
    }

    /**
     * Creates a matrix.
     *
     * @param rowVectors row vectors.
     * @return a matrix.
     */
    public static ByteMatrix create(ByteVector[] rowVectors) {
        int rows = rowVectors.length;
        MathPreconditions.checkPositive("rows", rows);
        int columns = rowVectors[0].getNum();
        MathPreconditions.checkPositive("columns", columns);
        for (ByteVector rowVector : rowVectors) {
            MathPreconditions.checkEqual("columns", "row.length", columns, rowVector.getNum());
        }
        ByteMatrix matrix = new ByteMatrix();
        matrix.rowVectors = rowVectors;
        return matrix;
    }

    /**
     * Creates a random matrix.
     *
     * @param rows         rows.
     * @param columns      columns.
     * @param secureRandom random state.
     * @return a matrix.
     */
    public static ByteMatrix createRandom(int rows, int columns, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        ByteMatrix matrix = new ByteMatrix();
        matrix.rowVectors = IntStream.range(0, rows)
            .mapToObj(i -> ByteVector.createRandom(columns, secureRandom))
            .toArray(ByteVector[]::new);
        return matrix;
    }

    /**
     * Creates a random matrix.
     *
     * @param rows    rows.
     * @param columns columns.
     * @param seed    seed.
     * @return a matrix.
     */
    public static ByteMatrix createRandom(int rows, int columns, byte[] seed) {
        SecureRandom secureRandom = CommonUtils.createSeedSecureRandom();
        secureRandom.setSeed(seed);
        return createRandom(rows, columns, secureRandom);
    }

    /**
     * Creates an all-zero matrix.
     *
     * @param rows    rows.
     * @param columns columns.
     * @return a matrix.
     */
    public static ByteMatrix createZeros(int rows, int columns) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        ByteMatrix matrix = new ByteMatrix();
        matrix.rowVectors = IntStream.range(0, rows)
            .mapToObj(i -> ByteVector.createZeros(columns))
            .toArray(ByteVector[]::new);
        return matrix;
    }

    /**
     * matrix
     */
    private ByteVector[] rowVectors;

    /**
     * private constructor.
     */
    private ByteMatrix() {
        // empty
    }

    @Override
    public ByteMatrix copy() {
        ByteVector[] copyRowVectors = IntStream.range(0, getRows())
            .mapToObj(i -> rowVectors[i].copy())
            .toArray(ByteVector[]::new);
        return create(copyRowVectors);
    }

    @Override
    public int getRows() {
        return rowVectors.length;
    }

    @Override
    public int getColumns() {
        // we ensure rows > 0 so that this must not throw an exception.
        return rowVectors[0].getNum();
    }

    /**
     * Gets element.
     *
     * @param i row index.
     * @param j column index.
     * @return element.
     */
    public byte get(int i, int j) {
        return rowVectors[i].getElement(j);
    }

    /**
     * Gets row.
     *
     * @param i row index.
     * @return row.
     */
    public ByteVector getRow(int i) {
        return rowVectors[i];
    }

    /**
     * Gets elements.
     *
     * @return elements.
     */
    public byte[][] getElements() {
        return IntStream.range(0, getRows())
            .mapToObj(i -> rowVectors[i].getElements())
            .toArray(byte[][]::new);
    }

    /**
     * Sets element.
     *
     * @param i       row index.
     * @param j       column index.
     * @param element element.
     */
    public void set(int i, int j, byte element) {
        rowVectors[i].setElement(j, element);
    }

    /**
     * Concatenates with that matrix. The two matrix must have the same columns.
     *
     * @param that that matrix.
     * @return concatenated matrix.
     */
    public ByteMatrix concat(ByteMatrix that) {
        MathPreconditions.checkEqual("this.columns", "that.columns", this.getColumns(), that.getColumns());
        int currentRows = getRows();
        int appendRows = that.getRows();
        ByteVector[] appendedRowVectors = IntStream.range(0, currentRows + appendRows)
            .mapToObj(i -> {
                if (i < currentRows) {
                    return rowVectors[i].copy();
                } else {
                    return that.getRow(i - currentRows).copy();
                }
            })
            .toArray(ByteVector[]::new);
        return create(appendedRowVectors);
    }

    /**
     * matrix addition.
     *
     * @param that that matrix.
     */
    public void addi(ByteMatrix that) {
        MathPreconditions.checkEqual("this.rows", "that.rows", this.getRows(), that.getRows());
        MathPreconditions.checkEqual("this.columns", "that.columns", this.getColumns(), that.getColumns());
        int rows = getRows();
        for (int i = 0; i < rows; i++) {
            rowVectors[i].addi(that.getRow(i));
        }
    }

    /**
     * matrix subtraction.
     *
     * @param that that matrix.
     */
    public void subi(ByteMatrix that) {
        MathPreconditions.checkEqual("this.rows", "that.rows", this.getRows(), that.getRows());
        MathPreconditions.checkEqual("this.columns", "that.columns", this.getColumns(), that.getColumns());
        int rows = getRows();
        for (int i = 0; i < rows; i++) {
            rowVectors[i].subi(that.getRow(i));
        }
    }

    /**
     * matrix multiplication.
     *
     * @param that that matrix.
     * @return result.
     */
    public ByteMatrix mul(ByteMatrix that) {
        MathPreconditions.checkEqual("this.columns", "that.rows", this.getColumns(), that.getRows());
        int rows = getRows();
        ByteVector[] mulRowVectors = IntStream.range(0, rows)
            .mapToObj(i -> that.leftMul(rowVectors[i]))
            .toArray(ByteVector[]::new);
        return create(mulRowVectors);
    }

    /**
     * matrix transposition.
     *
     * @return result.
     */
    public ByteMatrix transpose() {
        int rows = getRows();
        int columns = getColumns();
        ByteVector[] columnVectors = IntStream.range(0, columns)
            .mapToObj(j -> {
                ByteVector columnVector = ByteVector.createZeros(rows);
                for (int i = 0; i < rows; i++) {
                    columnVector.setElement(i, rowVectors[i].getElement(j));
                }
                return columnVector;
            })
            .toArray(ByteVector[]::new);
        return create(columnVectors);
    }

    /**
     * Left vector multiplication.
     *
     * @param vector vector.
     * @return result.
     */
    public ByteVector leftMul(ByteVector vector) {
        MathPreconditions.checkEqual("this.rows", "vector.length", this.getRows(), vector.getNum());
        int rows = getRows();
        int columns = getColumns();
        ByteVector leftMulVector = ByteVector.createZeros(columns);
        for (int i = 0; i < rows; i++) {
            leftMulVector.addi(rowVectors[i].mul(vector.getElement(i)));
        }
        return leftMulVector;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(rowVectors).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ByteMatrix that) {
            return Arrays.equals(this.rowVectors, that.rowVectors);
        }
        return false;
    }
}
