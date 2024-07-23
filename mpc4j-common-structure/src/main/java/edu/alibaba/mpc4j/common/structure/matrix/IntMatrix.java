package edu.alibaba.mpc4j.common.structure.matrix;

import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * int matrix.
 *
 * @author Weiran Liu
 * @date 2024/7/5
 */
public class IntMatrix implements Matrix {
    /**
     * Decomposes the matrix by treating each element as a base-p element vector.
     *
     * @param matrix matrix.
     * @param p      base p.
     * @return decomposed matrix.
     */
    public static IntMatrix[] decompose(IntMatrix matrix, int p) {
        MathPreconditions.checkInRangeClosed("p", p, 2, Integer.MAX_VALUE);
        int size = (int) Math.ceil(Integer.SIZE / Math.log(p));
        int rows = matrix.getRows();
        IntVector[][] decomposeRowVectors = new IntVector[size][rows];
        for (int i = 0; i < rows; i++) {
            IntVector[] decomposedVector = IntVector.decompose(matrix.rowVectors[i], p);
            for (int k = 0; k < size; k++) {
                decomposeRowVectors[k][i] = decomposedVector[k];
            }
        }
        return IntStream.range(0, size)
            .mapToObj(k -> create(decomposeRowVectors[k]))
            .toArray(IntMatrix[]::new);
    }

    /**
     * Composes base-p matrices to a matrix.
     *
     * @param matrices decomposed matrices.
     * @param p        base p.
     * @return composed matrix.
     */
    public static IntMatrix compose(IntMatrix[] matrices, int p) {
        MathPreconditions.checkInRangeClosed("p", p, 2, Integer.MAX_VALUE);
        int size = (int) Math.ceil(Integer.SIZE / Math.log(p));
        MathPreconditions.checkEqual("matrices.length", "size", matrices.length, size);
        int rows = matrices[0].getRows();
        int columns = matrices[0].getColumns();
        IntVector[] composedRowVectors = IntStream.range(0, rows)
            .mapToObj(i -> {
                IntVector rowVector = IntVector.createZeros(columns);
                for (int k = 0; k < size; k++) {
                    rowVector.muli(p);
                    rowVector.addi(matrices[k].getRow(i));
                }
                return rowVector;
            })
            .toArray(IntVector[]::new);
        return create(composedRowVectors);
    }

    /**
     * Decomposes the matrix by treating each element as a byte vector.
     *
     * @param matrix matrix.
     * @return decomposed matrix.
     */
    public static IntMatrix[] decomposeToByteVector(IntMatrix matrix) {
        int size = Integer.SIZE / Byte.SIZE;
        int rows = matrix.getRows();
        IntVector[][] decomposeRowVectors = new IntVector[size][rows];
        for (int i = 0; i < rows; i++) {
            IntVector[] decomposedVector = IntVector.decomposeToByteVector(matrix.rowVectors[i]);
            for (int k = 0; k < size; k++) {
                decomposeRowVectors[k][i] = decomposedVector[k];
            }
        }
        return IntStream.range(0, size)
            .mapToObj(k -> IntMatrix.create(decomposeRowVectors[k]))
            .toArray(IntMatrix[]::new);
    }

    /**
     * Composes byte matrices to a matrix.
     *
     * @param matrices decomposed matrices.
     * @return composed matrix.
     */
    public static IntMatrix composeByteVector(IntMatrix[] matrices) {
        int size = Integer.SIZE / Byte.SIZE;
        MathPreconditions.checkEqual("matrices.length", "size", matrices.length, size);
        int rows = matrices[0].getRows();
        int columns = matrices[0].getColumns();
        IntVector[] composedRowVectors = IntStream.range(0, rows)
            .mapToObj(i -> {
                IntVector rowVector = IntVector.createZeros(columns);
                for (int k = 0; k < size; k++) {
                    rowVector.shiftLefti(Byte.SIZE);
                    rowVector.addi(matrices[k].getRow(i));
                }
                return rowVector;
            })
            .toArray(IntVector[]::new);
        return create(composedRowVectors);
    }

    /**
     * Creates a matrix.
     *
     * @param elements elements.
     * @return a matrix.
     */
    public static IntMatrix create(int[][] elements) {
        int rows = elements.length;
        MathPreconditions.checkPositive("rows", rows);
        int columns = elements[0].length;
        MathPreconditions.checkPositive("columns", columns);
        for (int[] row : elements) {
            MathPreconditions.checkEqual("rows", "row.length", rows, row.length);
        }
        IntMatrix matrix = new IntMatrix();
        matrix.rowVectors = Arrays.stream(elements)
            .map(IntVector::create)
            .toArray(IntVector[]::new);
        return matrix;
    }

    /**
     * Creates a matrix.
     *
     * @param rowVectors row vectors.
     * @return a matrix.
     */
    public static IntMatrix create(IntVector[] rowVectors) {
        int rows = rowVectors.length;
        MathPreconditions.checkPositive("rows", rows);
        int columns = rowVectors[0].getNum();
        MathPreconditions.checkPositive("columns", columns);
        for (IntVector rowVector : rowVectors) {
            MathPreconditions.checkEqual("columns", "row.length", columns, rowVector.getNum());
        }
        IntMatrix matrix = new IntMatrix();
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
    public static IntMatrix createRandom(int rows, int columns, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        IntMatrix matrix = new IntMatrix();
        matrix.rowVectors = IntStream.range(0, rows)
            .mapToObj(i -> IntVector.createRandom(columns, secureRandom))
            .toArray(IntVector[]::new);
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
    public static IntMatrix createRandom(int rows, int columns, byte[] seed) {
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
    public static IntMatrix createZeros(int rows, int columns) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        IntMatrix matrix = new IntMatrix();
        matrix.rowVectors = IntStream.range(0, rows)
            .mapToObj(i -> IntVector.createZeros(columns))
            .toArray(IntVector[]::new);
        return matrix;
    }

    /**
     * matrix
     */
    private IntVector[] rowVectors;

    /**
     * private constructor.
     */
    private IntMatrix() {
        // empty
    }

    @Override
    public IntMatrix copy() {
        IntVector[] copyRowVectors = IntStream.range(0, getRows())
            .mapToObj(i -> rowVectors[i].copy())
            .toArray(IntVector[]::new);
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
    public int get(int i, int j) {
        return rowVectors[i].getElement(j);
    }

    /**
     * Gets row.
     *
     * @param i row index.
     * @return row.
     */
    public IntVector getRow(int i) {
        return rowVectors[i];
    }

    /**
     * Gets elements.
     *
     * @return elements.
     */
    public int[][] getElements() {
        return IntStream.range(0, getRows())
            .mapToObj(i -> rowVectors[i].getElements())
            .toArray(int[][]::new);
    }

    /**
     * Sets element.
     *
     * @param i       row index.
     * @param j       column index.
     * @param element element.
     */
    public void set(int i, int j, int element) {
        rowVectors[i].setElement(j, element);
    }

    /**
     * Modulus each element by 2^l.
     *
     * @param l bit length.
     */
    public void module(int l) {
        for (IntVector rowVector : rowVectors) {
            rowVector.module(l);
        }
    }

    /**
     * Concatenates with that matrix. The two matrix must have the same columns.
     *
     * @param that that matrix.
     * @return concatenated matrix.
     */
    public IntMatrix concat(IntVector that) {
        MathPreconditions.checkEqual("this.columns", "that.columns", this.getColumns(), that.getNum());
        int currentRows = getRows();
        IntVector[] appendedRowVectors = IntStream.range(0, currentRows + 1)
            .mapToObj(i -> {
                if (i < currentRows) {
                    return rowVectors[i].copy();
                } else {
                    return that.copy();
                }
            })
            .toArray(IntVector[]::new);
        return create(appendedRowVectors);
    }

    /**
     * matrix addition.
     *
     * @param that that matrix.
     */
    public void addi(IntMatrix that) {
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
    public void subi(IntMatrix that) {
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
    public IntMatrix mul(IntMatrix that) {
        MathPreconditions.checkEqual("this.columns", "that.rows", this.getColumns(), that.getRows());
        int rows = getRows();
        IntVector[] mulRowVectors = IntStream.range(0, rows)
            .mapToObj(i -> that.leftMul(rowVectors[i]))
            .toArray(IntVector[]::new);
        return create(mulRowVectors);
    }

    /**
     * matrix transposition.
     *
     * @return result.
     */
    public IntMatrix transpose() {
        int rows = getRows();
        int columns = getColumns();
        IntVector[] columnVectors = IntStream.range(0, columns)
            .mapToObj(j -> {
                IntVector columnVector = IntVector.createZeros(rows);
                for (int i = 0; i < rows; i++) {
                    columnVector.setElement(i, rowVectors[i].getElement(j));
                }
                return columnVector;
            })
            .toArray(IntVector[]::new);
        return create(columnVectors);
    }

    /**
     * Left vector multiplication.
     *
     * @param vector vector.
     * @return result.
     */
    public IntVector leftMul(IntVector vector) {
        MathPreconditions.checkEqual("this.rows", "vector.length", this.getRows(), vector.getNum());
        int rows = getRows();
        int columns = getColumns();
        IntVector leftMulVector = IntVector.createZeros(columns);
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
        if (obj instanceof IntMatrix that) {
            return Arrays.equals(this.rowVectors, that.rowVectors);
        }
        return false;
    }
}
