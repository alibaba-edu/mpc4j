package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.structure.matrix.Matrix;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32WprfMatrixFactory.F32WprfMatrixType;

/**
 * F3 -> F2 weak PRF matrix.
 *
 * @author Feng Han
 * @date 2024/10/16
 */
public interface F32WprfMatrix extends Matrix {
    /**
     * rows
     */
    int ROWS = F32Wprf.N;
    /**
     * columns
     */
    int COLUMNS = F32Wprf.M;

    /**
     * Left Multiplication.
     *
     * @param vector the vector.
     * @return the result.
     */
    byte[] leftMul(byte[] vector);

    /**
     * Gets the matrix type.
     *
     * @return matrix type.
     */
    F32WprfMatrixType getType();
}
