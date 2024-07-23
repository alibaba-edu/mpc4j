package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * BP-CDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public interface BpCdpprfSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta Δ.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum batch num.
     * @param eachNum  each num.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BpCdpprfSenderOutput puncture(int batchNum, int eachNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum        batch num.
     * @param eachNum         each num.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output, where Δ is the same as Δ in pre-computed COT.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BpCdpprfSenderOutput puncture(int batchNum, int eachNum, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
