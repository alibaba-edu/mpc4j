package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Secret-shared bit vector.
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
public interface SbitVector {
    /**
     * Whether the share bit vector is in plain state.
     *
     * @return the share bit vector is in plain state.
     */
    boolean isPlain();

    /**
     * Copy the share bit vector.
     *
     * @return the copied share bit vector.
     */
    SbitVector copy();

    /**
     * Get the number of bits in the share bit vector.
     *
     * @return the number of bits in the share bit vector.
     */
    int bitNum();

    /**
     * Get the number of bytes in the share bit vector.
     *
     * @return the number of bytes in the share bit vector.
     */
    int byteNum();

    /**
     * Replace the bit vector.
     *
     * @param bitVector the bit vector.
     * @param plain if the share bit vector is in plain state.
     */
    void replaceCopy(BitVector bitVector, boolean plain);

    /**
     * Get the inner bit vector.
     *
     * @return the inner bit vector.
     */
    BitVector getBitVector();

    /**
     * Get the share bit vector represented by bytes.
     *
     * @return the share bit vector represented by bytes.
     */
    default byte[] getBytes() {
        return getBitVector().getBytes();
    }

    /**
     * Split a share bit vector with the given number of bits. The current share bit vector keeps the remaining bits.
     *
     * @param bitNum the assigned number of bits.
     * @return the split share bit vector.
     */
    SbitVector split(int bitNum);

    /**
     * Reduce the share bit vector with the given number of bits.
     *
     * @param bitNum the assigned number of bits.
     */
    void reduce(int bitNum);

    /**
     * Merge the other share bit vector.
     *
     * @param that the other share bit vector.
     */
    void merge(SbitVector that);

    /**
     * XOR operation.
     *
     * @param that the other share bit vector.
     * @param plain the result plain state.
     * @return the XOR result.
     */
    SbitVector xor(SbitVector that, boolean plain);

    /**
     * Inner XOR operation.
     *
     * @param that the other share bit vector.
     * @param plain the result plain state.
     */
    void xori(SbitVector that, boolean plain);

    /**
     * AND operation.
     *
     * @param that the other share bit vector.
     * @return the AND result.
     */
    SbitVector and(SbitVector that);

    /**
     * Inner AND operation.
     *
     * @param that the other share bit vector.
     */
    void andi(SbitVector that);

    /**
     * OR operation.
     *
     * @param that the other share bit vector.
     * @return the OR result.
     */
    SbitVector or(SbitVector that);

    /**
     * Inner OR operation.
     *
     * @param that the other share bit vector.
     */
    void ori(SbitVector that);

    /**
     * NOT operation.
     *
     * @return the NOT result.
     */
    SbitVector not();

    /**
     * Inner NOT operation.
     */
    void noti();
}
