package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Single client-specific preprocessing KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public interface SingleCpKsPirClient<T> extends TwoPartyPto {
    /**
     * Client initializes the protocol.
     *
     * @param n database size.
     * @param l value bit length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int n, int l) throws MpcAbortException;

    /**
     * Client executes the protocol.
     *
     * @param x keyword.
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] pir(T x) throws MpcAbortException;
}
