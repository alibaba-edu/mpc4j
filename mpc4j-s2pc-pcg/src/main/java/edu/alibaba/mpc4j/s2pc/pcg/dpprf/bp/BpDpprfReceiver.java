package edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * batch-point DPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface BpDpprfReceiver extends TwoPartyPto {
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
     * @param alphaArray α array.
     * @param alphaBound α upper bound.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BpDpprfReceiverOutput puncture(int[] alphaArray, int alphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param alphaArray        α array.
     * @param alphaBound        α upper bound.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BpDpprfReceiverOutput puncture(int[] alphaArray, int alphaBound, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException;
}
