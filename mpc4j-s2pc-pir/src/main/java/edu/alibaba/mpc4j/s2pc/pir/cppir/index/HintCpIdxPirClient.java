package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirClient;

/**
 * Hint-based client-specific preprocessing index PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/7/15
 */
public interface HintCpIdxPirClient extends IdxPirClient {
    /**
     * Generates and sends the query.
     *
     * @param x queried input.
     * @param i batch index.
     */
    void query(int x, int i);

    /**
     * Recovers the entry.
     *
     * @param x queried input.
     * @param i batch index.
     * @return entry.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] recover(int x, int i) throws MpcAbortException;

    /**
     * Update keys.
     */
    void updateKeys();
}
