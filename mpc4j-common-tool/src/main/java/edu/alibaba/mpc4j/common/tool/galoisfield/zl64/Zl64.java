package edu.alibaba.mpc4j.common.tool.galoisfield.zl64;

import edu.alibaba.mpc4j.common.tool.galoisfield.LongRing;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory.Zl64Type;

/**
 * The Zl64 interface. All operations are done module 2^l where 0 <= l <= 62.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
public interface Zl64 extends LongRing {
    /**
     * Gets the Zl64 type.
     *
     * @return the Zl64 type.
     */
    Zl64Type getZl64Type();

    /**
     * Computes a mod p.
     *
     * @param a the input a.
     * @return a mod p.
     */
    long module(final long a);

    /**
     * Gets the range bound, i.e., {0, 1}^l. Here are some special cases:
     * <li>If l = 64, then the range bound is 0. </li>
     * <li>If l = 63, then the range bound is 0x8000000000000000L, which is a negative number.</li>
     *
     * @return the range bound.
     */
    @Override
    long getRangeBound();

    /**
     * Gets mask so that any value a & mask must be a valid element in Zl64.
     *
     * @return mask.
     */
    long getMask();

    /**
     * Computes p + q without module.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p + q without module.
     */
    default long lazyAdd(final long p, final long q) {
        return p + q;
    }

    /**
     * Computes -p without module.
     *
     * @param p the element p.
     * @return -p without module.
     */
    default long lazyNeg(long p) {
        return -p;
    }

    /**
     * Computes p - q without module.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p - q without module.
     */
    default long lazySub(final long p, final long q) {
        return p - q;
    }

    /**
     * Computes p · q without module.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p · q without module.
     */
    default long lazyMul(long p, long q) {
        return p * q;
    }

    /**
     * Shifts left.
     *
     * @param p input.
     * @param i number of bits.
     * @return (p << i) ∈ Z_{2^l}.
     */
    default long shiftLeft(final long p, int i) {
        return module(p << i);
    }

    /**
     * Shifts right.
     *
     * @param p input.
     * @param i number of bits.
     * @return (p > > i) ∈ Z_{2^l}.
     */
    default long shiftRight(final long p, int i) {
        return p >>> i;
    }
}
