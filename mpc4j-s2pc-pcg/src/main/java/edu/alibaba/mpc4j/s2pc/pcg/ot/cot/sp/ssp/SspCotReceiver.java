package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * Single-point COT receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/13
 */
public interface SspCotReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alpha α.
     * @param num   num.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SspCotReceiverOutput receive(int alpha, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alpha             α.
     * @param num               num.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SspCotReceiverOutput receive(int alpha, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}
