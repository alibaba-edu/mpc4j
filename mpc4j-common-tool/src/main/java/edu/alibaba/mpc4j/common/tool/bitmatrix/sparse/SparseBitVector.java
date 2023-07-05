package edu.alibaba.mpc4j.common.tool.bitmatrix.sparse;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * sparse bit vector x, i.e., only a small number of positions are 1. For example:
 * <p>
 * If the vector length is 8, and positions 1, 2, 5 are 1, others are 0, we set positions = (1,2,5), bitNum = 8.
 * </p>
 *
 * @author Hanwen Feng, Weiran Liu
 * @date 2022/3/2
 */

public class SparseBitVector {
    /**
     * the total number of bits in the bit vector.
     */
    private final int bitNum;
    /**
     * the positions with value 1.
     */
    private TIntArrayList positions;

    /**
     * Creates from the positions. The input positions will be de-duplicated and sorted.
     *
     * @param positions positions with value 1.
     * @param bitNum    the total number of bits.
     * @return a sparse bit vector.
     */
    public static SparseBitVector create(int[] positions, int bitNum) {
        MathPreconditions.checkPositive("bitNum", bitNum);
        SparseBitVector bitVector = new SparseBitVector(bitNum);
        if (positions.length != 0) {
            // non-zero positions
            positions = Arrays.stream(positions)
                .peek(position -> MathPreconditions.checkNonNegativeInRange("position", position, bitNum))
                .distinct()
                .toArray();
            Arrays.sort(positions);
        }
        // zero positions
        bitVector.positions = new TIntArrayList(positions);
        return bitVector;
    }

    /**
     * Creates from the positions without validate check.
     *
     * @param positions positions with value 1.
     * @param bitNum    the total number of bits.
     * @return a sparse bit vector.
     */
    public static SparseBitVector createUncheck(int[] positions, int bitNum) {
        SparseBitVector bitVector = new SparseBitVector(bitNum);
        bitVector.positions = new TIntArrayList(positions);
        return bitVector;
    }

    /**
     * Creates from the positions. The input positions will be de-duplicated and sorted.
     *
     * @param positions positions with value 1.
     * @param bitNum    the total number of bits.
     * @return a sparse bit vector.
     */
    public static SparseBitVector create(TIntArrayList positions, int bitNum) {
        MathPreconditions.checkPositive("bitNum", bitNum);
        SparseBitVector bitVector = new SparseBitVector(bitNum);
        // distinct
        TIntSet tIntSet = new TIntHashSet(positions);
        positions = new TIntArrayList(tIntSet);
        for (int i = 0; i < positions.size(); i++) {
            MathPreconditions.checkNonNegativeInRange("position", positions.get(i), bitNum);
        }
        positions.sort();
        bitVector.positions = positions;
        return bitVector;
    }

    /**
     * Creates from the positions without validate check.
     *
     * @param positions positions with value 1.
     * @param bitNum    the total number of bits.
     * @return a sparse bit vector.
     */
    public static SparseBitVector createUncheck(TIntArrayList positions, int bitNum) {
        SparseBitVector bitVector = new SparseBitVector(bitNum);
        bitVector.positions = positions;
        return bitVector;
    }

    /**
     * Creates an empty sparse bit vector.
     *
     * @param ensureCapacity the desired number of positions with value 1.
     * @param bitNum         the total number of bits.
     */
    public static SparseBitVector createEmpty(int ensureCapacity, int bitNum) {
        MathPreconditions.checkPositive("bitNum", bitNum);
        SparseBitVector bitVector = new SparseBitVector(bitNum);
        bitVector.positions = new TIntArrayList(ensureCapacity);
        return bitVector;
    }

    /**
     * Creates an empty sparse bit vector.
     *
     * @param bitNum the total number of bits.
     */
    public static SparseBitVector createEmpty(int bitNum) {
        MathPreconditions.checkPositive("bitNum", bitNum);
        SparseBitVector bitVector = new SparseBitVector(bitNum);
        bitVector.positions = new TIntArrayList();
        return bitVector;
    }

    /**
     * Creates a random sparse bit vector.
     *
     * @param size         the number of positions.
     * @param bitNum       the total number of bits.
     * @param secureRandom random state.
     * @return a random sparse bit vector.
     */
    public static SparseBitVector createRandom(int size, int bitNum, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("bitNum", bitNum);
        MathPreconditions.checkNonNegativeInRangeClosed("size", size, bitNum);
        if (size == bitNum) {
            // all entries are 1
            TIntArrayList tIntArrayList = new TIntArrayList();
            for (int i = 0; i < bitNum; i++) {
                tIntArrayList.add(i);
            }
            return SparseBitVector.createUncheck(tIntArrayList, bitNum);
        }
        TIntSet tIntSet = new TIntHashSet(size);
        while (tIntSet.size() < size) {
            int position = secureRandom.nextInt(bitNum);
            tIntSet.add(position);
        }
        TIntArrayList tIntArrayList = new TIntArrayList(tIntSet);
        tIntArrayList.sort();
        return SparseBitVector.createUncheck(tIntArrayList, bitNum);
    }

    /**
     * private constructor.
     */
    private SparseBitVector(int bitNum) {
        this.bitNum = bitNum;
    }

    /**
     * Copies the bit vector.
     *
     * @return copied result.
     */
    public SparseBitVector copy() {
        int[] cPositions = positions.toArray();
        return createUncheck(cPositions, bitNum);
    }

    /**
     * Copies the sparse bit vector with the positions assigned as positions[from ... to - 1]).
     * For example:
     * <p>
     * if positions = (1,4,6,8), from = 1, to = 3, it copies the sparse bit vector with positions = (4,6), and
     * bitNum = targetBitNum.
     * </p>
     *
     * @param fromIndex    from index.
     * @param toIndex      to index.
     * @param targetBitNum the number of bits in the copied sparse bit vector.
     * @return the copied sparse bit vector with the assigned position and bitNum.
     */
    public SparseBitVector copyOfRange(int fromIndex, int toIndex, int targetBitNum) {
        MathPreconditions.checkNonNegativeInRangeClosed("toIndex", toIndex, positions.size());
        MathPreconditions.checkNonNegativeInRangeClosed("fromIndex", fromIndex, toIndex);
        TIntArrayList subPositions = new TIntArrayList(positions.subList(fromIndex, toIndex));
        for (int i = 0; i < subPositions.size(); i++) {
            MathPreconditions.checkNonNegativeInRange("subPosition", subPositions.get(i), targetBitNum);
        }
        return createUncheck(subPositions, targetBitNum);
    }

    /**
     * Creates a cyclic shift right of the current sparse bit vector. For example:
     * <p>
     * If positions = (3,4,9), bitNum = 10, it creates a sparse bit vector of positions  = (0,4,5), bitNum = 10.
     * </p>
     *
     * @return a cyclic shift right sparse bit vector.
     */
    public SparseBitVector cyclicShiftRight() {
        int size = positions.size();
        TIntArrayList cyclicShiftRightArrayList = new TIntArrayList(size);
        if (positions.get(size - 1) == bitNum - 1) {
            // if the last position reaches bitNum, the last position becomes the first position
            cyclicShiftRightArrayList.add(0);
            // shift right other positions
            for (int i = 0; i < size - 1; i++) {
                cyclicShiftRightArrayList.add(positions.get(i) + 1);
            }
        } else {
            // simply shift right all positions
            for (int i = 0; i < size; i++) {
                cyclicShiftRightArrayList.add(positions.get(i) + 1);
            }
        }
        return createUncheck(cyclicShiftRightArrayList, bitNum);
    }

    /**
     * Creates a shift right of the current sparse bit vector. For example:
     * <p>
     * If positions = (1,3,5), bitNum = 6, shiftNum = 1, it creates a sparse bit vector of positions = (2,4), bitNum = 6.
     * </p>
     *
     * @param shiftNum number of bits to shift.
     * @return a shift right of the current sparse bit vector.
     */
    public SparseBitVector shiftRight(int shiftNum) {
        TIntArrayList shiftRightArrayList = new TIntArrayList(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            int shiftRightPosition = positions.get(i) + shiftNum;
            if (shiftRightPosition < bitNum) {
                shiftRightArrayList.add(shiftRightPosition);
            } else {
                // if we find a shift right position that beyonds bitNum, then all others must also beyond bitNum.
                break;
            }
        }
        shiftRightArrayList.trimToSize();
        return createUncheck(shiftRightArrayList, bitNum);
    }

    /**
     * Creates a sub sparse bit vector with the positions range from [0, toValue - fromValue) with the positions of the
     * current sparse bit vector.
     * <p>
     * If positions = (2,4,5,8,10), fromPosition = 3, toPosition = 6, it first creates a sub-positions = (4,5).
     * Since fromPosition = 3, it shift left 3 of (4, 5) to be (1, 2).
     * </p>
     *
     * @param fromPosition from position.
     * @param toPosition   to position.
     * @return a sub sparse bit vector.
     */
    public SparseBitVector sub(int fromPosition, int toPosition) {
        MathPreconditions.checkNonNegativeInRangeClosed("toValue", toPosition, bitNum);
        MathPreconditions.checkNonNegativeInRange("fromValue", fromPosition, toPosition);
        int[] cPositions = positions.toArray();
        int targetBitNum = toPosition - fromPosition;
        if (positions.size() == 0) {
            return createEmpty(0, targetBitNum);
        }
        int startIndexI = Arrays.binarySearch(cPositions, fromPosition);
        if (startIndexI < 0) {
            startIndexI = (startIndexI + 1) * (-1);
        }
        int endIndexI = Arrays.binarySearch(cPositions, toPosition);
        if (endIndexI < 0) {
            endIndexI = (endIndexI + 1) * (-1);
        }
        int[] subPositions = Arrays.copyOfRange(cPositions, startIndexI, endIndexI);
        for (int i = 0; i < subPositions.length; i++) {
            subPositions[i] = subPositions[i] - fromPosition;
        }
        return createUncheck(subPositions, targetBitNum);
    }

    /**
     * Gets the number of positions.
     *
     * @return the number of positions.
     */
    public int getSize() {
        return positions.size();
    }

    /**
     * Gets the total number of bits in the sparse bit vector.
     *
     * @return the total number of bits in the sparse bit vector.
     */
    public int getBitNum() {
        return bitNum;
    }

    /**
     * Adds the sparse bit vector.
     *
     * @param that that sparse bit vector.
     * @return the added sparse bit vector.
     */
    public SparseBitVector xor(SparseBitVector that) {
        MathPreconditions.checkEqual("this.bitNum", "that.bitNum", this.bitNum, that.bitNum);
        assert bitNum == that.bitNum;
        TIntArrayList addArrayList = new TIntArrayList(this.positions.size() + that.positions.size());
        // Note that the two positions are sorted. Therefore, we can use the merge idea to add in O(n) operations.
        int thisIndex = 0;
        int thatIndex = 0;
        int thisSize = this.positions.size();
        int thatSize = that.positions.size();
        while (thisIndex != thisSize && thatIndex != thatSize) {
            int thisPosition = this.positions.get(thisIndex);
            int thatPosition = that.positions.get(thatIndex);
            if (thisPosition < thatPosition) {
                addArrayList.add(thisPosition);
                thisIndex++;
            } else if (thisPosition > thatPosition) {
                addArrayList.add(thatPosition);
                thatIndex++;
            } else {
                ++thisIndex;
                ++thatIndex;
            }
        }
        // add remaining this positions
        while (thisIndex != thisSize) {
            addArrayList.add(this.positions.get(thisIndex));
            thisIndex++;
        }
        // add remaining that positions
        while (thatIndex != thatSize) {
            addArrayList.add(that.positions.get(thatIndex));
            thatIndex++;
        }
        addArrayList.trimToSize();
        return createUncheck(addArrayList, bitNum);
    }

    /**
     * Given a boolean vector v, computes t = &lt;x,v&gt;.
     *
     * @param v the boolean vector v.
     * @return the result boolean vector t.
     */
    public boolean rightMultiply(final boolean[] v) {
        MathPreconditions.checkEqual("bitNum", "v.length", bitNum, v.length);
        boolean t = false;
        for (int i = 0; i < positions.size(); i++) {
            t = (v[positions.get(i)] != t);
        }
        return t;
    }

    /**
     * Given a GF2L vector v, computes &lt;x,v&gt; by treating each entry in x as 1's in the GF2L field.
     *
     * @param v the GF2L vector v.
     * @return the result GF2L vector t.
     */
    public byte[] rightGf2lMultiply(final byte[][] v) {
        MathPreconditions.checkEqual("bitNum", "v.length", bitNum, v.length);
        byte[] t = new byte[v[0].length];
        for (int i = 0; i < positions.size(); i++) {
            BytesUtils.xori(t, v[positions.get(i)]);
        }
        return t;
    }

    /**
     * Gieven a GF2L vector v, computes t = &lt;x,v&gt; ⊕ t by treating each entry in v as 1's in the GF2L field.
     *
     * @param v the GF2E vector v.
     * @param t the GF2E element t.
     */
    public void rightGf2lMultiplyXori(final byte[][] v, byte[] t) {
        assert bitNum == v.length;
        assert v[0].length == t.length;
        for (int i = 0; i < positions.size(); i++) {
            BytesUtils.xori(t, v[positions.get(i)]);
        }
    }

    /**
     * Gets x[i].
     *
     * @param index the index.
     * @return x[i].
     */
    public boolean get(int index) {
        return positions.contains(index);
    }

    /**
     * Gets the position of the given index.
     *
     * @param index the index.
     * @return the value.
     */
    public int getPosition(int index) {
        return positions.get(index);
    }

    /**
     * Gets the first position.
     *
     * @return the first position.
     */
    public int getFirstPosition() {
        return positions.get(0);
    }

    /**
     * Gets the last position.
     *
     * @return the last position.
     */
    public int getLastPosition() {
        return positions.get(positions.size() - 1);
    }

    /**
     * Gets positions.
     *
     * @return positions.
     */
    public int[] getPositions() {
        return positions.toArray();
    }

    /**
     * Return if it is an all-zero bit vector.
     *
     * @return true if it is an all-zero bit vector; false otherwise.
     */
    public boolean isZero() {
        return positions.size() == 0;
    }

    /**
     * to byte array representation.
     *
     * @return byte array representation.
     */
    public byte[] toByteArray() {
        int byteBitNum = CommonUtils.getByteLength(bitNum);
        int offsetBitNum = byteBitNum * Byte.SIZE - bitNum;
        byte[] byteArray = new byte[byteBitNum];
        for (int position : positions.toArray()) {
            BinaryUtils.setBoolean(byteArray, offsetBitNum + position, true);
        }
        return byteArray;
    }

    /**
     * to dense representation.
     *
     * @return dense representation.
     */
    public BitVector toDense() {
        return BitVectorFactory.create(bitNum, toByteArray());
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < bitNum; i++) {
            if (get(i)) {
                stringBuilder.append(1);
            } else {
                stringBuilder.append(0);
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(positions)
            .append(bitNum)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SparseBitVector)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        SparseBitVector that = (SparseBitVector) obj;
        return new EqualsBuilder()
            .append(this.positions, that.positions)
            .append(this.bitNum, that.bitNum)
            .isEquals();
    }

}