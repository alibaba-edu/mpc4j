package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * naive sparse bit matrix
 *
 * @author Hanwen Feng
 * @date 2022/09/20
 */
public class NaiveSparseBitMatrix implements SparseBitMatrix {
    /**
     * Creates from column bit vector lists.
     *
     * @param columnList column bit vector list.
     * @return a sparse bit matrix.
     */
    public static NaiveSparseBitMatrix createFromColumnList(ArrayList<SparseBitVector> columnList) {
        int columns = columnList.size();
        MathPreconditions.checkPositive("columns", columnList.size());
        int rows = columnList.get(0).getBitNum();
        for (SparseBitVector bitVector : columnList) {
            MathPreconditions.checkEqual("rows", "vector.bitNum", rows, bitVector.getBitNum());
        }
        NaiveSparseBitMatrix sparseBitMatrix = new NaiveSparseBitMatrix(rows, columns);
        sparseBitMatrix.columnList = columnList;
        return sparseBitMatrix;
    }

    /**
     * Creates from cyclic bit vector.
     *
     * @param rows                 number of rows.
     * @param columns              number of columns.
     * @param firstColumnPositions positions in the first column.
     * @return a sparse bit matrix.
     */
    public static NaiveSparseBitMatrix createFromCyclic(int rows, int columns, int[] firstColumnPositions) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        NaiveSparseBitMatrix sparseBitMatrix = new NaiveSparseBitMatrix(rows, columns);
        SparseBitVector cyclicBitVector = SparseBitVector.create(firstColumnPositions, rows);
        // creates the column bit vector list
        sparseBitMatrix.columnList = new ArrayList<>();
        sparseBitMatrix.columnList.ensureCapacity(columns);
        // cyclic shift the bit vector
        SparseBitVector tempVector = cyclicBitVector.copy();
        for (int iColumn = 0; iColumn < columns; iColumn++) {
            sparseBitMatrix.columnList.add(tempVector);
            tempVector = tempVector.cyclicShiftRight();
        }
        return sparseBitMatrix;
    }

    /**
     * Creates a random sparse bit matrix.
     *
     * @param rows         number of rows.
     * @param columns      number of columns.
     * @param weight       weight, i.e., number of 1's in each column.
     * @param secureRandom the random state.
     * @return a random sparse bit matrix.
     */
    public static NaiveSparseBitMatrix createRandom(int rows, int columns, int weight, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        MathPreconditions.checkNonNegativeInRangeClosed("weight", weight, rows);
        ArrayList<SparseBitVector> colsList = IntStream.range(0, columns)
            .mapToObj(colIndex -> SparseBitVector.createRandom(weight, rows, secureRandom))
            .collect(Collectors.toCollection(ArrayList::new));
        return NaiveSparseBitMatrix.createFromColumnList(colsList);
    }

    /**
     * sparse bit vectors in each column
     */
    protected ArrayList<SparseBitVector> columnList;
    /**
     * number of rows
     */
    protected final int rows;
    /**
     * number of columns
     */
    protected final int columns;
    /**
     * parallel computation
     */
    protected boolean parallel;

    /**
     * private constructor.
     */
    protected NaiveSparseBitMatrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        parallel = false;
    }

    /**
     * Gets parallel operations.
     *
     * @return parallel operations.
     */
    public boolean getParallel() {
        return parallel;
    }

    /**
     * Sets parallel operations.
     *
     * @param parallel parallel operations.
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * Adds the two sparse bit matrix.
     *
     * @param that that sparse bit matrix.
     * @return the added result.
     */
    public NaiveSparseBitMatrix xor(NaiveSparseBitMatrix that) {
        MathPreconditions.checkEqual("this.rows", "that.rows", this.rows, that.rows);
        MathPreconditions.checkEqual("this.columns", "that.columns", this.columns, that.columns);
        IntStream iColumnStream = IntStream.range(0, columns);
        iColumnStream = parallel ? iColumnStream.parallel() : iColumnStream;
        ArrayList<SparseBitVector> addColumnList = iColumnStream
            .mapToObj(index -> columnList.get(index).xor(that.columnList.get(index)))
            .collect(Collectors.toCollection(ArrayList::new));
        return createFromColumnList(addColumnList);
    }

    /**
     * creates a sub-matrix. The rows in the sub-matrix are in range [fromRowIndex, toRowIndex), and the columns in
     * the sub-matrix are in range [fromColumnIndex, toColumnIndex).
     *
     * @param fromColumnIndex from column index.
     * @param toColumnIndex   to column index.
     * @param fromRowIndex    from row index.
     * @param toRowIndex      to row index.
     * @return the sub-matrix.
     */
    public NaiveSparseBitMatrix subMatrix(int fromColumnIndex, int toColumnIndex, int fromRowIndex, int toRowIndex) {
        MathPreconditions.checkNonNegativeInRangeClosed("toColumnIndex", toColumnIndex, columns);
        MathPreconditions.checkNonNegativeInRange("fromColumnIndex", fromColumnIndex, toColumnIndex);
        MathPreconditions.checkNonNegativeInRangeClosed("toRowIndex", toRowIndex, rows);
        MathPreconditions.checkNonNegativeInRange("fromRowIndex", fromRowIndex, toRowIndex);
        IntStream iColumnStream = IntStream.range(fromColumnIndex, toColumnIndex);
        iColumnStream = parallel ? iColumnStream.parallel() : iColumnStream;
        ArrayList<SparseBitVector> subColumnList = iColumnStream
            .mapToObj(index -> columnList.get(index).sub(fromRowIndex, toRowIndex))
            .collect(Collectors.toCollection(ArrayList::new));
        return createFromColumnList(subColumnList);
    }

    protected ArrayList<SparseBitVector> getRowList() {
        // init the row list
        ArrayList<TIntArrayList> rowPositionList = IntStream.range(0, rows)
            .mapToObj(iRow -> new TIntArrayList(columns))
            .collect(Collectors.toCollection(ArrayList::new));
        // assign the row list
        for (int iColumn = 0; iColumn < columns; iColumn++) {
            int[] positions = columnList.get(iColumn).getPositions();
            for (int iRow : positions) {
                rowPositionList.get(iRow).add(iColumn);
            }
        }
        Stream<TIntArrayList> rowPositionStream = rowPositionList.stream();
        rowPositionStream = parallel ? rowPositionStream.parallel() : rowPositionStream;
        return rowPositionStream
            .map(tIntArrayList -> SparseBitVector.create(tIntArrayList, columns))
            .collect(Collectors.toCollection(ArrayList<SparseBitVector>::new));
    }

    /**
     * Transposes a matrix.
     *
     * @return result.
     */
    public NaiveSparseBitMatrix transpose() {
        ArrayList<SparseBitVector> rowList = getRowList();
        return createFromColumnList(rowList);
    }

    @Override
    public DenseBitMatrix toDense() {
        ArrayList<SparseBitVector> rowList = getRowList();
        byte[][] byteArrays = rowList.stream()
            .map(SparseBitVector::toByteArray)
            .toArray(byte[][]::new);
        return ByteDenseBitMatrix.createFromDense(columns, byteArrays);
    }

    @Override
    public DenseBitMatrix transposeDense() {
        IntStream iColumnStream = IntStream.range(0, columns);
        iColumnStream = parallel ? iColumnStream.parallel() : iColumnStream;
        int[][] positions = iColumnStream
            .mapToObj(iColumn -> columnList.get(iColumn).getPositions())
            .toArray(int[][]::new);
        return ByteDenseBitMatrix.createFromSparse(rows, positions);
    }

    /**
     * Transposes the matrix M, and then multiplies the dense matrix D, i.e., computes M^T·D.
     *
     * @param denseBitMatrix the dense bit matrix D.
     * @return the result.
     */
    public DenseBitMatrix transposeMultiply(DenseBitMatrix denseBitMatrix) {
        MathPreconditions.checkEqual("rows", "D.rows", rows, denseBitMatrix.getRows());
        // treat the column of M as row, and bit-wise xor.
        byte[][] result = lExtMul(denseBitMatrix.getByteArrayData());
        return ByteDenseBitMatrix.createFromDense(denseBitMatrix.getColumns(), result);
    }

    /**
     * to extreme sparse bit matrix.
     *
     * @return extreme sparse bit matrix.
     */
    public ExtremeSparseBitMatrix toExtremeSparseBitMatrix() {
        ArrayList<Integer> indexList = new ArrayList<>();
        ArrayList<SparseBitVector> nColsList = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            if (columnList.get(i).getSize() != 0) {
                nColsList.add(columnList.get(i));
                indexList.add(i);
            }
        }
        int[] nonEmptyIndex = indexList.stream().mapToInt(k -> k).toArray();
        return ExtremeSparseBitMatrix.createUncheck(rows, columns, nonEmptyIndex, nColsList);
    }

    @Override
    public boolean[] lmul(boolean[] x) {
        // validation check for x will be done during the computation.
        boolean[] outputs = new boolean[columns];
        lmulAddi(x, outputs);
        return outputs;
    }

    @Override
    public void lmulAddi(boolean[] x, boolean[] y) {
        // validation check for x will be done during the computation.
        MathPreconditions.checkEqual("columns", "y.length", columns, y.length);
        IntStream iColumnStream = IntStream.range(0, columns);
        iColumnStream = parallel ? iColumnStream.parallel() : iColumnStream;
        iColumnStream.forEach(iColumn -> y[iColumn] ^= columnList.get(iColumn).rightMultiply(x));
    }

    @Override
    public byte[][] lExtMul(byte[][] x) {
        // validation check for x will be done during the computation.
        byte[][] outputs = new byte[columns][];
        IntStream iColumnStream = IntStream.range(0, columns);
        iColumnStream = parallel ? iColumnStream.parallel() : iColumnStream;
        iColumnStream.forEach(iColumn -> outputs[iColumn] = columnList.get(iColumn).rightGf2lMultiply(x));
        return outputs;
    }

    @Override
    public void lExtMulAddi(byte[][] x, byte[][] y) {
        // validation check for x will be done during the computation.
        MathPreconditions.checkEqual("columns", "y.length", columns, y.length);
        IntStream iColumnStream = IntStream.range(0, columns);
        iColumnStream = parallel ? iColumnStream.parallel() : iColumnStream;
        iColumnStream.forEach(iColumn -> columnList.get(iColumn).rightGf2lMultiplyXori(x, y[iColumn]));
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
    public SparseBitVector getColumn(int index) {
        return columnList.get(index);
    }

    @Override
    public int getSize() {
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        return rows;
    }

    @Override
    public boolean get(int x, int y) {
        return columnList.get(y).get(x);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(rows)
            .append(columns)
            .append(columnList)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NaiveSparseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        NaiveSparseBitMatrix that = (NaiveSparseBitMatrix) obj;
        return new EqualsBuilder()
            .append(this.rows, that.rows)
            .append(this.columns, that.columns)
            .append(this.columnList, that.columnList)
            .isEquals();
    }

    @Override
    public String toString() {
        ArrayList<SparseBitVector> rowList = getRowList();
        StringBuilder stringBuilder = new StringBuilder();
        for (int iRow = 0; iRow < rows; iRow++) {
            SparseBitVector row = rowList.get(iRow);
            stringBuilder.append(row.toString());
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
