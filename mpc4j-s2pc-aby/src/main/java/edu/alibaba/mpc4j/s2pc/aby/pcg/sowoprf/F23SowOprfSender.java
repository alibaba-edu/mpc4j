package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * (F2, F3)-sowOPRF sender. The sender (P0) has key k ∈ F_2^n, where n = 4λ.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public interface F23SowOprfSender extends TwoPartyPto {
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
     * Computes PRF. Here the input is x ∈ F_2^n, where n = 4λ.
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

    /**
     * Executes the protocol.
     *
     * @param batchSize          batch size.
     * @param preCotSenderOutput pre-computed COT sender output.
     * @return sender's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] oprf(int batchSize, CotSenderOutput preCotSenderOutput) throws MpcAbortException;
}
