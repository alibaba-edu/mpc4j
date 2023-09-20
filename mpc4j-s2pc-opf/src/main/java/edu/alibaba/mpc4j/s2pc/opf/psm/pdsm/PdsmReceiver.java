package edu.alibaba.mpc4j.s2pc.opf.psm.pdsm;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * private (distinct) set membership receiver. In PDSM, we require elements in all sets are distinct.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public interface PdsmReceiver extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL   max input bit length.
     * @param maxD   max point num.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxD, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param l          input bit length.
     * @param d          point num.
     * @param inputArray the receiver's input array.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector pdsm(int l, int d, byte[][] inputArray) throws MpcAbortException;
}
