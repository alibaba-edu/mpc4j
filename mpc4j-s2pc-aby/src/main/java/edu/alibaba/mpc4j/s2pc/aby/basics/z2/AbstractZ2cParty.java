package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.DyadicBcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryBcOperator;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * abstract Z2 circuit party.
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public abstract class AbstractZ2cParty extends AbstractTwoPartyPto implements Z2cParty {
    /**
     * config
     */
    private final Z2cConfig config;
    /**
     * current number of bits.
     */
    protected int bitNum;

    public AbstractZ2cParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, Z2cConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
    }

    protected void setInitInput(int expectTotalNum) {
        MathPreconditions.checkPositive("expect_total_num", expectTotalNum);
        initState();
    }

    @Override
    public void init() throws MpcAbortException {
        init(config.defaultRoundNum());
    }

    protected void setShareOwnInput(BitVector xi) {
        checkInitialized();
        MathPreconditions.checkPositive("bitNum", xi.bitNum());
        bitNum = xi.bitNum();
    }

    protected void setShareOtherInput(int bitNum) {
        checkInitialized();
        MathPreconditions.checkPositive("bitNum", bitNum);
        this.bitNum = bitNum;
    }

    protected void setDyadicOperatorInput(SquareZ2Vector xi, SquareZ2Vector yi) {
        checkInitialized();
        MathPreconditions.checkEqual("xi.bitNum", "yi.bitNum", xi.getNum(), yi.getNum());
        MathPreconditions.checkPositive("bitNum", xi.bitNum());
        bitNum = xi.bitNum();
    }

    protected void setDyadicOperatorInput(SquareZ2Vector[] xiArray, SquareZ2Vector[] yiArray) {
        checkInitialized();
        MathPreconditions.checkEqual("xiArray.length", "yiArray.length", xiArray.length, yiArray.length);
        bitNum = xiArray[0].getNum();
        MathPreconditions.checkPositive("bitNum", bitNum);
        boolean xIsPlain = xiArray[0].isPlain();
        boolean yIsPlain = yiArray[0].isPlain();
        for(int i = 0; i < xiArray.length; i++) {
            MathPreconditions.checkEqual("xi.bitNum", "bitNum", xiArray[i].getNum(), bitNum);
            MathPreconditions.checkEqual("yi.bitNum", "bitNum", yiArray[i].getNum(), bitNum);
            Preconditions.checkArgument(xIsPlain == xiArray[i].isPlain());
            Preconditions.checkArgument(yIsPlain == yiArray[i].isPlain());
        }
    }

    protected void setRevealOwnInput(SquareZ2Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositive("xi.bitNum", xi.bitNum());
        bitNum = xi.bitNum();
    }

    protected void setRevealOtherInput(SquareZ2Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositive("xi.bitNum", xi.bitNum());
        bitNum = xi.bitNum();
    }

    @Override
    public MpcZ2Vector create(boolean isPlain, BitVector... bitVector) {
        MathPreconditions.checkEqual("bitVector.length", "1", bitVector.length, 1);
        return SquareZ2Vector.create(bitVector[0], isPlain);
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
    public SquareZ2Vector createEmpty(boolean plain) {
        return SquareZ2Vector.createEmpty(plain);
    }

    @Override
    public BitVector[] open(MpcZ2Vector[] xiArray) throws MpcAbortException {
        BitVector[] res;
        if (rpc.ownParty().getPartyId() == 0) {
            res = revealOwn(xiArray);
            revealOther(xiArray);
        } else {
            revealOther(xiArray);
            res = revealOwn(xiArray);
        }
        return res;
    }

    @Override
    public SquareZ2Vector not(MpcZ2Vector xi) throws MpcAbortException {
        return xor(xi, createOnes(xi.getNum()));
    }

    @Override
    public SquareZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        return operate(DyadicBcOperator.AND, xiArray, yiArray);
    }

    @Override
    public SquareZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        return operate(DyadicBcOperator.XOR, xiArray, yiArray);
    }

    @Override
    public SquareZ2Vector[] not(MpcZ2Vector[] xiArray) throws MpcAbortException {
        return operate(UnaryBcOperator.NOT, xiArray);
    }

    private SquareZ2Vector[] operate(DyadicBcOperator operator, MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray)
        throws MpcAbortException {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new SquareZ2Vector[0];
        }
        int length = xiArray.length;
        SquareZ2Vector[] xiSquareZ2Array = Arrays.stream(xiArray)
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] yiSquareZ2Array = Arrays.stream(yiArray)
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] ziSquareZ2Array = new SquareZ2Vector[length];
        // plain v.s. plain
        operate(operator, xiSquareZ2Array, yiSquareZ2Array, length, ziSquareZ2Array, true, true);
        // plain v.s. secret
        operate(operator, xiSquareZ2Array, yiSquareZ2Array, length, ziSquareZ2Array, true, false);
        // secret v.s. plain
        operate(operator, xiSquareZ2Array, yiSquareZ2Array, length, ziSquareZ2Array, false, true);
        // secret v.s. secret
        operate(operator, xiSquareZ2Array, yiSquareZ2Array, length, ziSquareZ2Array, false, false);

        return ziSquareZ2Array;
    }

    private void operate(DyadicBcOperator operator, SquareZ2Vector[] xiArray, SquareZ2Vector[] yiArray, int length,
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
        SquareZ2Vector mergeSelectZs;
        switch (operator) {
            case AND -> mergeSelectZs = and(mergeSelectXs, mergeSelectYs);
            case XOR -> mergeSelectZs = xor(mergeSelectXs, mergeSelectYs);
            default -> throw new IllegalStateException();
        }
        SquareZ2Vector[] selectZs = Arrays.stream(split(mergeSelectZs, bitNums))
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
        assert selectZs.length == selectIndexes.length;
        IntStream.range(0, selectIndexes.length).forEach(index -> ziArray[selectIndexes[index]] = selectZs[index]);
    }

    @SuppressWarnings("SameParameterValue")
    private SquareZ2Vector[] operate(UnaryBcOperator operator, MpcZ2Vector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new SquareZ2Vector[0];
        }
        SquareZ2Vector mergeXiArray = (SquareZ2Vector) merge(xiArray);
        SquareZ2Vector mergeZiArray;
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NOT -> mergeZiArray = not(mergeXiArray);
            default -> throw new IllegalStateException();
        }
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(MpcZ2Vector::getNum).toArray();
        return Arrays.stream(split(mergeZiArray, bitNums))
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
    }

    @Override
    public MpcZ2Vector xorSelfAllElement(MpcZ2Vector x) {
        return create(x.isPlain(), x.getBitVector().numOf1IsOdd()
            ? BitVectorFactory.createOnes(1)
            : BitVectorFactory.createZeros(1));
    }

    /**
     * y_i = \sum_0^{i} x_i
     *
     * @param x the input data
     * @return y
     */
    @Override
    public MpcZ2Vector xorAllBeforeElement(MpcZ2Vector x) {
        return create(x.isPlain(), x.getBitVector().xorBeforeBit());
    }

    /**
     * generate randomness based on OT result
     *
     * @param cotReceiverOutput the OT receiver output
     * @param dim the required bit length for each data
     * @return transposed bit vectors
     */
    protected BitVector[] handleOtReceiverOutput(CotReceiverOutput cotReceiverOutput, int dim) {
        if (dim <= CommonConstants.BLOCK_BIT_LENGTH) {
            RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfFactory.CrhfType.MMO, cotReceiverOutput);
            byte[][] otResBytes = rotReceiverOutput.getRbArray();
            ZlDatabase zlDatabase = ZlDatabase.create(CommonConstants.BLOCK_BIT_LENGTH, otResBytes);
            BitVector[] transBitVec = zlDatabase.bitPartition(envType, parallel);
            return Arrays.copyOf(transBitVec, dim);
        } else {
            // we need to use PRG
            int targetByteLength = CommonUtils.getByteLength(dim);
            Prg prg = PrgFactory.createInstance(envType, targetByteLength);
            Stream<byte[]> otResStream = parallel ? Arrays.stream(cotReceiverOutput.getRbArray()).parallel() : Arrays.stream(cotReceiverOutput.getRbArray());
            byte[][] prgRes = otResStream.map(prg::extendToBytes).toArray(byte[][]::new);
            ZlDatabase zlDatabase = ZlDatabase.create(targetByteLength * 8, prgRes);
            BitVector[] transBitVec = zlDatabase.bitPartition(envType, parallel);
            return Arrays.copyOf(transBitVec, dim);
        }
    }

    /**
     * generate randomness based on OT result
     *
     * @param cotSenderOutput the OT sender output
     * @param dim the required bit length for each data
     * @return transposed bit vectors
     */
    protected BitVector[][] handleOtSenderOutput(CotSenderOutput cotSenderOutput, int dim) {
        if (dim <= CommonConstants.BLOCK_BIT_LENGTH) {
            RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfFactory.CrhfType.MMO, cotSenderOutput);
            byte[][] otResBytes0 = rotSenderOutput.getR0Array();
            ZlDatabase zlDatabase0 = ZlDatabase.create(CommonConstants.BLOCK_BIT_LENGTH, otResBytes0);
            BitVector[] transBitVec0 = zlDatabase0.bitPartition(envType, parallel);

            byte[][] otResBytes1 = rotSenderOutput.getR1Array();
            ZlDatabase zlDatabase1 = ZlDatabase.create(CommonConstants.BLOCK_BIT_LENGTH, otResBytes1);
            BitVector[] transBitVec1 = zlDatabase1.bitPartition(envType, parallel);
            return new BitVector[][]{Arrays.copyOf(transBitVec0, dim), Arrays.copyOf(transBitVec1, dim)};
        } else {
            // we need to use PRG
            int targetByteLength = CommonUtils.getByteLength(dim);
            Prg prg = PrgFactory.createInstance(envType, targetByteLength);
            Stream<byte[]> otResStream0 = parallel ? Arrays.stream(cotSenderOutput.getR0Array()).parallel() : Arrays.stream(cotSenderOutput.getR0Array());
            byte[][] prgRes0 = otResStream0.map(prg::extendToBytes).toArray(byte[][]::new);
            ZlDatabase zlDatabase0 = ZlDatabase.create(targetByteLength * 8, prgRes0);
            BitVector[] transBitVec0 = zlDatabase0.bitPartition(envType, parallel);

            Stream<byte[]> otResStream1 = parallel ? Arrays.stream(cotSenderOutput.getR1Array()).parallel() : Arrays.stream(cotSenderOutput.getR1Array());
            byte[][] prgRes1 = otResStream1.map(prg::extendToBytes).toArray(byte[][]::new);
            ZlDatabase zlDatabase1 = ZlDatabase.create(targetByteLength * 8, prgRes1);
            BitVector[] transBitVec1 = zlDatabase1.bitPartition(envType, parallel);
            return new BitVector[][]{Arrays.copyOf(transBitVec0, dim), Arrays.copyOf(transBitVec1, dim)};
        }
    }
}
