package edu.alibaba.mpc4j.common.structure.vector;

/**
 * the vector interface.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface Vector {
    /**
     * Sets parallel operation.
     *
     * @param parallel parallel operation.
     */
    void setParallel(boolean parallel);

    /**
     * Copies the vector.
     *
     * @return the copied vector.
     */
    Vector copy();

    /**
     * Replaces the vector with the copied given vector.
     *
     * @param other the other vector.
     */
    void replaceCopy(Vector other);

    /**
     * Gets the number of elements in the vector.
     *
     * @return the number of elements in the vector.
     */
    int getNum();

    /**
     * Splits a vector with the given num. The current vector keeps the remaining elements.
     *
     * @param splitNum the split num.
     * @return the split vector.
     */
    Vector split(int splitNum);

    /**
     * Reduce the vector with the given num.
     *
     * @param reduceNum the reduced num.
     */
    void reduce(int reduceNum);

    /**
     * Merge the other vector.
     *
     * @param that the other vector.
     */
    void merge(Vector that);
}
