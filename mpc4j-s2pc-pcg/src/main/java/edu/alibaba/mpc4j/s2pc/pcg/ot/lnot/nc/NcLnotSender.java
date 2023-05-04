package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

/**
 * no-choice 1-out-of-n (with n = 2^l) OT sender.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public interface NcLnotSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param l   choice bit length.
     * @param num num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int l, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    LnotSenderOutput send() throws MpcAbortException;
}
