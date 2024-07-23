package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Single Share Translation sender.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public interface SstSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException if the protocol aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param pi         permutation π.
     * @param byteLength element byte length.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SstSenderOutput shareTranslate(int[] pi, int byteLength) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param pi                permutation π.
     * @param byteLength        element byte length.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return sender output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SstSenderOutput shareTranslate(int[] pi, int byteLength, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}
