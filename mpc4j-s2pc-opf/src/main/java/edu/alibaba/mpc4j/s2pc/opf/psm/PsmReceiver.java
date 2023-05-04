package edu.alibaba.mpc4j.s2pc.opf.psm;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

/**
 * private set membership receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public interface PsmReceiver extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL   max input bit length.
     * @param d      point num.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int d, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param l          input bit length.
     * @param inputArray the receiver's input array.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector psm(int l, byte[][] inputArray) throws MpcAbortException;
}
