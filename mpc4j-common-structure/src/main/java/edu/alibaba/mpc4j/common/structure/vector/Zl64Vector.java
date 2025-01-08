package edu.alibaba.mpc4j.common.structure.vector;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSampler;
import edu.alibaba.mpc4j.common.structure.StructureUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.*;
import static edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.createInstance;

/**
 * Zl64 vector.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class Zl64Vector implements RingVector {
    /**
     * merges vectors.
     *
     * @param vectors vectors.
     * @return the merged vector.
     */
    public static Zl64Vector merge(Zl64Vector[] vectors) {
        MathPreconditions.checkPositive("vectors.length", vectors.length);
        Zl64 zl64 = vectors[0].getZl64();
        int length = Arrays.stream(vectors).mapToInt(Zl64Vector::getNum).sum();
        long[] mergeElements = new long[length];
        for (int i = 0, pos = 0; i < vectors.length; i++) {
            Preconditions.checkArgument(vectors[i].zl64.equals(zl64));
            MathPreconditions.checkPositive("vector.num", vectors[i].getNum());
            System.arraycopy(vectors[i].elements, 0, mergeElements, pos, vectors[i].elements.length);
            pos += vectors[i].elements.length;
        }
        return Zl64Vector.create(zl64, mergeElements);
    }

    /**
     * Creates a vector.
     *
     * @param zl64     Zl64 instance.
     * @param elements elements.
     * @return a vector.
     */
    public static Zl64Vector create(Zl64 zl64, long[] elements) {
        MathPreconditions.checkPositive("num", elements.length);
        Zl64Vector vector = new Zl64Vector(zl64);
        vector.elements = Arrays.stream(elements)
            .peek(element -> Preconditions.checkArgument(zl64.validateElement(element)))
            .toArray();
        return vector;
    }

    /**
     * Creates a lazy vector.
     *
     * @param zl64     Zl64 instance.
     * @param elements elements.
     * @return a lazy vector.
     */
    private static Zl64Vector createLazy(Zl64 zl64, long[] elements) {
        MathPreconditions.checkPositive("num", elements.length);
        Zl64Vector vector = new Zl64Vector(zl64);
        // do not verify lazy state
        vector.elements = elements;
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
        MathPreconditions.checkPositive("num", num);
        Zl64Vector vector = new Zl64Vector(zl64);
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
        MathPreconditions.checkPositive("num", num);
        Zl64Vector vector = new Zl64Vector(zl64);
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
        MathPreconditions.checkPositive("num", num);
        Zl64Vector vector = new Zl64Vector(zl64);
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
        MathPreconditions.checkPositive("num", num);
        Zl64Vector vector = new Zl64Vector(zl64);
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

    /**
     * Zl instance
     */
    private final Zl64 zl64;
    /**
     * elements
     */
    private long[] elements;

    private Zl64Vector(Zl64 zl64) {
        this.zl64 = zl64;
    }

    @Override
    public Zl64Vector copy() {
        Zl64Vector copy = new Zl64Vector(zl64);
        copy.elements = LongUtils.clone(elements);
        return copy;
    }

    @Override
    public int getNum() {
        return elements.length;
    }

    @Override
    public Zl64Vector split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        long[] splitElements = new long[splitNum];
        long[] remainElements = new long[num - splitNum];
        System.arraycopy(elements, num - splitNum, splitElements, 0, splitNum);
        System.arraycopy(elements, 0, remainElements, 0, num - splitNum);
        elements = remainElements;
        return Zl64Vector.create(zl64, splitElements);
    }

    /**
     * splits the vector.
     *
     * @param nums nums for each of the split vector.
     * @return the split vectors.
     */
    public Zl64Vector[] split(int[] nums) {
        int num = this.getNum();
        MathPreconditions.checkEqual("sum(nums)", "mergeVector.getNum()", Arrays.stream(nums).sum(), num);
        long[][] spRes = new long[nums.length][];
        for (int i = 0, startPos = 0; i < nums.length; i++) {
            spRes[i] = Arrays.copyOfRange(this.elements, startPos, startPos + nums[i]);
            startPos += nums[i];
        }
        this.elements = new long[0];
        return Arrays.stream(spRes).map(x -> Zl64Vector.create(this.getZl64(), x)).toArray(Zl64Vector[]::new);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // reduce if the reduced rows is less than rows.
            long[] remainElements = new long[reduceNum];
            System.arraycopy(elements, num - reduceNum, remainElements, 0, reduceNum);
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
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zl64.add(this.elements[index], that.elements[index]))
            .toArray();
        return Zl64Vector.create(zl64, results);
    }

    @Override
    public void addi(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zl64.add(this.elements[index], that.elements[index])
        );
    }

    @Override
    public Zl64Vector neg() {
        long[] results = Arrays.stream(elements).map(zl64::neg).toArray();
        return Zl64Vector.create(zl64, results);
    }

    @Override
    public void negi() {
        IntStream.range(0, elements.length).forEach(index -> elements[index] = zl64.neg(elements[index]));
    }

    @Override
    public Zl64Vector sub(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zl64.sub(this.elements[index], that.elements[index]))
            .toArray();
        return Zl64Vector.create(zl64, results);
    }

    @Override
    public void subi(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zl64.sub(this.elements[index], that.elements[index])
        );
    }

    @Override
    public Zl64Vector mul(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zl64.mul(this.elements[index], that.elements[index]))
            .toArray();
        return Zl64Vector.create(zl64, results);
    }

    @Override
    public void muli(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zl64.mul(this.elements[index], that.elements[index])
        );
    }

    /**
     * Module elements.
     */
    public void module() {
        IntStream.range(0, elements.length).forEach(index -> this.elements[index] = zl64.module(this.elements[index]));
    }

    /**
     * Lazy addition.
     *
     * @param that that vector.
     * @return result.
     */
    public Zl64Vector lazyAdd(Zl64Vector that) {
        checkInputs(that);
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zl64.lazyAdd(this.elements[index], that.elements[index]))
            .toArray();
        return createLazy(zl64, results);
    }

    /**
     * Lazy in-place addition.
     *
     * @param that that vector.
     */
    public void lazyAddi(Zl64Vector that) {
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zl64.lazyAdd(this.elements[index], that.elements[index])
        );
    }

    /**
     * Lazy negation.
     *
     * @return result.
     */
    public Zl64Vector lazyNeg() {
        long[] results = Arrays.stream(elements).map(zl64::lazyNeg).toArray();
        return Zl64Vector.createLazy(zl64, results);
    }

    /**
     * Lazy in-place negation.
     */
    public void lazyNegi() {
        IntStream.range(0, elements.length).forEach(index -> elements[index] = zl64.lazyNeg(elements[index]));
    }

    /**
     * Lazy subtraction.
     *
     * @param that that vector.
     * @return result.
     */
    public Zl64Vector lazySub(Zl64Vector that) {
        checkInputs(that);
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zl64.lazySub(this.elements[index], that.elements[index]))
            .toArray();
        return Zl64Vector.createLazy(zl64, results);
    }

    /**
     * Lazy in-place subtraction.
     *
     * @param that that vector.
     */
    public void lazySubi(Zl64Vector that) {
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zl64.lazySub(this.elements[index], that.elements[index])
        );
    }

    /**
     * Lazy multiplication.
     *
     * @param that that vector.
     * @return result.
     */
    public Zl64Vector lazyMul(Zl64Vector that) {
        checkInputs(that);
        long[] results = IntStream.range(0, elements.length)
            .mapToLong(index -> zl64.lazyMul(this.elements[index], that.elements[index]))
            .toArray();
        return Zl64Vector.createLazy(zl64, results);
    }

    /**
     * Lazy in-place multiplication.
     *
     * @param that that vector.
     */
    public void lazyMuli(Zl64Vector that) {
        checkInputs(that);
        IntStream.range(0, elements.length).forEach(index ->
            this.elements[index] = zl64.lazyMul(this.elements[index], that.elements[index])
        );
    }

    /**
     * Inner production.
     *
     * @param other the other vector.
     * @return inner-product result.
     */
    public long innerProduct(RingVector other) {
        Zl64Vector that = (Zl64Vector) other;
        checkInputs(that);
        long result = IntStream.range(0, elements.length)
            // we can do lazy multiplication and addition
            .mapToLong(i -> zl64.lazyMul(this.elements[i], that.elements[i]))
            .reduce(zl64::add)
            .orElseThrow(Error::new);
        return zl64.module(result);
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
     * Sets element.
     *
     * @param index   index.
     * @param element element.
     */
    public void setElement(int index, long element) {
        assert zl64.validateElement(element);
        elements[index] = element;
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
        if (obj instanceof Zl64Vector that) {
            return new EqualsBuilder()
                .append(this.zl64, that.zl64)
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
        return this.getClass().getSimpleName() + " (l = " + zl64.getL() + "): " + Arrays.toString(stringData);
    }
}
