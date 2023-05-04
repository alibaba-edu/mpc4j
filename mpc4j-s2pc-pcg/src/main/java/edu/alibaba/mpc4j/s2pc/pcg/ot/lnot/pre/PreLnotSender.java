package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

/**
 * pre-compute 1-out-of-n (with n = 2^l) OT sender.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface PreLnotSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param preSenderOutput pre-compute sender output.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    LnotSenderOutput send(LnotSenderOutput preSenderOutput) throws MpcAbortException;
}
