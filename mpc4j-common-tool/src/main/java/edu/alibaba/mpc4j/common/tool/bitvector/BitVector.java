package edu.alibaba.mpc4j.common.tool.bitvector;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory.BitVectorType;

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
     * @return the split bit vectors.
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

    /**
     * Extends number of bits in the bit vector by padding enough zeros in the front.
     *
     * @param extendBitNum extend number of bits.
     */
    void extendBitNum(int extendBitNum);

    /**
     * Shift left by padding zero in the end.
     *
     * @param n shift distance, in bits.
     * @return result.
     */
    BitVector padShiftLeft(int n);

    /**
     * Inner shift left by fixing number of bits in the bit vector.
     *
     * @param n shift distance, in bits.
     */
    void fixShiftLefti(int n);

    /**
     * Shift right by reducing number of bits in the bit vector.
     *
     * @param n shift distance, in bits.
     * @return result.
     */
    BitVector reduceShiftRight(int n);

    /**
     * Inner shift right by reducing number of bits in the bit vector.
     *
     * @param n shift distance, in bits.
     */
    void reduceShiftRighti(int n);

    /**
     * Inner shift right by fixing number of bits in the bit vector.
     *
     * @param n shift distance, in bits.
     */
    void fixShiftRighti(int n);

    /**
     * Sets values in bytes from source, so that this[thisPos, thisPos + length) = source[srcPos, srcPos + length).
     *
     * @param source     source
     * @param srcPos     starting position of source (in byte).
     * @param thisPos    starting position of current byte array (in byte)
     * @param byteLength the number of bytes to be set.
     */
    void setBytes(byte[] source, int srcPos, int thisPos, int byteLength);

    /**
     * Splits vectors with padding. This is used when the bit vector is merged by calling mergeWithPadding.
     * <p></p>
     * This is more efficient since we can directly copy in bytes without shifting.
     * <p></p>
     * Here we do not check if each split bit vector is padding with zero. The reason is that we may do some (secure)
     * operations on the merged form so that the padding may not be zero.
     *
     * @param bitNums the bit length for each origin vectors.
     * @return the split bit vectors.
     */
    BitVector[] uncheckSplitWithPadding(int[] bitNums);

    /**
     * reverse the bits.
     */
    void reverseBits();

    /**
     * Gets bits by extracting each bit via a given interval. If the last position exceeds, then extract the last bit.
     *
     * @param pos      the start position.
     * @param num      total number of bits to get.
     * @param interval interval when extracting from the current vector.
     * @return result.
     */
    default BitVector getBitsByInterval(int pos, int num, int interval) {
        MathPreconditions.checkNonNegative("pos", pos);
        MathPreconditions.checkPositive("num", num);
        MathPreconditions.checkPositive("interval", interval);
        MathPreconditions.checkLessOrEqual("pos + (num - 2) * interval", pos + (num - 2) * interval, bitNum());
        BitVectorType type = getType();
        BitVector res = BitVectorFactory.createZeros(type, num);
        for (int i = 0, targetIndex = pos; i < num; i++, targetIndex += interval) {
            targetIndex = (i == num - 1 && targetIndex >= bitNum()) ? bitNum() - 1 : targetIndex;
            if (get(targetIndex)) {
                res.set(i, true);
            }
        }
        return res;
    }

    /**
     * Sets bits from source bit vector by setting via a given interval.
     *
     * @param source   source bit vector.
     * @param pos      the start position of current vector.
     * @param num      total number of bits to set.
     * @param interval interval when setting to the current vector.
     */
    default void setBitsByInterval(BitVector source, int pos, int num, int interval) {
        MathPreconditions.checkNonNegative("pos", pos);
        pos = pos >= bitNum() ? bitNum() - 1 : pos;
        MathPreconditions.checkPositive("num", num);
        MathPreconditions.checkPositive("interval", interval);
        MathPreconditions.checkLessOrEqual("pos", pos, bitNum());
        MathPreconditions.checkLessOrEqual("num", num, source.bitNum());
        MathPreconditions.checkLessOrEqual("pos + (num - 2) * interval", pos + (num - 2) * interval, bitNum());
        for (int i = 0, targetIndex = pos; i < num; i++, targetIndex += interval) {
            targetIndex = (i == num - 1 && targetIndex >= bitNum()) ? bitNum() - 1 : targetIndex;
            set(targetIndex, source.get(i));
        }
    }

    /**
     * Gets if the number of 1 in the vector is odd.
     *
     * @return true if the number of 1 in the vector is odd; false otherwise.
     */
    boolean numOf1IsOdd();
}
