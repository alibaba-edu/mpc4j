package edu.alibaba.mpc4j.common.tool.bitvector;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * combined bit vector that combines BytesBitVector and BigIntegerBitVector.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class CombinedBitVector implements BitVector {

    static BitVector create(int bitNum, byte[] bytes) {
        CombinedBitVector bitVector = new CombinedBitVector();
        bitVector.innerBitVector = BytesBitVector.create(bitNum, bytes);
        return bitVector;
    }

    static BitVector create(int bitNum, BigInteger bigInteger) {
        CombinedBitVector bitVector = new CombinedBitVector();
        bitVector.innerBitVector = BigIntegerBitVector.create(bitNum, bigInteger);
        return bitVector;
    }

    static BitVector createRandom(int bitNum, Random random) {
        CombinedBitVector bitVector = new CombinedBitVector();
        bitVector.innerBitVector = BytesBitVector.createRandom(bitNum, random);
        return bitVector;
    }

    static BitVector createOnes(int bitNum) {
        CombinedBitVector bitVector = new CombinedBitVector();
        bitVector.innerBitVector = BytesBitVector.createOnes(bitNum);
        return bitVector;
    }

    static BitVector createZeros(int bitNum) {
        CombinedBitVector bitVector = new CombinedBitVector();
        bitVector.innerBitVector = BytesBitVector.createZeros(bitNum);
        return bitVector;
    }

    static BitVector createEmpty() {
        CombinedBitVector bitVector = new CombinedBitVector();
        // create empty usually relates to merge / split, we use BitIntegerBitVector
        bitVector.innerBitVector = BigIntegerBitVector.createEmpty();
        return bitVector;
    }

    private static BitVector create(BitVector bitVector) {
        CombinedBitVector combinedBitVector = new CombinedBitVector();
        switch (bitVector.getType()) {
            case BYTES_BIT_VECTOR:
            case BIGINTEGER_BIT_VECTOR:
                combinedBitVector.innerBitVector = bitVector;
                return combinedBitVector;
            case COMBINED_BIT_VECTOR:
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * inner bit vector
     */
    private BitVector innerBitVector;

    @Override
    public BitVectorFactory.BitVectorType getType() {
        return BitVectorFactory.BitVectorType.COMBINED_BIT_VECTOR;
    }

    private void innerBitVectorToBytesBitVector() {
        switch (innerBitVector.getType()) {
            case BYTES_BIT_VECTOR:
                break;
            case BIGINTEGER_BIT_VECTOR:
                if (innerBitVector.bitNum() == 0) {
                    innerBitVector = BytesBitVector.createEmpty();
                } else {
                    innerBitVector = BytesBitVector.create(innerBitVector.bitNum(), innerBitVector.getBytes());
                }
                break;
            case COMBINED_BIT_VECTOR:
            default:
                throw new IllegalStateException();
        }
    }

    private void innerBitVectorToBigIntegerBitVector() {
        switch (innerBitVector.getType()) {
            case BYTES_BIT_VECTOR:
                if (innerBitVector.bitNum() == 0) {
                    innerBitVector = BigIntegerBitVector.createEmpty();
                } else {
                    innerBitVector = BigIntegerBitVector.create(innerBitVector.bitNum(), innerBitVector.getBigInteger());
                }
                break;
            case BIGINTEGER_BIT_VECTOR:
                break;
            case COMBINED_BIT_VECTOR:
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void set(int index, boolean value) {
        innerBitVectorToBytesBitVector();
        innerBitVector.set(index, value);
    }

    @Override
    public boolean get(int index) {
        return innerBitVector.get(index);
    }

    @Override
    public BitVector copy() {
        CombinedBitVector copyBitVector = new CombinedBitVector();
        copyBitVector.innerBitVector = innerBitVector.copy();
        return copyBitVector;
    }

    @Override
    public void replaceCopy(BitVector that) {
        innerBitVector.replaceCopy(that);
    }

    @Override
    public int bitNum() {
        return innerBitVector.bitNum();
    }

    @Override
    public int byteNum() {
        return innerBitVector.byteNum();
    }

    @Override
    public byte[] getBytes() {
        return innerBitVector.getBytes();
    }

    @Override
    public BigInteger getBigInteger() {
        return innerBitVector.getBigInteger();
    }

    @Override
    public BitVector split(int bitNum) {
        innerBitVectorToBigIntegerBitVector();
        return create(innerBitVector.split(bitNum));
    }

    @Override
    public void reduce(int bitNum) {
//        innerBitVectorToBigIntegerBitVector();
        innerBitVector.reduce(bitNum);
    }

    @Override
    public void merge(BitVector that) {
        innerBitVectorToBigIntegerBitVector();
        innerBitVector.merge(that);
    }

    @Override
    public BitVector xor(BitVector that) {
        innerBitVectorToBytesBitVector();
        return create(innerBitVector.xor(that));
    }

    @Override
    public void xori(BitVector that) {
        innerBitVectorToBytesBitVector();
        innerBitVector.xori(that);
    }

    @Override
    public BitVector and(BitVector that) {
        innerBitVectorToBytesBitVector();
        return create(innerBitVector.and(that));
    }

    @Override
    public void andi(BitVector that) {
        innerBitVectorToBytesBitVector();
        innerBitVector.andi(that);
    }

    @Override
    public BitVector or(BitVector that) {
        innerBitVectorToBytesBitVector();
        return create(innerBitVector.or(that));
    }

    @Override
    public void ori(BitVector that) {
        innerBitVectorToBytesBitVector();
        innerBitVector.ori(that);
    }

    @Override
    public BitVector not() {
        innerBitVectorToBytesBitVector();
        return create(innerBitVector.not());
    }

    @Override
    public void noti() {
        innerBitVectorToBytesBitVector();
        innerBitVector.noti();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(getBytes())
            .append(bitNum())
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BitVector) {
            BitVector that = (BitVector) obj;
            return new EqualsBuilder()
                .append(this.getBytes(), that.getBytes())
                .append(this.bitNum(), that.bitNum())
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return innerBitVector.toString();
    }

    @Override
    public void extendBitNum(int extendBitNum) {
        innerBitVector.extendBitNum(extendBitNum);
    }

    @Override
    public BitVector padShiftLeft(int n) {
        return innerBitVector.padShiftLeft(n);
    }

    @Override
    public void fixShiftLefti(int n) {
        innerBitVector.fixShiftLefti(n);
    }

    @Override
    public BitVector reduceShiftRight(int n) {
        return innerBitVector.reduceShiftRight(n);
    }

    @Override
    public void reduceShiftRighti(int n) {
        innerBitVector.reduceShiftRighti(n);
    }

    @Override
    public void fixShiftRighti(int n) {
        innerBitVector.fixShiftRighti(n);
    }

    @Override
    public void setBytes(byte[] source, int srcPos, int thisPos, int byteLength) {
        innerBitVector.setBytes(source, srcPos, thisPos, byteLength);
    }

    @Override
    public BitVector[] uncheckSplitWithPadding(int[] bitNums) {
        BitVector[] res = innerBitVector.uncheckSplitWithPadding(bitNums);
        return Arrays.stream(res).map(CombinedBitVector::create).toArray(BitVector[]::new);
    }

    @Override
    public void reverseBits() {
        innerBitVector.reverseBits();
    }

    @Override
    public boolean numOf1IsOdd() {
        return innerBitVector.numOf1IsOdd();
    }
}
