package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * single-point DPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface SpDpprfReceiver extends TwoPartyPto {
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
     * @param alpha      α.
     * @param alphaBound α upper bound.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpDpprfReceiverOutput puncture(int alpha, int alphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param alpha             α.
     * @param alphaBound        α upper bound.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpDpprfReceiverOutput puncture(int alpha, int alphaBound, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}
