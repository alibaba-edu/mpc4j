package edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.SquareRingVector;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Square share Zl vector ([x]). The share is of the form: x = x_0 + x_1.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class SquareZlVector implements SquareRingVector {
    /**
     * the vector
     */
    private ZlVector vector;
    /**
     * the plain state.
     */
    private boolean plain;

    /**
     * Create a share vector.
     *
     * @param zl       Zl instance.
     * @param elements the elements.
     * @param plain    the plain state.
     * @return a share vector.
     */
    public static SquareZlVector create(Zl zl, BigInteger[] elements, boolean plain) {
        SquareZlVector shareVector = new SquareZlVector();
        shareVector.vector = ZlVector.create(zl, elements);
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
    public static SquareZlVector create(ZlVector vector, boolean plain) {
        SquareZlVector shareVector = new SquareZlVector();
        shareVector.vector = vector;
        shareVector.plain = plain;

        return shareVector;
    }

    /**
     * Create a (plain) random share vector.
     *
     * @param zl           Zl instance.
     * @param num          the num.
     * @param secureRandom the random states.
     * @return a share vector.
     */
    public static SquareZlVector createRandom(Zl zl, int num, SecureRandom secureRandom) {
        SquareZlVector shareVector = new SquareZlVector();
        shareVector.vector = ZlVector.createRandom(zl, num, secureRandom);
        shareVector.plain = true;

        return shareVector;
    }

    /**
     * Create a (plain) all-one share vector.
     *
     * @param zl  Zl instance.
     * @param num the num.
     * @return a share vector.
     */
    public static SquareZlVector createOnes(Zl zl, int num) {
        SquareZlVector shareVector = new SquareZlVector();
        shareVector.vector = ZlVector.createOnes(zl, num);
        shareVector.plain = true;

        return shareVector;
    }

    /**
     * Create a (plain) all-zero share vector.
     *
     * @param zl  Zl instance.
     * @param num the num.
     * @return a share vector.
     */
    public static SquareZlVector createZeros(Zl zl, int num) {
        SquareZlVector shareVector = new SquareZlVector();
        shareVector.vector = ZlVector.createZeros(zl, num);
        shareVector.plain = true;

        return shareVector;
    }

    /**
     * Create an empty share vector.
     *
     * @param zl  Zl instance.
     * @param plain the plain state.
     * @return a share vector.
     */
    public static SquareZlVector createEmpty(Zl zl, boolean plain) {
        SquareZlVector shareVector = new SquareZlVector();
        shareVector.vector = ZlVector.createEmpty(zl);
        shareVector.plain = plain;

        return shareVector;
    }

    private SquareZlVector() {
        // empty
    }

    @Override
    public SquareZlVector copy() {
        SquareZlVector clone = new SquareZlVector();
        clone.vector = vector.copy();
        clone.plain = plain;

        return clone;
    }

    @Override
    public int getNum() {
        return vector.getNum();
    }

    @Override
    public SquareZlVector add(SquareRingVector other, boolean plain) {
        SquareZlVector that = (SquareZlVector) other;
        ZlVector resultVector = vector.add(that.vector);
        return SquareZlVector.create(resultVector, plain);
    }

    @Override
    public void addi(SquareRingVector other, boolean plain) {
        SquareZlVector that = (SquareZlVector) other;
        vector.addi(that.vector);
        this.plain = plain;
    }

    @Override
    public SquareZlVector neg(boolean plain) {
        ZlVector resultVector = vector.neg();
        return SquareZlVector.create(resultVector, plain);
    }

    @Override
    public void negi(boolean plain) {
        vector.negi();
        this.plain = plain;
    }

    @Override
    public SquareZlVector sub(SquareRingVector other, boolean plain) {
        SquareZlVector that = (SquareZlVector) other;
        ZlVector resultVector = vector.sub(that.vector);
        return SquareZlVector.create(resultVector, plain);
    }

    @Override
    public void subi(SquareRingVector other, boolean plain) {
        SquareZlVector that = (SquareZlVector) other;
        vector.subi(that.vector);
        this.plain = plain;
    }

    @Override
    public SquareZlVector mul(SquareRingVector other) {
        SquareZlVector that = (SquareZlVector) other;
        ZlVector resultVector = vector.mul(that.vector);
        return SquareZlVector.create(resultVector, plain && that.plain);
    }

    @Override
    public void muli(SquareRingVector other) {
        SquareZlVector that = (SquareZlVector) other;
        vector.muli(that.vector);
        plain = plain && that.plain;
    }

    @Override
    public ZlVector getVector() {
        return vector;
    }

    public BigInteger getElement(int index) {
        return vector.getElement(index);
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public SquareZlVector split(int splitNum) {
        ZlVector splitVector = vector.split(splitNum);
        return SquareZlVector.create(splitVector, plain);
    }

    @Override
    public void reduce(int splitNum) {
        vector.reduce(splitNum);
    }

    @Override
    public void merge(MpcVector other) {
        SquareZlVector that = (SquareZlVector) other;
        Preconditions.checkArgument(this.plain == that.plain, "plain state mismatch");
        vector.merge(that.getVector());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(vector)
            .append(plain)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SquareZlVector) {
            SquareZlVector that = (SquareZlVector) obj;
            return new EqualsBuilder()
                .append(this.vector, that.vector)
                .append(this.plain, that.plain)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", plain ? "plain" : "secret", vector.toString());
    }
}
