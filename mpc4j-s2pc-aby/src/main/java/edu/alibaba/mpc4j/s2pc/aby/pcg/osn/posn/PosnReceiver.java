package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnPartyOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;

/**
 * pre-computed OSN receiver.
 *
 * @author Feng Han
 * @date 2024/05/08
 */
public interface PosnReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param pi                    permutation π.
     * @param byteLength            element byte length.
     * @param preRosnReceiverOutput pre-computed random OSN receiver output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    DosnPartyOutput posn(int[] pi, int byteLength, RosnReceiverOutput preRosnReceiverOutput) throws MpcAbortException;
}
