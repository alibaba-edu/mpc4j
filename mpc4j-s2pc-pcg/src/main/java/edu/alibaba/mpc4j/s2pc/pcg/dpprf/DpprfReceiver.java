package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * DPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface DpprfReceiver extends TwoPartyPto, SecurePto {
    /**
     * Get the protocol type.
     *
     * @return the protocol type.
     */
    @Override
    DpprfFactory.DpprfType getPtoType();

    /**
     * Init the protocol.
     *
     * @param maxBatchNum   maximal batch num.
     * @param maxAlphaBound maximal α upper bound.
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
    DpprfReceiverOutput puncture(int[] alphaArray, int alphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param alphaArray        α array.
     * @param alphaBound        α upper bound.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    DpprfReceiverOutput puncture(int[] alphaArray, int alphaBound, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException;
}
