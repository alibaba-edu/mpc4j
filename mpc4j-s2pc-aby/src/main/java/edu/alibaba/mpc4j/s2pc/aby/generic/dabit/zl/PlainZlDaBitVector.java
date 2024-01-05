package edu.alibaba.mpc4j.s2pc.aby.generic.dabit.zl;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * plain Zl daBit vector.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class PlainZlDaBitVector implements MergedPcgPartyOutput {
    /**
     * Zl instance
     */
    private final Zl zl;
    /**
     * Zl vector
     */
    private ZlVector zlVector;
    /**
     * Z2 vector
     */
    private BitVector bitVector;

    /**
     * Creates random plain daBit vector.
     *
     * @param zl           Zl instance.
     * @param num          num.
     * @param secureRandom random state.
     * @return a plain daBit vector.
     */
    public static PlainZlDaBitVector createRandom(Zl zl, int num, SecureRandom secureRandom) {
        PlainZlDaBitVector plainDaBitVector = new PlainZlDaBitVector(zl);
        MathPreconditions.checkPositive("num", num);
        plainDaBitVector.bitVector = BitVectorFactory.createRandom(num, secureRandom);
        // P_i computes r_j = Σ_{i=0}^{m−1} {r_i * 2^i)
        BigInteger[] zlArray = IntStream.range(0, num)
            .mapToObj(index -> {
                boolean ri = plainDaBitVector.bitVector.get(index);
                return ri ? zl.createOne() : zl.createZero();
            })
            .toArray(BigInteger[]::new);
        plainDaBitVector.zlVector = ZlVector.create(zl, zlArray);

        return plainDaBitVector;
    }

    /**
     * Creates a plain daBit vector.
     *
     * @param zlVector the Zl vector.
     * @param bitVector the bit vector.
     * @return a plain daBit vector.
     */
    public static PlainZlDaBitVector create(ZlVector zlVector, BitVector bitVector) {
        int num = zlVector.getNum();
        MathPreconditions.checkPositive("num", num);
        MathPreconditions.checkEqual("num", "bitVector.num", num, bitVector.bitNum());
        Zl zl = zlVector.getZl();
        PlainZlDaBitVector plainDaBitVector = new PlainZlDaBitVector(zl);
        plainDaBitVector.zlVector = zlVector;
        plainDaBitVector.bitVector = bitVector;

        return plainDaBitVector;
    }

    /**
     * Creates an empty plain daBit vector.
     *
     * @param zl Zl instance.
     * @return a plain daBit vector.
     */
    public static PlainZlDaBitVector createEmpty(Zl zl) {
        PlainZlDaBitVector plainDaBitVector = new PlainZlDaBitVector(zl);
        plainDaBitVector.zlVector = ZlVector.createEmpty(zl);
        plainDaBitVector.bitVector = BitVectorFactory.createEmpty();

        return plainDaBitVector;
    }

    /**
     * private constructor.
     */
    private PlainZlDaBitVector(Zl zl) {
        this.zl = zl;
    }

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    public Zl getZl() {
        return zlVector.getZl();
    }

    @Override
    public PlainZlDaBitVector split(int splitNum) {
        PlainZlDaBitVector splitPlainDaBitVector = new PlainZlDaBitVector(zl);
        splitPlainDaBitVector.zlVector = zlVector.split(splitNum);
        splitPlainDaBitVector.bitVector = bitVector.split(splitNum);

        return splitPlainDaBitVector;
    }

    @Override
    public void reduce(int reduceNum) {
        zlVector.reduce(reduceNum);
        bitVector.reduce(reduceNum);
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        PlainZlDaBitVector that = (PlainZlDaBitVector) other;
        this.zlVector.merge(that.zlVector);
        this.bitVector.merge(that.bitVector);
    }

    /**
     * Gets the vector.
     *
     * @return the vector.
     */
    public ZlVector getZlVector() {
        return zlVector;
    }

    /**
     * Gets the element.
     *
     * @param index index.
     * @return the element.
     */
    public BigInteger getZlElement(int index) {
        return zlVector.getElement(index);
    }

    /**
     * Gets the bit vector.
     *
     * @return the bit vector.
     */
    public BitVector getBitVector() {
        return bitVector;
    }

    /**
     * Gets the Z2 element.
     *
     * @param index index.
     * @return the Z2 element.
     */
    public BigInteger getZ2Element(int index) {
        boolean ri = bitVector.get(index);
        return ri ? zl.createOne() : zl.createZero();
    }

    @Override
    public int getNum() {
        return zlVector.getNum();
    }
}
