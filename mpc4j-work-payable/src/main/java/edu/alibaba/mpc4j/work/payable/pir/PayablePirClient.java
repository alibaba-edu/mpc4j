package edu.alibaba.mpc4j.work.payable.pir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;

/**
 * Payable PIR client interface.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public interface PayablePirClient extends TwoPartyPto {

    /**
     * client initializes protocol.
     *
     * @param serverElementSize server element size.
     * @param valueByteLength   value byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int serverElementSize, int valueByteLength) throws MpcAbortException;

    /**
     * client executes protocol.
     *
     * @param retrievalKey retrieval key.
     * @return corresponding value.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] pir(ByteBuffer retrievalKey) throws MpcAbortException;
}

