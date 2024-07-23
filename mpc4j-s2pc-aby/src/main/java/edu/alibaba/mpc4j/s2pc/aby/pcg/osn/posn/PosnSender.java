package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;

/**
 * pre-computed OSN sender.
 *
 * @author Feng Han
 * @date 2024/05/08
 */
public interface PosnSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param inputVector         sender input vector.
     * @param preRosnSenderOutput precomputed random osn output
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    DosnPartyOutput posn(byte[][] inputVector, RosnSenderOutput preRosnSenderOutput) throws MpcAbortException;
}
