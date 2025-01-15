package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirServer;

/**
 * Hint-based client-specific preprocessing index PIR.
 *
 * @author Liqiang Peng
 * @date 2024/7/15
 */
public interface HintCpIdxPirServer extends IdxPirServer {
    /**
     * Answers the query.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void answer() throws MpcAbortException;
}
