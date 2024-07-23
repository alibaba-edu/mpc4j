package edu.alibaba.mpc4j.s2pc.aby.pcg.dabit;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Zl daBit tuple.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
public class ZlDaBitTuple implements MergedPcgPartyOutput {
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
    public static ZlDaBitTuple create(SquareZlVector squareZlVector, SquareZ2Vector squareZ2Vector) {
        MathPreconditions.checkEqual("Zl.length", "Z2.length", squareZlVector.getNum(), squareZ2Vector.getNum());
        // Zl vector and Z2 vector must be secret
        Preconditions.checkArgument(!squareZlVector.isPlain());
        Preconditions.checkArgument(!squareZ2Vector.isPlain());
        int num = squareZ2Vector.getNum();
        Zl zl = squareZlVector.getZl();
        if (num == 0) {
            return createEmpty(zl);
        } else {
            ZlDaBitTuple daBitVector = new ZlDaBitTuple();
            daBitVector.squareZlVector = squareZlVector;
            daBitVector.squareZ2Vector = squareZ2Vector;
            return daBitVector;
        }
    }

    /**
     * Creates an empty daBit vector.
     *
     * @param zl Zl instance.
     * @return a daBit vector.
     */
    public static ZlDaBitTuple createEmpty(Zl zl) {
        ZlDaBitTuple daBitVector = new ZlDaBitTuple();
        daBitVector.squareZlVector = SquareZlVector.createEmpty(zl, false);
        daBitVector.squareZ2Vector = SquareZ2Vector.createEmpty(false);
        return daBitVector;
    }

    /**
     * create a random daBit vector.
     *
     * @param zl           Zl instance.
     * @param num          num.
     * @param secureRandom random state.
     * @return a random daBit vector.
     */
    public static ZlDaBitTuple createRandom(Zl zl, int num, SecureRandom secureRandom) {
        if (num == 0) {
            return createEmpty(zl);
        } else {
            BitVector bitVector = BitVectorFactory.createRandom(num, secureRandom);
            SquareZ2Vector squareZ2Vector = SquareZ2Vector.create(bitVector, false);
            ZlVector zlVector = ZlVector.createRandom(zl, num, secureRandom);
            SquareZlVector squareZlVector = SquareZlVector.create(zlVector, false);
            return create(squareZlVector, squareZ2Vector);
        }
    }

    /**
     * Creates a random daBit vector.
     *
     * @param that         that daBit vector.
     * @param secureRandom random state.
     * @return a random daBit vector.
     */
    public static ZlDaBitTuple createRandom(ZlDaBitTuple that, SecureRandom secureRandom) {
        int num = that.getNum();
        Zl zl = that.getZl();
        if (num == 0) {
            return createEmpty(zl);
        } else {
            BitVector randomZ2Vector = BitVectorFactory.createRandom(num, secureRandom);
            BitVector thisBitVector = randomZ2Vector.xor(that.getSquareZ2Vector().getBitVector());
            SquareZ2Vector thisSquareZ2Vector = SquareZ2Vector.create(thisBitVector, false);
            BigInteger[] randomZlArray = IntStream.range(0, num)
                .mapToObj(i -> {
                    if (randomZ2Vector.get(i)) {
                        return zl.createOne();
                    } else {
                        return zl.createZero();
                    }
                })
                .toArray(BigInteger[]::new);
            ZlVector randomZlVector = ZlVector.create(zl, randomZlArray);
            ZlVector thisZlVector = randomZlVector.sub(that.getSquareZlVector().getZlVector());
            SquareZlVector thisSquareZlVector = SquareZlVector.create(thisZlVector, false);
            return create(thisSquareZlVector, thisSquareZ2Vector);
        }
    }

    /**
     * private constructor.
     */
    private ZlDaBitTuple() {
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

    /**
     * Copies vector.
     *
     * @return vector.
     */
    public ZlDaBitTuple copy() {
        ZlDaBitTuple copy = new ZlDaBitTuple();
        copy.squareZlVector = squareZlVector.copy();
        copy.squareZ2Vector = squareZ2Vector.copy();
        return copy;
    }

    @Override
    public ZlDaBitTuple split(int splitNum) {
        ZlDaBitTuple splitDaBitVector = new ZlDaBitTuple();
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
        ZlDaBitTuple that = (ZlDaBitTuple) other;
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

    @Override
    public int getNum() {
        return squareZlVector.getNum();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(squareZlVector)
            .append(squareZ2Vector)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZlDaBitTuple that) {
            return new EqualsBuilder()
                .append(this.squareZlVector, that.squareZlVector)
                .append(this.squareZ2Vector, that.squareZ2Vector)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("[\n%s,\n%s\n]", squareZlVector.toString(), squareZ2Vector.toString());
    }
}
