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
 * lower triangular square sparse bit matrix. All entries on the diagonal are 1. All the entries above the main diagonal
 * are 0. That is, the bit matrix is like:
 * <p> | 1 0 0 0 | </p>
 * <p> | ? 1 0 0 | </p>
 * <p> | ? ? 1 0 | </p>
 * <p> | ? ? ? 1 | </p>
 * This type of matrix supports efficient inverse multiplication.
 *
 * @author Hanwen Feng
 * @date 2022/9/20
 */
public class LowerTriSquareSparseBitMatrix extends NaiveSparseBitMatrix implements TriSquareSparseBitMatrix {
    /**
     * Creates a matrix from the column list.
     *
     * @param columnList the column list.
     * @return a lower triangular square sparse bit matrix.
     */
    public static LowerTriSquareSparseBitMatrix create(ArrayList<SparseBitVector> columnList) {
        int columns = columnList.size();
        MathPreconditions.checkPositive("columns", columns);
        int rows = columnList.get(0).getBitNum();
        // rows must be equal to columns (for a square matrix)
        MathPreconditions.checkEqual("rows", "columns", rows, columns);
        for (int iColumn = 0; iColumn < columnList.size(); iColumn++) {
            SparseBitVector columnVector = columnList.get(iColumn);
            MathPreconditions.checkEqual("rows", "columnVector.length", rows, columnVector.getBitNum());
            // the first position must be the entry on the diagonal.
            MathPreconditions.checkEqual(
                "iColumn", "columnVector.first_position", iColumn, columnVector.getFirstPosition()
            );
        }
        LowerTriSquareSparseBitMatrix sparseBitMatrix = new LowerTriSquareSparseBitMatrix(columns);
        sparseBitMatrix.columnList = columnList;
        return sparseBitMatrix;
    }

    /**
     * Creates a matrix from the column list without validation check.
     *
     * @param columnList the column list.
     * @return a lower triangular square sparse bit matrix.
     */
    public static LowerTriSquareSparseBitMatrix createUncheck(ArrayList<SparseBitVector> columnList) {
        LowerTriSquareSparseBitMatrix sparseBitMatrix = new LowerTriSquareSparseBitMatrix(columnList.size());
        sparseBitMatrix.columnList = columnList;
        return sparseBitMatrix;
    }

    /**
     * Creates a random lower triangular sparse bit matrix.
     *
     * @param size         size.
     * @param maxWeight    max weight, i.e., the max number of 1's in each column.
     * @param secureRandom the random state.
     * @return a random lower triangular sparse bit matrix.
     */
    public static LowerTriSquareSparseBitMatrix createRandom(int size, int maxWeight, SecureRandom secureRandom) {
        // weight must be greater than 0
        MathPreconditions.checkPositiveInRangeClosed("maxWeight", maxWeight, size);
        ArrayList<SparseBitVector> columnList = IntStream.range(0, size)
            .mapToObj(columnIndex -> {
                TIntSet positionSet = new TIntHashSet();
                TIntArrayList positionList = new TIntArrayList();
                // the first position is columnIndex
                positionSet.add(columnIndex);
                positionList.add(columnIndex);
                // all other positions should be in range [columnIndex + 1, size)
                int range = size - columnIndex - 1;
                int randomWeight = (maxWeight == 1) ? 1 : secureRandom.nextInt(maxWeight - 1) + 1;
                // max column weight are min(maxWeight, range)
                int columnMaxWeight =  Math.min(size - columnIndex, randomWeight);
                if (columnMaxWeight == size - columnIndex) {
                    // all other bits should be 1
                    for (int i = columnIndex + 1; i < size; i++) {
                        positionList.add(i);
                    }
                } else {
                    // randomly choose 1's
                    for (int i = 1; i < columnMaxWeight; i++) {
                        int position = secureRandom.nextInt(range) + columnIndex + 1;
                        while (!positionSet.add(position)) {
                            position = secureRandom.nextInt(range) + columnIndex + 1;
                        }
                        positionList.add(position);
                    }
                    positionList.sort();
                }
                return SparseBitVector.create(positionList, size);
            })
            .collect(Collectors.toCollection(ArrayList::new));
        return LowerTriSquareSparseBitMatrix.create(columnList);
    }

    /**
     * size
     */
    private final int size;

    private LowerTriSquareSparseBitMatrix(int size) {
        super(size, size);
        this.size = size;
    }

    @Override
    public boolean[] invLmul(boolean[] v) {
        MathPreconditions.checkEqual("size", "v.length", size, v.length);
        /*
         *                  | 1 0 0 |^T   | x0 |   | v0 |        | 1 ? ? |   | x0 |   | v0 |
         * We need to solve | ? 1 0 |   · | x1 | = | v1 |, i.e., | 0 1 ? | · | x1 | = | v1 |
         *                  | ? ? 1 |     | x2 |   | v2 |        | 0 0 1 |   | x2 | = | v2 |
         * To do this, we first compute xi = vi, and ⊕ it with other column positions.
         */
        boolean[] x = new boolean[size];
        // bottom-up solving the equations
        for (int iRow = size - 1; iRow >= 0; --iRow) {
            x[iRow] = v[iRow];
            for (int iPosition = 1; iPosition < columnList.get(iRow).getSize(); iPosition++) {
                // note that we need to get the transpose of M
                int iColumn = columnList.get(iRow).getPosition(iPosition);
                x[iRow] ^= x[iColumn];
            }
        }
        return x;
    }

    @Override
    public void invLmulAddi(final boolean[] v, boolean[] t) {
        MathPreconditions.checkEqual("size", "t.length", size, t.length);
        // computes x = v · M^{-1}
        boolean[] x = invLmul(v);
        // inplace add to t
        for (int iRow = 0; iRow < size; iRow++) {
            t[iRow] ^= x[iRow];
        }
    }

    @Override
    public byte[][] invLextMul(final byte[][] v) {
        MathPreconditions.checkEqual("size", "v.length", size, v.length);
        /*
         *                  | 1 0 0 |^T   | x0 |   | v0 |        | 1 ? ? |   | x0 |   | v0 |
         * We need to solve | ? 1 0 |   · | x1 | = | v1 |, i.e., | 0 1 ? | · | x1 | = | v1 |
         *                  | ? ? 1 |     | x2 |   | v2 |        | 0 0 1 |   | x2 | = | v2 |
         * To do this, we first compute xi = vi, and ⊕ it with other column positions.
         */
        byte[][] x = new byte[size][];
        for (int iRow = size - 1; iRow >= 0; --iRow) {
            x[iRow] = BytesUtils.clone(v[iRow]);
            for (int iPosition = 1; iPosition < columnList.get(iRow).getSize(); iPosition++) {
                int iColumn = columnList.get(iRow).getPosition(iPosition);
                BytesUtils.xori(x[iRow], x[iColumn]);
            }
        }
        return x;
    }

    @Override
    public void invLextMulAddi(final byte[][] v, byte[][] t) {
        MathPreconditions.checkEqual("size", "t.length", size, t.length);
        // computes x = v · M^{-1}
        byte[][] x = invLextMul(v);
        // inplace add to t
        for (int iRow = 0; iRow < size; iRow++) {
            BytesUtils.xori(t[iRow], x[iRow]);
        }
    }

    @Override
    public UpperTriSquareSparseBitMatrix transpose() {
        ArrayList<SparseBitVector> rowList = getRowList();
        return UpperTriSquareSparseBitMatrix.createUncheck(rowList);
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LowerTriSquareSparseBitMatrix)) {
            return false;
        }
        return super.equals(obj);
    }
}
