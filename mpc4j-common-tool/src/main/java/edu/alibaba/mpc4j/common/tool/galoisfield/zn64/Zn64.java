package edu.alibaba.mpc4j.common.tool.galoisfield.zn64;

import edu.alibaba.mpc4j.common.tool.galoisfield.LongRing;

/**
 * @author Weiran Liu
 * @date 2023/3/15
 */
public interface Zn64 extends LongRing {
    /**
     * Gets the Zn64 type.
     *
     * @return the Zn64 type.
     */
    Zn64Factory.Zn64Type getZn64Type();

    /**
     * Gets the name.
     *
     * @return the name.
     */
    @Override
    default String getName() {
        return getZn64Type().name();
    }

    /**
     * Gets the modulus n.
     *
     * @return the modulus n.
     */
    long getN();

    /**
     * Computes a mod n.
     *
     * @param a the input n.
     * @return a mod n.
     */
    long module(final long a);
}
