package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Single Share Translation receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public interface SstReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException if the protocol aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num        num.
     * @param byteLength element byte length.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SstReceiverOutput shareTranslate(int num, int byteLength) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num             num.
     * @param byteLength      element byte length.
     * @param preSenderOutput pre-computed COT sender output.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SstReceiverOutput shareTranslate(int num, int byteLength, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
