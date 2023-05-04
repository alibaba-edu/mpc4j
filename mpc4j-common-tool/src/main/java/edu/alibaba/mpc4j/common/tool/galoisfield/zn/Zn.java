package edu.alibaba.mpc4j.common.tool.galoisfield.zn;

import edu.alibaba.mpc4j.common.tool.galoisfield.BigIntegerRing;

import java.math.BigInteger;

/**
 * The Zn interface. All operations are done module n, where n is an integer (not necessarily be 2^l or a prime).
 *
 * @author Weiran Liu
 * @date 2023/3/14
 */
public interface Zn extends BigIntegerRing {
    /**
     * Gets the Zn type.
     *
     * @return the Zn type.
     */
    ZnFactory.ZnType getZnType();

    /**
     * Gets the name.
     *
     * @return the name.
     */
    @Override
    default String getName() {
        return getZnType().name();
    }

    /**
     * Gets the modulus n.
     *
     * @return the modulus n.
     */
    BigInteger getN();

    /**
     * Computes a mod n.
     *
     * @param a the input a.
     * @return a mod n.å
     */
    BigInteger module(final BigInteger a);
}
