package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

/**
 * pre-compute 1-out-of-n (with n = 2^l) OT receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface PreLnotReceiver extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param preReceiverOutput pre-compute receiver output.
     * @param choiceArray the choice array.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    LnotReceiverOutput receive(LnotReceiverOutput preReceiverOutput, int[] choiceArray) throws MpcAbortException;
}
