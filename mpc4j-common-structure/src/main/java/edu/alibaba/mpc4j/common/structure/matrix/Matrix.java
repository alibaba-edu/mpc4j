package edu.alibaba.mpc4j.common.structure.matrix;

/**
 * the matrix interface.
 *
 * @author Liqiang Peng
 * @date 2023/5/23
 */
public interface Matrix {
    /**
     * Sets parallel operation.
     *
     * @param parallel parallel operation.
     */
    void setParallel(boolean parallel);

    /**
     * Copies the matrix.
     *
     * @return the copied matrix.
     */
    Matrix copy();

    /**
     * get the rows of the matrix.
     *
     * @return the rows of the matrix.
     */
    int getRows();

    /**
     * get the cols of the matrix.
     *
     * @return the cols of the matrix.
     */
    int getColumns();
}
