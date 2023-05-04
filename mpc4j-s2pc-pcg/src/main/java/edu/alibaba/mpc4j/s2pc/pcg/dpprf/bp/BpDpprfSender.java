package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * batch-point DPPRF sender.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface BpDpprfSender extends TwoPartyPto {
    /**
     * Init the protocol.
     *
     * @param maxBatchNum   max batch num.
     * @param maxAlphaBound max α upper bound.
     * @throws MpcAbortException if the protocol aborts.
     */
    void init(int maxBatchNum, int maxAlphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param batchNum   batch num.
     * @param alphaBound α upper bound.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BpDpprfSenderOutput puncture(int batchNum, int alphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param batchNum        batch num.
     * @param alphaBound      α upper bound.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BpDpprfSenderOutput puncture(int batchNum, int alphaBound, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
