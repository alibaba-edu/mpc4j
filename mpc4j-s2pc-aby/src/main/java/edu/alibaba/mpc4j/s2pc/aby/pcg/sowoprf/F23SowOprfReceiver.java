package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * (F2, F3)-sowOPRF receiver. The receiver (P1) has input x ∈ F_2^n, where n = 4λ.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public interface F23SowOprfReceiver extends TwoPartyPto {
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
     * Executes the protocol.
     *
     * @param inputs inputs.
     * @return receiver's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] oprf(byte[][] inputs) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param inputs               inputs.
     * @param preCotReceiverOutput pre-computed COT receiver output.
     * @return receiver's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] oprf(byte[][] inputs, CotReceiverOutput preCotReceiverOutput) throws MpcAbortException;
}
