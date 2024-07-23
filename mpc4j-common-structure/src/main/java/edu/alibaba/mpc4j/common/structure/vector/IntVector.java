package edu.alibaba.mpc4j.common.structure.vector;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSampler;
import edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.DiscGaussSamplerType;
import edu.alibaba.mpc4j.common.structure.StructureUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.createInstance;

/**
 * int vector.
 *
 * @author Weiran Liu
 * @date 2024/7/5
 */
public class IntVector implements RingVector {
    /**
     * Merges vectors.
     *
     * @param vectors vectors.
     * @return vector.
     */
    public static IntVector merge(IntVector[] vectors) {
        int sumLength = Arrays.stream(vectors).mapToInt(IntVector::getNum).sum();
        MathPreconditions.checkPositive("merged_length", sumLength);
        int[] data = new int[sumLength];
        for (int i = 0, pos = 0; i < vectors.length; i++) {
            System.arraycopy(vectors[i].elements, 0, data, pos, vectors[i].getNum());
            pos += vectors[i].getNum();
        }
        return IntVector.create(data);
    }

    /**
     * Splits vector into vectors.
     *
     * @param splits num for each split vectors.
     * @return split vectors.
     */
    public static IntVector[] split(IntVector mergedVector, int[] splits) {
        int sumLength = Arrays.stream(splits).sum();
        MathPreconditions.checkEqual("num", "sumLength", mergedVector.getNum(), sumLength);
        IntVector[] vectors = new IntVector[splits.length];
        for (int i = 0, pos = 0; i < splits.length; pos += splits[i], i++) {
            vectors[i] = IntVector.create(Arrays.copyOfRange(mergedVector.elements, pos, pos + splits[i]));
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
    public static IntVector[] decompose(IntVector vector, int p) {
        MathPreconditions.checkInRangeClosed("p", p, 2, Integer.MAX_VALUE);
        int size = (int) Math.ceil(Integer.SIZE / Math.log(p));
        IntVector[] decomposedVectors = new IntVector[size];
        IntVector tempVector = vector.copy();
        for (int i = size - 1; i >= 0; i--) {
            decomposedVectors[i] = tempVector.remainder(p);
            tempVector.divi(p);
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
    public static IntVector compose(IntVector[] vectors, int p) {
        MathPreconditions.checkInRangeClosed("p", p, 2, Integer.MAX_VALUE);
        int size = (int) Math.ceil(Integer.SIZE / Math.log(p));
        MathPreconditions.checkEqual("vectors.length", "size", vectors.length, size);
        int num = vectors[0].getNum();
        IntVector vector = IntVector.createZeros(num);
        for (int i = 0; i < size; i++) {
            MathPreconditions.checkEqual("num", "vector.length", num, vectors[i].getNum());
            vector.muli(p);
            vector.addi(vectors[i]);
        }
        return vector;
    }

    /**
     * Decomposes the vector to byte vectors by treating each element as a byte element vector.
     *
     * @param vector vector.
     * @return decomposed vector.
     */
    public static IntVector[] decomposeToByteVector(IntVector vector) {
        int size = Integer.SIZE / Byte.SIZE;
        IntVector[] decomposedVectors = new IntVector[size];
        IntVector tempVector = vector.copy();
        for (int i = size - 1; i >= 0; i--) {
            decomposedVectors[i] = tempVector.remainderByte();
            tempVector.shiftRighti(Byte.SIZE);
        }
        return decomposedVectors;
    }

    /**
     * Composes byte vectors to a vector.
     *
     * @param vectors vectors.
     * @return composed vector.
     */
    public static IntVector composeByteVector(IntVector[] vectors) {
        int size = Integer.SIZE / Byte.SIZE;
        MathPreconditions.checkEqual("vectors.length", "size", vectors.length, size);
        int num = vectors[0].getNum();
        IntVector vector = IntVector.createZeros(num);
        for (int i = 0; i < size; i++) {
            MathPreconditions.checkEqual("num", "vector.length", num, vectors[i].getNum());
            vector.shiftLefti(Byte.SIZE);
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
    public static IntVector copyOfRange(IntVector other, int from, int to) {
        MathPreconditions.checkNonNegativeInRange("from", from, to);
        MathPreconditions.checkLessOrEqual("to", other.elements.length, to);
        return create(Arrays.copyOfRange(other.elements, from, to));
    }

    /**
     * Creates a vector.
     *
     * @param elements elements.
     * @return a vector.
     */
    public static IntVector create(int[] elements) {
        IntVector vector = new IntVector();
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
    public static IntVector createRandom(int num, SecureRandom secureRandom) {
        IntVector vector = new IntVector();
        vector.elements = IntStream.range(0, num)
            .map(index -> secureRandom.nextInt())
            .toArray();
        return vector;
    }

    /**
     * Creates an all-one vector.
     *
     * @param num num.
     * @return an all-one vector.
     */
    public static IntVector createOnes(int num) {
        IntVector vector = new IntVector();
        vector.elements = new int[num];
        Arrays.fill(vector.elements, 1);
        return vector;
    }

    /**
     * Creates an all-zero vector.
     *
     * @param num the num.
     * @return an all-zero vector.
     */
    public static IntVector createZeros(int num) {
        IntVector vector = new IntVector();
        vector.elements = new int[num];
        return vector;
    }

    /**
     * Creates an empty vector.
     *
     * @return an empty vector.
     */
    public static IntVector createEmpty() {
        IntVector vector = new IntVector();
        vector.elements = new int[0];
        return vector;
    }

    /**
     * Creates a Gaussian sample vector.
     *
     * @param num   the num.
     * @param sigma the width parameter σ.
     * @return a vector.
     */
    public static IntVector createGaussian(int num, double sigma) {
        IntVector vector = new IntVector();
        DiscGaussSampler discGaussSampler = createInstance(DiscGaussSamplerType.CONVOLUTION, 0, sigma);
        vector.elements = IntStream.range(0, num).map(i -> {
            int noise = discGaussSampler.sample();
            // we need to correct negative noise
            if (noise < 0) {
                noise = -noise;
            }
            return noise;
        }).toArray();
        return vector;
    }

    /**
     * elements
     */
    private int[] elements;

    /**
     * private constructor.
     */
    private IntVector() {
        // empty
    }

    @Override
    public IntVector copy() {
        IntVector copy = new IntVector();
        copy.elements = IntUtils.clone(elements);
        return copy;
    }

    @Override
    public int getNum() {
        return elements.length;
    }

    @Override
    public IntVector add(RingVector other) {
        IntVector that = (IntVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        return create(IntStream.range(0, elements.length).map(i -> elements[i] + that.elements[i]).toArray());
    }

    @Override
    public void addi(RingVector other) {
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), other.getNum());
        IntVector that = (IntVector) other;
        IntStream.range(0, elements.length).forEach(i -> elements[i] += that.elements[i]);
    }

    @Override
    public IntVector neg() {
        return create(Arrays.stream(elements).map(element -> -element).toArray());
    }

    @Override
    public void negi() {
        IntStream.range(0, elements.length).forEach(i -> elements[i] = -elements[i]);
    }

    @Override
    public IntVector sub(RingVector other) {
        IntVector that = (IntVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        return create(IntStream.range(0, elements.length).map(i -> elements[i] - that.elements[i]).toArray());
    }

    @Override
    public void subi(RingVector other) {
        IntVector that = (IntVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream.range(0, elements.length).forEach(i -> elements[i] -= that.elements[i]);
    }

    @Override
    public IntVector mul(RingVector other) {
        IntVector that = (IntVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        return create(IntStream.range(0, elements.length).map(i -> elements[i] * that.elements[i]).toArray());
    }

    @Override
    public void muli(RingVector other) {
        IntVector that = (IntVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream.range(0, elements.length).forEach(i -> elements[i] = elements[i] * that.elements[i]);
    }

    /**
     * Adds the value.
     *
     * @param i     index.
     * @param value value.
     */
    public void addi(int i, int value) {
        elements[i] += value;
    }

    /**
     * Multiplies a value.
     *
     * @param value value.
     * @return result.
     */
    public IntVector mul(int value) {
        int[] mulElements = new int[elements.length];
        for (int i = 0; i < elements.length; i++) {
            mulElements[i] = elements[i] * value;
        }
        return create(mulElements);
    }

    /**
     * Multiplies a value.
     *
     * @param value value.
     */
    public void muli(int value) {
        for (int i = 0; i < elements.length; i++) {
            elements[i] = elements[i] * value;
        }
    }

    /**
     * Shift left.
     *
     * @param bit bit.
     */
    public void shiftLefti(int bit) {
        for (int i = 0; i < elements.length; i++) {
            elements[i] = elements[i] << bit;
        }
    }

    /**
     * Shift right.
     *
     * @param bit bit.
     */
    public void shiftRighti(int bit) {
        for (int i = 0; i < elements.length; i++) {
            elements[i] = elements[i] >>> bit;
        }
    }

    /**
     * Computes (this / value), done under unsigned form.
     *
     * @param val value.
     */
    public void divi(int val) {
        Preconditions.checkArgument(val != 0);
        for (int i = 0; i < elements.length; i++) {
            elements[i] = Integer.divideUnsigned(elements[i], val);
        }
    }

    /**
     * Computes (this % val), done under unsigned form.
     *
     * @param val value.
     * @return the remainder (this % val).
     */
    public IntVector remainder(int val) {
        Preconditions.checkArgument(val != 0);
        int[] remainderElements = new int[elements.length];
        for (int i = 0; i < elements.length; i++) {
            remainderElements[i] = Integer.remainderUnsigned(elements[i], val);
        }
        return IntVector.create(remainderElements);
    }

    /**
     * Computes this % Byte.
     *
     * @return this % Byte.
     */
    public IntVector remainderByte() {
        int[] remainderElements = new int[elements.length];
        for (int i = 0; i < elements.length; i++) {
            remainderElements[i] = (elements[i] & 0xFF);
        }
        return IntVector.create(remainderElements);
    }

    /**
     * Modulus each element by 2^l.
     *
     * @param l bit length.
     */
    public void module(int l) {
        MathPreconditions.checkPositiveInRangeClosed("l", l, Integer.SIZE);
        // do not need to operate when l = Integer.SIZE.
        if (l < Integer.SIZE) {
            int andModule = (1 << l) - 1;
            for (int i = 0; i < elements.length; i++) {
                elements[i] = (elements[i] & andModule);
            }
        }
    }

    @Override
    public IntVector split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        int[] splitElements = new int[splitNum];
        int[] remainElements = new int[num - splitNum];
        System.arraycopy(elements, num - splitNum, splitElements, 0, splitNum);
        System.arraycopy(elements, 0, remainElements, 0, num - splitNum);
        elements = remainElements;
        return IntVector.create(splitElements);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // reduce if the reduced rows is less than rows.
            int[] remainElements = new int[reduceNum];
            System.arraycopy(elements, 0, remainElements, 0, reduceNum);
            elements = remainElements;
        }
    }

    @Override
    public void merge(Vector other) {
        IntVector that = (IntVector) other;
        int[] mergeElements = new int[this.elements.length + that.elements.length];
        System.arraycopy(this.elements, 0, mergeElements, 0, this.elements.length);
        System.arraycopy(that.elements, 0, mergeElements, this.elements.length, that.elements.length);
        elements = mergeElements;
    }

    /**
     * Sets element.
     *
     * @param index   index.
     * @param element element.
     */
    public void setElement(int index, int element) {
        elements[index] = element;
    }

    /**
     * Gets the element.
     *
     * @param index index.
     * @return element.
     */
    public int getElement(int index) {
        return elements[index];
    }

    /**
     * Gets elements.
     *
     * @return elements.
     */
    public int[] getElements() {
        return elements;
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
        MathPreconditions.checkPositive("interval", interval);
        IntVector sourceVector = (IntVector) source;
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
    public IntVector getElementsByInterval(int pos, int num, int interval) {
        MathPreconditions.checkPositive("interval", interval);
        int[] intervalElements = IntStream.range(0, num).map(i -> elements[pos + i * interval]).toArray();
        return create(intervalElements);
    }

    /**
     * Gets the sum of all elements.
     *
     * @return the sum of all elements.
     */
    public int sum() {
        return Arrays.stream(elements).sum();
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
        if (obj instanceof IntVector that) {
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
