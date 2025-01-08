package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;

/**
 * Decision OSN sender.
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public interface DosnSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param inputVector sender input vector.
     * @param byteLength  element byte length.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    DosnPartyOutput dosn(byte[][] inputVector, int byteLength) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param inputVector      sender input vector.
     * @param byteLength       element byte length.
     * @param rosnSenderOutput the temp output of rosn with the same permutation
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    DosnPartyOutput dosn(byte[][] inputVector, int byteLength, RosnSenderOutput rosnSenderOutput) throws MpcAbortException;
}
