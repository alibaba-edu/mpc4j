package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * shuffle sender interface
 *
 * @author Feng Han
 * @date 2024/9/26
 */
public interface ShuffleParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * shuffle the input shared data
     *
     * @param xiArray xi array, in column form.
     * @param dataNum the row number
     * @param dimNum the dim of input vectors, which is the number of column
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[] shuffle(MpcZ2Vector[] xiArray, int dataNum, int dimNum) throws MpcAbortException;
}
