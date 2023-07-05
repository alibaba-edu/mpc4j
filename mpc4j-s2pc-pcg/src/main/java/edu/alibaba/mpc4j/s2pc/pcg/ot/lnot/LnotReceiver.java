package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 1-out-of-n (with n = 2^l) OT receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public interface LnotReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param l         choice bit length.
     * @param updateNum update num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int l, int updateNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param choiceArray the choice array.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    LnotReceiverOutput receive(int[] choiceArray) throws MpcAbortException;
}
