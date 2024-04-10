package edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.crypto.CryptoException;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * the replicated shared bit vector
 * this structure only support shared values
 *
 * @author Feng Han
 * @date 2023/12/15
 */
public class TripletRpZ2Vector implements TripletZ2Vector {
    public static TripletRpZ2Vector create(BitVector... innerVec) {
        assert innerVec.length == 2;
        assert innerVec[0].bitNum() == innerVec[1].bitNum();
        TripletRpZ2Vector shareBitVector = new TripletRpZ2Vector();
        shareBitVector.innerVec = innerVec;
        return shareBitVector;
    }

    public static TripletRpZ2Vector create(byte[][] bytes, int bitNum) {
        assert bytes.length == 2;
        assert bytes[0].length == bytes[1].length;
        return create(BitVectorFactory.create(bitNum, bytes[0]), BitVectorFactory.create(bitNum, bytes[1]));
    }

    public static TripletRpZ2Vector createEmpty(int bitNum) {
        assert bitNum >= 0;
        TripletRpZ2Vector shareBitVector = new TripletRpZ2Vector();
        shareBitVector.innerVec = new BitVector[]{BitVectorFactory.createZeros(bitNum), BitVectorFactory.createZeros(bitNum)};
        return shareBitVector;
    }

    public static TripletRpZ2Vector copyOfByte(TripletRpZ2Vector other, int startByteIndex, int endByteIndex, int targetBitNum) {
        assert targetBitNum >= 0 && startByteIndex >= 0;
        MathPreconditions.checkEqual("endByteIndex - startByteIndex", "targetByteNum",
            endByteIndex - startByteIndex, CommonUtils.getByteLength(targetBitNum));
        MathPreconditions.checkGreaterOrEqual("other.byteNum() >= endByteIndex", other.byteNum(), endByteIndex);
        TripletRpZ2Vector tmp = create(
            Arrays.stream(other.innerVec).map(each -> Arrays.copyOfRange(each.getBytes(), startByteIndex, endByteIndex)).toArray(byte[][]::new),
            (endByteIndex - startByteIndex) << 3);
        if ((targetBitNum & 7) > 0) {
            tmp.reduce(targetBitNum);
        }
        return tmp;
    }

    public static TripletRpZ2Vector mergeWithPadding(TripletRpZ2Vector[] data) {
        if (data.length == 1) {
            return data[0].copy();
        }
        BitVector[][] inner = IntStream.range(0, 2).mapToObj(i ->
            Arrays.stream(data).map(x -> x.getBitVectors()[i]).toArray(BitVector[]::new)).toArray(BitVector[][]::new);
        return create(Arrays.stream(inner).map(BitVectorFactory::mergeWithPadding).toArray(BitVector[]::new));
    }

    /**
     * the bit vector
     */
    private BitVector[] innerVec;

    /**
     * private constructor.
     */
    private TripletRpZ2Vector() {

    }

    @Override
    public boolean isPlain() {
        return false;
    }

    @Override
    public TripletRpZ2Vector copy() {
        assert !isPlain();
        TripletRpZ2Vector clone = new TripletRpZ2Vector();
        clone.innerVec = Arrays.stream(innerVec).map(BitVector::copy).toArray(BitVector[]::new);
        return clone;
    }

    @Override
    public int getNum() {
        return innerVec[0].bitNum();
    }

    @Override
    public MpcVector split(int splitNum) {
        assert !isPlain();
        BitVector[] splitBitVectors = Arrays.stream(innerVec).map(x -> x.split(splitNum)).toArray(BitVector[]::new);
        return TripletRpZ2Vector.create(splitBitVectors);
    }

    @Override
    public void reduce(int reduceNum) {
        for (BitVector bitVector : innerVec) {
            bitVector.reduce(reduceNum);
        }
    }

    @Override
    public void merge(MpcVector other) {
        TripletRpZ2Vector that = (TripletRpZ2Vector) other;
        for (int i = 0; i < innerVec.length; i++) {
            innerVec[i].merge(that.getBitVectors()[i]);
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(innerVec)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TripletRpZ2Vector) {
            TripletRpZ2Vector that = (TripletRpZ2Vector) obj;
            return new EqualsBuilder()
                .append(this.innerVec[0], that.innerVec[0])
                .append(this.innerVec[1], that.innerVec[1])
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("secret: [%s, %s]", innerVec[0].toString(), innerVec[1].toString());
    }


    @Override
    public BitVector getBitVector() {
        try {
            throw new CryptoException("should not call this function in aby3");
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BitVector[] getBitVectors() {
        return innerVec;
    }

    @Override
    public void extendLength(int targetBitLength) {
        Arrays.stream(innerVec).forEach(each -> each.extendBitNum(targetBitLength));
    }

    @Override
    public TripletZ2Vector padShiftLeft(int n) {
        BitVector[] bitVectors = Arrays.stream(innerVec).map(each -> each.padShiftLeft(n)).toArray(BitVector[]::new);
        return create(bitVectors);
    }

    @Override
    public TripletZ2Vector reduceShiftRight(int n) {
        BitVector[] bitVectors = Arrays.stream(innerVec).map(each -> each.reduceShiftRight(n)).toArray(BitVector[]::new);
        return create(bitVectors);
    }

    @Override
    public void reduceShiftRighti(int n) {
        Arrays.stream(innerVec).forEach(each -> each.reduceShiftRighti(n));
    }

    @Override
    public void fixShiftRighti(int n) {
        Arrays.stream(innerVec).forEach(each -> each.fixShiftRighti(n));
    }

    @Override
    public void fixShiftLefti(int n) {
        Arrays.stream(innerVec).forEach(each -> each.fixShiftLefti(n));
    }

    @Override
    public void setBitVectors(BitVector... data) {
        assert data.length == 2;
        System.arraycopy(data, 0, innerVec, 0, 2);
    }

    @Override
    public int byteNum() {
        return innerVec[0].byteNum();
    }

    @Override
    public void reverseBits() {
        Arrays.stream(innerVec).forEach(BitVector::reverseBits);
    }

    @Override
    public TripletRpZ2Vector[] splitWithPadding(int[] bitNums) {
        if (bitNums.length == 1) {
            TripletRpZ2Vector tmp = this.copy();
            tmp.reduce(bitNums[0]);
            return new TripletRpZ2Vector[]{tmp};
        }
        BitVector[] s0 = innerVec[0].uncheckSplitWithPadding(bitNums);
        BitVector[] s1 = innerVec[1].uncheckSplitWithPadding(bitNums);
        return IntStream.range(0, bitNums.length).mapToObj(i ->
            create(s0[i], s1[i])).toArray(TripletRpZ2Vector[]::new);
    }

    @Override
    public MpcZ2Vector extendBitsWithSkip(int destBitLen, int skipLen) {
        byte[] destByte0 = Z2VectorUtils.extendBitsWithSkip(this.innerVec[0], destBitLen, skipLen);
        byte[] destByte1 = Z2VectorUtils.extendBitsWithSkip(this.innerVec[1], destBitLen, skipLen);
        return TripletRpZ2Vector.create(new byte[][]{destByte0, destByte1}, destBitLen);
    }

    @Override
    public MpcZ2Vector[] getBitsWithSkip(int totalBitNum, int skipLen) {
        byte[][] res0 = Z2VectorUtils.getBitsWithSkip(this.innerVec[0], totalBitNum, skipLen);
        byte[][] res1 = Z2VectorUtils.getBitsWithSkip(this.innerVec[1], totalBitNum, skipLen);
        return IntStream.range(0, res0.length).mapToObj(i ->
            TripletRpZ2Vector.create(new byte[][]{res0[i], res1[i]}, totalBitNum)).toArray(TripletRpZ2Vector[]::new);
    }

    @Override
    public MpcZ2Vector getPointsWithFixedSpace(int startPos, int num, int skipLen) {
        return TripletRpZ2Vector.create(innerVec[0].getBitsByInterval(startPos, num, skipLen),
            innerVec[1].getBitsByInterval(startPos, num, skipLen));
    }

    @Override
    public void setPointsWithFixedSpace(MpcZ2Vector source, int startPos, int num, int skipLen) {
        assert isPlain() == source.isPlain();
        assert source instanceof TripletRpZ2Vector;
        TripletRpZ2Vector that = (TripletRpZ2Vector) source;
        innerVec[0].setBitsByInterval(that.getBitVectors()[0], startPos, num, skipLen);
        innerVec[1].setBitsByInterval(that.getBitVectors()[1], startPos, num, skipLen);
    }

    /**
     * set values in bytes: copy (data[sourceStartIndex] - data[sourceStartIndex + byteLen]) into (this[targetStartIndex], this[targetStartIndex + byteLen])
     *
     * @param data             source data
     * @param sourceStartIndex starting position in the source array.
     * @param targetStartIndex starting position in the destination byte array
     * @param byteLen          the number of bytes to be copied.
     */
    public void setBytes(TripletRpZ2Vector data, int sourceStartIndex, int targetStartIndex, int byteLen) {
        innerVec[0].setBytes(data.getBitVectors()[0].getBytes(), sourceStartIndex, targetStartIndex, byteLen);
        innerVec[1].setBytes(data.getBitVectors()[1].getBytes(), sourceStartIndex, targetStartIndex, byteLen);
    }
}
