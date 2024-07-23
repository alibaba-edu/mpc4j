package edu.alibaba.mpc4j.common.circuit.zl64;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

/**
 * MPC Zl64 Vector.
 *
 * @author Weiran Liu
 * @date 2024/6/20
 */
public interface MpcZl64Vector extends MpcVector {
    /**
     * Get the inner Zl64 vector.
     *
     * @return the inner Zl64 vector.
     */
    Zl64Vector getZl64Vector();

    /**
     * Gets Zl64 instance.
     *
     * @return Zl64 instance.
     */
    default Zl64 getZl64() {
        return getZl64Vector().getZl64();
    }
}
