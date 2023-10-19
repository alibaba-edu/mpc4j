package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Single client-specific preprocessing PIR client.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public interface SingleCpPirClient extends TwoPartyPto {
    /**
     * Client initializes the protocol.
     *
     * @param n database size.
     * @param l value bit length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int n, int l) throws MpcAbortException;

    /**
     * Client executes the protocol. We note that x should be chosen at random.
     *
     * @param x index value.
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] pir(int x) throws MpcAbortException;
}
