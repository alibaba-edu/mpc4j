package edu.alibaba.mpc4j.common.tool.galoisfield;

import java.security.SecureRandom;

/**
 * LongRing interface. Elements in LongRing are represented as long.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
public interface LongRing {
    /**
     * Gets the name.
     *
     * @return the name.
     */
    String getName();

    /**
     * Gets the maximal l (in bit length) so that all elements in {0, 1}^l is a valid element.
     *
     * @return the maximal l (in bit length).
     */
    int getL();

    /**
     * Gets the maximal l (in byte length) so that all elements in {0, 1}^l is a valid element.
     *
     * @return the maximal l (in byte length).
     */
    int getByteL();

    /**
     * Gets the bit length that represents an element.
     *
     * @return the bit length that represents an element.
     */
    int getElementBitLength();

    /**
     * Gets the element byte length that represents an element.
     *
     * @return the element byte length that represents an element.
     */
    int getElementByteLength();

    /**
     * Gets the range bound, i.e., {0, 1}^l.
     *
     * @return the range bound.
     */
    long getRangeBound();

    /**
     * Computes p + q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p + q.
     */
    long add(final long p, final long q);

    /**
     * Computes -p.
     *
     * @param p the element p.
     * @return -p.
     */
    long neg(long p);

    /**
     * Computes p - q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p - q.
     */
    long sub(final long p, final long q);

    /**
     * Computes p · q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p · q.
     */
    long mul(long p, long q);

    /**
     * Computes p^q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p^q.
     */
    long pow(final long p, final long q);

    /**
     * Creates a zero element.
     *
     * @return a zero element.
     */
    long createZero();

    /**
     * Creates an identity element.
     *
     * @return an identity element.
     */
    long createOne();

    /**
     * Creates a random element.
     *
     * @param secureRandom the random state.
     * @return a random element.
     */
    long createRandom(SecureRandom secureRandom);

    /**
     * Creates a random element based on the seed.
     *
     * @param seed the seed.
     * @return a random element based on the seed.
     */
    long createRandom(byte[] seed);

    /**
     * Creates a non-zero random element.
     *
     * @param secureRandom the random state.
     * @return a non-zero random element.
     */
    long createNonZeroRandom(SecureRandom secureRandom);

    /**
     * Creates a non-zero random element based on the seed.
     *
     * @param seed the seed.
     * @return a non-zero random element based on the seed.
     */
    long createNonZeroRandom(byte[] seed);

    /**
     * Creates a random element in range [0, 2^l).
     *
     * @param secureRandom the random state.
     * @return a random element in range [0, 2^l).
     */
    long createRangeRandom(SecureRandom secureRandom);

    /**
     * Creates a random element in range [0, 2^l) based on the seed.
     *
     * @param seed the seed.
     * @return a random element in range [0, 2^l) based on the seed.
     */
    long createRangeRandom(byte[] seed);

    /**
     * Checks if the element p is zero.
     *
     * @param p the element p.
     * @return true if the element p is zero; false otherwise.
     */
    boolean isZero(long p);

    /**
     * Checks if the element p is identity.
     *
     * @param p the element p.
     * @return true if the element p is identity; false otherwise.
     */
    boolean isOne(long p);

    /**
     * Checks if the element p is a valid element.
     *
     * @param p the element p.
     * @return true if the element p is a valid element; false otherwise.
     */
    boolean validateElement(long p);

    /**
     * Checks if the element p is p valid non-zero element.
     *
     * @param p the element p.
     * @return true if the element p is a valid non-zero element; false otherwise.
     */
    boolean validateNonZeroElement(long p);

    /**
     * Checks if the element p is a valid element in range [0, 2^l).
     *
     * @param p the element p.
     * @return true if the element p is a valid element in range [0, 2^l).
     */
    boolean validateRangeElement(long p);

    /**
     * Returns whether the two elements are equal.
     *
     * @param p p.
     * @param q q.
     * @return true if p == q, false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isEqual(long p, long q) {
        validateElement(p);
        validateElement(q);
        return p == q;
    }
}
