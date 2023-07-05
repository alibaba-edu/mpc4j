package edu.alibaba.mpc4j.common.circuit.z2.multiplier;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Multiplier interface.
 *
 * @author Li Peng
 * @date 2023/6/6
 */
public interface Multiplier {
    /**
     * x * y. Computation is performed in big-endian order.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    MpcZ2Vector[] mul(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray)
        throws MpcAbortException;
}
