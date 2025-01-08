package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Updatable client-specific preprocessing index PIR client.
 *
 * @author Weiran Liu
 * @date 2024/10/10
 */
public interface StreamCpIdxPirClient extends CpIdxPirClient {
    /**
     * Updates an entry.
     */
    default void update() throws MpcAbortException {
        update(1);
    }

    /**
     * Updates entries.
     *
     * @param updateNum update num.
     */
    void update(int updateNum) throws MpcAbortException;
}
