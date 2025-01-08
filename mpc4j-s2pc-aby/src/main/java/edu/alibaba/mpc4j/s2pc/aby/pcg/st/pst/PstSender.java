package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Partial ST sender, where the ST corresponds to the left or the right part of network
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public interface PstSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException if the protocol aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param piArray    permutation π array.
     * @param byteLength element byte length.
     * @param isLeft     is the required st is the left part of the network
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BstSenderOutput shareTranslate(int[][] piArray, int byteLength, boolean isLeft) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param piArray           permutation π array.
     * @param byteLength        element byte length.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @param isLeft            is the required st is the left part of the network
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BstSenderOutput shareTranslate(int[][] piArray, int byteLength, CotReceiverOutput preReceiverOutput, boolean isLeft) throws MpcAbortException;
}