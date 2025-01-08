package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Partial ST receiver, where the ST corresponds to the left or the right part of network
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public interface PstReceiver extends TwoPartyPto {
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
     * @param isLeft     is the required st is the left part of the network
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength, boolean isLeft) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum        batch num.
     * @param eachNum         each num.
     * @param byteLength      element byte length.
     * @param preSenderOutput pre-computed COT sender output.
     * @param isLeft          is the required st is the left part of the network
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength, CotSenderOutput preSenderOutput, boolean isLeft) throws MpcAbortException;
}