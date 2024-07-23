package edu.alibaba.mpc4j.common.circuit.zl64;

import edu.alibaba.mpc4j.common.structure.vector.Vector;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

import java.security.SecureRandom;

/**
 * plain Zl64 vector.
 *
 * @author Weiran Liu
 * @date 2024/6/20
 */
public class PlainZl64Vector implements MpcZl64Vector {
    /**
     * Create a plain vector with the assigned value.
     *
     * @param zl64   Zl64 instance.
     * @param values the assigned values.
     * @return a plain vector.
     */
    public static PlainZl64Vector create(Zl64 zl64, long[] values) {
        PlainZl64Vector plainZl64Vector = new PlainZl64Vector();
        plainZl64Vector.zl64Vector = Zl64Vector.create(zl64, values);
        return plainZl64Vector;
    }

    /**
     * Creates a plain vector with the assigned vector.
     *
     * @param zl64Vector the assigned vector.
     * @return a plain vector.
     */
    public static PlainZl64Vector create(Zl64Vector zl64Vector) {
        PlainZl64Vector plainZl64Vector = new PlainZl64Vector();
        plainZl64Vector.zl64Vector = zl64Vector;
        return plainZl64Vector;
    }

    /**
     * Create a random plain vector.
     *
     * @param zl64         Zl64 instance.
     * @param num          num.
     * @param secureRandom random states.
     * @return a plain vector.
     */
    public static PlainZl64Vector createRandom(Zl64 zl64, int num, SecureRandom secureRandom) {
        PlainZl64Vector plainZl64Vector = new PlainZl64Vector();
        plainZl64Vector.zl64Vector = Zl64Vector.createRandom(zl64, num, secureRandom);
        return plainZl64Vector;
    }

    /**
     * Create a plain all-one vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  num.
     * @return a plain vector.
     */
    public static PlainZl64Vector createOnes(Zl64 zl64, int num) {
        PlainZl64Vector plainZl64Vector = new PlainZl64Vector();
        plainZl64Vector.zl64Vector = Zl64Vector.createOnes(zl64, num);
        return plainZl64Vector;
    }

    /**
     * Create a plain all-zero vector.
     *
     * @param zl64 Zl64 instance.
     * @param num  num.
     * @return a plain vector.
     */
    public static PlainZl64Vector createZeros(Zl64 zl64, int num) {
        PlainZl64Vector plainZl64Vector = new PlainZl64Vector();
        plainZl64Vector.zl64Vector = Zl64Vector.createZeros(zl64, num);
        return plainZl64Vector;
    }

    /**
     * Create an empty plain vector.
     *
     * @param zl64 Zl64 instance.
     * @return a plain vector.
     */
    public static PlainZl64Vector createEmpty(Zl64 zl64) {
        PlainZl64Vector plainZl64Vector = new PlainZl64Vector();
        plainZl64Vector.zl64Vector = Zl64Vector.createEmpty(zl64);
        return plainZl64Vector;
    }

    /**
     * Zl64 vector
     */
    private Zl64Vector zl64Vector;

    /**
     * private constructor.
     */
    private PlainZl64Vector() {
        // empty
    }

    @Override
    public Zl64Vector getZl64Vector() {
        return zl64Vector;
    }

    @Override
    public boolean isPlain() {
        return true;
    }

    @Override
    public PlainZl64Vector copy() {
        PlainZl64Vector clone = new PlainZl64Vector();
        clone.zl64Vector = zl64Vector.copy();

        return clone;
    }

    @Override
    public int getNum() {
        return zl64Vector.getNum();
    }

    @Override
    public PlainZl64Vector split(int splitNum) {
        Zl64Vector splitVector = zl64Vector.split(splitNum);
        return PlainZl64Vector.create(splitVector);
    }

    @Override
    public void reduce(int reduceNum) {
        zl64Vector.reduce(reduceNum);
    }

    @Override
    public void merge(Vector other) {
        PlainZl64Vector that = (PlainZl64Vector) other;
        zl64Vector.merge(that.getZl64Vector());
    }
}
