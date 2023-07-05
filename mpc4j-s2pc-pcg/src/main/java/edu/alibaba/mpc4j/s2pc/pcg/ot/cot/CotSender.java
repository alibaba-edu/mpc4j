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
     * @param updateNum update num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta, int updateNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    CotSenderOutput send(int num) throws MpcAbortException;
}
