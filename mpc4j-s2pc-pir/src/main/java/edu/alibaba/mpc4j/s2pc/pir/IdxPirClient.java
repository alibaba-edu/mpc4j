package edu.alibaba.mpc4j.s2pc.pir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * index PIR client.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public interface IdxPirClient extends TwoPartyPto {
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
     * Clients executes the protocol.
     *
     * @param xs index array.
     * @return retrieval results.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] pir(int[] xs) throws MpcAbortException;

    /**
     * Client executes the protocol.
     *
     * @param x index value.
     * @return retrieval result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default byte[] pir(int x) throws MpcAbortException {
        return pir(new int[]{x})[0];
    }
}
