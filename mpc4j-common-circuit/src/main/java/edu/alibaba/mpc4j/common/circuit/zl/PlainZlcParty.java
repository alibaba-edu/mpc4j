package edu.alibaba.mpc4j.common.circuit.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

import java.util.Arrays;

/**
 * plain Zl circuit party.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public class PlainZlcParty implements MpcZlcParty {
    /**
     * max l
     */
    private int maxL;
    /**
     * initialized
     */
    private boolean initialized;

    public PlainZlcParty() {
        // empty
    }

    @Override
    public MpcZlVector create(ZlVector zlVector) {
        MathPreconditions.checkPositiveInRangeClosed("l", zlVector.getZl().getL(), maxL);
        return PlainZlVector.create(zlVector);
    }

    @Override
    public PlainZlVector createOnes(Zl zl, int num) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl.getL(), maxL);
        return PlainZlVector.createOnes(zl, num);
    }

    @Override
    public PlainZlVector createZeros(Zl zl, int num) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl.getL(), maxL);
        return PlainZlVector.createZeros(zl, num);
    }

    @Override
    public PlainZlVector createEmpty(Zl zl, boolean plain) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl.getL(), maxL);
        return PlainZlVector.createEmpty(zl);
    }

    @Override
    public void init(int maxL, int expectTotalNum) {
        MathPreconditions.checkPositive("maxL", maxL);
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
    public PlainZlVector shareOwn(ZlVector xi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl().getL(), maxL);
        MathPreconditions.checkPositive("num", xi.getNum());
        return PlainZlVector.create(xi);
    }

    @Override
    public PlainZlVector[] shareOwn(ZlVector[] xiArray) {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(xiArray).mapToInt(ZlVector::getNum).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        return Arrays.stream(xiArray).map(PlainZlVector::create).toArray(PlainZlVector[]::new);
    }

    @Override
    public PlainZlVector shareOther(Zl zl, int num) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", zl.getL(), maxL);
        MathPreconditions.checkPositive("num", num);
        return PlainZlVector.createZeros(zl, num);
    }

    @Override
    public PlainZlVector[] shareOther(Zl zl, int[] nums) {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(nums).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        return Arrays.stream(nums).mapToObj(num -> PlainZlVector.createZeros(zl, num)).toArray(PlainZlVector[]::new);
    }

    @Override
    public ZlVector revealOwn(MpcZlVector xi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl().getL(), maxL);
        MathPreconditions.checkPositive("num", xi.getNum());
        return xi.getZlVector();
    }

    @Override
    public ZlVector[] revealOwn(MpcZlVector[] xiArray) {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(xiArray).mapToInt(MpcZlVector::getNum).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        return Arrays.stream(xiArray).map(MpcZlVector::getZlVector).toArray(ZlVector[]::new);
    }

    @Override
    public void revealOther(MpcZlVector xi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl().getL(), maxL);
        MathPreconditions.checkPositive("num", xi.getNum());
        // do nothing
    }

    @Override
    public void revealOther(MpcZlVector[] xiArray) {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(xiArray).mapToInt(MpcZlVector::getNum).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        // do nothing
    }

    @Override
    public PlainZlVector add(MpcZlVector xi, MpcZlVector yi) {
        checkDyadicOperationInputs(xi, yi);
        PlainZlVector plainXi = (PlainZlVector) xi;
        PlainZlVector plainYi = (PlainZlVector) yi;
        return PlainZlVector.create(plainXi.getZlVector().add(plainYi.getZlVector()));
    }

    @Override
    public PlainZlVector[] add(MpcZlVector[] xiArray, MpcZlVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZlVector[0];
        }
        // merge xi and yi
        PlainZlVector mergeXiArray = (PlainZlVector) merge(xiArray);
        PlainZlVector mergeYiArray = (PlainZlVector) merge(yiArray);
        // and operation
        PlainZlVector mergeZiArray = add(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZlVector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, lengths))
            .map(vector -> (PlainZlVector) vector)
            .toArray(PlainZlVector[]::new);
    }

    @Override
    public PlainZlVector sub(MpcZlVector xi, MpcZlVector yi) {
        checkDyadicOperationInputs(xi, yi);
        PlainZlVector plainXi = (PlainZlVector) xi;
        PlainZlVector plainYi = (PlainZlVector) yi;
        return PlainZlVector.create(plainXi.getZlVector().sub(plainYi.getZlVector()));
    }

    @Override
    public PlainZlVector[] sub(MpcZlVector[] xiArray, MpcZlVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZlVector[0];
        }
        // merge xi and yi
        PlainZlVector mergeXiArray = (PlainZlVector) merge(xiArray);
        PlainZlVector mergeYiArray = (PlainZlVector) merge(yiArray);
        // xor operation
        PlainZlVector mergeZiArray = sub(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(MpcZlVector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, lengths))
            .map(vector -> (PlainZlVector) vector)
            .toArray(PlainZlVector[]::new);
    }

    @Override
    public PlainZlVector neg(MpcZlVector xi) {
        checkUnaryOperationInput(xi);
        PlainZlVector plainXi = (PlainZlVector) xi;
        return PlainZlVector.create(plainXi.getZlVector().neg());
    }

    @Override
    public PlainZlVector[] neg(MpcZlVector[] xiArray) {
        if (xiArray.length == 0) {
            return new PlainZlVector[0];
        }
        // merge xi
        PlainZlVector mergeXiArray = (PlainZlVector) merge(xiArray);
        // not operation
        PlainZlVector mergeZiArray = neg(mergeXiArray);
        // split
        int[] nums = Arrays.stream(xiArray).mapToInt(MpcZlVector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, nums))
            .map(vector -> (PlainZlVector) vector)
            .toArray(PlainZlVector[]::new);
    }

    @Override
    public PlainZlVector mul(MpcZlVector xi, MpcZlVector yi) {
        checkDyadicOperationInputs(xi, yi);
        PlainZlVector plainXi = (PlainZlVector) xi;
        PlainZlVector plainYi = (PlainZlVector) yi;
        return PlainZlVector.create(plainXi.getZlVector().mul(plainYi.getZlVector()));
    }

    @Override
    public PlainZlVector[] mul(MpcZlVector[] xiArray, MpcZlVector[] yiArray) {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new PlainZlVector[0];
        }
        // merge xi and yi
        PlainZlVector mergeXiArray = (PlainZlVector) merge(xiArray);
        PlainZlVector mergeYiArray = (PlainZlVector) merge(yiArray);
        // or operation
        PlainZlVector mergeZiArray = mul(mergeXiArray, mergeYiArray);
        // split
        int[] nums = Arrays.stream(xiArray).mapToInt(MpcZlVector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, nums))
            .map(vector -> (PlainZlVector) vector)
            .toArray(PlainZlVector[]::new);
    }

    private void checkUnaryOperationInput(MpcZlVector xi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl().getL(), maxL);
        MathPreconditions.checkPositive("num", xi.getNum());
    }

    private void checkDyadicOperationInputs(MpcZlVector xi, MpcZlVector yi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl().getL(), maxL);
        Preconditions.checkArgument(xi.getZl().equals(yi.getZl()));
        MathPreconditions.checkPositive("num", xi.getNum());
        MathPreconditions.checkEqual("xi.num", "yi.num", xi.getNum(), yi.getNum());
    }
}
