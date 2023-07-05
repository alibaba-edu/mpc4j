package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.Arrays;

/**
 * Plain Boolean Circuit Party.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
public class PlainZ2cParty implements MpcZ2cParty {

    @Override
    public MpcZ2Vector create(BitVector bitVector) {
        return PlainZ2Vector.create(bitVector);
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
    public void init(int updateBitNum) {
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
        PlainZ2Vector mergeXiArray = (PlainZ2Vector) merge(xiArray);
        PlainZ2Vector mergeYiArray = (PlainZ2Vector) merge(yiArray);
        // and operation
        PlainZ2Vector mergeZiArray = and(mergeXiArray, mergeYiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        return Arrays.stream(split(mergeZiArray, bitNums))
            .map(vector -> (PlainZ2Vector) vector)
            .toArray(PlainZ2Vector[]::new);
    }

    @Override
    public PlainZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) {
        PlainZ2Vector plainXi = (PlainZ2Vector) xi;
        PlainZ2Vector plainYi = (PlainZ2Vector) yi;
        return PlainZ2Vector.create(plainXi.getBitVector().xor(plainYi.getBitVector()));
    }

    @Override
    public PlainZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        // merge xi and yi
        PlainZ2Vector mergeXiArray = (PlainZ2Vector) merge(xiArray);
        PlainZ2Vector mergeYiArray = (PlainZ2Vector) merge(yiArray);
        // xor operation
        PlainZ2Vector mergeZiArray = xor(mergeXiArray, mergeYiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        return Arrays.stream(split(mergeZiArray, bitNums))
            .map(vector -> (PlainZ2Vector) vector)
            .toArray(PlainZ2Vector[]::new);
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
        PlainZ2Vector mergeXiArray = (PlainZ2Vector) merge(xiArray);
        PlainZ2Vector mergeYiArray = (PlainZ2Vector) merge(yiArray);
        // or operation
        PlainZ2Vector mergeZiArray = or(mergeXiArray, mergeYiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        return Arrays.stream(split(mergeZiArray, bitNums))
            .map(vector -> (PlainZ2Vector) vector)
            .toArray(PlainZ2Vector[]::new);
    }

    @Override
    public PlainZ2Vector not(MpcZ2Vector xi) {
        return xor(xi, PlainZ2Vector.createOnes(xi.bitNum()));
    }

    @Override
    public PlainZ2Vector[] not(MpcZ2Vector[] xiArray) {
        if (xiArray.length == 0) {
            return new PlainZ2Vector[0];
        }
        // merge xi
        PlainZ2Vector mergeXiArray = (PlainZ2Vector) merge(xiArray);
        // not operation
        PlainZ2Vector mergeZiArray = not(mergeXiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::bitNum).toArray();
        return Arrays.stream(split(mergeZiArray, bitNums))
            .map(vector -> (PlainZ2Vector) vector)
            .toArray(PlainZ2Vector[]::new);
    }
}
