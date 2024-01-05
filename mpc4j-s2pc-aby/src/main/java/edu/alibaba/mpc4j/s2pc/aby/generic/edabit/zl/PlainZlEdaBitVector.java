package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * plain Zl edaBit vector.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
public class PlainZlEdaBitVector implements MergedPcgPartyOutput {
    /**
     * Zl instance
     */
    private final Zl zl;
    /**
     * l
     */
    private final int l;
    /**
     * l in byte
     */
    private final int byteL;
    /**
     * offset
     */
    private final int offset;
    /**
     * Zl vector
     */
    private ZlVector zlVector;
    /**
     * Z2 vectors
     */
    private BitVector[] bitVectors;

    /**
     * Creates random plain edaBit vector.
     *
     * @param zl           Zl instance.
     * @param num          num.
     * @param secureRandom random state.
     * @return a plain edaBit vector.
     */
    public static PlainZlEdaBitVector createRandom(Zl zl, int num, SecureRandom secureRandom) {
        PlainZlEdaBitVector plainEdaBitVector = new PlainZlEdaBitVector(zl);
        MathPreconditions.checkPositive("num", num);
        // P_i samples r_0, ... , r_{m−1} ∈ Z_2
        plainEdaBitVector.bitVectors = IntStream.range(0, plainEdaBitVector.l)
            .mapToObj(i -> BitVectorFactory.createRandom(num, secureRandom))
            .toArray(BitVector[]::new);
        // P_i computes r_j = Σ_{i=0}^{m−1} {r_i * 2^i)
        BigInteger[] zlArray = IntStream.range(0, num)
            .mapToObj(index -> {
                BigInteger element = zl.createZero();
                for (int i = 0; i < plainEdaBitVector.l; i++) {
                    boolean ri = plainEdaBitVector.bitVectors[i].get(index);
                    if (ri) {
                        element = zl.add(element, BigInteger.ONE.shiftLeft(plainEdaBitVector.l - 1 - i));
                    }
                }
                return element;
            })
            .toArray(BigInteger[]::new);
        plainEdaBitVector.zlVector = ZlVector.create(zl, zlArray);

        return plainEdaBitVector;
    }

    /**
     * Creates a plain edaBit vector.
     *
     * @param zlVector   the vector.
     * @param bitVectors the bit vectors.
     * @return a plain edaBit vector.
     */
    public static PlainZlEdaBitVector create(ZlVector zlVector, BitVector[] bitVectors) {
        int num = zlVector.getNum();
        MathPreconditions.checkPositive("num", num);
        Zl zl = zlVector.getZl();
        int l = zl.getL();
        MathPreconditions.checkEqual("l", "bitVectors.length", l, bitVectors.length);
        IntStream.range(0, l).forEach(i ->
            MathPreconditions.checkEqual("num", i + "-th bitVector.bitNum", num, bitVectors[i].bitNum())
        );
        PlainZlEdaBitVector plainEdaBitVector = new PlainZlEdaBitVector(zl);
        plainEdaBitVector.zlVector = zlVector;
        plainEdaBitVector.bitVectors = bitVectors;

        return plainEdaBitVector;
    }

    /**
     * Creates an empty plain edaBit vector.
     *
     * @param zl Zl instance.
     * @return a plain edaBit vector.
     */
    public static PlainZlEdaBitVector createEmpty(Zl zl) {
        PlainZlEdaBitVector plainEdaBitVector = new PlainZlEdaBitVector(zl);
        plainEdaBitVector.zlVector = ZlVector.createEmpty(zl);
        plainEdaBitVector.bitVectors = IntStream.range(0, plainEdaBitVector.l)
            .mapToObj(i -> BitVectorFactory.createEmpty())
            .toArray(BitVector[]::new);
        return plainEdaBitVector;
    }

    /**
     * private constructor.
     */
    private PlainZlEdaBitVector(Zl zl) {
        this.zl = zl;
        l = zl.getL();
        byteL = zl.getByteL();
        offset = byteL * Byte.SIZE - l;
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
    public PlainZlEdaBitVector split(int splitNum) {
        PlainZlEdaBitVector splitPrivateEdaBitVector = new PlainZlEdaBitVector(zl);
        splitPrivateEdaBitVector.zlVector = zlVector.split(splitNum);
        splitPrivateEdaBitVector.bitVectors = IntStream.range(0, l)
            .mapToObj(i -> bitVectors[i].split(splitNum))
            .toArray(BitVector[]::new);

        return splitPrivateEdaBitVector;
    }

    @Override
    public void reduce(int reduceNum) {
        zlVector.reduce(reduceNum);
        IntStream.range(0, l).forEach(i -> bitVectors[i].reduce(reduceNum));
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        PlainZlEdaBitVector that = (PlainZlEdaBitVector) other;
        this.zlVector.merge(that.zlVector);
        IntStream.range(0, l).forEach(i -> this.bitVectors[i].merge(that.bitVectors[i]));
    }

    /**
     * Gets vector.
     *
     * @return vector.
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
     * Gets bit vectors.
     *
     * @return bit vectors.
     */
    public BitVector[] getBitVectors() {
        return bitVectors;
    }

    /**
     * Gets the Z2 element.
     *
     * @param index index.
     * @return the Z2 element.
     */
    public BigInteger getZ2Element(int index) {
        byte[] element = new byte[byteL];
        for (int i = 0; i < l; i++) {
            boolean ri = bitVectors[i].get(index);
            BinaryUtils.setBoolean(element, offset + i, ri);
        }
        return BigIntegerUtils.byteArrayToNonNegBigInteger(element);
    }

    @Override
    public int getNum() {
        return zlVector.getNum();
    }
}
