package edu.alibaba.mpc4j.s2pc.pir.payable;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Payable PIR server interface.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public interface PayablePirServer extends TwoPartyPto {
    /**
     * server initializes protocol.
     *
     * @param keyValueMap      key value map.
     * @param valueByteLength  value byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Map<ByteBuffer, byte[]> keyValueMap, int valueByteLength) throws MpcAbortException;

    /**
     * server executes protocol.
     *
     * @return whether the server set contains retrieval item.
     * @throws MpcAbortException the protocol failure aborts.
     */
    boolean pir() throws MpcAbortException;
}
