package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Zl lookup table protocol sender.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public interface ZlLutSender extends TwoPartyPto {
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
     * @param table table.
     * @param m     input bit length.
     * @param n     output bit length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void lookupTable(byte[][][] table, int m, int n) throws MpcAbortException;
}
