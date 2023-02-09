package edu.alibaba.mpc4j.common.tool.bitvector;

import java.math.BigInteger;

/**
 * Bit Vector.
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
public interface BitVector {
    /**
     * Get BitVector type.
     *
     * @return BitVector type.
     */
    BitVectorFactory.BitVectorType getType();

    /**
     * Set the value at the given index.
     *
     * @param index the index.
     * @param value the value.
     */
    void set(int index, boolean value);

    /**
     * Get the value at the index.
     *
     * @param index the index.
     * @return the value at the index.
     */
    boolean get(int index);

    /**
     * Copy the bit vector.
     *
     * @return the copied bit vector.
     */
    BitVector copy();

    /**
     * Replace the bit vector with the copied given bit vector.
     *
     * @param that the other bit vector.
     */
    void replaceCopy(BitVector that);

    /**
     * Get the number of bits in the bit vector.
     *
     * @return the number of bits in the bit vector.
     */
    int bitNum();

    /**
     * Get the number of bytes in the bit vector.
     *
     * @return the number of bytes in the bit vector.
     */
    int byteNum();

    /**
     * Get the bit vector represented by bytes.
     *
     * @return the bit vector represented by bytes.
     */
    byte[] getBytes();

    /**
     * Get the bit vector represented by non-negative BigInteger. Return 0 if the number of bits is 0.
     *
     * @return the bit vector represented by non-negative BigInteger.
     */
    BigInteger getBigInteger();

    /**
     * Split a bit vector with the given number of bits. The current bit vector keeps the remaining bits.
     *
     * @param bitNum the assigned number of bits.
     * @return the split bit vector.
     */
    BitVector split(int bitNum);

    /**
     * Reduce the bit vector with the given number of bits.
     *
     * @param bitNum the assigned number of bits.
     */
    void reduce(int bitNum);

    /**
     * Merge the other bit vector.
     *
     * @param that the other bit vector.
     */
    void merge(BitVector that);

    /**
     * XOR operation.
     *
     * @param that the other bit vector.
     * @return the XOR result.
     */
    BitVector xor(BitVector that);

    /**
     * Inner XOR operation.
     *
     * @param that the other bit vector.
     */
    void xori(BitVector that);

    /**
     * AND operation.
     *
     * @param that the other bit vector.
     * @return the AND result.
     */
    BitVector and(BitVector that);

    /**
     * Inner AND operation.
     *
     * @param that the other bit vector.
     */
    void andi(BitVector that);

    /**
     * OR operation.
     *
     * @param that the other bit vector.
     * @return the OR result.
     */
    BitVector or(BitVector that);

    /**
     * Inner OR operation.
     *
     * @param that the other bit vector.
     */
    void ori(BitVector that);

    /**
     * NOT operation.
     *
     * @return the NOT result.
     */
    BitVector not();

    /**
     * Inner NOT operation.
     */
    void noti();
}
