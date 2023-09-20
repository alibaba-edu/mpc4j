package edu.alibaba.mpc4j.s2pc.opf.psm.pesm;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * private (equal) set membership sender. In PESM, we only require elements in each set are distinct.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public interface PesmSender extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL   max input bit length.
     * @param maxD   max set size.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxD, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param l           input bit length.
     * @param inputArrays the sender's input arrays.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector pesm(int l, byte[][][] inputArrays) throws MpcAbortException;
}
