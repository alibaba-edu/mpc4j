package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;

/**
 * Single Index Client-specific Preprocessing PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public interface SingleIndexCpPirServer extends TwoPartyPto {
    /**
     * Server initializes the protocol.
     *
     * @param database database.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(ZlDatabase database) throws MpcAbortException;

    /**
     * Server executes the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void pir() throws MpcAbortException;
}
