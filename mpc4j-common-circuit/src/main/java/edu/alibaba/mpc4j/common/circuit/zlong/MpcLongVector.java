package edu.alibaba.mpc4j.common.circuit.zlong;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;

/**
 * MPC Zlong Vector.
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public interface MpcLongVector extends MpcVector {
    /**
     * Get the inner long vector.
     *
     * @return the inner long vector.
     */
    LongVector[] getVectors();
    /**
     * set the inner long vector.
     *
     * @param vec  the inner long vector.
     */
    void setVectors(LongVector... vec);

    /**
     * split the elements into multiple vectors.
     *
     * @param splitNums the data lengths.
     * @return the elements.
     */
    MpcLongVector[] split(int[] splitNums);
}
