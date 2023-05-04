package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Abstract Boolean circuit party.
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public abstract class AbstractBcParty extends AbstractTwoPartyPto implements BcParty {
    /**
     * maximum number of bits in round.
     */
    protected int maxRoundBitNum;
    /**
     * total number of bits for updates.
     */
    protected long updateBitNum;
    /**
     * current number of bits.
     */
    protected int bitNum;
    /**
     * the number of input bits
     */
    protected long inputBitNum;
    /**
     * the number of AND gates.
     */
    protected long andGateNum;
    /**
     * the number of XOR gates.
     */
    protected long xorGateNum;
    /**
     * the number of output bits
     */
    protected long outputBitNum;

    public AbstractBcParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, BcConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        andGateNum = 0;
        xorGateNum = 0;
    }

    protected void setInitInput(int maxRoundBitNum, int updateBitNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxRoundBitNum", maxRoundBitNum, updateBitNum);
        this.maxRoundBitNum = maxRoundBitNum;
        this.updateBitNum = updateBitNum;
        initState();
    }

    protected void setShareOwnInput(BitVector bitVector) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("bitNum", bitVector.bitNum(), maxRoundBitNum);
        bitNum = bitVector.bitNum();
        inputBitNum += bitNum;
    }

    protected void setShareOtherInput(int bitNum) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("bitNum", bitNum, maxRoundBitNum);
        this.bitNum = bitNum;
        inputBitNum += bitNum;
    }

    protected void setAndInput(SquareZ2Vector xi, SquareZ2Vector yi) {
        checkInitialized();
        MathPreconditions.checkEqual("xi.bitNum", "yi.bitNum", xi.getNum(), yi.getNum());
        MathPreconditions.checkPositiveInRangeClosed("bitNum", xi.getNum(), maxRoundBitNum);
        // the number of AND gates is added during the protocol execution.
        bitNum = xi.getNum();
    }

    protected void setXorInput(SquareZ2Vector xi, SquareZ2Vector yi) {
        checkInitialized();
        MathPreconditions.checkEqual("xi.bitNum", "yi.bitNum", xi.getNum(), yi.getNum());
        MathPreconditions.checkPositiveInRangeClosed("bitNum", xi.getNum(), maxRoundBitNum);
        // the number of XOR gates is added during the protocol execution.
        bitNum = xi.getNum();
    }

    protected void setRevealOwnInput(SquareZ2Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("xi.bitNum", xi.getNum(), maxRoundBitNum);
        // the number of output bits is added during the protocol execution.
        bitNum = xi.getNum();
    }

    protected void setRevealOtherInput(SquareZ2Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("xi.bitNum", xi.getNum(), maxRoundBitNum);
        // the number of output bits is added during the protocol execution.
        bitNum = xi.getNum();
    }

    @Override
    public SquareZ2Vector createOnes(int bitNum) {
        return SquareZ2Vector.createOnes(bitNum);
    }

    @Override
    public SquareZ2Vector createZeros(int bitNum) {
        return SquareZ2Vector.createZeros(bitNum);
    }

    @Override
    public SquareZ2Vector create(int bitNum, boolean value) {
        return SquareZ2Vector.create(bitNum, value);
    }

    @Override
    public SquareZ2Vector createEmpty(boolean plain) {
        return SquareZ2Vector.createEmpty(plain);
    }

    @Override
    public SquareZ2Vector[] shareOwn(BitVector[] xArray) {
        if (xArray.length == 0) {
            return new SquareZ2Vector[0];
        }
        // merge
        BitVector mergeX = BitVectorFactory.merge(xArray);
        // share
        SquareZ2Vector mergeShareXi = shareOwn(mergeX);
        // split
        int[] bitNums = Arrays.stream(xArray).mapToInt(BitVector::bitNum).toArray();
        return Arrays.stream(split(mergeShareXi, bitNums))
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
    }

    @Override
    public SquareZ2Vector[] shareOther(int[] bitNums) throws MpcAbortException {
        if (bitNums.length == 0) {
            return new SquareZ2Vector[0];
        }
        // share
        int bitNum = Arrays.stream(bitNums).sum();
        SquareZ2Vector mergeShareXi = shareOther(bitNum);
        // split
        return Arrays.stream(split(mergeShareXi, bitNums))
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
    }

    @Override
    public SquareZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new SquareZ2Vector[0];
        }
        int length = xiArray.length;
        SquareZ2Vector[] square2pcZ2XiArray = Arrays.stream(xiArray)
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] square2pcZ2YiArray = Arrays.stream(yiArray)
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] square2pcZ2ZiArray = new SquareZ2Vector[length];
        // plain v.s. plain
        and(square2pcZ2XiArray, square2pcZ2YiArray, length, square2pcZ2ZiArray, true, true);
        // plain v.s. secret
        and(square2pcZ2XiArray, square2pcZ2YiArray, length, square2pcZ2ZiArray, true, false);
        // secret v.s. plain
        and(square2pcZ2XiArray, square2pcZ2YiArray, length, square2pcZ2ZiArray, false, true);
        // secret v.s. secret
        and(square2pcZ2XiArray, square2pcZ2YiArray, length, square2pcZ2ZiArray, false, false);

        return square2pcZ2ZiArray;
    }

    private void and(SquareZ2Vector[] xiArray, SquareZ2Vector[] yiArray, int length,
                     SquareZ2Vector[] ziArray, boolean is0Plain, boolean is1Plain) throws MpcAbortException {
        int[] selectIndexes = IntStream.range(0, length)
            .filter(index -> (xiArray[index].isPlain() == is0Plain) && (yiArray[index].isPlain() == is1Plain))
            .toArray();
        if (selectIndexes.length == 0) {
            return;
        }
        SquareZ2Vector[] selectXs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> xiArray[selectIndex])
            .toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] selectYs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> yiArray[selectIndex])
            .toArray(SquareZ2Vector[]::new);
        int[] bitNums = Arrays.stream(selectIndexes)
            .map(selectIndex -> {
                int bitNum = xiArray[selectIndex].getNum();
                assert yiArray[selectIndex].getNum() == bitNum;
                return bitNum;
            })
            .toArray();
        SquareZ2Vector mergeSelectXs = (SquareZ2Vector) merge(selectXs);
        SquareZ2Vector mergeSelectYs = (SquareZ2Vector) merge(selectYs);
        SquareZ2Vector mergeSelectZs = and(mergeSelectXs, mergeSelectYs);
        SquareZ2Vector[] selectZs = Arrays.stream(split(mergeSelectZs, bitNums))
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
        assert selectZs.length == selectIndexes.length;
        IntStream.range(0, selectIndexes.length).forEach(index -> ziArray[selectIndexes[index]] = selectZs[index]);
    }

    @Override
    public SquareZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new SquareZ2Vector[0];
        }
        int length = xiArray.length;
        SquareZ2Vector[] square2pcZ2XiArray = Arrays.stream(xiArray)
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] square2pcZ2YiArray = Arrays.stream(yiArray)
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] square2pcZ2ZiArray = new SquareZ2Vector[length];
        // plain v.s. plain
        xor(square2pcZ2XiArray, square2pcZ2YiArray, length, square2pcZ2ZiArray, true, true);
        // plain v.s. secret
        xor(square2pcZ2XiArray, square2pcZ2YiArray, length, square2pcZ2ZiArray, true, false);
        // secret v.s. plain
        xor(square2pcZ2XiArray, square2pcZ2YiArray, length, square2pcZ2ZiArray, false, true);
        // secret v.s. secret
        xor(square2pcZ2XiArray, square2pcZ2YiArray, length, square2pcZ2ZiArray, false, false);

        return square2pcZ2ZiArray;
    }

    private void xor(SquareZ2Vector[] xiArray, SquareZ2Vector[] yiArray, int length,
                     SquareZ2Vector[] ziArray, boolean is0Plain, boolean is1Plain) throws MpcAbortException {
        int[] selectIndexes = IntStream.range(0, length)
            .filter(index -> (xiArray[index].isPlain() == is0Plain) && (yiArray[index].isPlain() == is1Plain))
            .toArray();
        if (selectIndexes.length == 0) {
            return;
        }
        SquareZ2Vector[] selectXs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> xiArray[selectIndex])
            .toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] selectYs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> yiArray[selectIndex])
            .toArray(SquareZ2Vector[]::new);
        int[] bitNums = Arrays.stream(selectIndexes)
            .map(selectIndex -> {
                int bitNum = xiArray[selectIndex].getNum();
                assert yiArray[selectIndex].getNum() == bitNum;
                return bitNum;
            })
            .toArray();
        SquareZ2Vector mergeSelectXs = (SquareZ2Vector) merge(selectXs);
        SquareZ2Vector mergeSelectYs = (SquareZ2Vector) merge(selectYs);
        SquareZ2Vector mergeSelectZs = xor(mergeSelectXs, mergeSelectYs);
        SquareZ2Vector[] selectZs = Arrays.stream(split(mergeSelectZs, bitNums))
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
        assert selectZs.length == selectIndexes.length;
        IntStream.range(0, selectIndexes.length).forEach(index -> ziArray[selectIndexes[index]] = selectZs[index]);
    }

    @Override
    public SquareZ2Vector not(MpcZ2Vector xi) throws MpcAbortException {
        return xor(xi, createOnes(xi.getNum()));
    }

    @Override
    public SquareZ2Vector[] not(MpcZ2Vector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new SquareZ2Vector[0];
        }
        // merge xi
        SquareZ2Vector mergeXiArray = (SquareZ2Vector) merge(xiArray);
        // not operation
        SquareZ2Vector mergeZiArray = not(mergeXiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, bitNums))
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
    }

    @Override
    public BitVector[] revealOwn(SquareZ2Vector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new BitVector[0];
        }
        // merge
        SquareZ2Vector mergeXiArray = (SquareZ2Vector) merge(xiArray);
        // reveal
        BitVector mergeX = revealOwn(mergeXiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(SquareZ2Vector::getNum).toArray();
        return BitVectorFactory.split(mergeX, bitNums);
    }

    @Override
    public void revealOther(SquareZ2Vector[] xiArray) {
        //noinspection StatementWithEmptyBody
        if (xiArray.length == 0) {
            // do nothing for 0 length
        }
        // merge
        SquareZ2Vector mergeXiArray = (SquareZ2Vector) merge(xiArray);
        // reveal
        revealOther(mergeXiArray);
    }
}
