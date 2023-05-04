package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * single-point DPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface SpDpprfSender extends TwoPartyPto {
    /**
     * Init the protocol.
     *
     * @param maxAlphaBound maximal α upper bound.
     * @throws MpcAbortException if the protocol aborts.
     */
    void init(int maxAlphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param alphaBound α upper bound.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpDpprfSenderOutput puncture(int alphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param alphaBound      α upper bound.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpDpprfSenderOutput puncture(int alphaBound, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
