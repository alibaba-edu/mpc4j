package edu.alibaba.mpc4j.common.structure.vector;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.StructureUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Zp64 vector.
 *
 * @author Weiran Liu
 * @date 2024/5/25
 */
public class Zp64Vector implements FieldVector {
    /**
     * merges vectors.
     *
     * @param vectors vectors.
     * @return the merged vector.
     */
    public static Zp64Vector merge(Zp64Vector[] vectors) {
        MathPreconditions.checkPositive("vectors.length", vectors.length);
        Zp64 zp64 = vectors[0].getZp64();
        int length = Arrays.stream(vectors).mapToInt(Zp64Vector::getNum).sum();
        long[] mergeElements = new long[length];
        for (int i = 0, pos = 0; i < vectors.length; i++) {
            Preconditions.checkArgument(vectors[i].zp64.equals(zp64));
            MathPreconditions.checkPositive("vector.num", vectors[i].getNum());
            System.arraycopy(vectors[i].elements, 0, mergeElements, pos, vectors[i].elements.length);
            pos += vectors[i].elements.length;
        }
        return Zp64Vector.create(zp64, mergeElements);
    }

    /**
     * Creates a vector.
     *
     * @param zp64     Zp64 instance.
     * @param elements elements.
     * @return a vector.
     */
    public static Zp64Vector create(Zp64 zp64, long[] elements) {
        MathPreconditions.checkPositive("num", elements.length);
        Zp64Vector vector = new Zp64Vector(zp64);
        vector.elements = Arrays.stream(elements)
            .peek(element -> Preconditions.checkArgument(zp64.validateElement(element)))
            .toArray();
        return vector;
    }

    /**
     * Creates a random vector.
     *
     * @param zp64         Zp64 instance.
     * @param num          num.
     * @param secureRandom random state.
     * @return a vector.
     */
    public static Zp64Vector createRandom(Zp64 zp64, int num, SecureRandom secureRandom) {
        MathPreconditions.checkPositive("num", num);
        Zp64Vector vector = new Zp64Vector(zp64);
        vector.elements = IntStream.range(0, num)
            .mapToLong(index -> zp64.createRandom(secureRandom))
            .toArray();
        return vector;
    }

    /**
     * Creates an all-one vector.
     *
     * @param zp64 Zp64 instance.
     * @param num  num.
     * @return a vector.
     */
    public static Zp64Vector createOnes(Zp64 zp64, int num) {
        MathPreconditions.checkPositive("num", num);
        Zp64Vector vector = new Zp64Vector(zp64);
        vector.elements = IntStream.range(0, num)
            .mapToLong(index -> zp64.createOne())
            .toArray();
        return vector;
    }

    /**
     * Creates an all-zero vector.
     *
     * @param zp64 Zp64 instance.
     * @param num  num.
     * @return a vector.
     */
    public static Zp64Vector createZeros(Zp64 zp64, int num) {
        MathPreconditions.checkPositive("num", num);
        Zp64Vector vector = new Zp64Vector(zp64);
        vector.elements = IntStream.range(0, num)
            .mapToLong(index -> zp64.createZero())
            .toArray();
        return vector;
    }

    /**
     * Creates an empty vector.
     *
     * @param zp64 Zp64 instance.
     * @return a vector.
     */
    public static Zp64Vector createEmpty(Zp64 zp64) {
        Zp64Vector vector = new Zp64Vector(zp64);
        vector.elements = new long[0];

        return vector;
    }

    /**
     * Zp64 instance
     */
    private final Zp64 zp64;
    /**
     * elements
     */
    private long[] elements;

    private Zp64Vector(Zp64 zp64) {
        this.zp64 = zp64;
    }

    @Override
    public Zp64Vector copy() {
        long[] copyElements = LongUtils.clone(elements);
        return Zp64Vector.create(zp64, copyElements);
    }

    @Override
    public int getNum() {
        return elements.length;
    }

    @Override
    public Zp64Vector split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, num);
        long[] splitElements = new long[splitNum];
        long[] remainElements = new long[num - splitNum];
        System.arraycopy(elements, num - splitNum, splitElements, 0, splitNum);
        System.arraycopy(elements, 0, remainElements, 0, num - splitNum);
        elements = remainElements;
        return Zp64Vector.create(zp64, splitElements);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduce_num", reduceNum, num);
        if (reduceNum < num) {
            // reduce if the reduced rows is less than rows.
            long[] remainElements = new long[reduceNum];
            System.arraycopy(elements, num - reduceNum, remainElements, 0, reduceNum);
            elements = remainElements;
        }
    }

    @Override
    public void merge(Vector other) {
        Zp64Vector that = (Zp64Vector) other;
        Preconditions.checkArgument(this.zp64.equals(that.zp64));
        long[] mergeElements = new long[this.elements.length + that.elements.length];
        System.arraycopy(this.elements, 0, mergeElements, 0, this.elements.length);
        System.arraycopy(that.elements, 0, mergeElements, this.elements.length, that.elements.length);
        elements = mergeElements;
    }

    @Override
    public Zp64Vector add(RingVector other) {
        Zp64Vector that = (Zp64Vector) other;
        checkInputs(that);
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zp64.add(this.elements[index], that.elements[index]))
            .toArray();
        return Zp64Vector.create(zp64, results);
    }

    @Override
    public void addi(RingVector other) {
        Zp64Vector that = (Zp64Vector) other;
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zp64.add(this.elements[index], that.elements[index])
        );
    }

    @Override
    public Zp64Vector neg() {
        long[] results = Arrays.stream(elements).map(zp64::neg).toArray();
        return Zp64Vector.create(zp64, results);
    }

    @Override
    public void negi() {
        IntStream.range(0, elements.length).forEach(index -> elements[index] = zp64.neg(elements[index]));
    }

    @Override
    public Zp64Vector sub(RingVector other) {
        Zp64Vector that = (Zp64Vector) other;
        checkInputs(that);
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zp64.sub(this.elements[index], that.elements[index]))
            .toArray();
        return Zp64Vector.create(zp64, results);
    }

    @Override
    public void subi(RingVector other) {
        Zp64Vector that = (Zp64Vector) other;
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zp64.sub(this.elements[index], that.elements[index])
        );
    }

    @Override
    public Zp64Vector mul(RingVector other) {
        Zp64Vector that = (Zp64Vector) other;
        checkInputs(that);
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zp64.mul(this.elements[index], that.elements[index]))
            .toArray();
        return Zp64Vector.create(zp64, results);
    }

    @Override
    public void muli(RingVector other) {
        Zp64Vector that = (Zp64Vector) other;
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zp64.mul(this.elements[index], that.elements[index])
        );
    }

    public long innerProduct(RingVector other) {
        Zp64Vector that = (Zp64Vector) other;
        checkInputs(that);
        return IntStream.range(0, elements.length)
            .mapToLong(i -> zp64.mul(this.elements[i], that.elements[i]))
            .reduce(zp64::add)
            .orElseThrow(Error::new);
    }

    @Override
    public FieldVector inv() {
        long[] results = Arrays.stream(elements).map(zp64::inv).toArray();
        return Zp64Vector.create(zp64, results);
    }

    @Override
    public void invi() {
        IntStream.range(0, elements.length).forEach(index -> elements[index] = zp64.inv(elements[index]));
    }

    @Override
    public FieldVector div(FieldVector other) {
        Zp64Vector that = (Zp64Vector) other;
        checkInputs(that);
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zp64.div(this.elements[index], that.elements[index]))
            .toArray();
        return Zp64Vector.create(zp64, results);
    }

    @Override
    public void divi(FieldVector other) {
        Zp64Vector that = (Zp64Vector) other;
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zp64.div(this.elements[index], that.elements[index])
        );
    }

    private void checkInputs(Zp64Vector that) {
        Preconditions.checkArgument(this.zp64.equals(that.zp64));
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
    }

    /**
     * Gets Zp64 instance.
     *
     * @return Zp64 instance.
     */
    public Zp64 getZp64() {
        return zp64;
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
        return new HashCodeBuilder()
            .append(zp64)
            .append(elements)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Zp64Vector that) {
            if (this.getNum() != that.getNum()) {
                return false;
            }
            return new EqualsBuilder()
                .append(this.zp64, that.zp64)
                .append(this.elements, that.elements)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        String[] stringData = Arrays.stream(Arrays.copyOf(elements, Math.min(elements.length, StructureUtils.DISPLAY_NUM)))
            .mapToObj(String::valueOf)
            .toArray(String[]::new);
        return this.getClass().getSimpleName() + " (p = " + zp64.getPrime() + "): " + Arrays.toString(stringData);
    }
}
