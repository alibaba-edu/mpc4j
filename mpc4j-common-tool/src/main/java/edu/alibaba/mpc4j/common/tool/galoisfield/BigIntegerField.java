package edu.alibaba.mpc4j.common.tool.galoisfield;

import java.math.BigInteger;

/**
 * BigIntegerField interface. Elements in BigIntegerField are represented as BigInteger.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
public interface BigIntegerField extends BigIntegerRing {
    /**
     * Computes p / q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p / q.
     */
    BigInteger div(BigInteger p, BigInteger q);

    /**
     * Computes 1 / p.
     *
     * @param p the element p.
     * @return 1 / p.
     */
    BigInteger inv(BigInteger p);
}
