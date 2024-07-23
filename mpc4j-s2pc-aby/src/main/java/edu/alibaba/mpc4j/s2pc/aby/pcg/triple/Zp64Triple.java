package edu.alibaba.mpc4j.s2pc.aby.pcg.triple;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.vector.Zp64Vector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * Zp64 triple.
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Zp64Triple implements MergedPcgPartyOutput {
    /**
     * the Zp64 instance
     */
    private final Zp64 zp64;
    /**
     * a
     */
    private Zp64Vector a;
    /**
     * b
     */
    private Zp64Vector b;
    /**
     * c
     */
    private Zp64Vector c;

    /**
     * Creates a triple.
     *
     * @param zp64 Zp64 instance.
     * @param a    a.
     * @param b    b.
     * @param c    c.
     * @return a triple.
     */
    public static Zp64Triple create(Zp64 zp64, long[] a, long[] b, long[] c) {
        int num = a.length;
        MathPreconditions.checkEqual("num", "a.length", num, a.length);
        MathPreconditions.checkEqual("num", "b.length", num, b.length);
        MathPreconditions.checkEqual("num", "c.length", num, c.length);
        if (num == 0) {
            return createEmpty(zp64);
        } else {
            Zp64Triple triple = new Zp64Triple(zp64);
            triple.a = Zp64Vector.create(zp64, a);
            triple.b = Zp64Vector.create(zp64, b);
            triple.c = Zp64Vector.create(zp64, c);

            return triple;
        }
    }

    /**
     * Creates an empty triple.
     *
     * @param zp64 the Zp64 instance.
     * @return an empty triple.
     */
    public static Zp64Triple createEmpty(Zp64 zp64) {
        Zp64Triple triple = new Zp64Triple(zp64);
        triple.a = Zp64Vector.createEmpty(zp64);
        triple.b = Zp64Vector.createEmpty(zp64);
        triple.c = Zp64Vector.createEmpty(zp64);

        return triple;
    }

    /**
     * create a random triple.
     *
     * @param zp64         Zp64 instance.
     * @param num          num.
     * @param secureRandom random state.
     * @return a random triple.
     */
    public static Zp64Triple createRandom(Zp64 zp64, int num, SecureRandom secureRandom) {
        if (num == 0) {
            return createEmpty(zp64);
        } else {
            Zp64Triple triple = new Zp64Triple(zp64);
            triple.a = Zp64Vector.createRandom(zp64, num, secureRandom);
            triple.b = Zp64Vector.createRandom(zp64, num, secureRandom);
            triple.c = Zp64Vector.createRandom(zp64, num, secureRandom);

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
    public static Zp64Triple createRandom(Zp64Triple that, SecureRandom secureRandom) {
        int num = that.getNum();
        if (num == 0) {
            return createEmpty(that.zp64);
        } else {
            Zp64Triple triple = new Zp64Triple(that.zp64);
            triple.a = Zp64Vector.createRandom(that.zp64, num, secureRandom);
            triple.b = Zp64Vector.createRandom(that.zp64, num, secureRandom);
            // compute c1 = (a0 + a1) * (b0 + b1) - c0
            Zp64Vector a = triple.a.add(that.a);
            Zp64Vector b = triple.b.add(that.b);
            triple.c = a.mul(b);
            triple.c.subi(that.c);

            return triple;
        }
    }

    /**
     * create a triple where each element is represented by Zp64Vector.
     *
     * @param zp64 Zp64 instance.
     * @param a    a represented by Zp64Vector.
     * @param b    b represented by Zp64Vector.
     * @param c    c represented by Zp64Vector.
     * @return a triple.
     */
    private static Zp64Triple create(Zp64 zp64, Zp64Vector a, Zp64Vector b, Zp64Vector c) {
        assert a.getNum() == b.getNum() && a.getNum() == c.getNum();
        Zp64Triple triple = new Zp64Triple(zp64);
        triple.a = a;
        triple.b = b;
        triple.c = c;

        return triple;
    }

    /**
     * private constructor.
     *
     * @param zp64 Zp64.
     */
    private Zp64Triple(Zp64 zp64) {
        this.zp64 = zp64;
    }

    @Override
    public int getNum() {
        return a.getNum();
    }

    @Override
    public Zp64Triple copy() {
        Zp64Triple copy = new Zp64Triple(zp64);
        copy.a = a.copy();
        copy.b = b.copy();
        copy.c = c.copy();
        return copy;
    }

    @Override
    public Zp64Triple split(int splitNum) {
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, getNum());
        Zp64Vector splitA = a.split(splitNum);
        Zp64Vector spiltB = b.split(splitNum);
        Zp64Vector splitC = c.split(splitNum);

        return create(zp64, splitA, spiltB, splitC);
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
        Zp64Triple that = (Zp64Triple) other;
        Preconditions.checkArgument(this.zp64.equals(that.zp64));
        a.merge(that.a);
        b.merge(that.b);
        c.merge(that.c);
    }

    /**
     * Gets the Zp64 instance.
     *
     * @return the Zp64 instance.
     */
    public Zp64 getZp64() {
        return zp64;
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
    public Zp64Vector getVectorA() {
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
    public Zp64Vector getVectorB() {
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
    public Zp64Vector getVectorC() {
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
        if (obj instanceof Zp64Triple that) {
            return new EqualsBuilder()
                .append(this.a, that.a)
                .append(this.b, that.b)
                .append(this.c, that.c)
                .isEquals();
        }
        return false;
    }
}
