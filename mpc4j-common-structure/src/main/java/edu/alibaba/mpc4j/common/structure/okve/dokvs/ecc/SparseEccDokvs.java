package edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc;

import java.util.stream.IntStream;

/**
 * sparse DOKVS. The positions can be split into the sparse part and the dense part.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
public interface SparseEccDokvs<T> extends EccDokvs<T> {
    /**
     * Gets the sparse position range. All sparse positions are in range [0, sparseRange).
     *
     * @return the sparse range.
     */
    int sparsePositionRange();

    /**
     * Gets the sparse positions.
     *
     * @param key key.
     * @return the sparse positions.
     */
    int[] sparsePositions(T key);

    /**
     * Gets the sparse num.
     *
     * @return the sparse num.
     */
    int sparsePositionNum();

    /**
     * Gets the dense positions.
     *
     * @param key key.
     * @return the dense positions.
     */
    boolean[] binaryDensePositions(T key);

    /**
     * Gets the dense position range. All dense positions are in range [sparseRange, sparseRange + denseRange).
     *
     * @return dense position range.
     */
    int densePositionRange();

    /**
     * Gets the maximal num.
     *
     * @return the maximal num.
     */
    @Override
    default int maxPositionNum() {
        return sparsePositionNum() + densePositionRange();
    }

    /**
     * Gets the positions.
     *
     * @param key the key.
     * @return the positions.
     */
    @Override
    default int[] positions(T key) {
        int sparseRange = sparsePositionRange();
        int denseNum = densePositionRange();
        int[] sparsePositions = sparsePositions(key);
        boolean[] binaryDensePositions = binaryDensePositions(key);
        int[] densePositions = IntStream.range(0, denseNum)
            .filter(denseIndex -> binaryDensePositions[denseIndex])
            .map(densePosition -> densePosition + sparseRange)
            .toArray();
        int[] positions = new int[sparsePositions.length + densePositions.length];
        System.arraycopy(sparsePositions, 0, positions, 0, sparsePositions.length);
        System.arraycopy(densePositions, 0, positions, sparsePositions.length, densePositions.length);
        return positions;
    }
}
