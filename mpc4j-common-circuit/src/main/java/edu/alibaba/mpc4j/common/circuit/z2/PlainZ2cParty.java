package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Plain Boolean Circuit Party.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
public class PlainZ2cParty implements MpcZ2cParty {

    @Override
    public boolean getParallel() {
        return true;
    }

    @Override
    public MpcZ2Vector create(boolean isPlain, BitVector... bitVector) {
        assert isPlain && bitVector.length == 1;
        return PlainZ2Vector.create(bitVector[0]);
    }

    @Override
    public PlainZ2Vector createOnes(int bitNum) {
        return PlainZ2Vector.createOnes(bitNum);
    }

    @Override
    public PlainZ2Vector createZeros(int bitNum) {
        return PlainZ2Vector.createZeros(bitNum);
    }

    @Override
    public PlainZ2Vector createEmpty(boolean plain) {
        return PlainZ2Vector.createEmpty();
    }

    @Override
    public void init(long updateBitNum) {
        MathPreconditions.checkPositive("updateBitNum", updateBitNum);
    }

    @Override
    public PlainZ2Vector shareOwn(BitVector xi) {
        MathPreconditions.checkPositive("bitNum", xi.bitNum());
        // do nothing
        return null;
    }

    @Override
    public MpcZ2Vector[] shareOwn(BitVector[] xiArray) {
        int totalBitNum = Arrays.stream(xiArray).mapToInt(BitVector::bitNum).sum();
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        // do nothing
        return null;
    }

    @Override
    public MpcZ2Vector shareOther(int bitNum) throws MpcAbortException {
        MathPreconditions.checkPositive("bitNum", bitNum);
        // do nothing
        return null;
    }

    @Override
    public MpcZ2Vector[] shareOther(int[] bitNums) throws MpcAbortException {
        int totalBitNum = Arrays.stream(bitNums).sum();
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        // do nothing
        return null;
    }

    @Override
    public BitVector[] open(MpcZ2Vector[] xiArray) throws MpcAbortException{
        int totalBitNum = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).sum();
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        // do nothing
        return null;
    }

    @Override
    public BitVector revealOwn(MpcZ2Vector xi) throws MpcAbortException {
        MathPreconditions.checkPositive("bitNum", xi.bitNum());
        // do nothing
        return null;
    }

    @Override
    public BitVector[] revealOwn(MpcZ2Vector[] xiArray) throws MpcAbortException {
        int totalBitNum = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).sum();
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        // do nothing
        return null;
    }

    @Override
    public void revealOther(MpcZ2Vector xi) {
        MathPreconditions.checkPositive("bitNum", xi.bitNum());
        // do nothing
    }

    @Override
    public void revealOther(MpcZ2Vector[] xiArray) {
        int totalBitNum = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).sum();
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        // do nothing
    }

    @Override
    public PlainZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) {
        PlainZ2Vector plainXi = (PlainZ2Vector) xi;
        PlainZ2Vector plainYi = (PlainZ2Vector) yi;
        return PlainZ2Vector.create(plainXi.getBitVector().and(plainYi.getBitVector()));
    }

    @Override
    public PlainZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        // merge xi and yi
        PlainZ2Vector mergeXiArray = (PlainZ2Vector) mergeWithPadding(xiArray);
        PlainZ2Vector mergeYiArray = (PlainZ2Vector) mergeWithPadding(yiArray);
        // and operation
        PlainZ2Vector mergeZiArray = and(mergeXiArray, mergeYiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        return Arrays.stream(mergeZiArray.splitWithPadding(bitNums)).toArray(PlainZ2Vector[]::new);
    }

    @Override
    public PlainZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) {
        PlainZ2Vector plainXi = (PlainZ2Vector) xi;
        PlainZ2Vector plainYi = (PlainZ2Vector) yi;
        return PlainZ2Vector.create(plainXi.getBitVector().xor(plainYi.getBitVector()));
    }

    @Override
    public void xori(MpcZ2Vector xi, MpcZ2Vector yi) {
        xi.getBitVector().xori(yi.getBitVector());
    }


    @Override
    public PlainZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        return IntStream.range(0, xiArray.length).mapToObj(i -> xor(xiArray[i], yiArray[i])).toArray(PlainZ2Vector[]::new);
    }

    @Override
    public PlainZ2Vector or(MpcZ2Vector xi, MpcZ2Vector yi) {
        PlainZ2Vector plainXi = (PlainZ2Vector) xi;
        PlainZ2Vector plainYi = (PlainZ2Vector) yi;
        return PlainZ2Vector.create(plainXi.getBitVector().or(plainYi.getBitVector()));
    }

    @Override
    public PlainZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        // merge xi and yi
        PlainZ2Vector mergeXiArray = (PlainZ2Vector) mergeWithPadding(xiArray);
        PlainZ2Vector mergeYiArray = (PlainZ2Vector) mergeWithPadding(yiArray);
        // or operation
        PlainZ2Vector mergeZiArray = or(mergeXiArray, mergeYiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        return Arrays.stream(mergeZiArray.splitWithPadding(bitNums)).toArray(PlainZ2Vector[]::new);
    }

    @Override
    public PlainZ2Vector not(MpcZ2Vector xi) {
        return xor(xi, PlainZ2Vector.createOnes(xi.bitNum()));
    }

    @Override
    public void noti(MpcZ2Vector xi) {
        xi.getBitVector().noti();
    }

    @Override
    public PlainZ2Vector[] not(MpcZ2Vector[] xiArray) {
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        // merge xi
        PlainZ2Vector mergeXiArray = (PlainZ2Vector) mergeWithPadding(xiArray);
        // not operation
        PlainZ2Vector mergeZiArray = not(mergeXiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        return Arrays.stream(mergeZiArray.splitWithPadding(bitNums)).toArray(PlainZ2Vector[]::new);
    }

    @Override
    public PlainZ2Vector[] setPublicValues(BitVector[] data) {
        assert data != null && data.length > 0;
        int bitNum = data[0].bitNum();
        return Arrays.stream(data).map(x -> {
            MathPreconditions.checkEqual("data[i].bitNum()", "data[0].bitNum()", x.bitNum(), bitNum);
            return PlainZ2Vector.create(x);
        }).toArray(PlainZ2Vector[]::new);
    }
}
