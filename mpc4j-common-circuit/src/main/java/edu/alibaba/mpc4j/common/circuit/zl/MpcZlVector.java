package edu.alibaba.mpc4j.common.circuit.zl;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

/**
 * MPC Zl Vector.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public interface MpcZlVector extends MpcVector {
    /**
     * Get the inner Zl vector.
     *
     * @return the inner Zl vector.
     */
    ZlVector getZlVector();

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    default Zl getZl() {
        return getZlVector().getZl();
    }
}
