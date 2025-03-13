package edu.alibaba.mpc4j.crypto.fhe.seal.iterator;

/**
 * SEAL iterator. It is similar to the iterator in SEAL, but under the Java style.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/iterator.h">iterator.h</a>.
 *
 * @author Weiran Liu
 * @date 2025/2/24
 */
public interface SealIterator {
    /**
     * Gets wrapped coefficients.
     *
     * @return coefficients.
     */
    long[] coeff();

    /**
     * Gets the starting position of the wrapped iterator.
     *
     * @return the starting position.
     */
    int pos();

    /**
     * Gets the step size of the iterator.
     *
     * @return step size.
     */
    int stepSize();

    /**
     * Gets the current offset of the iterator.
     *
     * @return current offset.
     */
    int offset();

    /**
     * Sets the current offset of the iterator.
     *
     * @param offset the offset to set.
     */
    void setOffset(int offset);

    /**
     * Gets the current pointer of the iterator.
     *
     * @return current pointer.
     */
    default int ptr() {
        return pos() + offset();
    }

    /**
     * Returns if the iterator has next element.
     *
     * @return true if the iterator has next element, otherwise false.
     */
    default boolean hasNext() {
        return ptr() + stepSize() < coeff().length;
    }

    /**
     * Moves the iterator to the next element.
     */
    default void next() {
        // we cannot verify it has next, since the offset would overflow after the last next.
        setOffset(offset() + stepSize());
    }

    /**
     * Returns if the iterator has previous element.
     *
     * @return true if the iterator has previous element, otherwise false.
     */
    default boolean hasPrevious() {
        return ptr() - stepSize() < 0;
    }

    /**
     * Moves the iterator to the previous element.
     */
    default void previous() {
        // we cannot verify it has previous, since the iterator would overflow the last previous.
        setOffset(offset() - stepSize());
    }

    /**
     * Resets the iterator to the initial state.
     */
    default void reset() {
        setOffset(0);
    }
}
