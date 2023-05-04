package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * unbalanced related-Batch OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
public interface UrbopprfReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param l         the output bit length.
     * @param batchSize batch size.
     * @param pointNum  the number of programmed points.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int l, int batchSize, int pointNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param inputArray the batched input array.
     * @return the receiver outputs.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][][] opprf(byte[][] inputArray) throws MpcAbortException;
}
