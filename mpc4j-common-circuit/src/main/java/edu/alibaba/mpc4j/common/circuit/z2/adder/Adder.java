package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Adder interface.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public interface Adder {
    /**
     * x + y + carry-in. Computation is performed in big-endian order.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @param cin     carry-in bit.
     * @return (carry_out bit, result).
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, MpcZ2Vector cin)
        throws MpcAbortException;

    /**
     * x + y + carry-in. Computation is performed in big-endian order.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @param cin     carry-in bit.
     * @return (carry_out bit, result).
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, boolean cin)
        throws MpcAbortException;
}
