package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
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
public class SquareZlVector implements MpcZlVector {
    /**
     * the vector
     */
    private ZlVector zlVector;
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
        shareVector.zlVector = ZlVector.create(zl, elements);
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
        shareVector.zlVector = vector;
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
        shareVector.zlVector = ZlVector.createRandom(zl, num, secureRandom);
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
        shareVector.zlVector = ZlVector.createOnes(zl, num);
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
        shareVector.zlVector = ZlVector.createZeros(zl, num);
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
    public static SquareZlVector createEmpty(Zl zl, boolean plain) {
        SquareZlVector shareVector = new SquareZlVector();
        shareVector.zlVector = ZlVector.createEmpty(zl);
        shareVector.plain = plain;

        return shareVector;
    }

    private SquareZlVector() {
        // empty
    }

    @Override
    public SquareZlVector copy() {
        SquareZlVector clone = new SquareZlVector();
        clone.zlVector = zlVector.copy();
        clone.plain = plain;

        return clone;
    }

    @Override
    public int getNum() {
        return zlVector.getNum();
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public ZlVector getZlVector() {
        return zlVector;
    }

    @Override
    public SquareZlVector split(int splitNum) {
        ZlVector splitVector = zlVector.split(splitNum);
        return SquareZlVector.create(splitVector, plain);
    }

    @Override
    public void reduce(int splitNum) {
        zlVector.reduce(splitNum);
    }

    @Override
    public void merge(MpcVector other) {
        SquareZlVector that = (SquareZlVector) other;
        Preconditions.checkArgument(this.plain == that.plain, "plain state mismatch");
        zlVector.merge(that.getZlVector());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(zlVector)
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
                .append(this.zlVector, that.zlVector)
                .append(this.plain, that.plain)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", plain ? "plain" : "secret", zlVector.toString());
    }
}
