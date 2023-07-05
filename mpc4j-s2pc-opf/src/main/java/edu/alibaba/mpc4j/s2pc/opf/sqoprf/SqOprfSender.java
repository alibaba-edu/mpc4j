package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * single-query OPRF sender.
 *
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public interface SqOprfSender extends TwoPartyPto {
    /**
     * Generates a sing-query OPRF key.
     *
     * @return a single-query OPRF key.
     */
    SqOprfKey keyGen();

    /**
     * Inits the protocol.
     *
     * @param maxBatchSize max batch size.
     * @param key          the single-query OPRF key.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxBatchSize, SqOprfKey key) throws MpcAbortException;


    /**
     * Executes the protocol.
     *
     * @param batchSize the batch size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void oprf(int batchSize) throws MpcAbortException;
}
