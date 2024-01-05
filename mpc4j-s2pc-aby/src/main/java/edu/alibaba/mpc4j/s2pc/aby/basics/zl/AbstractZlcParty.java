package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract Zl circuit party.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public abstract class AbstractZlcParty extends AbstractTwoPartyPto implements ZlcParty {
    /**
     * Zl instance
     */
    protected final Zl zl;
    /**
     * l
     */
    protected final int l;
    /**
     * l in bytes
     */
    protected final int byteL;
    /**
     * total num for updates.
     */
    protected long updateNum;
    /**
     * current num.
     */
    protected int num;

    public AbstractZlcParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlcConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.zl = config.getZl();
        l = zl.getL();
        byteL = zl.getByteL();
    }

    protected void setInitInput(int updateNum) {
        MathPreconditions.checkPositive("updateNum", updateNum);
        this.updateNum = updateNum;
        initState();
    }

    protected void setShareOwnInput(ZlVector xi) {
        checkInitialized();
        MathPreconditions.checkPositive("num", xi.getNum());
        num = xi.getNum();
    }

    protected void setShareOtherInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
    }

    protected void setDyadicOperatorInput(SquareZlVector xi, SquareZlVector yi) {
        checkInitialized();
        MathPreconditions.checkEqual("xi.num", "yi.num", xi.getNum(), yi.getNum());
        MathPreconditions.checkPositive("num", xi.getNum());
        num = xi.getNum();
    }

    protected void setRevealOwnInput(SquareZlVector xi) {
        checkInitialized();
        MathPreconditions.checkPositive("xi.num", xi.getNum());
        num = xi.getNum();
    }

    protected void setRevealOtherInput(SquareZlVector xi) {
        checkInitialized();
        MathPreconditions.checkPositive("xi.num", xi.getNum());
        num = xi.getNum();
    }

    @Override
    public SquareZlVector create(ZlVector zlVector) {
        return SquareZlVector.create(zlVector, true);
    }

    @Override
    public SquareZlVector createOnes(int num) {
        return SquareZlVector.createOnes(zl, num);
    }

    @Override
    public SquareZlVector createZeros(int num) {
        return SquareZlVector.createZeros(zl, num);
    }

    @Override
    public SquareZlVector createEmpty(boolean plain) {
        return SquareZlVector.createEmpty(zl, plain);
    }

    @Override
    public SquareZlVector[] add(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException {
        return operate(DyadicAcOperator.ADD, xiArray, yiArray);
    }

    @Override
    public SquareZlVector[] sub(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException {
        return operate(DyadicAcOperator.SUB, xiArray, yiArray);
    }

    @Override
    public SquareZlVector[] mul(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException {
        return operate(DyadicAcOperator.MUL, xiArray, yiArray);
    }

    @Override
    public SquareZlVector neg(MpcZlVector xi) throws MpcAbortException {
        return sub(createZeros(num), xi);
    }

    @Override
    public SquareZlVector[] neg(MpcZlVector[] xiArray) throws MpcAbortException {
        return operate(UnaryAcOperator.NEG, xiArray);
    }

    private SquareZlVector[] operate(DyadicAcOperator operator, MpcZlVector[] xiArray, MpcZlVector[] yiArray)
        throws MpcAbortException {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new SquareZlVector[0];
        }
        int length = xiArray.length;
        SquareZlVector[] xiSquareZlArray = Arrays.stream(xiArray)
            .map(vector -> (SquareZlVector) vector)
            .toArray(SquareZlVector[]::new);
        SquareZlVector[] yiSquareZlArray = Arrays.stream(yiArray)
            .map(vector -> (SquareZlVector) vector)
            .toArray(SquareZlVector[]::new);
        SquareZlVector[] ziSquareZlArray = new SquareZlVector[length];
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

    private void operate(DyadicAcOperator operator, SquareZlVector[] xiArray, SquareZlVector[] yiArray, int length,
                         SquareZlVector[] ziArray, boolean is0Plain, boolean is1Plain) throws MpcAbortException {
        int[] selectIndexes = IntStream.range(0, length)
            .filter(index -> (xiArray[index].isPlain() == is0Plain) && (yiArray[index].isPlain() == is1Plain))
            .toArray();
        if (selectIndexes.length == 0) {
            return;
        }
        SquareZlVector[] selectXs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> xiArray[selectIndex])
            .toArray(SquareZlVector[]::new);
        SquareZlVector[] selectYs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> yiArray[selectIndex])
            .toArray(SquareZlVector[]::new);
        int[] nums = Arrays.stream(selectIndexes)
            .map(selectIndex -> {
                int num = xiArray[selectIndex].getNum();
                assert yiArray[selectIndex].getNum() == num;
                return num;
            })
            .toArray();
        SquareZlVector mergeSelectXs = (SquareZlVector) merge(selectXs);
        SquareZlVector mergeSelectYs = (SquareZlVector) merge(selectYs);
        SquareZlVector mergeSelectZs;
        switch (operator) {
            case ADD:
                mergeSelectZs = add(mergeSelectXs, mergeSelectYs);
                break;
            case SUB:
                mergeSelectZs = sub(mergeSelectXs, mergeSelectYs);
                break;
            case MUL:
                mergeSelectZs = mul(mergeSelectXs, mergeSelectYs);
                break;
            default:
                throw new IllegalStateException();
        }
        SquareZlVector[] selectZs = Arrays.stream(split(mergeSelectZs, nums))
            .map(vector -> (SquareZlVector) vector)
            .toArray(SquareZlVector[]::new);
        assert selectZs.length == selectIndexes.length;
        IntStream.range(0, selectIndexes.length).forEach(index -> ziArray[selectIndexes[index]] = selectZs[index]);
    }

    @SuppressWarnings("SameParameterValue")
    private SquareZlVector[] operate(UnaryAcOperator operator, MpcZlVector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new SquareZlVector[0];
        }
        SquareZlVector mergeXiArray = (SquareZlVector) merge(xiArray);
        SquareZlVector mergeZiArray;
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG:
                mergeZiArray = neg(mergeXiArray);
                break;
            default:
                throw new IllegalStateException();
        }
        // split
        int[] nums = Arrays.stream(xiArray).mapToInt(MpcZlVector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, nums))
            .map(vector -> (SquareZlVector) vector)
            .toArray(SquareZlVector[]::new);
    }


}
