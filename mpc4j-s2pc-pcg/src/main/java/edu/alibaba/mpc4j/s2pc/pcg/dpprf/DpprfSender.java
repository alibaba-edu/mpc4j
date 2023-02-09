package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * DPPRF sender.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface DpprfSender extends TwoPartyPto, SecurePto {
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
     * @param batchNum   batch num.
     * @param alphaBound α upper bound.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    DpprfSenderOutput puncture(int batchNum, int alphaBound) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param batchNum        batch num.
     * @param alphaBound      α upper bound.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    DpprfSenderOutput puncture(int batchNum, int alphaBound, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
