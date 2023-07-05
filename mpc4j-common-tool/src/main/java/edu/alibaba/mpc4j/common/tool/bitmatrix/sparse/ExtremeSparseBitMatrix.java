package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Extreme sparse bit matrix is a sparse bit matrix where some columns are empty. A typical one is as follows:
 * <p> | ? 0 ? 0 |</p>
 * <p> | ? 0 ? 0 |</p>
 * <p> | ? 0 ? 0 |</p>
 * <p> | ? 0 ? 0 |</p>
 * <p> | ? 0 ? 0 |</p>
 * Note that the 2nd and the 4th columns are empty. We can do computations on extreme sparse bit matrix more efficiently.
 *
 * @author Hanwen Feng
 * @date 2022/03/10
 */
public class ExtremeSparseBitMatrix implements SparseBitMatrix {
    /**
     * Creates a extreme sparse bit matrix.
     *
     * @param rows                  number of rows.
     * @param columns               number of columns.
     * @param nonEmptyColumnIndexes non-empty column indexes.
     * @param nonEmptyColumnList    non-empty column list.
     * @return a extreme sparse bit matrix.
     */
    public static ExtremeSparseBitMatrix create(int rows, int columns,
                                                int[] nonEmptyColumnIndexes,
                                                ArrayList<SparseBitVector> nonEmptyColumnList) {
        MathPreconditions.checkPositive("rows", rows);
        MathPreconditions.checkPositive("columns", columns);
        MathPreconditions.checkEqual(
            "index.length", "column.size", nonEmptyColumnIndexes.length, nonEmptyColumnList.size()
        );
        // all column index should be in range [0, columns).
        long distinctIndexNum = Arrays.stream(nonEmptyColumnIndexes)
            .peek(columnIndex -> MathPreconditions.checkNonNegativeInRange("columnIndex", columnIndex, columns))
            .distinct()
            .count();
        MathPreconditions.checkEqual(
            "distinctIndexNum", "index.length", distinctIndexNum, nonEmptyColumnIndexes.length
        );
        ExtremeSparseBitMatrix sparseBitMatrix = new ExtremeSparseBitMatrix(rows, columns);
        int columnNum = nonEmptyColumnIndexes.length;
        sparseBitMatrix.nonEmptyColumnMap = new TIntObjectHashMap<>();
        for (int columnIndex = 0; columnIndex < columnNum; columnIndex++) {
            sparseBitMatrix.nonEmptyColumnMap.put(nonEmptyColumnIndexes[columnIndex], nonEmptyColumnList.get(columnIndex));
        }
        return sparseBitMatrix;
    }

    /**
     * Creates a extreme sparse bit matrix without validation check.
     *
     * @param rows                  number of rows.
     * @param columns               number of columns.
     * @param nonEmptyColumnIndexes non-empty column indexes.
     * @param nonEmptyColumnList    non-empty column list.
     * @return a extreme sparse bit matrix.
     */
    public static ExtremeSparseBitMatrix createUncheck(int rows, int columns,
                                                       int[] nonEmptyColumnIndexes,
                                                       ArrayList<SparseBitVector> nonEmptyColumnList) {
        ExtremeSparseBitMatrix sparseBitMatrix = new ExtremeSparseBitMatrix(rows, columns);
        int columnNum = nonEmptyColumnIndexes.length;
        sparseBitMatrix.nonEmptyColumnMap = new TIntObjectHashMap<>();
        for (int columnIndex = 0; columnIndex < columnNum; columnIndex++) {
            sparseBitMatrix.nonEmptyColumnMap.put(nonEmptyColumnIndexes[columnIndex], nonEmptyColumnList.get(columnIndex));
        }
        return sparseBitMatrix;
    }

    /**
     * number of rows
     */
    private final int rows;
    /**
     * number of columns
     */
    private final int columns;
    /**
     * non-empty column map
     */
    private TIntObjectMap<SparseBitVector> nonEmptyColumnMap;

    private ExtremeSparseBitMatrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
    }

    @Override
    public boolean[] lmul(boolean[] xVec) {
        assert xVec.length == rows;
        boolean[] outputs = new boolean[columns];
        for (int columnIndex : nonEmptyColumnMap.keys()) {
            outputs[columnIndex] = nonEmptyColumnMap.get(columnIndex).rightMultiply(xVec);
        }
        return outputs;
    }

    @Override
    public void lmulAddi(boolean[] xVec, boolean[] yVec) {
        assert xVec.length == rows;
        assert yVec.length == columns;
        for (int columnIndex : nonEmptyColumnMap.keys()) {
            yVec[columnIndex] ^= nonEmptyColumnMap.get(columnIndex).rightMultiply(xVec);
        }
    }

    @Override
    public byte[][] lExtMul(byte[][] xVec) {
        assert xVec.length == rows;
        byte[][] outputs = new byte[columns][xVec[0].length];
        for (int columnIndex : nonEmptyColumnMap.keys()) {
            nonEmptyColumnMap.get(columnIndex).rightGf2lMultiplyXori(xVec, outputs[columnIndex]);
        }
        return outputs;
    }

    @Override
    public void lExtMulAddi(byte[][] xVec, byte[][] yVec) {
        assert xVec.length == rows;
        assert yVec.length == columns;
        assert xVec[0].length == yVec[0].length;
        for (int columnIndex : nonEmptyColumnMap.keys()) {
            nonEmptyColumnMap.get(columnIndex).rightGf2lMultiplyXori(xVec, yVec[columnIndex]);
        }
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
        if (!nonEmptyColumnMap.containsKey(index)) {
            return SparseBitVector.createEmpty(rows);
        } else {
            return nonEmptyColumnMap.get(index);
        }
    }

    @Override
    public int getSize() {
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        return rows;
    }

    @Override
    public boolean get(int x, int y) {
        if (!nonEmptyColumnMap.containsKey(y)) {
            return false;
        }
        return nonEmptyColumnMap.get(y).get(x);
    }

    protected ArrayList<SparseBitVector> getRowList() {
        // init the row list
        ArrayList<TIntArrayList> rowPositionList = IntStream.range(0, rows)
            .mapToObj(iRow -> new TIntArrayList(columns))
            .collect(Collectors.toCollection(ArrayList::new));
        // assign the row list
        for (int iColumn = 0; iColumn < columns; iColumn++) {
            int[] positions = getColumn(iColumn).getPositions();
            for (int iRow : positions) {
                rowPositionList.get(iRow).add(iColumn);
            }
        }
        return rowPositionList.stream()
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
        return NaiveSparseBitMatrix.createFromColumnList(rowList);
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
        int[][] positions = IntStream.range(0, columns)
            .mapToObj(iColumn -> getColumn(iColumn).getPositions())
            .toArray(int[][]::new);
        return ByteDenseBitMatrix.createFromSparse(rows, positions);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExtremeSparseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ExtremeSparseBitMatrix that = (ExtremeSparseBitMatrix) obj;
        return new EqualsBuilder()
            .append(this.rows, that.rows)
            .append(this.columns, that.columns)
            .append(this.nonEmptyColumnMap, that.nonEmptyColumnMap)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(rows)
            .append(columns)
            .append(nonEmptyColumnMap)
            .toHashCode();
    }
}
