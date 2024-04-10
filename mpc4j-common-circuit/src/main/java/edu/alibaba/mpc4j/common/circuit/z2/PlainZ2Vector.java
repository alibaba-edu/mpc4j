package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;

import java.util.Arrays;
import java.util.Random;

/**
 * Plain Z2 Vector.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public class PlainZ2Vector implements MpcZ2Vector {
    /**
     * Creates a plain z2 vector with the assigned bit vector.
     *
     * @param bitVector the assigned bit vector.
     * @return a plain zl vector.
     */
    public static PlainZ2Vector create(BitVector bitVector) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = bitVector;
        return plainZ2Vector;
    }

    /**
     * Create a plain z2 vector with the assigned value.
     *
     * @param bitNum the bit num.
     * @param bytes  the assigned bits represented by bytes.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector create(int bitNum, byte[] bytes) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.create(bitNum, bytes);

        return plainZ2Vector;
    }

    /**
     * Creates a random plain z2 vector.
     *
     * @param bitNum the bit num.
     * @param random the random states.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createRandom(int bitNum, Random random) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createRandom(bitNum, random);
        return plainZ2Vector;
    }

    /**
     * Creates a plain all-one z2 vector.
     *
     * @param bitNum the bit num.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createOnes(int bitNum) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createOnes(bitNum);
        return plainZ2Vector;
    }

    /**
     * Creates a plain all-zero z2 vector.
     *
     * @param bitNum the bit num.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createZeros(int bitNum) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createZeros(bitNum);
        return plainZ2Vector;
    }

    /**
     * Creates an empty plain z2 vector.
     *
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createEmpty() {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createEmpty();
        return plainZ2Vector;
    }

    /**
     * merge inputs by padding zeros to make each input full
     *
     * @param vectors merge data
     */
    public static PlainZ2Vector mergeWithPadding(PlainZ2Vector[] vectors) {
        assert vectors.length > 0 : "merged vector length must be greater than 0";
        BitVector mergeBit = BitVectorFactory.mergeWithPadding(Arrays.stream(vectors)
            .map(PlainZ2Vector::getBitVector).toArray(BitVector[]::new));
        return create(mergeBit);
    }

    /**
     * the bit vector
     */
    private BitVector bitVector;

    /**
     * private constructor.
     */
    private PlainZ2Vector() {
        // empty
    }

    @Override
    public int byteNum() {
        return bitVector.byteNum();
    }

    @Override
    public BitVector getBitVector() {
        return bitVector;
    }

    @Override
    public BitVector[] getBitVectors() {
        return new BitVector[]{bitVector};
    }

    @Override
    public void setBitVectors(BitVector[] data){
        assert data.length == 1;
        bitVector = data[0];
    }

    @Override
    public boolean isPlain() {
        return true;
    }

    @Override
    public PlainZ2Vector copy() {
        PlainZ2Vector clone = new PlainZ2Vector();
        clone.bitVector = bitVector.copy();

        return clone;
    }

    @Override
    public int getNum() {
        return bitVector.bitNum();
    }

    @Override
    public PlainZ2Vector split(int splitNum) {
        BitVector splitVector = bitVector.split(splitNum);
        return PlainZ2Vector.create(splitVector);
    }

    @Override
    public void reduce(int reduceNum) {
        bitVector.reduce(reduceNum);
    }

    @Override
    public void merge(MpcVector other) {
        PlainZ2Vector that = (PlainZ2Vector) other;
        bitVector.merge(that.getBitVector());
    }

    @Override
    public void reverseBits(){
        this.bitVector.reverseBits();
    }

    @Override
    public PlainZ2Vector[] splitWithPadding(int[] bitNums) {
        BitVector[] splitBitVectors = getBitVector().uncheckSplitWithPadding(bitNums);
        return Arrays.stream(splitBitVectors).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
    }

    @Override
    public MpcZ2Vector extendBitsWithSkip(int destBitLen, int skipLen) {
        byte[] destByte = Z2VectorUtils.extendBitsWithSkip(this.bitVector, destBitLen, skipLen);
        return PlainZ2Vector.create(destBitLen, destByte);
    }

    @Override
    public MpcZ2Vector[] getBitsWithSkip(int totalBitNum, int skipLen) {
        byte[][] res = Z2VectorUtils.getBitsWithSkip(this.bitVector, totalBitNum, skipLen);
        return Arrays.stream(res).map(x -> PlainZ2Vector.create(totalBitNum, x)).toArray(PlainZ2Vector[]::new);
    }

    @Override
    public MpcZ2Vector getPointsWithFixedSpace(int startPos, int num, int skipLen){
        return PlainZ2Vector.create(bitVector.getBitsByInterval(startPos, num, skipLen));
    }

    public void andi(PlainZ2Vector plainZ2Vector) {
        bitVector.andi(plainZ2Vector.bitVector);
    }
}
