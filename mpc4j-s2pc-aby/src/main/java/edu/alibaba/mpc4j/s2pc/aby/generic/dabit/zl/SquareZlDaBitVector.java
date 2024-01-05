package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

/**
 * square Zl daBit vector.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public class SquareZlDaBitVector implements MergedPcgPartyOutput {
    /**
     * square Zl vector
     */
    private SquareZlVector squareZlVector;
    /**
     * square Z2 vector
     */
    private SquareZ2Vector squareZ2Vector;

    /**
     * Creates a daBit vector.
     *
     * @param squareZlVector square Zl vector.
     * @param squareZ2Vector square Z2 vector.
     * @return a daBit vector.
     */
    public static SquareZlDaBitVector create(SquareZlVector squareZlVector, SquareZ2Vector squareZ2Vector) {
        MathPreconditions.checkPositive("num", squareZlVector.getNum());
        MathPreconditions.checkEqual("Zl.length", "Z2.length", squareZlVector.getNum(), squareZ2Vector.bitNum());
        // Zl vector and Z2 vector must be secret
        Preconditions.checkArgument(!squareZlVector.isPlain());
        Preconditions.checkArgument(!squareZ2Vector.isPlain());
        SquareZlDaBitVector daBitVector = new SquareZlDaBitVector();
        daBitVector.squareZlVector = squareZlVector;
        daBitVector.squareZ2Vector = squareZ2Vector;

        return daBitVector;
    }

    /**
     * Creates an empty daBit vector.
     *
     * @param zl Zl instance.
     * @return a daBit vector.
     */
    public static SquareZlDaBitVector createEmpty(Zl zl) {
        SquareZlDaBitVector daBitVector = new SquareZlDaBitVector();
        daBitVector.squareZlVector = SquareZlVector.createEmpty(zl, false);
        daBitVector.squareZ2Vector = SquareZ2Vector.createEmpty(false);
        return daBitVector;
    }

    /**
     * private constructor.
     */
    private SquareZlDaBitVector() {
        // empty
    }

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    public Zl getZl() {
        return squareZlVector.getZl();
    }

    @Override
    public SquareZlDaBitVector split(int splitNum) {
        SquareZlDaBitVector splitDaBitVector = new SquareZlDaBitVector();
        splitDaBitVector.squareZlVector = squareZlVector.split(splitNum);
        splitDaBitVector.squareZ2Vector = squareZ2Vector.split(splitNum);

        return splitDaBitVector;
    }

    @Override
    public void reduce(int reduceNum) {
        squareZlVector.reduce(reduceNum);
        squareZ2Vector.reduce(reduceNum);
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        SquareZlDaBitVector that = (SquareZlDaBitVector) other;
        Preconditions.checkArgument(this.getZl().equals(that.getZl()));
        this.squareZlVector.merge(that.squareZlVector);
        this.squareZ2Vector.merge(that.squareZ2Vector);
    }

    /**
     * Gets square vector.
     *
     * @return square vector.
     */
    public SquareZlVector getSquareZlVector() {
        return squareZlVector;
    }

    /**
     * Gets square Z2 vector.
     *
     * @return square Z2 vector.
     */
    public SquareZ2Vector getSquareZ2Vector() {
        return squareZ2Vector;
    }

    /**
     * Reveals the plain daBit vector.
     *
     * @param that other daBit vector.
     * @return revealed daBit vector.
     */
    public PlainZlDaBitVector reveal(SquareZlDaBitVector that) {
        ZlVector zlVector = this.squareZlVector.getZlVector().add(that.squareZlVector.getZlVector());
        BitVector bitVector = this.squareZ2Vector.getBitVector().xor(that.squareZ2Vector.getBitVector());

        return PlainZlDaBitVector.create(zlVector, bitVector);
    }

    @Override
    public int getNum() {
        return squareZlVector.getNum();
    }
}
