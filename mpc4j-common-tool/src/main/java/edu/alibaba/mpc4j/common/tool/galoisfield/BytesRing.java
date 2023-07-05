package edu.alibaba.mpc4j.common.tool.galoisfield;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * BytesRing interface. Elements in BytesRing are represented as byte array.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
public interface BytesRing {
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
     * Computes p + q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p + q.
     */
    byte[] add(final byte[] p, final byte[] q);

    /**
     * Computes p + q. The result is in-placed in p.
     *
     * @param p the element p.
     * @param q the element q.
     */
    void addi(byte[] p, final byte[] q);

    /**
     * Computes -p.
     *
     * @param p the element p.
     * @return -p.
     */
    byte[] neg(byte[] p);

    /**
     * Computes -p. The result is in-placed in p.
     *
     * @param p the element p.
     */
    void negi(byte[] p);

    /**
     * Computes p - q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p - q.
     */
    byte[] sub(final byte[] p, final byte[] q);

    /**
     * Computes p - q. The result is in-placed in p.
     *
     * @param p the element p.
     * @param q the element q.
     */
    void subi(byte[] p, final byte[] q);

    /**
     * Computes p · q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p · q.
     */
    byte[] mul(byte[] p, byte[] q);

    /**
     * Computes p · q. The result is in-placed in p.
     *
     * @param p the element p.
     * @param q the element q.
     */
    void muli(byte[] p, byte[] q);

    /**
     * Creates a zero element.
     *
     * @return a zero element.
     */
    byte[] createZero();

    /**
     * Creates an identity element.
     *
     * @return an identity element.
     */
    byte[] createOne();

    /**
     * Creates a random element.
     *
     * @param secureRandom the random state.
     * @return a random element.
     */
    byte[] createRandom(SecureRandom secureRandom);

    /**
     * Creates a random element based on the seed.
     *
     * @param seed the seed.
     * @return a random element based on the seed.
     */
    byte[] createRandom(byte[] seed);

    /**
     * Creates a non-zero random element.
     *
     * @param secureRandom the random state.
     * @return a non-zero random element.
     */
    byte[] createNonZeroRandom(SecureRandom secureRandom);

    /**
     * Creates a non-zero random element based on the seed.
     *
     * @param seed the seed.
     * @return a non-zero random element based on the seed.
     */
    byte[] createNonZeroRandom(byte[] seed);

    /**
     * Creates a random element in range [0, 2^l).
     *
     * @param secureRandom the random state.
     * @return a random element in range [0, 2^l).
     */
    byte[] createRangeRandom(SecureRandom secureRandom);

    /**
     * Creates a random element in range [0, 2^l) based on the seed.
     *
     * @param seed the seed.
     * @return a random element in range [0, 2^l) based on the seed.
     */
    byte[] createRangeRandom(byte[] seed);

    /**
     * Checks if the element p is zero.
     *
     * @param p the element p.
     * @return true if the element p is zero; false otherwise.
     */
    boolean isZero(byte[] p);

    /**
     * Checks if the element p is identity.
     *
     * @param p the element p.
     * @return true if the element p is identity; false otherwise.
     */
    boolean isOne(byte[] p);

    /**
     * Checks if the element p is a valid element.
     *
     * @param p the element p.
     * @return true if the element p is a valid element; false otherwise.
     */
    boolean validateElement(byte[] p);

    /**
     * Checks if the element p is p valid non-zero element.
     *
     * @param p the element p.
     * @return true if the element p is a valid non-zero element; false otherwise.
     */
    boolean validateNonZeroElement(byte[] p);

    /**
     * Checks if the element p is a valid element in range [0, 2^l).
     *
     * @param p the element p.
     * @return true if the element p is a valid element in range [0, 2^l).
     */
    boolean validateRangeElement(byte[] p);

    /**
     * Returns whether the two elements are equal.
     *
     * @param p p.
     * @param q q.
     * @return true if p == q, false otherwise.
     */
    default boolean isEqual(byte[] p, byte[] q) {
        validateElement(p);
        validateElement(q);
        return Arrays.equals(p, q);
    }
}
