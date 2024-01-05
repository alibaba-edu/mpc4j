package edu.alibaba.mpc4j.common.structure.okve.tool;

import gnu.trove.set.TIntSet;

/**
 * the output of the function rowEchelonFrom for all linear solver.
 *
 * @author Weiran Liu
 * @date 2023/6/20
 */
class RowEchelonFormInfo {
    /**
     * number of zero columns
     */
    private final int nZeroColumns;
    /**
     * max linear independent columns
     */
    private final TIntSet maxLisColumns;

    public RowEchelonFormInfo(int nZeroColumns, TIntSet maxLisColumns) {
        this.nZeroColumns = nZeroColumns;
        this.maxLisColumns = maxLisColumns;
    }

    /**
     * Gets the number of zero columns when getting the roe Echelon form.
     *
     * @return the number of zero columns.
     */
    public int getZeroColumnNum() {
        return nZeroColumns;
    }

    /**
     * gets the indexes of the max linear independent columns.
     *
     * @return the indexes of the max linear independent columns.
     */
    public TIntSet getMaxLisColumns() {
        return maxLisColumns;
    }
}
