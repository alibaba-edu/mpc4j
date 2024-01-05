package edu.alibaba.mpc4j.common.circuit.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

import java.util.Arrays;

/**
 * plain Zl party.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public class PlainZlParty implements MpcZlParty {
    /**
     * the Zl instance
     */
    private final Zl zl;
    /**
     * initialized
     */
    private boolean initialized;

    public PlainZlParty(Zl zl) {
        this.zl = zl;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public MpcZlVector create(ZlVector zlVector) {
        return PlainZlVector.create(zlVector);
    }

    @Override
    public PlainZlVector createOnes(int num) {
        return PlainZlVector.createOnes(zl, num);
    }

    @Override
    public PlainZlVector createZeros(int num) {
        return PlainZlVector.createZeros(zl, num);
    }

    @Override
    public PlainZlVector createEmpty(boolean plain) {
        return PlainZlVector.createEmpty(zl);
    }

    @Override
    public void init(int updateNum) {
        MathPreconditions.checkPositive("updateNum", updateNum);
        initialized = true;
    }

    @Override
    public PlainZlVector shareOwn(ZlVector xi) {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositive("num", xi.getNum());
        // do nothing
        return null;
    }

    @Override
    public PlainZlVector[] shareOwn(ZlVector[] xiArray) {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(xiArray).mapToInt(ZlVector::getNum).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        // do nothing
        return null;
    }

    @Override
    public PlainZlVector shareOther(int num) throws MpcAbortException {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositive("num", num);
        // do nothing
        return null;
    }

    @Override
    public PlainZlVector[] shareOther(int[] nums) throws MpcAbortException {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(nums).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        // do nothing
        return null;
    }

    @Override
    public ZlVector revealOwn(MpcZlVector xi) throws MpcAbortException {
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositive("num", xi.getNum());
        // do nothing
        return null;
    }

    @Override
    public ZlVector[] revealOwn(MpcZlVector[] xiArray) throws MpcAbortException {
        Preconditions.checkArgument(initialized);
        int totalNum = Arrays.stream(xiArray).mapToInt(MpcZlVector::getNum).sum();
        MathPreconditions.checkPositive("totalNum", totalNum);
        // do nothing
        return null;
    }

    @Override
    public void revealOther(MpcZlVector xi) {
        Preconditions.checkArgument(initialized);
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
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositive("num", xi.getNum());
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
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositive("num", xi.getNum());
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
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositive("num", xi.getNum());
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
        Preconditions.checkArgument(initialized);
        MathPreconditions.checkPositive("num", xi.getNum());
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
}
