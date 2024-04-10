package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Square Z2 vector ([x]). The share is of the form: x = x_0 ⊕ x_1.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class SquareZ2Vector implements MpcZ2Vector {
    /**
     * the bit vector
     */
    private BitVector bitVector;
    /**
     * the plain state.
     */
    private boolean plain;

    /**
     * Create a share bit vector.
     *
     * @param bitNum the bit num.
     * @param bytes  the assigned bits represented by bytes.
     * @param plain  the plain state.
     * @return a share bit vector.
     */
    public static SquareZ2Vector create(int bitNum, byte[] bytes, boolean plain) {
        SquareZ2Vector shareBitVector = new SquareZ2Vector();
        shareBitVector.bitVector = BitVectorFactory.create(bitNum, bytes);
        shareBitVector.plain = plain;

        return shareBitVector;
    }

    /**
     * Create a share bit vector.
     *
     * @param bitVector the bit vector.
     * @param plain     the plain state.
     * @return a share bit vector.
     */
    public static SquareZ2Vector create(BitVector bitVector, boolean plain) {
        SquareZ2Vector shareBitVector = new SquareZ2Vector();
        shareBitVector.bitVector = bitVector;
        shareBitVector.plain = plain;

        return shareBitVector;
    }

    /**
     * Create a (plain) share bit vector with all bits equal assigned boolean.
     *
     * @param bitNum the bit num.
     * @param value  assigned value.
     * @return a share bit vector.
     */
    public static SquareZ2Vector create(int bitNum, boolean value) {
        return value ? SquareZ2Vector.createOnes(bitNum) : SquareZ2Vector.createZeros(bitNum);
    }

    /**
     * Create a (plain) random share bit vector.
     *
     * @param bitNum       the bit num.
     * @param secureRandom the random states.
     * @return a share bit vector.
     */
    public static SquareZ2Vector createRandom(int bitNum, SecureRandom secureRandom) {
        SquareZ2Vector shareBitVector = new SquareZ2Vector();
        shareBitVector.bitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        shareBitVector.plain = true;

        return shareBitVector;
    }

    /**
     * Create a (plain) all-one share bit vector.
     *
     * @param bitNum the bit num.
     * @return a share bit vector.
     */
    public static SquareZ2Vector createOnes(int bitNum) {
        SquareZ2Vector squareShareBitVector = new SquareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createOnes(bitNum);
        squareShareBitVector.plain = true;

        return squareShareBitVector;
    }

    /**
     * Create a (plain) all-zero bit vector.
     *
     * @param bitNum the bit num.
     * @return a share bit vector.
     */
    public static SquareZ2Vector createZeros(int bitNum) {
        SquareZ2Vector squareShareBitVector = new SquareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createZeros(bitNum);
        squareShareBitVector.plain = true;

        return squareShareBitVector;
    }

    /**
     * Create an empty share bit vector.
     *
     * @param plain the plain state.
     * @return a share bit vector.
     */
    public static SquareZ2Vector createEmpty(boolean plain) {
        SquareZ2Vector squareShareBitVector = new SquareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createEmpty();
        squareShareBitVector.plain = plain;

        return squareShareBitVector;
    }

    private SquareZ2Vector() {
        // empty
    }

    @Override
    public SquareZ2Vector copy() {
        SquareZ2Vector clone = new SquareZ2Vector();
        clone.bitVector = bitVector.copy();
        clone.plain = plain;

        return clone;
    }

    @Override
    public int getNum() {
        return bitVector.bitNum();
    }

    @Override
    public int byteNum() {
        return bitVector.byteNum();
    }

    @Override
    public void reverseBits() {
        bitVector.reverseBits();
    }

    @Override
    public SquareZ2Vector[] splitWithPadding(int[] bitNums) {
        BitVector[] splitBitVectors = getBitVector().uncheckSplitWithPadding(bitNums);
        return Arrays.stream(splitBitVectors).map(each -> create(each, plain)).toArray(SquareZ2Vector[]::new);
    }

    @Override
    public SquareZ2Vector extendBitsWithSkip(int destBitLen, int skipLen) {
        byte[] destByte = Z2VectorUtils.extendBitsWithSkip(this.bitVector, destBitLen, skipLen);
        return create(destBitLen, destByte, plain);
    }

    @Override
    public SquareZ2Vector[] getBitsWithSkip(int totalBitNum, int skipLen) {
        byte[][] res0 = Z2VectorUtils.getBitsWithSkip(bitVector, totalBitNum, skipLen);
        return Arrays.stream(res0).map(bytes -> create(totalBitNum, bytes, plain)).toArray(SquareZ2Vector[]::new);
    }

    @Override
    public SquareZ2Vector getPointsWithFixedSpace(int startPos, int num, int skipLen) {
        return create(bitVector.getBitsByInterval(startPos, num, skipLen), plain);
    }

    @Override
    public BitVector getBitVector() {
        return bitVector;
    }

    @Override
    public BitVector[] getBitVectors() {
        return new BitVector[]{bitVector};
    }

    @Override
    public void setBitVectors(BitVector... data) {
        MathPreconditions.checkEqual("data.length", "1", data.length, 1);
        this.bitVector = data[0];
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public SquareZ2Vector split(int bitNum) {
        BitVector splitBitVector = bitVector.split(bitNum);
        return SquareZ2Vector.create(splitBitVector, plain);
    }

    @Override
    public void reduce(int bitNum) {
        bitVector.reduce(bitNum);
    }

    @Override
    public void merge(MpcVector other) {
        SquareZ2Vector that = (SquareZ2Vector) other;
        assert this.plain == that.isPlain() : "merged ones must have the same public state";
        bitVector.merge(that.getBitVector());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(bitVector)
            .append(plain)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SquareZ2Vector) {
            SquareZ2Vector that = (SquareZ2Vector) obj;
            return new EqualsBuilder()
                .append(this.bitVector, that.bitVector)
                .append(this.plain, that.plain)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", plain ? "plain" : "secret", bitVector.toString());
    }
}
