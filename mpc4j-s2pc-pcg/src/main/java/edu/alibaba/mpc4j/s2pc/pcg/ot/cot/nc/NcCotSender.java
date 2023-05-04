package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * no-choice COT sender.
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface NcCotSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param delta Δ.
     * @param num   num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(byte[] delta, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    CotSenderOutput send() throws MpcAbortException;
}
