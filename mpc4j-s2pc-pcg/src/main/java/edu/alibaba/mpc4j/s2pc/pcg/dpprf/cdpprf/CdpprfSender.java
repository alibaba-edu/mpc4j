package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.DpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Correlated DPPRF sender.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public interface CdpprfSender extends DpprfSender {
    /**
     * Init the protocol.
     *
     * @param delta         Δ.
     * @param maxBatchNum   maximal batch num.
     * @param maxAlphaBound maximal α upper bound.
     * @throws MpcAbortException if the protocol aborts.
     */
    void init(byte[] delta, int maxBatchNum, int maxAlphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param batchNum   batch num.
     * @param alphaBound α upper bound.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    @Override
    CdpprfSenderOutput puncture(int batchNum, int alphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param batchNum        batch num.
     * @param alphaBound      α upper bound.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    @Override
    CdpprfSenderOutput puncture(int batchNum, int alphaBound, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
