package edu.alibaba.mpc4j.s2pc.opf.pmpeqt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Permuted Matrix Private Equality Test receiver interface.
 *
 * @author Liqiang Peng
 * @date 2024/3/5
 */
public interface PmPeqtReceiver extends TwoPartyPto {

    /**
     * receiver initializes the protocol.
     *
     * @param maxRow    max row num.
     * @param maxColumn max column num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxRow, int maxColumn) throws MpcAbortException;

    /**
     * receiver executes the protocol.
     *
     * @param inputMatrix receiver input matrix.
     * @param byteLength  element byte length.
     * @param row         row num.
     * @param column      column num.
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    boolean[][] pmPeqt(byte[][][] inputMatrix, int byteLength, int row, int column) throws MpcAbortException;
}
