package edu.alibaba.mpc4j.common.circuit.zl64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

import java.util.Arrays;

/**
 * plain Zl64 circuit party.
 *
 * @author Weiran Liu
 * @date 2024/6/20
 */
public class PlainZl64cParty implements MpcZl64cParty {
    /**
     * max l
     */
    private int maxL;
    /**
     * initialized
     */
    private boolean initialized;

    public PlainZl64cParty() {
        // empty
    }

    @Override
    public MpcZl64Vector create(Zl64Vector zl64Vector) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl64Vector.getZl64().getL(), maxL);
        return PlainZl64Vector.create(zl64Vector);
    }

    @Override
    public PlainZl64Vector createOnes(Zl64 zl64, int num) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl64.getL(), maxL);
        return PlainZl64Vector.createOnes(zl64, num);
    }

    @Override
    public PlainZl64Vector createZeros(Zl64 zl64, int num) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl64.getL(), maxL);
        return PlainZl64Vector.createZeros(zl64, num);
    }

    @Override
    public PlainZl64Vector createEmpty(Zl64 zl64, boolean plain) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl64.getL(), maxL);
        return PlainZl64Vector.createEmpty(zl64);
    }

    @Override
    public void init(int maxL, int expectTotalNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxL", maxL, Long.SIZE);
        MathPreconditions.checkPositive("expect_total_num", expectTotalNum);
        this.maxL = maxL;
        initialized = true;
    }

    @Override
    public void init(int maxL) {
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxL = maxL;
        initialized = true;
    }

    @Override
    public PlainZl64Vector shareOwn(Zl64Vector xi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl64().getL(), maxL);
        MathPreconditions.checkPositive("num", xi.getNum());
        return PlainZl64Vector.create(xi);
    }

    @Override
    public PlainZl64Vector[] shareOwn(Zl64Vector[] xiArray) {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(xiArray).mapToInt(Zl64Vector::getNum).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        return Arrays.stream(xiArray).map(PlainZl64Vector::create).toArray(PlainZl64Vector[]::new);
    }

    @Override
    public PlainZl64Vector shareOther(Zl64 zl64, int num) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", zl64.getL(), maxL);
        MathPreconditions.checkPositive("num", num);
        return PlainZl64Vector.createZeros(zl64, num);
    }

    @Override
    public PlainZl64Vector[] shareOther(Zl64 zl64, int[] nums) {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(nums).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        return Arrays.stream(nums).mapToObj(num -> PlainZl64Vector.createZeros(zl64, num)).toArray(PlainZl64Vector[]::new);
    }

    @Override
    public Zl64Vector revealOwn(MpcZl64Vector xi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl64().getL(), maxL);
        MathPreconditions.checkPositive("num", xi.getNum());
        return xi.getZl64Vector();
    }

    @Override
    public Zl64Vector[] revealOwn(MpcZl64Vector[] xiArray) {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(xiArray).mapToInt(MpcZl64Vector::getNum).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        return Arrays.stream(xiArray).map(MpcZl64Vector::getZl64Vector).toArray(Zl64Vector[]::new);
    }

    @Override
    public void revealOther(MpcZl64Vector xi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl64().getL(), maxL);
        MathPreconditions.checkPositive("num", xi.getNum());
        // do nothing
    }

    @Override
    public void revealOther(MpcZl64Vector[] xiArray) {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(xiArray).mapToInt(MpcZl64Vector::getNum).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        // do nothing
    }

    @Override
    public PlainZl64Vector add(MpcZl64Vector xi, MpcZl64Vector yi) {
        checkDyadicOperationInputs(xi, yi);
        PlainZl64Vector plainXi = (PlainZl64Vector) xi;
        PlainZl64Vector plainYi = (PlainZl64Vector) yi;
        return PlainZl64Vector.create(plainXi.getZl64Vector().add(plainYi.getZl64Vector()));
    }

    @Override
    public PlainZl64Vector[] add(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZl64Vector[0];
        }
        // merge xi and yi
        PlainZl64Vector mergeXiArray = (PlainZl64Vector) merge(xiArray);
        PlainZl64Vector mergeYiArray = (PlainZl64Vector) merge(yiArray);
        // and operation
        PlainZl64Vector mergeZiArray = add(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZl64Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, lengths))
            .map(vector -> (PlainZl64Vector) vector)
            .toArray(PlainZl64Vector[]::new);
    }

    @Override
    public PlainZl64Vector sub(MpcZl64Vector xi, MpcZl64Vector yi) {
        checkDyadicOperationInputs(xi, yi);
        PlainZl64Vector plainXi = (PlainZl64Vector) xi;
        PlainZl64Vector plainYi = (PlainZl64Vector) yi;
        return PlainZl64Vector.create(plainXi.getZl64Vector().sub(plainYi.getZl64Vector()));
    }

    @Override
    public PlainZl64Vector[] sub(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZl64Vector[0];
        }
        // merge xi and yi
        PlainZl64Vector mergeXiArray = (PlainZl64Vector) merge(xiArray);
        PlainZl64Vector mergeYiArray = (PlainZl64Vector) merge(yiArray);
        // xor operation
        PlainZl64Vector mergeZiArray = sub(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZl64Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, lengths))
            .map(vector -> (PlainZl64Vector) vector)
            .toArray(PlainZl64Vector[]::new);
    }

    @Override
    public PlainZl64Vector neg(MpcZl64Vector xi) {
        checkUnaryOperationInput(xi);
        PlainZl64Vector plainXi = (PlainZl64Vector) xi;
        return PlainZl64Vector.create(plainXi.getZl64Vector().neg());
    }

    @Override
    public PlainZl64Vector[] neg(MpcZl64Vector[] xiArray) {
        if (xiArray.length == 0) {
            return new PlainZl64Vector[0];
        }
        // merge xi
        PlainZl64Vector mergeXiArray = (PlainZl64Vector) merge(xiArray);
        // not operation
        PlainZl64Vector mergeZiArray = neg(mergeXiArray);
        // split
        int[] nums = Arrays.stream(xiArray).mapToInt(MpcZl64Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, nums))
            .map(vector -> (PlainZl64Vector) vector)
            .toArray(PlainZl64Vector[]::new);
    }

    @Override
    public PlainZl64Vector mul(MpcZl64Vector xi, MpcZl64Vector yi) {
        checkDyadicOperationInputs(xi, yi);
        PlainZl64Vector plainXi = (PlainZl64Vector) xi;
        PlainZl64Vector plainYi = (PlainZl64Vector) yi;
        return PlainZl64Vector.create(plainXi.getZl64Vector().mul(plainYi.getZl64Vector()));
    }

    @Override
    public PlainZl64Vector[] mul(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZl64Vector[0];
        }
        // merge xi and yi
        PlainZl64Vector mergeXiArray = (PlainZl64Vector) merge(xiArray);
        PlainZl64Vector mergeYiArray = (PlainZl64Vector) merge(yiArray);
        // or operation
        PlainZl64Vector mergeZiArray = mul(mergeXiArray, mergeYiArray);
        // split
        int[] nums = Arrays.stream(xiArray).mapToInt(MpcZl64Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, nums))
            .map(vector -> (PlainZl64Vector) vector)
            .toArray(PlainZl64Vector[]::new);
    }

    private void checkUnaryOperationInput(MpcZl64Vector xi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl64().getL(), maxL);
        MathPreconditions.checkPositive("num", xi.getNum());
    }

    private void checkDyadicOperationInputs(MpcZl64Vector xi, MpcZl64Vector yi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl64().getL(), maxL);
        Preconditions.checkArgument(xi.getZl64().equals(yi.getZl64()));
        MathPreconditions.checkPositive("num", xi.getNum());
        MathPreconditions.checkEqual("xi.num", "yi.num", xi.getNum(), yi.getNum());
    }
}
