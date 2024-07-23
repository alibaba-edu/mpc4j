package edu.alibaba.mpc4j.common.tool.galoisfield.zl;

import edu.alibaba.mpc4j.common.tool.galoisfield.BigIntegerRing;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory.ZlType;

import java.math.BigInteger;

/**
 * The Zl interface. All operations are done module 2^l.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
public interface Zl extends BigIntegerRing {
    /**
     * Gets the Zl type.
     *
     * @return the Zl type.
     */
    ZlType getZlType();

    /**
     * Computes a mod p.
     *
     * @param a input.
     * @return a mod p.
     */
    BigInteger module(final BigInteger a);

    /**
     * Shifts left.
     *
     * @param a input.
     * @param i number of bits.
     * @return (a < < i) ∈ Z_{2^l}.
     */
    default BigInteger shiftLeft(final BigInteger a, int i) {
        return module(a.shiftLeft(i));
    }

    /**
     * Shifts right.
     *
     * @param a input.
     * @param i number of bits.
     * @return (a > > i) ∈ Z_{2^l}.
     */
    default BigInteger shiftRight(final BigInteger a, int i) {
        return a.shiftRight(i);
    }
}
