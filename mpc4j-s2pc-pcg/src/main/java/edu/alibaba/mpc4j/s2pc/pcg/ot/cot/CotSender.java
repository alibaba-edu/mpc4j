package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * COT sender.
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public interface CotSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta     Δ.
     * @param expectNum expect num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta, int expectNum) throws MpcAbortException;

    /**
     * Init the protocol.
     *
     * @param delta Δ.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    CotSenderOutput send(int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    CotSenderOutput sendRandom(int num) throws MpcAbortException;
}
