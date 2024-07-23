package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Random OSN receiver.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public interface RosnReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException if the protocol aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param pi         permutation π.
     * @param byteLength element byte length.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    RosnReceiverOutput rosn(int[] pi, int byteLength) throws MpcAbortException;
}
