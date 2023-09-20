package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * multi single-point COT sender.
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface MspCotSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta  Δ.
     * @param maxT   max sparse num.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta, int maxT, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param t   sparse num.
     * @param num num.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MspCotSenderOutput send(int t, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param t               sparse num.
     * @param num             num.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MspCotSenderOutput send(int t, int num, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
