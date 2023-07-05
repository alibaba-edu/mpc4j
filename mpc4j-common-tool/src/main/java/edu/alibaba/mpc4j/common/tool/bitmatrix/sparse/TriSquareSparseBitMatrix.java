package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

/**
 * triangular square sparse bit matrix.
 *
 * @author Weiran Liu
 * @date 2023/6/27
 */
public interface TriSquareSparseBitMatrix extends SparseBitMatrix {
    /**
     * Given a boolean vector v, computes x = v · M^{-1}, which is equal to solve the equations x · M = v or M^T · x = v.
     *
     * @param v the boolean vector v.
     * @return the result boolean vector x.
     */
    boolean[] invLmul(boolean[] v);

    /**
     * Given a boolean vector v and a boolean vector t, computes t = v · M^{-1} ⊕ t.
     *
     * @param v the boolean vector v.
     * @param t the boolean vector t.
     */
    void invLmulAddi(final boolean[] v, boolean[] t);

    /**
     * Given a GF2L vector v, computes x = v · M^{-1} by treating each entry in M as 1' in GF2L field.
     *
     * @param v the GF2L vector v.
     * @return the result GF2l vector x.
     */
    byte[][] invLextMul(final byte[][] v);

    /**
     * Given a GF2L vector v and a GF2L vector t, computes t = v · M^{-1} ⊕ t by treating each entry in M as 1' in GF2L
     * field.
     *
     * @param v the GF2L vector v.
     * @param t the GF2L vector t.
     */
    void invLextMulAddi(final byte[][] v, byte[][] t);

    /**
     * Transposes the matrix.
     *
     * @return the transposed matrix.
     */
    TriSquareSparseBitMatrix transpose();
}
