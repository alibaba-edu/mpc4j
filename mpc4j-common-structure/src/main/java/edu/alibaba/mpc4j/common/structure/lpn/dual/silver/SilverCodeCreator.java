package edu.alibaba.mpc4j.common.structure.lpn.dual.silver;

import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;

/**
 * Silver Code Creator.
 *
 * @author Hanwen Feng
 * @date 2022/3/11
 */
public interface SilverCodeCreator {
    /**
     * Creates the SilverCoder.
     *
     * @return the SilverCoder.
     */
    SilverCoder createCoder();

    /**
     * Gets the LPN parameter.
     *
     * @return the LPN parameter.
     */
    LpnParams getLpnParams();
}
