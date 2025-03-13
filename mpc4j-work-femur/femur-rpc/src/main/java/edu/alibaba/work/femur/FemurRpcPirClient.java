package edu.alibaba.work.femur;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * PGM-index range keyword PIR client interface.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public interface FemurRpcPirClient extends TwoPartyPto {
    /**
     * Client initializes the protocol.
     *
     * @param n           database size.
     * @param l           value bit length.
     * @param maxBatchNum max batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int n, int l, int maxBatchNum) throws MpcAbortException;

    /**
     * Client initializes the protocol.
     *
     * @param n database size.
     * @param l value bit length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default void init(int n, int l) throws MpcAbortException {
        init(n, l, 1);
    }

    /**
     * Client executes the protocol.
     *
     * @param keys       keyword.
     * @param rangeBound range bound.
     * @param epsilon    epsilon.
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] pir(long[] keys, int rangeBound, double epsilon) throws MpcAbortException;

    /**
     * Client executes the protocol.
     *
     * @param key        keyword.
     * @param rangeBound range bound.
     * @param epsilon    ep
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default byte[] pir(long key, int rangeBound, double epsilon) throws MpcAbortException {
        return pir(new long[]{key}, rangeBound, epsilon)[0];
    }

    /**
     * Gets client generate query time.
     *
     * @return client generate query time.
     */
    long getGenQueryTime();

    /**
     * Gets client handle response time.
     *
     * @return client handle response time.
     */
    long getHandleResponseTime();
}
