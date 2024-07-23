package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 1-out-of-2^l COT sender output.
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public interface LcotSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param l     choice bit length.
     * @param delta Δ.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int l, byte[] delta) throws MpcAbortException;

    /**
     * Inits the protocol.
     *
     * @param l choice bit length.
     * @return Δ.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] init(int l) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num num.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    LcotSenderOutput send(int num) throws MpcAbortException;
}
