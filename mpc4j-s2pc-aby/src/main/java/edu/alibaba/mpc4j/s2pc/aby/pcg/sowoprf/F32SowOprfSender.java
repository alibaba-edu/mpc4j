package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * (F3, F2)-sowOPRF sender. The sender (P0) has key k ∈ F_2^n, where n = 4λ.
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public interface F32SowOprfSender extends TwoPartyPto {
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
     * Computes PRF. Here the input is x ∈ F_3^n, where n = 4λ.
     *
     * @param x input x.
     * @return PRF.
     */
    byte[] prf(byte[] x);

    /**
     * Executes the protocol.
     *
     * @param batchSize batch size.
     * @return sender's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] oprf(int batchSize) throws MpcAbortException;
}
