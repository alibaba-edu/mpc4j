package edu.alibaba.mpc4j.s2pc.upso.upsu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * UPSU receiver interface.
 *
 * @author Liqiang Peng
 * @date 2024/3/7
 */
public interface UpsuReceiver extends TwoPartyPto {

    /**
     * Receiver initializes the protocol.
     *
     * @param receiverElementSet   receiver element set.
     * @param maxSenderElementSize max sender element size.
     * @param elementByteLength    element byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Set<ByteBuffer> receiverElementSet, int maxSenderElementSize, int elementByteLength) throws MpcAbortException;

    /**
     * Receiver executes the protocol.
     *
     * @param senderElementSize sender element size.
     * @return union set.
     * @throws MpcAbortException the protocol failure aborts.
     */
    UpsuReceiverOutput psu(int senderElementSize) throws MpcAbortException;
}
