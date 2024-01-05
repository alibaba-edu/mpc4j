package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.stream.IntStream;

/**
 * global square Zl EdaBit vector.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
public class SquareZlEdaBitVector implements MergedPcgPartyOutput {
    /**
     * Zl instance
     */
    private final Zl zl;
    /**
     * l
     */
    private final int l;
    /**
     * square Zl vector
     */
    private SquareZlVector squareZlVector;
    /**
     * square Z2 vectors
     */
    private SquareZ2Vector[] squareZ2Vectors;

    /**
     * Creates a square edaBit vector.
     *
     * @param squareZlVector  square vector.
     * @param squareZ2Vectors square vector.
     * @return a square edaBit vector.
     */
    public static SquareZlEdaBitVector create(SquareZlVector squareZlVector, SquareZ2Vector[] squareZ2Vectors) {
        int num = squareZlVector.getNum();
        MathPreconditions.checkPositive("num", num);
        Zl zl = squareZlVector.getZl();
        int l = zl.getL();
        MathPreconditions.checkEqual("l", "Z2Vector.length", l, squareZ2Vectors.length);
        // Zl vector must be secret
        Preconditions.checkArgument(!squareZlVector.isPlain());
        IntStream.range(0, l).forEach(i -> {
            // verify num
            MathPreconditions.checkEqual("num", i + "-th Z2vector.bitNum", num, squareZ2Vectors[i].bitNum());
            // Z2 vector must be secret
            Preconditions.checkArgument(!squareZ2Vectors[i].isPlain());
        });
        SquareZlEdaBitVector globalEdaBitVector = new SquareZlEdaBitVector(zl);
        globalEdaBitVector.squareZlVector = squareZlVector;
        globalEdaBitVector.squareZ2Vectors = squareZ2Vectors;

        return globalEdaBitVector;
    }

    /**
     * private constructor.
     */
    private SquareZlEdaBitVector(Zl zl) {
        this.zl = zl;
        l = zl.getL();
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
    public SquareZlEdaBitVector split(int splitNum) {
        SquareZlEdaBitVector splitGlobalEdaBitVector = new SquareZlEdaBitVector(zl);
        splitGlobalEdaBitVector.squareZlVector = squareZlVector.split(splitNum);
        splitGlobalEdaBitVector.squareZ2Vectors = IntStream.range(0, l)
            .mapToObj(i -> squareZ2Vectors[i].split(splitNum))
            .toArray(SquareZ2Vector[]::new);

        return splitGlobalEdaBitVector;
    }

    @Override
    public void reduce(int reduceNum) {
        squareZlVector.reduce(reduceNum);
        IntStream.range(0, l).forEach(i -> squareZ2Vectors[i].reduce(reduceNum));
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        SquareZlEdaBitVector that = (SquareZlEdaBitVector) other;
        Preconditions.checkArgument(this.zl.equals(that.zl));
        this.squareZlVector.merge(that.squareZlVector);
        IntStream.range(0, l).forEach(i -> this.squareZ2Vectors[i].merge(that.squareZ2Vectors[i]));
    }

    /**
     * Gets square vector.
     *
     * @return square Zl vector.
     */
    public SquareZlVector getSquareZlVector() {
        return squareZlVector;
    }

    /**
     * Gets square bit vectors.
     *
     * @return square bit vectors.
     */
    public SquareZ2Vector[] getSquareZ2Vectors() {
        return squareZ2Vectors;
    }

    /**
     * Reveals the plain edaBit vector.
     *
     * @param that other edaBit vector.
     * @return revealed edaBit vector.
     */
    public PlainZlEdaBitVector reveal(SquareZlEdaBitVector that) {
        ZlVector zlVector = this.squareZlVector.getZlVector().add(that.squareZlVector.getZlVector());
        BitVector[] bitVectors = IntStream.range(0, l)
            .mapToObj(i -> this.squareZ2Vectors[i].getBitVector().xor(that.squareZ2Vectors[i].getBitVector()))
            .toArray(BitVector[]::new);

        return PlainZlEdaBitVector.create(zlVector, bitVectors);
    }

    @Override
    public int getNum() {
        return squareZlVector.getNum();
    }
}
