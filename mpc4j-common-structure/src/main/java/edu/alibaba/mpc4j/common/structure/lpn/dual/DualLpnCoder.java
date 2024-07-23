package edu.alibaba.mpc4j.common.structure.lpn.dual;

import edu.alibaba.mpc4j.common.structure.lpn.LpnCoder;

/**
 * Dual LPN Coder.
 * <p></p>
 * Dual LPN coder is a coder with an n * k (dense) bit generation matrix where k = O(n) and n > k.
 * <p></p>
 * Dual LPN coder codes e = (e_1, ..., e_n) to w = (w_1, ..., w_k) by XOR non-zero positions for e, that is, computing
 * w = e · G.
 * <p></p>
 * We note that compared with the Primal LPN coder, the number of rows and columns in Dual LPN coder are reversed.
 * <p></p>
 * Since G is typically a dense matrix, the complexity of naive encoding is O(n * k). Therefore, we typically use
 * Low Density Parity Check (LDPC) code, that is, the parity check matrix H (with H^T · G = 0) is a sparse matrix.
 * This allows us to compute w = e · G by using H very efficiently.
 *
 * @author Weiran Liu
 * @date 2024/1/3
 */
public interface DualLpnCoder extends LpnCoder {
    /**
     * Encodes binary vector e = (e_1, ..., e_n) to binary vector w = (w_1, ..., w_k).
     *
     * @param es binary vector e = (e_1, ..., e_n).
     * @return binary vector w = (w_1, ..., w_k).
     */
    boolean[] dualEncode(boolean[] es);

    /**
     * Encodes GF2E vector e = (e_1, ..., e_n) to GF2E vector w = (w_1, ..., w_k).
     *
     * @param es GF2E vector e = (e_1, ..., e_n).
     * @return GF2E vector w = (w_1, ..., w_k).
     */
    byte[][] dualEncode(byte[][] es);

    /**
     * Gets the rows for the parity check matrix, that is, n - k.
     *
     * @return n - k.
     */
    default int getParityRows() {
        return getCodeSize() - getMessageSize();
    }

    /**
     * Gets the columns for the parity check matrix, that is, n.
     *
     * @return n.
     */
    default int getParityColumns() {
        return getCodeSize();
    }
}
