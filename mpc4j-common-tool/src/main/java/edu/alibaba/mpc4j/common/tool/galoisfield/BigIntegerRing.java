package edu.alibaba.mpc4j.common.tool.galoisfield;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * BigIntegerRing interface. Elements in LongRing are represented as BigInteger.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
public interface BigIntegerRing {
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
    BigInteger getRangeBound();

    /**
     * Computes p + q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p + q.
     */
    BigInteger add(final BigInteger p, final BigInteger q);

    /**
     * Computes -p.
     *
     * @param p the element p.
     * @return -p.
     */
    BigInteger neg(BigInteger p);

    /**
     * Computes p - q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p - q.
     */
    BigInteger sub(final BigInteger p, final BigInteger q);

    /**
     * Computes p · q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p · q.
     */
    BigInteger mul(BigInteger p, BigInteger q);

    /**
     * Computes p^q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p^q.
     */
    BigInteger pow(final BigInteger p, final BigInteger q);

    /**
     * Computes the inner-product of zp vector and binary vector.
     *
     * @param elementVector the element vector.
     * @param binaryVector  the binary vector.
     * @return the inner product.
     */
    default BigInteger innerProduct(final BigInteger[] elementVector, final boolean[] binaryVector) {
        assert elementVector.length == binaryVector.length
            : "element vector length must be equal to binary vector length = "
            + binaryVector.length + ": " + binaryVector.length;
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < elementVector.length; i++) {
            validateElement(elementVector[i]);
            if (binaryVector[i]) {
                value = add(value, elementVector[i]);
            }
        }
        return value;
    }

    /**
     * Computes the inner-product of zp vector and positions.
     *
     * @param elementVector the element vector.
     * @param positions  positions.
     * @return the inner product.
     */
    default BigInteger innerProduct(final BigInteger[] elementVector, final int[] positions) {
        BigInteger value = BigInteger.ZERO;
        for (int position : positions) {
            validateElement(elementVector[position]);
            value = add(value, elementVector[position]);
        }
        return value;
    }

    /**
     * Creates a zero element.
     *
     * @return a zero element.
     */
    BigInteger createZero();

    /**
     * Creates an identity element.
     *
     * @return an identity element.
     */
    BigInteger createOne();

    /**
     * Creates a random element.
     *
     * @param secureRandom the random state.
     * @return a random element.
     */
    BigInteger createRandom(SecureRandom secureRandom);

    /**
     * Creates a random element based on the seed.
     *
     * @param seed the seed.
     * @return a random element based on the seed.
     */
    BigInteger createRandom(byte[] seed);

    /**
     * Creates a non-zero random element.
     *
     * @param secureRandom the random state.
     * @return a non-zero random element.
     */
    BigInteger createNonZeroRandom(SecureRandom secureRandom);

    /**
     * Creates a non-zero random element based on the seed.
     *
     * @param seed the seed.
     * @return a non-zero random element based on the seed.
     */
    BigInteger createNonZeroRandom(byte[] seed);

    /**
     * Creates a random element in range [0, 2^l).
     *
     * @param secureRandom the random state.
     * @return a random element in range [0, 2^l).
     */
    BigInteger createRangeRandom(SecureRandom secureRandom);

    /**
     * Creates a random element in range [0, 2^l) based on the seed.
     *
     * @param seed the seed.
     * @return a random element in range [0, 2^l) based on the seed.
     */
    BigInteger createRangeRandom(byte[] seed);

    /**
     * Checks if the element p is zero.
     *
     * @param p the element p.
     * @return true if the element p is zero; false otherwise.
     */
    boolean isZero(BigInteger p);

    /**
     * Checks if the element p is identity.
     *
     * @param p the element p.
     * @return true if the element p is identity; false otherwise.
     */
    boolean isOne(BigInteger p);

    /**
     * Checks if the element p is a valid element.
     *
     * @param p the element p.
     * @return true if the element p is a valid element; false otherwise.
     */
    boolean validateElement(BigInteger p);

    /**
     * Checks if the element p is p valid non-zero element.
     *
     * @param p the element p.
     * @return true if the element p is a valid non-zero element; false otherwise.
     */
    boolean validateNonZeroElement(BigInteger p);

    /**
     * Checks if the element p is a valid element in range [0, 2^l).
     *
     * @param p the element p.
     * @return true if the element p is a valid element in range [0, 2^l).
     */
    boolean validateRangeElement(BigInteger p);

    /**
     * Returns whether the two elements are equal.
     *
     * @param p p.
     * @param q q.
     * @return true if p == q, false otherwise.
     */
    default boolean isEqual(BigInteger p, BigInteger q) {
        validateElement(p);
        validateElement(q);
        return p.equals(q);
    }
}
