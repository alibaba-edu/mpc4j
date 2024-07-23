package edu.alibaba.mpc4j.s2pc.pir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Map;

/**
 * keyword PIR server interface.
 *
 * @author Liqiang Peng
 * @date 2024/7/22
 */
public interface KeyPirServer<T> extends TwoPartyPto {
    /**
     * Server initializes the protocol.
     *
     * @param keyValueMap   key-value map.
     * @param l             l.
     * @param matchBatchNum match batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Map<T, byte[]> keyValueMap, int l, int matchBatchNum) throws MpcAbortException;

    /**
     * Server initializes the protocol.
     *
     * @param keyValueMap key-value map.
     * @param l           l.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default void init(Map<T, byte[]> keyValueMap, int l) throws MpcAbortException {
        init(keyValueMap, l, 1);
    }

    /**
     * Server executes the protocol.
     *
     * @param batchNum batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void pir(int batchNum) throws MpcAbortException;

    /**
     * Server executes the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    default void pir() throws MpcAbortException {
        pir(1);
    }
}
