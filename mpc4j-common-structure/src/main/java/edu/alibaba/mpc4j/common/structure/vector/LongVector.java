package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.StructureUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * long vector.
 *
 * @author Feng Han
 * @date 2023/1/8
 */
public class LongVector implements RingVector {
    /**
     * Merges vectors.
     *
     * @param vectors vectors.
     * @return vector.
     */
    public static LongVector merge(LongVector[] vectors) {
        int sumLength = Arrays.stream(vectors).mapToInt(LongVector::getNum).sum();
        MathPreconditions.checkPositive("merged_length", sumLength);
        long[] data = new long[sumLength];
        for (int i = 0, pos = 0; i < vectors.length; i++) {
            System.arraycopy(vectors[i].elements, 0, data, pos, vectors[i].getNum());
            pos += vectors[i].getNum();
        }
        return LongVector.create(data);
    }

    /**
     * Splits vector into vectors.
     *
     * @param splits num for each split vectors.
     * @return split vectors.
     */
    public static LongVector[] split(LongVector mergedVector, int[] splits) {
        int sumLength = Arrays.stream(splits).sum();
        MathPreconditions.checkEqual("num", "sumLength", mergedVector.getNum(), sumLength);
        LongVector[] vectors = new LongVector[splits.length];
        for (int i = 0, pos = 0; i < splits.length; pos += splits[i], i++) {
            vectors[i] = LongVector.create(Arrays.copyOfRange(mergedVector.elements, pos, pos + splits[i]));
        }
        return vectors;
    }

    /**
     * Decomposes the vector to base-p vectors by treating each element as a base-p element vector.
     *
     * @param vector vector.
     * @param p      base p.
     * @return decomposed vector.
     */
    public static LongVector[] decompose(LongVector vector, int p) {
        MathPreconditions.checkInRangeClosed("p", p, 2, Long.MAX_VALUE);
        int size = (int) Math.ceil(Long.SIZE / Math.log(p));
        LongVector[] decomposedVectors = new LongVector[size];
        LongVector tempVector = vector.copy();
        for (int i = size - 1; i >= 0; i--) {
            LongVector[] quotientAndRemainder = tempVector.divideAndRemainder(p);
            tempVector = quotientAndRemainder[0];
            decomposedVectors[i] = quotientAndRemainder[1];
        }
        return decomposedVectors;
    }

    /**
     * Composes base-p vectors to a vector.
     *
     * @param vectors vectors.
     * @param p       base p.
     * @return composed vector.
     */
    public static LongVector compose(LongVector[] vectors, int p) {
        MathPreconditions.checkInRangeClosed("p", p, 2, Long.MAX_VALUE);
        int size = (int) Math.ceil(Long.SIZE / Math.log(p));
        MathPreconditions.checkEqual("vectors.length", "size", vectors.length, size);
        int num = vectors[0].getNum();
        LongVector vector = LongVector.createZeros(num);
        for (int i = 0; i < size; i++) {
            MathPreconditions.checkEqual("num", "vector.length", num, vectors[i].getNum());
            vector.muli(p);
            vector.addi(vectors[i]);
        }
        return vector;
    }

    /**
     * Copies the specified range of the specified vector into a new vector.
     *
     * @param other the vector from which a range is to be copied.
     * @param from  the initial index of the range to be copied, inclusive.
     * @param to    the final index of the range to be copied, exclusive.
     * @return a new vector containing the specified range from the original vector.
     */
    public static LongVector copyOfRange(LongVector other, int from, int to) {
        MathPreconditions.checkNonNegativeInRange("from", from, to);
        MathPreconditions.checkInRangeClosed("to", to, from, other.elements.length);
        return create(Arrays.copyOfRange(other.getElements(), from, to));
    }

    /**
     * elements
     */
    private long[] elements;

    /**
     * Creates a vector.
     *
     * @param elements elements.
     * @return a vector.
     */
    public static LongVector create(long[] elements) {
        LongVector vector = new LongVector();
        vector.elements = elements;
        return vector;
    }

    /**
     * Creates a random vector.
     *
     * @param num          num.
     * @param secureRandom random state.
     * @return a random vector.
     */
    public static LongVector createRandom(int num, SecureRandom secureRandom) {
        LongVector vector = new LongVector();
        vector.elements = IntStream.range(0, num)
            .mapToLong(index -> secureRandom.nextLong())
            .toArray();
        return vector;
    }

    /**
     * Creates an all-one vector.
     *
     * @param num num.
     * @return an all-one vector.
     */
    public static LongVector createOnes(int num) {
        LongVector vector = new LongVector();
        vector.elements = new long[num];
        Arrays.fill(vector.elements, 1L);
        return vector;
    }

    /**
     * Creates an all-zero vector.
     *
     * @param num the num.
     * @return an all-zero vector.
     */
    public static LongVector createZeros(int num) {
        LongVector vector = new LongVector();
        vector.elements = new long[num];
        return vector;
    }

    /**
     * Creates an empty vector.
     *
     * @return an empty vector.
     */
    public static LongVector createEmpty() {
        LongVector vector = new LongVector();
        vector.elements = new long[0];
        return vector;
    }

    /**
     * private constructor.
     */
    private LongVector() {
        // empty
    }

    @Override
    public LongVector add(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        return create(IntStream.range(0, elements.length).mapToLong(i -> elements[i] + that.elements[i]).toArray());
    }

    @Override
    public void addi(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream.range(0, elements.length).forEach(i -> elements[i] += that.elements[i]);
    }

    @Override
    public LongVector neg() {
        return create(Arrays.stream(elements).map(x -> -x).toArray());
    }

    @Override
    public void negi() {
        IntStream.range(0, elements.length).forEach(i -> elements[i] = -elements[i]);
    }

    @Override
    public LongVector sub(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        return create(IntStream.range(0, elements.length).mapToLong(i -> elements[i] - that.elements[i]).toArray());
    }

    @Override
    public void subi(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream.range(0, elements.length).forEach(i -> elements[i] -= that.elements[i]);
    }

    /**
     * Multiplies a value.
     *
     * @param value value.
     * @return result.
     */
    public LongVector mul(long value) {
        long[] mulElements = new long[elements.length];
        for (int i = 0; i < mulElements.length; i++) {
            mulElements[i] = elements[i] * value;
        }
        return create(mulElements);
    }

    @Override
    public LongVector mul(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        return create(IntStream.range(0, elements.length).mapToLong(i -> elements[i] * that.elements[i]).toArray());
    }

    /**
     * Multiplies a value.
     *
     * @param value value.
     */
    public void muli(long value) {
        for (int i = 0; i < elements.length; i++) {
            elements[i] = elements[i] * value;
        }
    }

    @Override
    public void muli(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream.range(0, elements.length).forEach(i -> elements[i] = elements[i] * that.elements[i]);
    }

    /**
     * Divides a value, gets the quotient and the remainder.
     *
     * @param val value.
     * @return an array of two vectors: the quotient (this / val), and the remainder (this % val).
     */
    public LongVector[] divideAndRemainder(long val) {
        MathPreconditions.checkInRangeClosed("val", val, 2, Long.MAX_VALUE);
        long[] quotientElements = new long[elements.length];
        long[] remainderElements = new long[elements.length];
        for (int i = 0; i < elements.length; i++) {
            quotientElements[i] = Long.divideUnsigned(elements[i], val);
            remainderElements[i] = elements[i] - quotientElements[i] * val;
        }
        return new LongVector[]{create(quotientElements), create(remainderElements)};
    }

    /**
     * Modulus each element by 2^l.
     *
     * @param l bit length.
     */
    public void module(int l) {
        MathPreconditions.checkPositiveInRangeClosed("l", l, Long.SIZE);
        // do not need to operate when l = Long.SIZE.
        if (l < Long.SIZE) {
            long andModule = (1L << l) - 1;
            for (int i = 0; i < elements.length; i++) {
                elements[i] = (elements[i] & andModule);
            }
        }
    }

    @Override
    public LongVector copy() {
        LongVector copy = new LongVector();
        copy.elements = LongUtils.clone(elements);
        return copy;
    }

    @Override
    public int getNum() {
        return elements.length;
    }

    @Override
    public LongVector split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        long[] splitElements = new long[splitNum];
        long[] remainElements = new long[num - splitNum];
        System.arraycopy(elements, num - splitNum, splitElements, 0, splitNum);
        System.arraycopy(elements, 0, remainElements, 0, num - splitNum);
        elements = remainElements;
        return LongVector.create(splitElements);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // reduce if the reduced rows is less than rows.
            long[] remainElements = new long[reduceNum];
            System.arraycopy(elements, 0, remainElements, 0, reduceNum);
            elements = remainElements;
        }
    }

    @Override
    public void merge(Vector other) {
        LongVector that = (LongVector) other;
        long[] mergeElements = new long[this.elements.length + that.elements.length];
        System.arraycopy(this.elements, 0, mergeElements, 0, this.elements.length);
        System.arraycopy(that.elements, 0, mergeElements, this.elements.length, that.elements.length);
        elements = mergeElements;
    }

    /**
     * Sets elements from source vector, so that this[destPos, destPos + length) = source[srcPos, srcPos + length).
     *
     * @param source source vector.
     * @param srcPos start position of source vector.
     * @param pos    start position of the current vector.
     * @param length length of the copied data.
     */
    public void setElements(RingVector source, int srcPos, int pos, int length) {
        LongVector sourceVector = (LongVector) source;
        System.arraycopy(sourceVector.elements, srcPos, this.elements, pos, length);
    }

    /**
     * Sets elements from source vector by setting via a given interval.
     * That is, for i ∈ [0, num), we have this[desPos + i * interval] = source[i].
     *
     * @param source   source vector.
     * @param pos      start position of current vector.
     * @param num      total number of values to set.
     * @param interval interval.
     */
    public void setElementsByInterval(RingVector source, int pos, int num, int interval) {
        LongVector sourceVector = (LongVector) source;
        IntStream.range(0, num).forEach(i -> elements[pos + i * interval] = sourceVector.elements[i]);
    }

    /**
     * Gets elements by extracting each value via a given interval.
     * That is, for i ∈ [0, num), we have result[i] = this[desPos + i * interval].
     *
     * @param pos      the start position.
     * @param num      total number of values to extract.
     * @param interval interval when extracting from the current vector.
     */
    public LongVector getElementsByInterval(int pos, int num, int interval) {
        long[] intervalElements = IntStream.range(0, num).mapToLong(i -> elements[pos + i * interval]).toArray();
        return create(intervalElements);
    }

    /**
     * Gets the sum of all elements.
     *
     * @return the sum of all elements.
     */
    public long sum() {
        return Arrays.stream(elements).sum();
    }

    /**
     * Gets the element.
     *
     * @param index the index.
     * @return the element.
     */
    public long getElement(int index) {
        return elements[index];
    }

    /**
     * Gets the elements.
     *
     * @return the elements.
     */
    public long[] getElements() {
        return elements;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(elements).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LongVector that) {
            return Arrays.equals(this.elements, that.elements);
        }
        return false;
    }

    @Override
    public String toString() {
        int displayNum = Math.min(elements.length, StructureUtils.DISPLAY_NUM);
        String[] stringData = Arrays.stream(Arrays.copyOf(elements, displayNum))
            .mapToObj(String::valueOf)
            .toArray(String[]::new);
        return this.getClass().getSimpleName() + ": " + Arrays.toString(stringData);
    }
}
