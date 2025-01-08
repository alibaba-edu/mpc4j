package edu.alibaba.mpc4j.s2pc.pir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.ArrayList;

/**
 * keyword PIR client interface.
 *
 * @author Liqiang Peng
 * @date 2024/7/22
 */
public interface KeyPirClient<T> extends TwoPartyPto {
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
     * @param keys keyword array.
     * @return retrieval results.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] pir(ArrayList<T> keys) throws MpcAbortException;

    /**
     * Client executes the protocol.
     *
     * @param key keyword.
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default byte[] pir(T key) throws MpcAbortException {
        ArrayList<T> xs = new ArrayList<>(1);
        xs.add(key);
        return pir(xs)[0];
    }
}
