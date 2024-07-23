package edu.alibaba.mpc4j.common.circuit;

import edu.alibaba.mpc4j.common.structure.vector.Vector;

/**
 * Mpc Vector.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
public interface MpcVector extends Vector {
    /**
     * Whether the share vector is in plain state.
     *
     * @return the share vector is in plain state.
     */
    boolean isPlain();
}
