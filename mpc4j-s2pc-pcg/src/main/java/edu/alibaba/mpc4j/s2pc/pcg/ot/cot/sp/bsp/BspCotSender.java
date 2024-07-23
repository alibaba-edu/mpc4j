package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * Batched single-point COT sender.
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface BspCotSender extends TwoPartyPto {
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
     * @param batchNum batch num.
     * @param eachNum  each num.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BspCotSenderOutput send(int batchNum, int eachNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param batchNum        batch num.
     * @param eachNum         each num.
     * @param preSenderOutput pre-computed COT sender output.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BspCotSenderOutput send(int batchNum, int eachNum, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
