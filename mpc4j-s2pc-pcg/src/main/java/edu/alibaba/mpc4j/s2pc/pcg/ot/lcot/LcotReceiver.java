package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 1-out-of-2^l COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public interface LcotReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param l choice bit length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int l) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param choices choices.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    LcotReceiverOutput receive(byte[][] choices) throws MpcAbortException;
}
