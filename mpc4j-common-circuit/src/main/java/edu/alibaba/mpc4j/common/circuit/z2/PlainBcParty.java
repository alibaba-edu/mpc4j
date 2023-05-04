package edu.alibaba.mpc4j.common.circuit.z2;

import java.util.Arrays;

/**
 * Plain Boolean Circuit Party.
 *
 * @author Li Peng
 * @date 2023/4/21
 */
public class PlainBcParty implements MpcBcParty {

    @Override
    public PlainZ2Vector createOnes(int bitNum) {
        return PlainZ2Vector.createOnes(bitNum);
    }

    @Override
    public PlainZ2Vector createZeros(int bitNum) {
        return PlainZ2Vector.createZeros(bitNum);
    }

    @Override
    public PlainZ2Vector create(int bitNum, boolean value) {
        return PlainZ2Vector.create(bitNum, value);
    }

    @Override
    public PlainZ2Vector createEmpty(boolean plain) {
        return PlainZ2Vector.createEmpty();
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
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, lengths))
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
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, lengths))
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
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, lengths))
            .map(vector -> (PlainZ2Vector) vector)
            .toArray(PlainZ2Vector[]::new);
    }

    @Override
    public PlainZ2Vector not(MpcZ2Vector xi) {
        return xor(xi, PlainZ2Vector.createOnes(xi.getNum()));
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
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, bitNums))
            .map(vector -> (PlainZ2Vector) vector)
            .toArray(PlainZ2Vector[]::new);
    }
}
