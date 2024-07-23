package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * batch-point RDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface BpRdpprfSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException if the protocol aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum batch num.
     * @param eachNum  each num.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BpRdpprfSenderOutput puncture(int batchNum, int eachNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum        batch num.
     * @param eachNum         each num.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BpRdpprfSenderOutput puncture(int batchNum, int eachNum, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
