package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract Zl circuit party.
 *
 * @author Li Peng
 * @date 2024/7/23
 */
public abstract class AbstractZl64cParty extends AbstractTwoPartyPto implements Zl64cParty {
    /**
     * config
     */
    protected final Zl64cConfig config;
    /**
     * max l
     */
    protected int maxL;
    /**
     * current num.
     */
    protected int num;

    public AbstractZl64cParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Zl64cConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxL, int expectTotalNum) {
        MathPreconditions.checkPositive("maxL", maxL);
        MathPreconditions.checkPositive("expect_total_num", expectTotalNum);
        this.maxL = maxL;
        initState();
    }

    @Override
    public void init(int maxL) throws MpcAbortException {
        init(maxL, config.defaultRoundNum(maxL));
    }

    protected void setShareOwnInput(Zl64Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl64().getL(), maxL);
        MathPreconditions.checkPositive("num", xi.getNum());
        num = xi.getNum();
    }

    protected void setShareOtherInput(Zl64 zl64, int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", zl64.getL(), maxL);
        MathPreconditions.checkPositive("num", num);
        this.num = num;
    }

    protected void setDyadicOperatorInput(SquareZl64Vector xi, SquareZl64Vector yi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl64().getL(), maxL);
        Preconditions.checkArgument(xi.getZl64().equals(yi.getZl64()));
        MathPreconditions.checkEqual("xi.num", "yi.num", xi.getNum(), yi.getNum());
        MathPreconditions.checkPositive("num", xi.getNum());
        num = xi.getNum();
    }

    protected void setRevealOwnInput(SquareZl64Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl64().getL(), maxL);
        MathPreconditions.checkPositive("xi.num", xi.getNum());
        num = xi.getNum();
    }

    protected void setRevealOtherInput(SquareZl64Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl64().getL(), maxL);
        MathPreconditions.checkPositive("xi.num", xi.getNum());
        num = xi.getNum();
    }

    @Override
    public SquareZl64Vector create(Zl64Vector zl64Vector) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl64Vector.getZl64().getL(), maxL);
        return SquareZl64Vector.create(zl64Vector, true);
    }

    @Override
    public SquareZl64Vector createOnes(Zl64 zl64, int num) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl64.getL(), maxL);
        return SquareZl64Vector.createOnes(zl64, num);
    }

    @Override
    public SquareZl64Vector createZeros(Zl64 zl64, int num) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl64.getL(), maxL);
        return SquareZl64Vector.createZeros(zl64, num);
    }

    @Override
    public SquareZl64Vector createEmpty(Zl64 zl64, boolean plain) {
        MathPreconditions.checkPositiveInRangeClosed("l", zl64.getL(), maxL);
        return SquareZl64Vector.createEmpty(zl64, plain);
    }

    @Override
    public SquareZl64Vector[] add(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) throws MpcAbortException {
        return operate(DyadicAcOperator.ADD, xiArray, yiArray);
    }

    @Override
    public SquareZl64Vector[] sub(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) throws MpcAbortException {
        return operate(DyadicAcOperator.SUB, xiArray, yiArray);
    }

    @Override
    public SquareZl64Vector[] mul(MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray) throws MpcAbortException {
        return operate(DyadicAcOperator.MUL, xiArray, yiArray);
    }

    @Override
    public SquareZl64Vector neg(MpcZl64Vector xi) throws MpcAbortException {
        return sub(createZeros(xi.getZl64(), num), xi);
    }

    @Override
    public SquareZl64Vector[] neg(MpcZl64Vector[] xiArray) throws MpcAbortException {
        return operate(UnaryAcOperator.NEG, xiArray);
    }

    private SquareZl64Vector[] operate(DyadicAcOperator operator, MpcZl64Vector[] xiArray, MpcZl64Vector[] yiArray)
        throws MpcAbortException {
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new SquareZl64Vector[0];
        }
        int length = xiArray.length;
        SquareZl64Vector[] xiSquareZlArray = Arrays.stream(xiArray)
            .map(vector -> (SquareZl64Vector) vector)
            .toArray(SquareZl64Vector[]::new);
        SquareZl64Vector[] yiSquareZlArray = Arrays.stream(yiArray)
            .map(vector -> (SquareZl64Vector) vector)
            .toArray(SquareZl64Vector[]::new);
        SquareZl64Vector[] ziSquareZlArray = new SquareZl64Vector[length];
        // plain v.s. plain
        operate(operator, xiSquareZlArray, yiSquareZlArray, length, ziSquareZlArray, true, true);
        // plain v.s. secret
        operate(operator, xiSquareZlArray, yiSquareZlArray, length, ziSquareZlArray, true, false);
        // secret v.s. plain
        operate(operator, xiSquareZlArray, yiSquareZlArray, length, ziSquareZlArray, false, true);
        // secret v.s. secret
        operate(operator, xiSquareZlArray, yiSquareZlArray, length, ziSquareZlArray, false, false);

        return ziSquareZlArray;
    }

    private void operate(DyadicAcOperator operator, SquareZl64Vector[] xiArray, SquareZl64Vector[] yiArray, int length,
                         SquareZl64Vector[] ziArray, boolean is0Plain, boolean is1Plain) throws MpcAbortException {
        int[] selectIndexes = IntStream.range(0, length)
            .filter(index -> (xiArray[index].isPlain() == is0Plain) && (yiArray[index].isPlain() == is1Plain))
            .toArray();
        if (selectIndexes.length == 0) {
            return;
        }
        SquareZl64Vector[] selectXs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> xiArray[selectIndex])
            .toArray(SquareZl64Vector[]::new);
        SquareZl64Vector[] selectYs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> yiArray[selectIndex])
            .toArray(SquareZl64Vector[]::new);
        int[] nums = Arrays.stream(selectIndexes)
            .map(selectIndex -> {
                int num = xiArray[selectIndex].getNum();
                assert yiArray[selectIndex].getNum() == num;
                return num;
            })
            .toArray();
        SquareZl64Vector mergeSelectXs = (SquareZl64Vector) merge(selectXs);
        SquareZl64Vector mergeSelectYs = (SquareZl64Vector) merge(selectYs);
        SquareZl64Vector mergeSelectZs;
        switch (operator) {
            case ADD -> mergeSelectZs = add(mergeSelectXs, mergeSelectYs);
            case SUB -> mergeSelectZs = sub(mergeSelectXs, mergeSelectYs);
            case MUL -> mergeSelectZs = mul(mergeSelectXs, mergeSelectYs);
            default -> throw new IllegalStateException();
        }
        SquareZl64Vector[] selectZs = Arrays.stream(split(mergeSelectZs, nums))
            .map(vector -> (SquareZl64Vector) vector)
            .toArray(SquareZl64Vector[]::new);
        assert selectZs.length == selectIndexes.length;
        IntStream.range(0, selectIndexes.length).forEach(index -> ziArray[selectIndexes[index]] = selectZs[index]);
    }

    @SuppressWarnings("SameParameterValue")
    private SquareZl64Vector[] operate(UnaryAcOperator operator, MpcZl64Vector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new SquareZl64Vector[0];
        }
        SquareZl64Vector mergeXiArray = (SquareZl64Vector) merge(xiArray);
        SquareZl64Vector mergeZiArray;
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG -> mergeZiArray = neg(mergeXiArray);
            default -> throw new IllegalStateException();
        }
        // split
        int[] nums = Arrays.stream(xiArray).mapToInt(MpcZl64Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, nums))
            .map(vector -> (SquareZl64Vector) vector)
            .toArray(SquareZl64Vector[]::new);
    }
}
