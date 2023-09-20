package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * multi single-point COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface MspCotReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxT   max sparse num.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxT, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param t   sparse num.
     * @param num num.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MspCotReceiverOutput receive(int t, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param t                 sparse num.
     * @param num               num.
     * @param preReceiverOutput pre-computed receiver output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MspCotReceiverOutput receive(int t, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}
