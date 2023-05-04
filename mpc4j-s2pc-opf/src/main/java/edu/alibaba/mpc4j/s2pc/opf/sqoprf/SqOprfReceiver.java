package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * single-query OPRF receiver.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public interface SqOprfReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxBatchSize the max batch size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxBatchSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param inputs the inputs.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SqOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException;

}
