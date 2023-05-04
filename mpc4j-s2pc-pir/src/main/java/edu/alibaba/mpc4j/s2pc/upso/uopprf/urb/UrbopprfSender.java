package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * unbalanced related-batch OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
public interface UrbopprfSender extends TwoPartyPto {
    /**
     * Inits the protocol.
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
