package edu.alibaba.mpc4j.s2pc.aby.pcg.triple;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * Zl64 triple.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
public class Zl64Triple implements MergedPcgPartyOutput {
    /**
     * Zl64 instance
     */
    private final Zl64 zl64;
    /**
     * a
     */
    private Zl64Vector a;
    /**
     * b
     */
    private Zl64Vector b;
    /**
     * c
     */
    private Zl64Vector c;

    /**
     * Creates a triple where each element is represented by longs.
     *
     * @param zl64 Zl64 instance.
     * @param a    a.
     * @param b    b.
     * @param c    c.
     * @return a triple.
     */
    public static Zl64Triple create(Zl64 zl64, long[] a, long[] b, long[] c) {
        int num = a.length;
        MathPreconditions.checkEqual("num", "a.length", num, a.length);
        MathPreconditions.checkEqual("num", "b.length", num, b.length);
        MathPreconditions.checkEqual("num", "c.length", num, c.length);
        if (num == 0) {
            return createEmpty(zl64);
        } else {
            Zl64Triple triple = new Zl64Triple(zl64);
            triple.a = Zl64Vector.create(zl64, a);
            triple.b = Zl64Vector.create(zl64, b);
            triple.c = Zl64Vector.create(zl64, c);

            return triple;
        }
    }

    /**
     * Creates an empty triple.
     *
     * @param zl64 Zl64 instance.
     * @return an empty triple.
     */
    public static Zl64Triple createEmpty(Zl64 zl64) {
        Zl64Triple emptyTriple = new Zl64Triple(zl64);
        emptyTriple.a = Zl64Vector.createEmpty(zl64);
        emptyTriple.b = Zl64Vector.createEmpty(zl64);
        emptyTriple.c = Zl64Vector.createEmpty(zl64);

        return emptyTriple;
    }

    /**
     * create a random triple.
     *
     * @param zl64 Zl64 instance.
     * @param num  num.
     * @return a random triple.
     */
    public static Zl64Triple createRandom(Zl64 zl64, int num, SecureRandom secureRandom) {
        if (num == 0) {
            return createEmpty(zl64);
        } else {
            Zl64Triple triple = new Zl64Triple(zl64);
            triple.a = Zl64Vector.createRandom(zl64, num, secureRandom);
            triple.b = Zl64Vector.createRandom(zl64, num, secureRandom);
            triple.c = Zl64Vector.createRandom(zl64, num, secureRandom);

            return triple;
        }
    }

    /**
     * Creates a random triple.
     *
     * @param that         given triple.
     * @param secureRandom random state.
     * @return a random triple.
     */
    public static Zl64Triple createRandom(Zl64Triple that, SecureRandom secureRandom) {
        int num = that.getNum();
        if (num == 0) {
            return createEmpty(that.zl64);
        } else {
            Zl64Triple triple = new Zl64Triple(that.zl64);
            triple.a = Zl64Vector.createRandom(that.zl64, num, secureRandom);
            triple.b = Zl64Vector.createRandom(that.zl64, num, secureRandom);
            // compute c1 = (a0 + a1) * (b0 + b1) - c0
            Zl64Vector a = triple.a.add(that.a);
            Zl64Vector b = triple.b.add(that.b);
            triple.c = a.mul(b);
            triple.c.subi(that.c);
            return triple;
        }
    }

    /**
     * create a triple where each element is represented by Zl64Vector.
     *
     * @param zl64 Zl64 instance.
     * @param a    a represented by Zl64Vector.
     * @param b    b represented by Zl64Vector.
     * @param c    c represented by Zl64Vector.
     * @return a triple.
     */
    private static Zl64Triple create(Zl64 zl64, Zl64Vector a, Zl64Vector b, Zl64Vector c) {
        Zl64Triple triple = new Zl64Triple(zl64);
        triple.a = a;
        triple.b = b;
        triple.c = c;

        return triple;
    }

    /**
     * private constructor.
     */
    private Zl64Triple(Zl64 zl64) {
        this.zl64 = zl64;
    }

    @Override
    public int getNum() {
        return a.getNum();
    }

    @Override
    public Zl64Triple copy() {
        Zl64Triple copy = new Zl64Triple(zl64);
        copy.a = a.copy();
        copy.b = b.copy();
        copy.c = c.copy();
        return copy;
    }

    @Override
    public Zl64Triple split(int splitNum) {
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, getNum());
        Zl64Vector splitA = a.split(splitNum);
        Zl64Vector spiltB = b.split(splitNum);
        Zl64Vector splitC = c.split(splitNum);
        return Zl64Triple.create(zl64, splitA, spiltB, splitC);
    }

    @Override
    public void reduce(int reduceNum) {
        MathPreconditions.checkPositiveInRangeClosed("reduce_num", reduceNum, getNum());
        a.reduce(reduceNum);
        b.reduce(reduceNum);
        c.reduce(reduceNum);
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Zl64Triple that = (Zl64Triple) other;
        Preconditions.checkArgument(this.zl64.equals(that.zl64));
        a.merge(that.a);
        b.merge(that.b);
        c.merge(that.c);
    }

    /**
     * Gets the Zl64 instance.
     *
     * @return the Zl64 instance.
     */
    public Zl64 getZl64() {
        return zl64;
    }

    /**
     * Gets a.
     *
     * @return a.
     */
    public long[] getA() {
        return a.getElements();
    }

    /**
     * Gets a.
     *
     * @return a.
     */
    public Zl64Vector getVectorA() {
        return a;
    }

    /**
     * Gets b.
     *
     * @return b.
     */
    public long[] getB() {
        return b.getElements();
    }

    /**
     * Gets b.
     *
     * @return b.
     */
    public Zl64Vector getVectorB() {
        return b;
    }

    /**
     * Gets c.
     *
     * @return c.
     */
    public long[] getC() {
        return c.getElements();
    }

    /**
     * Gets c.
     *
     * @return c.
     */
    public Zl64Vector getVectorC() {
        return c;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(a)
            .append(b)
            .append(c)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Zl64Triple that) {
            return new EqualsBuilder()
                .append(this.a, that.a)
                .append(this.b, that.b)
                .append(this.c, that.c)
                .isEquals();
        }
        return false;
    }
}
