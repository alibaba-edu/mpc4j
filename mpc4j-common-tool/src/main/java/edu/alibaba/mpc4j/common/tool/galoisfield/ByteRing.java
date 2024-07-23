package edu.alibaba.mpc4j.common.tool.galoisfield;

import java.security.SecureRandom;

/**
 * ByteRing interface. Elements in LongRing are represented as byte.
 *
 * @author Weiran Liu
 * @date 2024/5/22
 */
public interface ByteRing {
    /**
     * Computes p + q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p + q.
     */
    byte add(final byte p, final byte q);

    /**
     * Computes -p.
     *
     * @param p the element p.
     * @return -p.
     */
    byte neg(byte p);

    /**
     * Computes p - q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p - q.
     */
    byte sub(final byte p, final byte q);

    /**
     * Computes p · q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p · q.
     */
    byte mul(byte p, byte q);

    /**
     * Creates a zero element.
     *
     * @return a zero element.
     */
    byte createZero();

    /**
     * Creates an identity element.
     *
     * @return an identity element.
     */
    byte createOne();

    /**
     * Creates a random element.
     *
     * @param secureRandom the random state.
     * @return a random element.
     */
    byte createRandom(SecureRandom secureRandom);

    /**
     * Creates a non-zero random element.
     *
     * @param secureRandom the random state.
     * @return a non-zero random element.
     */
    byte createNonZeroRandom(SecureRandom secureRandom);

    /**
     * Checks if the element p is zero.
     *
     * @param p the element p.
     * @return true if the element p is zero; false otherwise.
     */
    boolean isZero(byte p);

    /**
     * Checks if the element p is identity.
     *
     * @param p the element p.
     * @return true if the element p is identity; false otherwise.
     */
    boolean isOne(byte p);

    /**
     * Checks if the element p is a valid element.
     *
     * @param p the element p.
     * @return true if the element p is a valid element; false otherwise.
     */
    boolean validateElement(byte p);

    /**
     * Checks if the element p is a valid non-zero element.
     *
     * @param p the element p.
     * @return true if the element p is a valid non-zero element; false otherwise.
     */
    boolean validateNonZeroElement(byte p);

    /**
     * Returns whether the two elements are equal.
     *
     * @param p p.
     * @param q q.
     * @return true if p == q, false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isEqual(byte p, byte q) {
        assert validateElement(p);
        assert validateElement(q);
        return p == q;
    }
}
