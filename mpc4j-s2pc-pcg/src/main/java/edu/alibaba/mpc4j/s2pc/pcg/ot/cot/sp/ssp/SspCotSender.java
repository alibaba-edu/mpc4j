package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Single single-point COT sender.
 *
 * @author Weiran Liu
 * @date 2023/7/13
 */
public interface SspCotSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta Δ.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SspCotSenderOutput send(int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num             num.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SspCotSenderOutput send(int num, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
