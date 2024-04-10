package edu.alibaba.mpc4j.s2pc.upso.upsu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * UPSU sender interface.
 *
 * @author Liqiang Peng
 * @date 2024/3/7
 */
public interface UpsuSender extends TwoPartyPto {

    /**
     * Sender initializes the protocol.
     *
     * @param maxSenderElementSize max sender element size.
     * @param receiverElementSize  receiver element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxSenderElementSize, int receiverElementSize) throws MpcAbortException;

    /**
     * Sender executes the protocol.
     *
     * @param senderElementSet  sender element set.
     * @param elementByteLength element byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void psu(Set<ByteBuffer> senderElementSet, int elementByteLength) throws MpcAbortException;
}
