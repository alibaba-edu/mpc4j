package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.structure.vector.Vector;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * Square share Zl vector ([x]). The share is of the form: x = x_0 + x_1.
 *
 * @author Li Peng
 * @date 2024/7/23
 */
public class SquareZl64Vector implements MpcZl64Vector {
    /**
     * the vector
     */
    private Zl64Vector zl64Vector;
    /**
     * the plain state.
     */
    private boolean plain;

    /**
     * Create a share vector.
     *
     * @param zl64     Zl64 instance.
     * @param elements the elements.
     * @param plain    the plain state.
     * @return a share vector.
     */
    public static SquareZl64Vector create(Zl64 zl64, long[] elements, boolean plain) {
        SquareZl64Vector shareVector = new SquareZl64Vector();
        shareVector.zl64Vector = Zl64Vector.create(zl64, elements);
        shareVector.plain = plain;

        return shareVector;
    }

    /**
     * Create a share vector.
     *
     * @param vector the vector.
     * @param plain  the plain state.
     * @return a share vector.
     */
    public static SquareZl64Vector create(Zl64Vector vector, boolean plain) {
        SquareZl64Vector shareVector = new SquareZl64Vector();
        shareVector.zl64Vector = vector;
        shareVector.plain = plain;

        return shareVector;
    }

    /**
     * Create a (plain) random share vector.
     *
     * @param zl64         Zl64 instance.
     * @param num          the num.
     * @param secureRandom the random states.
     * @return a share vector.
     */
    public static SquareZl64Vector createRandom(Zl64 zl64, int num, SecureRandom secureRandom) {
        SquareZl64Vector shareVector = new SquareZl64Vector();
        shareVector.zl64Vector = Zl64Vector.createRandom(zl64, num, secureRandom);
        shareVector.plain = true;

        return shareVector;
    }

    /**
     * Create a (plain) all-one share vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  the num.
     * @return a share vector.
     */
    public static SquareZl64Vector createOnes(Zl64 zl64, int num) {
        SquareZl64Vector shareVector = new SquareZl64Vector();
        shareVector.zl64Vector = Zl64Vector.createOnes(zl64, num);
        shareVector.plain = true;

        return shareVector;
    }

    /**
     * Create a (plain) all-zero share vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  the num.
     * @return a share vector.
     */
    public static SquareZl64Vector createZeros(Zl64 zl64, int num) {
        SquareZl64Vector shareVector = new SquareZl64Vector();
        shareVector.zl64Vector = Zl64Vector.createZeros(zl64, num);
        shareVector.plain = true;

        return shareVector;
    }

    /**
     * Create an empty share vector.
     *
     * @param zl    Zl instance.
     * @param plain the plain state.
     * @return a share vector.
     */
    public static SquareZl64Vector createEmpty(Zl64 zl64, boolean plain) {
        SquareZl64Vector shareVector = new SquareZl64Vector();
        shareVector.zl64Vector = Zl64Vector.createEmpty(zl64);
        shareVector.plain = plain;

        return shareVector;
    }

    private SquareZl64Vector() {
        // empty
    }

    @Override
    public SquareZl64Vector copy() {
        SquareZl64Vector clone = new SquareZl64Vector();
        clone.zl64Vector = zl64Vector.copy();
        clone.plain = plain;

        return clone;
    }

    @Override
    public int getNum() {
        return zl64Vector.getNum();
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public Zl64Vector getZl64Vector() {
        return zl64Vector;
    }

    @Override
    public SquareZl64Vector split(int splitNum) {
        Zl64Vector splitVector = zl64Vector.split(splitNum);
        return SquareZl64Vector.create(splitVector, plain);
    }

    @Override
    public void reduce(int splitNum) {
        zl64Vector.reduce(splitNum);
    }

    @Override
    public void merge(Vector other) {
        SquareZl64Vector that = (SquareZl64Vector) other;
        Preconditions.checkArgument(this.plain == that.plain, "plain state mismatch");
        zl64Vector.merge(that.getZl64Vector());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(zl64Vector)
            .append(plain)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SquareZl64Vector that) {
            return new EqualsBuilder()
                .append(this.zl64Vector, that.zl64Vector)
                .append(this.plain, that.plain)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", plain ? "plain" : "secret", zl64Vector.toString());
    }
}
