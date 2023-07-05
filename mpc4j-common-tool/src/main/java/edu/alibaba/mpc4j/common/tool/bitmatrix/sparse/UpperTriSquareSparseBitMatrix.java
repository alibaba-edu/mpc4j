package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * upper triangular square sparse bit matrix. All entries on the diagonal are 1. All the entries below the main diagonal
 * are 0. That is, the bit matrix is like:
 * <p> | 1 ? ? ? | </p>
 * <p> | 0 1 ? ? | </p>
 * <p> | 0 0 1 ? | </p>
 * <p> | 0 0 0 1 | </p>
 * This type of matrix supports efficient inverse multiplication.
 *
 * @author Hanwen Feng
 * @date 2022/9/20
 */
public class UpperTriSquareSparseBitMatrix extends NaiveSparseBitMatrix implements TriSquareSparseBitMatrix {
    /**
     * Creates a matrix from the column list.
     *
     * @param columnList the column list.
     * @return an upper triangular square sparse bit matrix.
     */
    public static UpperTriSquareSparseBitMatrix create(ArrayList<SparseBitVector> columnList) {
        int columns = columnList.size();
        MathPreconditions.checkPositive("columns", columns);
        int rows = columnList.get(0).getBitNum();
        // rows must be equal to columns (for a square matrix)
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        for (int iColumn = 0; iColumn < columns; iColumn++) {
            SparseBitVector columnVector = columnList.get(iColumn);
            MathPreconditions.checkEqual("rows", "columnVector.length", rows, columnVector.getBitNum());
            // the first position must be the entry on the diagonal.
            MathPreconditions.checkEqual(
                "iColumn", "columnVector.last_position", iColumn, columnVector.getLastPosition()
            );
        }
        UpperTriSquareSparseBitMatrix sparseBitMatrix = new UpperTriSquareSparseBitMatrix(columns);
        sparseBitMatrix.columnList = columnList;
        return sparseBitMatrix;
    }

    /**
     * Creates a matrix from the column list without validation check.
     *
     * @param columnList the column list.
     * @return an upper triangular square sparse bit matrix.
     */
    public static UpperTriSquareSparseBitMatrix createUncheck(ArrayList<SparseBitVector> columnList) {
        UpperTriSquareSparseBitMatrix sparseBitMatrix = new UpperTriSquareSparseBitMatrix(columnList.size());
        sparseBitMatrix.columnList = columnList;
        return sparseBitMatrix;
    }

    /**
     * Creates a random upper triangular sparse bit matrix.
     *
     * @param size         size.
     * @param maxWeight    max weight, i.e., the max number of 1's in each column.
     * @param secureRandom the random state.
     * @return a random upper triangular sparse bit matrix.
     */
    public static UpperTriSquareSparseBitMatrix createRandom(int size, int maxWeight, SecureRandom secureRandom) {
        // weight must be greater than 0
        MathPreconditions.checkPositiveInRangeClosed("maxWeight", maxWeight, size);
        ArrayList<SparseBitVector> columnList = IntStream.range(0, size)
            .mapToObj(columnIndex -> {
                TIntSet positionSet = new TIntHashSet();
                TIntArrayList positionList = new TIntArrayList();
                // the first position is columnIndex
                positionSet.add(columnIndex);
                positionList.add(columnIndex);
                // all other positions should be in range [0, columnIndex)
                int randomWeight = (maxWeight == 1) ? 1 : secureRandom.nextInt(maxWeight - 1) + 1;
                // max column weight are min(maxWeight, range)
                int columnMaxWeight =  Math.min(columnIndex + 1, randomWeight);
                if (columnMaxWeight == columnIndex + 1) {
                    // all other bits should be 1
                    for (int i = 0; i < columnIndex; i++) {
                        positionList.add(i);
                    }
                } else {
                    // randomly choose 1's
                    for (int i = 1; i < columnMaxWeight; i++) {
                        int position = secureRandom.nextInt(columnIndex);
                        while (!positionSet.add(position)) {
                            position = secureRandom.nextInt(columnIndex);
                        }
                        positionList.add(position);
                    }
                    positionList.sort();
                }
                return SparseBitVector.create(positionList, size);
            })
            .collect(Collectors.toCollection(ArrayList::new));
        return UpperTriSquareSparseBitMatrix.create(columnList);
    }

    /**
     * size
     */
    private final int size;

    private UpperTriSquareSparseBitMatrix(int size) {
        super(size, size);
        this.size = size;
    }

    @Override
    public boolean[] invLmul(boolean[] v) {
        MathPreconditions.checkEqual("size", "v.length", size, v.length);
        /*
         *                  | 1 ? ? |^T   | x0 |   | v0 |        | 1 0 0 |   | x0 |   | v0 |
         * We need to solve | 0 1 ? |   · | x1 | = | v1 |, i.e., | ? 1 0 | · | x1 | = | v1 |
         *                  | 0 0 1 |     | x2 |   | v2 |        | ? ? 1 |   | x2 | = | v2 |
         * To do this, we first compute xi = vi, and ⊕ it with other column positions.
         */
        boolean[] x = new boolean[size];
        for (int iRow = 0; iRow < size; iRow++) {
            x[iRow] = v[iRow];
            for (int iPosition = 0; iPosition < columnList.get(iRow).getSize() - 1; iPosition++) {
                int iColumn = columnList.get(iRow).getPosition(iPosition);
                x[iRow] ^= x[iColumn];
            }
        }
        return x;
    }

    @Override
    public void invLmulAddi(boolean[] v, boolean[] t) {
        MathPreconditions.checkEqual("size", "t.length", size, t.length);
        // computes x = v · M^{-1}
        boolean[] x = invLmul(v);
        // inplace add to t
        for (int iRow = 0; iRow < size; iRow++) {
            t[iRow] ^= x[iRow];
        }
    }

    @Override
    public byte[][] invLextMul(byte[][] v) {
        MathPreconditions.checkEqual("size", "v.length", size, v.length);
        /*
         *                  | 1 ? ? |^T   | x0 |   | v0 |        | 1 0 0 |   | x0 |   | v0 |
         * We need to solve | 0 1 ? |   · | x1 | = | v1 |, i.e., | ? 1 0 | · | x1 | = | v1 |
         *                  | 0 0 1 |     | x2 |   | v2 |        | ? ? 1 |   | x2 | = | v2 |
         * To do this, we first compute xi = vi, and ⊕ it with other column positions.
         */
        byte[][] x = new byte[size][];
        for (int iRow = 0; iRow < size; iRow++) {
            x[iRow] = BytesUtils.clone(v[iRow]);
            for (int iPosition = 0; iPosition < columnList.get(iRow).getSize() - 1; iPosition++) {
                int iColumn = columnList.get(iRow).getPosition(iPosition);
                BytesUtils.xori(x[iRow], x[iColumn]);
            }
        }
        return x;
    }

    @Override
    public void invLextMulAddi(byte[][] v, byte[][] t) {
        MathPreconditions.checkEqual("size", "t.length", size, t.length);
        // computes x = v · M^{-1}
        byte[][] outputs = invLextMul(v);
        // inplace add to t
        for (int iRow = 0; iRow < size; iRow++) {
            BytesUtils.xori(t[iRow], outputs[iRow]);
        }
    }

    @Override
    public LowerTriSquareSparseBitMatrix transpose() {
        ArrayList<SparseBitVector> rowList = getRowList();
        return LowerTriSquareSparseBitMatrix.createUncheck(rowList);
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UpperTriSquareSparseBitMatrix)) {
            return false;
        }
        return super.equals(obj);
    }
}
