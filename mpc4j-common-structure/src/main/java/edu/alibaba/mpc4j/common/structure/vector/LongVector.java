package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.matrix.MatrixUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * long vector.
 *
 * @author Feng Han
 * @date 2023/1/8
 */
public class LongVector implements RingVector {
    /**
     * threshold for parallel computation
     */
    private static final int PARALLEL_THRESHOLD = 1024;
    /**
     * elements
     */
    private long[] elements;
    /**
     * parallel operation.
     */
    private boolean parallel;

    /**
     * Creates a vector.
     *
     * @param elements elements.
     * @return a vector.
     */
    public static LongVector create(long[] elements) {
        LongVector vector = new LongVector();
        MathPreconditions.checkPositive("num", elements.length);
        vector.elements = elements;
        return vector;
    }

    /**
     * Creates a random vector.
     *
     * @param num          the num.
     * @param secureRandom the random state.
     * @return a vector.
     */
    public static LongVector createRandom(int num, SecureRandom secureRandom) {
        LongVector vector = new LongVector();
        MathPreconditions.checkPositive("num", num);
        vector.elements = IntStream.range(0, num)
            .mapToLong(index -> secureRandom.nextLong())
            .toArray();
        return vector;
    }

    /**
     * Creates an all-one vector.
     *
     * @param num the num.
     * @return a vector.
     */
    public static LongVector createOnes(int num) {
        LongVector vector = new LongVector();
        MathPreconditions.checkPositive("num", num);
        vector.elements = new long[num];
        Arrays.fill(vector.elements, 1L);
        return vector;
    }

    /**
     * Creates an all-zero vector.
     *
     * @param num the num.
     * @return a vector.
     */
    public static LongVector createZeros(int num) {
        LongVector vector = new LongVector();
        MathPreconditions.checkPositive("num", num);
        vector.elements = new long[num];
        return vector;
    }

    /**
     * Creates an empty vector.
     *
     * @return a vector.
     */
    public static LongVector createEmpty() {
        LongVector vector = new LongVector();
        vector.elements = new long[0];
        return vector;
    }

    public static LongVector merge(LongVector[] others) {
        int lengthSum = Arrays.stream(others).mapToInt(LongVector::getNum).sum();
        assert lengthSum > 0;
        long[] data = new long[lengthSum];
        for (int i = 0, pos = 0; i < others.length; i++) {
            System.arraycopy(others[i].elements, 0, data, pos, others[i].getNum());
            pos += others[i].getNum();
        }
        return LongVector.create(data);
    }

    public static LongVector copyOfRange(LongVector other, int startIndex, int endIndex) {
        MathPreconditions.checkNonNegative("startIndex", startIndex);
        MathPreconditions.checkGreaterOrEqual("other.getNum() >= endIndex", other.getNum(), endIndex);
        return create(Arrays.copyOfRange(other.getElements(), startIndex, endIndex));
    }


    private LongVector() {

    }

    @Override
    public LongVector add(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream intStream = parallel & elements.length > PARALLEL_THRESHOLD
            ? IntStream.range(0, elements.length).parallel()
            : IntStream.range(0, elements.length);
        return create(intStream.mapToLong(i -> elements[i] + that.elements[i]).toArray());
    }

    @Override
    public void addi(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream intStream = parallel & elements.length > PARALLEL_THRESHOLD
            ? IntStream.range(0, elements.length).parallel()
            : IntStream.range(0, elements.length);
        intStream.forEach(i -> elements[i] += that.elements[i]);
    }

    @Override
    public LongVector neg() {
        LongStream longStream = parallel & elements.length > PARALLEL_THRESHOLD
            ? Arrays.stream(elements).parallel()
            : Arrays.stream(elements);
        return create(longStream.map(x -> -x).toArray());
    }

    @Override
    public void negi() {
        IntStream intStream = parallel & elements.length > PARALLEL_THRESHOLD
            ? IntStream.range(0, elements.length).parallel()
            : IntStream.range(0, elements.length);
        intStream.forEach(i -> elements[i] = -elements[i]);
    }

    @Override
    public LongVector sub(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream intStream = parallel & elements.length > PARALLEL_THRESHOLD
            ? IntStream.range(0, elements.length).parallel()
            : IntStream.range(0, elements.length);
        return create(intStream.mapToLong(i -> elements[i] - that.elements[i]).toArray());
    }

    @Override
    public void subi(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream intStream = parallel & elements.length > PARALLEL_THRESHOLD
            ? IntStream.range(0, elements.length).parallel()
            : IntStream.range(0, elements.length);
        intStream.forEach(i -> elements[i] -= that.elements[i]);
    }

    @Override
    public LongVector mul(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream intStream = parallel & elements.length > PARALLEL_THRESHOLD
            ? IntStream.range(0, elements.length).parallel()
            : IntStream.range(0, elements.length);
        return create(intStream.mapToLong(i -> elements[i] * that.elements[i]).toArray());
    }

    @Override
    public void muli(RingVector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        IntStream intStream = parallel & elements.length > PARALLEL_THRESHOLD
            ? IntStream.range(0, elements.length).parallel()
            : IntStream.range(0, elements.length);
        intStream.forEach(i -> elements[i] = elements[i] * that.elements[i]);
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public LongVector copy() {
        return LongVector.create(Arrays.copyOf(elements, elements.length));
    }

    @Override
    public void replaceCopy(Vector other) {
        LongVector that = (LongVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        System.arraycopy(that.elements, 0, this.elements, 0, that.elements.length);
    }

    @Override
    public int getNum() {
        return elements.length;
    }

    @Override
    public LongVector split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        long[] subElements = new long[splitNum];
        long[] remainElements = new long[num - splitNum];
        System.arraycopy(elements, 0, subElements, 0, splitNum);
        System.arraycopy(elements, splitNum, remainElements, 0, num - splitNum);
        elements = remainElements;
        return LongVector.create(subElements);
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
     * split the elements into multiple vectors.
     *
     * @param splitNums the data lengths.
     * @return the elements.
     */
    public LongVector[] split(int[] splitNums) {
        MathPreconditions.checkEqual("this.num", "sum(splitNums)", this.getNum(), Arrays.stream(splitNums).sum());
        LongVector[] res = new LongVector[splitNums.length];
        for (int i = 0, pos = 0; i < splitNums.length; pos += splitNums[i], i++) {
            res[i] = LongVector.create(Arrays.copyOfRange(elements, pos, pos + splitNums[i]));
        }
        return res;
    }

    /**
     * Sets values from source long vector, so that this[thisPos, thisPos + length) = source[srcPos, srcPos + length).
     *
     * @param source  source long vector.
     * @param srcPos  the start position of source long vector.
     * @param thisPos the start position of the current long vector.
     * @param length  the length of the copied data.
     */
    public void setValues(LongVector source, int srcPos, int thisPos, int length) {
        assert srcPos >= 0 && thisPos >= 0 && length >= 0;
        MathPreconditions.checkLessOrEqual("thisPos + length", thisPos + length, getNum());
        MathPreconditions.checkLessOrEqual("srcPos + length", srcPos + length, source.getNum());
        System.arraycopy(source.elements, srcPos, this.elements, thisPos, length);
    }

    /**
     * Sets elements from source long vector by setting via a given interval.
     *
     * @param source   source long vector.
     * @param pos      the start position of current long vector.
     * @param num      total number of values to set.
     * @param interval interval when setting to the current long vector.
     */
    public void setElementsByInterval(LongVector source, int pos, int num, int interval) {
        // 0 <= num < source.getNum()
        MathPreconditions.checkNonNegativeInRangeClosed("num", num, source.getNum());
        MathPreconditions.checkNonNegativeInRange("pos", pos, getNum());
        MathPreconditions.checkLess("pos + (num - 1) * interval", pos + (num - 1) * interval, getNum());
        IntStream.range(0, num).forEach(i -> elements[pos + i * interval] = source.getElement(i));
    }

    /**
     * Gets elements by extracting each value via a given interval.
     *
     * @param pos      the start position.
     * @param num      total number of values to get.
     * @param interval interval when extracting from the current vector.
     */
    public LongVector getElementsByInterval(int pos, int num, int interval) {
        MathPreconditions.checkNonNegative("pos", pos);
        // 0 <= srcPos + num < getNum()
        MathPreconditions.checkNonNegativeInRangeClosed("pos + num", pos + num, getNum());
        MathPreconditions.checkLess("pos + (num - 1) * interval", pos + (num - 1) * interval, getNum());
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

    /**
     * Formats each element by the given bitLength, that is, reduces each element in {0,1}^{bitLength}.
     *
     * @param bitLength bit length.
     */
    public void format(int bitLength) {
        MathPreconditions.checkPositiveInRangeClosed("bitLength", bitLength, Long.SIZE);
        // do not need to operate when bitLength = Long.SIZE.
        if (bitLength < Long.SIZE) {
            long andModule = (1L << bitLength) - 1;
            // we do not to create a new array.
            for (int i = 0; i < elements.length; i++) {
                elements[i] = (elements[i] & andModule);
            }
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(elements)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LongVector) {
            LongVector that = (LongVector) obj;
            if (this.getNum() != that.getNum()) {
                return false;
            }
            return Arrays.equals(this.elements, that.elements);
        }
        return false;
    }

    @Override
    public String toString() {
        String[] stringData = Arrays.stream(Arrays.copyOf(elements, Math.min(elements.length, MatrixUtils.DISPLAY_NUM)))
            .mapToObj(String::valueOf)
            .toArray(String[]::new);
        return this.getClass().getSimpleName() + Arrays.toString(stringData);
    }
}
