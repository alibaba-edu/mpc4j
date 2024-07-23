package edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Batched Share Translation sender.
 *
 * @author Weiran Liu
 * @date 2024/4/23
 */
public interface BstSender extends TwoPartyPto {
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
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BstSenderOutput shareTranslate(int[][] piArray, int byteLength) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param piArray           permutation π array.
     * @param byteLength        element byte length.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BstSenderOutput shareTranslate(int[][] piArray, int byteLength, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}
