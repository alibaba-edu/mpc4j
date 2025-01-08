package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

/**
 * Updatable client-specific preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2024/10/10
 */
public interface StreamCpIdxPirServer extends CpIdxPirServer {
    /**
     * Updates an entry.
     *
     * @param i     index.
     * @param entry entry.
     */
    default void update(int i, byte[] entry) {
        update(new int[]{i}, new byte[][]{entry});
    }

    /**
     * Updates entries.
     *
     * @param xs      indexes.
     * @param entries entries.
     */
    void update(int[] xs, byte[][] entries);
}
