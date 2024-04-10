package edu.alibaba.mpc4j.s2pc.opf.pmpeqt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Permuted Matrix Private Equality Test sender interface.
 *
 * @author Liqiang Peng
 * @date 2024/2/29
 */
public interface PmPeqtSender extends TwoPartyPto {

    /**
     * sender initializes the protocol.
     *
     * @param maxRow    max row num.
     * @param maxColumn max column num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxRow, int maxColumn) throws MpcAbortException;

    /**
     * sender executes the protocol.
     *
     * @param inputMatrix          sender input matrix.
     * @param rowPermutationMap    row permutation map.
     * @param columnPermutationMap column permutation map.
     * @param byteLength           element byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void pmPeqt(byte[][][] inputMatrix, int[] rowPermutationMap, int[] columnPermutationMap, int byteLength)
        throws MpcAbortException;
}
