package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public interface CotReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param expectNum expect num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int expectNum) throws MpcAbortException;

    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param choices choices.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    CotReceiverOutput receive(boolean[] choices) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    CotReceiverOutput receiveRandom(int num) throws MpcAbortException;
}
