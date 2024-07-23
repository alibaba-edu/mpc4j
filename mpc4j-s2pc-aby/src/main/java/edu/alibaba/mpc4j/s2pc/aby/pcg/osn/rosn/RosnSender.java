package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Random OSN sender.
 *
 * @author Weiran Liu
 * @date 2024/5/8
 */
public interface RosnSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num        num.
     * @param byteLength element byte length.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    RosnSenderOutput rosn(int num, int byteLength) throws MpcAbortException;
}
