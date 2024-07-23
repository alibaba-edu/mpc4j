package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * single-point RDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface SpRdpprfSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException if the protocol aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num n.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpRdpprfSenderOutput puncture(int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num             n.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpRdpprfSenderOutput puncture(int num, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
