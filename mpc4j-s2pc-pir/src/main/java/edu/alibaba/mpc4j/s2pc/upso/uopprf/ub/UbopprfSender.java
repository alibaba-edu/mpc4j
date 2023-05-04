package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * unbalanced batched OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public interface UbopprfSender extends TwoPartyPto {
    /**
     * Generates the hint.
     *
     * @param l            the output bit length.
     * @param inputArrays  the batched input arrays.
     * @param targetArrays the batched target programmed arrays.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int l, byte[][][] inputArrays, byte[][][] targetArrays) throws MpcAbortException;

    /**
     * Executes opprf.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void opprf() throws MpcAbortException;
}
