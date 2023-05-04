package edu.alibaba.mpc4j.common.tool.bitvector;

import java.math.BigInteger;
import java.util.Random;

/**
 * BitVector Factory. The efficiency test shows that one may choose BIGINTEGER_BIT_VECTOR, since it is easy for merge,
 * split and reduce. The operation efficiency is also faster unless the number of bits (bitNum) is very short
 * (less than 2^{12}).
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
public class BitVectorFactory {
    /**
     * BitVector type
     */
    public enum BitVectorType {
        /**
         * bit vector represented by bytes, use this if the bit vector is often used for operations.
         */
        BYTES_BIT_VECTOR,
        /**
         * bit vector represented by BigInteger, use this if the bit vector is often used for split / merge / reduce.
         */
        BIGINTEGER_BIT_VECTOR,
    }

    /**
     * Create with assigned bits.
     *
     * @param bitNum the number of bits.
     * @param bytes  the assigned bits represented by bytes.
     * @return the created bit vector.
     */
    public static BitVector create(int bitNum, byte[] bytes) {
        return create(BitVectorType.BIGINTEGER_BIT_VECTOR, bitNum, bytes);
    }

    /**
     * Create with assigned bits.
     *
     * @param type   the BitVector type.
     * @param bitNum the number of bits.
     * @param bytes  the assigned bits represented by bytes.
     * @return the created bit vector.
     */
    public static BitVector create(BitVectorType type, int bitNum, byte[] bytes) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.create(bitNum, bytes);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.create(bitNum, bytes);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create with assigned bits.
     *
     * @param bitNum     the number of bits.
     * @param bigInteger the assigned bits represented by BigInteger.
     * @return the created bit vector.
     */
    public static BitVector create(int bitNum, BigInteger bigInteger) {
        return create(BitVectorType.BIGINTEGER_BIT_VECTOR, bitNum, bigInteger);
    }

    /**
     * Create with assigned bits.
     *
     * @param type       the BitVector type.
     * @param bitNum     the number of bits.
     * @param bigInteger the assigned bits represented by BigInteger.
     * @return the created bit vector.
     */
    public static BitVector create(BitVectorType type, int bitNum, BigInteger bigInteger) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.create(bitNum, bigInteger);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.create(bitNum, bigInteger);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create a random bit vector.
     *
     * @param bitNum the number of bits.
     * @param random the random state.
     * @return the created bit vector.
     */
    public static BitVector createRandom(int bitNum, Random random) {
        return createRandom(BitVectorType.BIGINTEGER_BIT_VECTOR, bitNum, random);
    }

    /**
     * Create a random bit vector.
     *
     * @param type   the BitVector type.
     * @param bitNum the number of bits.
     * @param random the random state.
     * @return the created bit vector.
     */
    public static BitVector createRandom(BitVectorType type, int bitNum, Random random) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.createRandom(bitNum, random);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.createRandom(bitNum, random);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create a bit vector with all bits are 1.
     *
     * @param bitNum the number of bits.
     * @return the created bit vector.
     */
    public static BitVector createOnes(int bitNum) {
        return createOnes(BitVectorType.BIGINTEGER_BIT_VECTOR, bitNum);
    }

    /**
     * Create a bit vector with all bits are 1.
     *
     * @param type   the BitVector type.
     * @param bitNum the number of bits.
     * @return the created bit vector.
     */
    public static BitVector createOnes(BitVectorType type, int bitNum) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.createOnes(bitNum);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.createOnes(bitNum);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create a bit vector with all bits are 0.
     *
     * @param bitNum the number of bits.
     * @return the created bit vector.
     */
    public static BitVector createZeros(int bitNum) {
        return createZeros(BitVectorType.BIGINTEGER_BIT_VECTOR, bitNum);
    }

    /**
     * Create a bit vector with all bits are 0.
     *
     * @param type   the BitVector type.
     * @param bitNum the number of bits.
     * @return the created bit vector.
     */
    public static BitVector createZeros(BitVectorType type, int bitNum) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.createZeros(bitNum);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.createZeros(bitNum);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an empty (0 number of bits) bit vector.
     *
     * @return the created bit vector.
     */
    public static BitVector createEmpty() {
        return createEmpty(BitVectorType.BIGINTEGER_BIT_VECTOR);
    }

    /**
     * Create an empty (0 number of bits) bit vector.
     *
     * @param type the BitVector type.
     * @return the created bit vector.
     */
    public static BitVector createEmpty(BitVectorType type) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.createEmpty();
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.createEmpty();
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * merges bit vectors.
     *
     * @param bitVectors bit vectors.
     * @return the merged bit vector.
     */
    public static BitVector merge(BitVector[] bitVectors) {
        assert bitVectors.length > 0 : "merged vector length must be greater than 0";
        BitVector mergeBitVector = BitVectorFactory.createEmpty();
        for (BitVector bitVector : bitVectors) {
            assert bitVector.bitNum() > 0 : "the number of bits must be greater than 0";
            mergeBitVector.merge(bitVector);
        }
        return mergeBitVector;
    }

    /**
     * splits the bit vector.
     *
     * @param mergeBitVector the merged bit vector.
     * @param bitNums        bits for each of the split vector.
     * @return the split bit vectors.
     */
    public static BitVector[] split(BitVector mergeBitVector, int[] bitNums) {
        BitVector[] bitVectors = new BitVector[bitNums.length];
        for (int index = 0; index < bitNums.length; index++) {
            bitVectors[index] = mergeBitVector.split(bitNums[index]);
        }
        assert mergeBitVector.bitNum() == 0 : "merged vector must remain 0 bits: " + mergeBitVector.bitNum();
        return bitVectors;
    }
}
