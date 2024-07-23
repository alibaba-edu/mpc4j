package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Batched Share Translation receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
public interface BstReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException if the protocol aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum   batch num.
     * @param eachNum    each num.
     * @param byteLength element byte length.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum        batch num.
     * @param eachNum         each num.
     * @param byteLength      element byte length.
     * @param preSenderOutput pre-computed COT sender output.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
