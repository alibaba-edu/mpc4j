package edu.alibaba.mpc4j.common.structure.okve.ovdm.zp;

import java.util.stream.IntStream;

/**
 * sparse (binary) OVDM. The OVDM can be split into the sparse part and the dense part.
 *
 * @author Weiran Liu
 * @date 2023/4/4
 */
public interface SparseZpOvdm<T> extends ZpOvdm<T> {
    /**
     * Gets the sparse position.
     *
     * @param key key.
     * @return the sparse position.
     */
    int[] sparsePositions(T key);

    /**
     * Gets the sparse position num.
     *
     * @return the sparse position num.
     */
    int sparsePositionNum();

    /**
     * Gets the dense position.
     *
     * @param key key.
     * @return the dense position.
     */
    boolean[] densePositions(T key);

    /**
     * Gets the dense position num.
     *
     * @return the dense position num.
     */
    int maxDensePositionNum();

    /**
     * Gets the binary positions for the given key. All positions are in range [0, m). The positions is distinct.
     *
     * @param key the key.
     * @return the binary positions.
     */
    default int[] positions(T key) {
        int sparsePositionNum = sparsePositionNum();
        int densePositionNum = maxDensePositionNum();
        int[] sparsePositions = sparsePositions(key);
        boolean[] binaryDensePositions = densePositions(key);
        int[] densePositions = IntStream.range(0, densePositionNum)
            .filter(rmIndex -> binaryDensePositions[rmIndex])
            .toArray();
        int[] positions = new int[sparsePositions.length + densePositions.length];
        System.arraycopy(sparsePositions, 0, densePositions, 0, sparsePositions.length);
        System.arraycopy(positions, 0, densePositions, sparsePositionNum, densePositions.length);
        return positions;
    }

    /**
     * Gets the maximal position num.
     *
     * @return the maximal position num.
     */
    default int maxPositionNum() {
        return sparsePositionNum() + maxDensePositionNum();
    }
}
