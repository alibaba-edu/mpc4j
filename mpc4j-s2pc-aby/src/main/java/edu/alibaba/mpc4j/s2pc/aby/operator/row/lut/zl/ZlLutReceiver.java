package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Zl lookup table protocol receiver.
 *
 * @author Liqiang Peng
 * @date 2024/6/3
 */
public interface ZlLutReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxM   max input bit length.
     * @param maxN   max output bit length.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxM, int maxN, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param inputs the party's inputs.
     * @param m      input bit length.
     * @param n      output bit length.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] lookupTable(byte[][] inputs, int m, int n) throws MpcAbortException;
}
