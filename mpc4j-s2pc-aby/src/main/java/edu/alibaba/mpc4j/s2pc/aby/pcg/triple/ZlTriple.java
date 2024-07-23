package edu.alibaba.mpc4j.s2pc.aby.pcg.triple;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Zl triple.
 *
 * @author Weiran Liu
 * @date 2022/4/11
 */
public class ZlTriple implements MergedPcgPartyOutput {
    /**
     * the Zl instance
     */
    private final Zl zl;
    /**
     * a
     */
    private ZlVector a;
    /**
     * b
     */
    private ZlVector b;
    /**
     * c
     */
    private ZlVector c;

    /**
     * Creates a triple where each element is represented by BigIntegers.
     *
     * @param zl  Zl instance.
     * @param a   a.
     * @param b   b.
     * @param c   c.
     * @return a triple.
     */
    public static ZlTriple create(Zl zl, BigInteger[] a, BigInteger[] b, BigInteger[] c) {
        int num = a.length;
        MathPreconditions.checkEqual("num", "a.length", num, a.length);
        MathPreconditions.checkEqual("num", "b.length", num, b.length);
        MathPreconditions.checkEqual("num", "c.length", num, c.length);
        if (num == 0) {
            return createEmpty(zl);
        } else {
            ZlTriple triple = new ZlTriple(zl);
            triple.a = ZlVector.create(zl, a);
            triple.b = ZlVector.create(zl, b);
            triple.c = ZlVector.create(zl, c);

            return triple;
        }
    }

    /**
     * Creates an empty triple.
     *
     * @param zl Zl instance.
     * @return an empty triple.
     */
    public static ZlTriple createEmpty(Zl zl) {
        ZlTriple triple = new ZlTriple(zl);
        triple.a = ZlVector.createEmpty(zl);
        triple.b = ZlVector.createEmpty(zl);
        triple.c = ZlVector.createEmpty(zl);

        return triple;
    }

    /**
     * create a random triple.
     *
     * @param zl           Zl instance.
     * @param num          num.
     * @param secureRandom random state.
     * @return a random triple.
     */
    public static ZlTriple createRandom(Zl zl, int num, SecureRandom secureRandom) {
        if (num == 0) {
            return createEmpty(zl);
        } else {
            ZlTriple triple = new ZlTriple(zl);
            triple.a = ZlVector.createRandom(zl, num, secureRandom);
            triple.b = ZlVector.createRandom(zl, num, secureRandom);
            triple.c = ZlVector.createRandom(zl, num, secureRandom);

            return triple;
        }
    }

    /**
     * Creates a random triple.
     *
     * @param that         that triple.
     * @param secureRandom random state.
     * @return a random triple.
     */
    public static ZlTriple createRandom(ZlTriple that, SecureRandom secureRandom) {
        int num = that.getNum();
        if (num == 0) {
            return createEmpty(that.zl);
        } else {
            ZlTriple triple = new ZlTriple(that.zl);
            triple.a = ZlVector.createRandom(that.zl, num, secureRandom);
            triple.b = ZlVector.createRandom(that.zl, num, secureRandom);
            // compute c1 = (a0 + a1) * (b0 + b1) - c0
            ZlVector a = triple.a.add(that.a);
            ZlVector b = triple.b.add(that.b);
            triple.c = a.mul(b);
            triple.c.subi(that.c);

            return triple;
        }
    }

    /**
     * create a triple where each element is represented by ZlVector.
     *
     * @param zl  Zl instance.
     * @param a   a represented by ZlVector.
     * @param b   b represented by ZlVector.
     * @param c   c represented by ZlVector.
     * @return a triple.
     */
    private static ZlTriple create(Zl zl, ZlVector a, ZlVector b, ZlVector c) {
        assert a.getNum() == b.getNum() && a.getNum() == c.getNum();
        ZlTriple triple = new ZlTriple(zl);
        triple.a = a;
        triple.b = b;
        triple.c = c;

        return triple;
    }

    /**
     * private constructor.
     *
     * @param zl Zl.
     */
    private ZlTriple(Zl zl) {
        this.zl = zl;
    }

    @Override
    public int getNum() {
        return a.getNum();
    }

    @Override
    public ZlTriple copy() {
        ZlTriple copy = new ZlTriple(zl);
        copy.a = a.copy();
        copy.b = b.copy();
        copy.c = c.copy();
        return copy;
    }

    @Override
    public ZlTriple split(int splitNum) {
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, getNum());
        ZlVector splitA = a.split(splitNum);
        ZlVector spiltB = b.split(splitNum);
        ZlVector splitC = c.split(splitNum);

        return create(zl, splitA, spiltB, splitC);
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
        ZlTriple that = (ZlTriple) other;
        Preconditions.checkArgument(this.zl.equals(that.zl));
        a.merge(that.a);
        b.merge(that.b);
        c.merge(that.c);
    }

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    public Zl getZl() {
        return zl;
    }

    /**
     * Gets a.
     *
     * @return a.
     */
    public BigInteger[] getA() {
        return a.getElements();
    }

    /**
     * Gets a.
     *
     * @return a.
     */
    public ZlVector getVectorA() {
        return a;
    }

    /**
     * Gets b.
     *
     * @return b.
     */
    public BigInteger[] getB() {
        return b.getElements();
    }

    /**
     * Gets b.
     *
     * @return b.
     */
    public ZlVector getVectorB() {
        return b;
    }

    /**
     * Gets c.
     *
     * @return c.
     */
    public BigInteger[] getC() {
        return c.getElements();
    }

    /**
     * Gets c.
     *
     * @return c.
     */
    public ZlVector getVectorC() {
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
        if (obj instanceof ZlTriple that) {
            return new EqualsBuilder()
                .append(this.a, that.a)
                .append(this.b, that.b)
                .append(this.c, that.c)
                .isEquals();
        }
        return false;
    }
}
