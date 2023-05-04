package edu.alibaba.mpc4j.common.circuit;

/**
 * Mpc Vector.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
public interface MpcVector {
    /**
     * Whether the share vector is in plain state.
     *
     * @return the share vector is in plain state.
     */
    boolean isPlain();

    /**
     * Copies the share vector.
     *
     * @return the copied share vector.
     */
    MpcVector copy();

    /**
     * Gets the number of shares in the share vector.
     *
     * @return the number of shares in the share vector.
     */
    int getNum();

    /**
     * Splits a share vector with the given num. The current share vector keeps the remaining shares.
     *
     * @param splitNum the split num.
     * @return the split share vector.
     */
    MpcVector split(int splitNum);

    /**
     * Reduce the share vector with the given num.
     *
     * @param reduceNum the reduced num.
     */
    void reduce(int reduceNum);

    /**
     * Merge the other share vector.
     *
     * @param other the other share vector.
     */
    void merge(MpcVector other);
}
