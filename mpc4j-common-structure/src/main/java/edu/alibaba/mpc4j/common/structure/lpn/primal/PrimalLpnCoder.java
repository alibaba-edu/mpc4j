package edu.alibaba.mpc4j.common.structure.lpn.primal;

/**
 * Primal LPN Coder.
 * <p></p>
 * Primal LPN coder is a coder with a k * n (sparse) bit generation matrix where n = O(k^2) and (n > k).
 * Give e = (e_1, ..., e_k), primal LPN coder provides w = (w_1, ..., w_n) by XOR non-zero positions for e, that is,
 * computing w = e · G.
 * <p></p>
 * Since G is typically a sparse bit matrix, the complexity of encoding is O(n).
 *
 * @author Weiran Liu
 * @date 2024/1/3
 */
public interface PrimalLpnCoder {
    /**
     * Encodes binary vector e = (e_1, ..., e_k) to binary vector w = (w_1, ..., w_n).
     *
     * @param e binary vector e = (e_1, ..., e_k).
     * @return binary vector w = (w_1, ..., w_n).
     */
    boolean[] encode(boolean[] e);

    /**
     * Encodes binary vector e = (e_1, ..., e_k) to binary vector w = (w_1, ..., w_n), both are compressed in bytes.
     *
     * @param e binary vector e = (e_1, ..., e_k) compressed in bytes.
     * @return binary vector w = (w_1, ..., w_n) compressed in bytes.
     */
    byte[] encode(byte[] e);

    /**
     * Encodes GF2E vector e = (e_1, ..., e_k) to GF2E vector w = (w_1, ..., w_n).
     *
     * @param e GF2E vector e = (e_1, ..., e_k).
     * @return GF2E vector w = (w_1, ..., w_n).
     */
    byte[][] encode(byte[][] e);

    /**
     * Gets code size (n).
     *
     * @return code size (n).
     */
    int getCodeSize();

    /**
     * Gets message size (k).
     *
     * @return message size (k).
     */
    int getMessageSize();

    /**
     * Sets parallel encoding.
     *
     * @param parallel parallel encoding.
     */
    void setParallel(boolean parallel);

    /**
     * Gets parallel encoding.
     *
     * @return parallel encoding.
     */
    boolean getParallel();
}
