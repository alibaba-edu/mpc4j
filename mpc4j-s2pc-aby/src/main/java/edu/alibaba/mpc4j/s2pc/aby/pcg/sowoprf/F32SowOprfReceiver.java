package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * (F3, F2)-sowOPRF receiver. The receiver (P1) has input x ∈ F_3^n, where n = 4λ.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public interface F32SowOprfReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param expectBatchSize expect batch size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int expectBatchSize) throws MpcAbortException;

    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executs the protocol.
     *
     * @param inputs inputs.
     * @return receiver's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] oprf(byte[][] inputs) throws MpcAbortException;
}
