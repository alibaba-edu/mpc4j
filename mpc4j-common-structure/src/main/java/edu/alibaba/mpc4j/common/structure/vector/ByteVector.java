package edu.alibaba.mpc4j.common.structure.vector;

import edu.alibaba.mpc4j.common.structure.StructureUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * byte vector.
 *
 * @author Weiran Liu
 * @date 2024/6/7
 */
public class ByteVector implements RingVector {
    /**
     * elements
     */
    private byte[] elements;

    /**
     * Creates a vector.
     *
     * @param elements elements.
     * @return a vector.
     */
    public static ByteVector create(byte[] elements) {
        ByteVector vector = new ByteVector();
        MathPreconditions.checkPositive("num", elements.length);
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
    public static ByteVector createRandom(int num, SecureRandom secureRandom) {
        ByteVector vector = new ByteVector();
        MathPreconditions.checkPositive("num", num);
        vector.elements = new byte[num];
        secureRandom.nextBytes(vector.elements);
        return vector;
    }

    /**
     * Creates an all-one vector.
     *
     * @param num num.
     * @return an all-one vector.
     */
    public static ByteVector createOnes(int num) {
        ByteVector vector = new ByteVector();
        MathPreconditions.checkPositive("num", num);
        vector.elements = new byte[num];
        Arrays.fill(vector.elements, (byte) 0b00000001);
        return vector;
    }

    /**
     * Creates an all-zero vector.
     *
     * @param num num.
     * @return an all-zero vector.
     */
    public static ByteVector createZeros(int num) {
        ByteVector vector = new ByteVector();
        MathPreconditions.checkPositive("num", num);
        vector.elements = new byte[num];
        return vector;
    }

    /**
     * Creates an empty vector.
     *
     * @return an empty vector.
     */
    public static ByteVector createEmpty() {
        ByteVector vector = new ByteVector();
        vector.elements = new byte[0];
        return vector;
    }

    /**
     * private constructor.
     */
    private ByteVector() {
        // empty
    }

    @Override
    public ByteVector add(RingVector other) {
        ByteVector that = (ByteVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        int num = getNum();
        byte[] results = new byte[num];
        System.arraycopy(this.elements, 0, results, 0, num);
        for (int i = 0; i < num; i++) {
            results[i] += that.elements[i];
        }
        return create(results);
    }

    @Override
    public void addi(RingVector other) {
        ByteVector that = (ByteVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        int num = getNum();
        for (int i = 0; i < num; i++) {
            this.elements[i] += that.elements[i];
        }
    }

    @Override
    public ByteVector neg() {
        int num = getNum();
        byte[] results = new byte[num];
        for (int i = 0; i < num; i++) {
            results[i] = (byte) (-elements[i] & 0xFF);
        }
        return create(results);
    }

    @Override
    public void negi() {
        int num = getNum();
        for (int i = 0; i < num; i++) {
            elements[i] = (byte) (-elements[i] & 0xFF);
        }
    }

    @Override
    public ByteVector sub(RingVector other) {
        ByteVector that = (ByteVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        int num = getNum();
        byte[] results = new byte[num];
        System.arraycopy(this.elements, 0, results, 0, num);
        for (int i = 0; i < num; i++) {
            results[i] -= that.elements[i];
        }
        return create(results);
    }

    @Override
    public void subi(RingVector other) {
        ByteVector that = (ByteVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        int num = getNum();
        for (int i = 0; i < num; i++) {
            this.elements[i] -= that.elements[i];
        }
    }

    @Override
    public ByteVector mul(RingVector other) {
        ByteVector that = (ByteVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        int num = getNum();
        byte[] results = new byte[num];
        System.arraycopy(this.elements, 0, results, 0, num);
        for (int i = 0; i < num; i++) {
            results[i] *= that.elements[i];
        }
        return create(results);
    }

    @Override
    public void muli(RingVector other) {
        ByteVector that = (ByteVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        int num = getNum();
        for (int i = 0; i < num; i++) {
            this.elements[i] *= that.elements[i];
        }
    }

    /**
     * Multiplies a value.
     *
     * @param value value.
     * @return result.
     */
    public ByteVector mul(byte value) {
        byte[] mulElements = new byte[elements.length];
        for (int i = 0; i < mulElements.length; i++) {
            mulElements[i] = (byte) ((elements[i] * value) & 0xFF);
        }
        return create(mulElements);
    }

    /**
     * Multiplies a value.
     *
     * @param value value.
     */
    public void muli(byte value) {
        for (int i = 0; i < elements.length; i++) {
            elements[i] = (byte) ((elements[i] * value) & 0xFF);
        }
    }

    @Override
    public ByteVector copy() {
        return ByteVector.create(Arrays.copyOf(elements, elements.length));
    }

    @Override
    public int getNum() {
        return elements.length;
    }

    @Override
    public ByteVector split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        byte[] splitElements = new byte[splitNum];
        byte[] remainElements = new byte[num - splitNum];
        System.arraycopy(elements, num - splitNum, splitElements, 0, splitNum);
        System.arraycopy(elements, 0, remainElements, 0, num - splitNum);
        elements = remainElements;
        return ByteVector.create(splitElements);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // reduce if the reduced rows is less than rows.
            byte[] remainElements = new byte[reduceNum];
            System.arraycopy(elements, num - reduceNum, remainElements, 0, reduceNum);
            elements = remainElements;
        }
    }

    @Override
    public void merge(Vector other) {
        ByteVector that = (ByteVector) other;
        byte[] mergeElements = new byte[this.elements.length + that.elements.length];
        System.arraycopy(this.elements, 0, mergeElements, 0, this.elements.length);
        System.arraycopy(that.elements, 0, mergeElements, this.elements.length, that.elements.length);
        elements = mergeElements;
    }

    /**
     * Gets element[index].
     *
     * @param index index.
     * @return element[index].
     */
    public byte getElement(int index) {
        return elements[index];
    }

    /**
     * Sets element[index].
     *
     * @param index   index.
     * @param element element[index].
     */
    public void setElement(int index, byte element) {
        elements[index] = element;
    }

    /**
     * Gets elements.
     *
     * @return elements.
     */
    public byte[] getElements() {
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
        if (obj instanceof ByteVector that) {
            return Arrays.equals(this.elements, that.elements);
        }
        return false;
    }

    @Override
    public String toString() {
        int displayNum = Math.min(elements.length, StructureUtils.DISPLAY_NUM);
        String[] stringData = new String[displayNum];
        for (int i = 0; i < displayNum; i++) {
            stringData[i] = String.valueOf(elements[i]);
        }
        return this.getClass().getSimpleName() + Arrays.toString(stringData);
    }
}
