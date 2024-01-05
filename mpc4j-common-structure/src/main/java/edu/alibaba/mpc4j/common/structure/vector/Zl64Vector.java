package edu.alibaba.mpc4j.common.structure.vector;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSampler;
import edu.alibaba.mpc4j.common.structure.matrix.MatrixUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.*;
import static edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.createInstance;

/**
 * the Zl64 vector.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class Zl64Vector implements RingVector {
    /**
     * Zl instance
     */
    private final Zl64 zl64;
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
     * @param zl64     Zl64 instance.
     * @param elements elements.
     * @return a vector.
     */
    public static Zl64Vector create(Zl64 zl64, long[] elements) {
        Zl64Vector vector = new Zl64Vector(zl64);
        MathPreconditions.checkPositive("num", elements.length);
        vector.elements = Arrays.stream(elements)
            .peek(element -> Preconditions.checkArgument(zl64.validateElement(element)))
            .toArray();
        return vector;
    }

    /**
     * Creates a random vector.
     *
     * @param zl64         Zl64 instance.
     * @param num          the num.
     * @param secureRandom the random state.
     * @return a vector.
     */
    public static Zl64Vector createRandom(Zl64 zl64, int num, SecureRandom secureRandom) {
        Zl64Vector vector = new Zl64Vector(zl64);
        MathPreconditions.checkPositive("num", num);
        vector.elements = IntStream.range(0, num)
            .mapToLong(index -> zl64.createRandom(secureRandom))
            .toArray();
        return vector;
    }

    /**
     * Creates an all-one vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  the num.
     * @return a vector.
     */
    public static Zl64Vector createOnes(Zl64 zl64, int num) {
        Zl64Vector vector = new Zl64Vector(zl64);
        MathPreconditions.checkPositive("num", num);
        vector.elements = IntStream.range(0, num)
            .mapToLong(index -> zl64.createOne())
            .toArray();
        return vector;
    }

    /**
     * Creates a Gaussian sample vector.
     *
     * @param zl64  Zl64 instance.
     * @param num   the num.
     * @param c     the mean of the distribution c.
     * @param sigma the width parameter σ.
     * @return a vector.
     */
    public static Zl64Vector createGaussianSample(Zl64 zl64, int num, int c, double sigma) {
        Zl64Vector vector = new Zl64Vector(zl64);
        MathPreconditions.checkPositive("num", num);
        DiscGaussSampler discGaussSampler = createInstance(DiscGaussSamplerType.CONVOLUTION, c, sigma);
        vector.elements = IntStream.range(0, num).mapToLong(i -> zl64.module(discGaussSampler.sample())).toArray();
        return vector;
    }

    /**
     * Creates an all-zero vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  the num.
     * @return a vector.
     */
    public static Zl64Vector createZeros(Zl64 zl64, int num) {
        Zl64Vector vector = new Zl64Vector(zl64);
        MathPreconditions.checkPositive("num", num);
        vector.elements = IntStream.range(0, num)
            .mapToLong(index -> zl64.createZero())
            .toArray();
        return vector;
    }

    /**
     * Creates an empty vector.
     *
     * @param zl64 Zl64 instance.
     * @return a vector.
     */
    public static Zl64Vector createEmpty(Zl64 zl64) {
        Zl64Vector vector = new Zl64Vector(zl64);
        vector.elements = new long[0];

        return vector;
    }

    private Zl64Vector(Zl64 zl64) {
        this.zl64 = zl64;
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public Zl64Vector copy() {
        long[] copyElements = LongUtils.clone(elements);
        return Zl64Vector.create(zl64, copyElements);
    }

    @Override
    public void replaceCopy(Vector other) {
        Zl64Vector that = (Zl64Vector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        int num = getNum();
        System.arraycopy(that.elements, 0, this.elements, 0, num);
    }

    @Override
    public int getNum() {
        return elements.length;
    }

    @Override
    public Zl64Vector split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        long[] subElements = new long[splitNum];
        long[] remainElements = new long[num - splitNum];
        System.arraycopy(elements, 0, subElements, 0, splitNum);
        System.arraycopy(elements, splitNum, remainElements, 0, num - splitNum);
        elements = remainElements;
        return Zl64Vector.create(zl64, subElements);
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
        Zl64Vector that = (Zl64Vector) other;
        Preconditions.checkArgument(this.zl64.equals(that.zl64));
        long[] mergeElements = new long[this.elements.length + that.elements.length];
        System.arraycopy(this.elements, 0, mergeElements, 0, this.elements.length);
        System.arraycopy(that.elements, 0, mergeElements, this.elements.length, that.elements.length);
        elements = mergeElements;
    }

    @Override
    public Zl64Vector add(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        long[] results = indexIntStream
            .mapToLong(index -> zl64.add(this.elements[index], that.elements[index]))
            .toArray();
        return Zl64Vector.create(zl64, results);
    }

    @Override
    public void addi(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> this.elements[index] = zl64.add(this.elements[index], that.elements[index]));
    }

    @Override
    public Zl64Vector neg() {
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        long[] results = indexIntStream
            .mapToLong(index -> zl64.neg(elements[index]))
            .toArray();
        return Zl64Vector.create(zl64, results);
    }

    @Override
    public void negi() {
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> elements[index] = zl64.neg(elements[index]));
    }

    @Override
    public Zl64Vector sub(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        long[] results = indexIntStream
            .mapToLong(index -> zl64.sub(this.elements[index], that.elements[index]))
            .toArray();
        return Zl64Vector.create(zl64, results);
    }

    @Override
    public void subi(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> this.elements[index] = zl64.sub(this.elements[index], that.elements[index]));
    }

    @Override
    public Zl64Vector mul(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        long[] results = indexIntStream
            .mapToLong(index -> zl64.mul(this.elements[index], that.elements[index]))
            .toArray();
        return Zl64Vector.create(zl64, results);
    }

    @Override
    public void muli(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> this.elements[index] = zl64.mul(this.elements[index], that.elements[index]));
    }

    public long innerProduct(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        int num = getNum();
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToLong(i -> zl64.mul(this.elements[i], that.elements[i]))
            .reduce(zl64::add)
            .orElseThrow(Error::new);
    }

    private void checkInputs(Zl64Vector that) {
        Preconditions.checkArgument(this.zl64.equals(that.zl64));
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
    }

    /**
     * Gets Zl64 instance.
     *
     * @return Zl64 instance.
     */
    public Zl64 getZl64() {
        return zl64;
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
            .append(zl64)
            .append(elements)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Zl64Vector) {
            Zl64Vector that = (Zl64Vector) obj;
            if (this.getNum() != that.getNum()) {
                return false;
            }
            return new EqualsBuilder()
                .append(this.zl64, that.zl64)
                .append(this.elements, that.elements)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        String[] stringData = Arrays.stream(Arrays.copyOf(elements, Math.min(elements.length, MatrixUtils.DISPLAY_NUM)))
            .mapToObj(String::valueOf)
            .toArray(String[]::new);
        return this.getClass().getSimpleName() + " (l = " + zl64.getL() + "): " + Arrays.toString(stringData);
    }
}
