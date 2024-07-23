package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * single-point CDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public interface SpCdpprfSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta Δ.
     * @throws MpcAbortException if the protocol aborts.
     */
    void init(byte[] delta) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num n.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpCdpprfSenderOutput puncture(int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num             n.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output, where Δ is the same as Δ in pre-computed COT.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpCdpprfSenderOutput puncture(int num, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
