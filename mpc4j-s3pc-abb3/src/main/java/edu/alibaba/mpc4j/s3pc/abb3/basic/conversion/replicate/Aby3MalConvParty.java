package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.adder.RippleCarryAdder;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * The malicious version of Replicated-sharing type conversion party
 *
 * @author Feng Han
 * @date 2024/01/17
 */
public class Aby3MalConvParty extends AbstractAby3ConvParty implements Aby3ConvParty {
    /**
     * RippleCarryAdder instance
     */
    public final RippleCarryAdder rippleCarryAdder;

    protected Aby3MalConvParty(TripletZ2cParty z2cParty, TripletLongParty zl64cParty, Aby3ConvConfig config) {
        super(z2cParty, zl64cParty, config);
        rippleCarryAdder = new RippleCarryAdder(circuit);
    }

    public int getParam4Adder(int bitLen){
        switch (adderType){
            case RIPPLE_CARRY:
                return 1;
            case BRENT_KUNG:
                return 4;
            case SKLANSKY:
            case KOGGE_STONE:
                return 2 * LongUtils.ceilLog2(bitLen);
            default:
                throw new IllegalArgumentException("illegal adder type: "+ adderType.name());
        }
    }

    @Override
    public long[] getTupleNum(ConvOp op, int inputDataNum, int dataDim, int bitLen){
        inputDataNum = CommonUtils.getByteLength(inputDataNum) << 3;
        switch (op){
            case A2B:{
                // pure bit tuples
                return new long[]{bitLen == 1 ? 0 : ((long) inputDataNum * dataDim) * bitLen * (2 + getParam4Adder(bitLen)), 0};
            }
            case B2A:{
                // pure bit tuples
                return new long[]{((long) inputDataNum * dataDim) * 64 * (2 + getParam4Adder(64)), 0};
            }
            case BIT2A:{
                // pure arithmetic tuples
                return new long[]{0, zl64cParty instanceof Cgh18RpLongParty ? 0L : 2L * inputDataNum * dataDim};
            }
            case A_MUL_B:{
                // pure arithmetic tuples
                return new long[]{0, zl64cParty instanceof Cgh18RpLongParty ? 0L : 3L * inputDataNum * dataDim};
            }
            case BIT_EXTRACTION:{
                // pure bit tuples
                return new long[]{((long) inputDataNum * dataDim) * (64 - bitLen) * 4, 0};
            }
            default:
                throw new IllegalArgumentException("illegal ConvOp type: "+ op.name());
        }
    }

    @Override
    protected TripletRpZ2Vector[][] transIntoSumOfTwoBinary(MpcLongVector x, int bitLen) throws MpcAbortException {
        TripletRpZ2Vector[][] tmpWire = aWire2bWire((TripletRpLongVector) x, bitLen);
        MpcZ2Vector[][] oneBitAddResult = rippleCarryAdder.addOneBit(tmpWire[0], tmpWire[1], tmpWire[2]);
        // re-format the result of rippleCarryAdder
        TripletRpZ2Vector[] cResult = new TripletRpZ2Vector[bitLen];
        IntStream.range(0, bitLen - 1).forEach(i -> cResult[i] = (TripletRpZ2Vector) oneBitAddResult[1][i + 1]);
        cResult[bitLen - 1] = TripletRpZ2Vector.createEmpty(x.getNum());
        return new TripletRpZ2Vector[][]{Arrays.stream(oneBitAddResult[0]).map(each -> (TripletRpZ2Vector)each).toArray(TripletRpZ2Vector[]::new), cResult};
    }

    /**
     * convert arithmetic sharing into three Binary sharing
     *
     * @param data    value to be converted
     * @param keepBit the number of bit should be converted
     * @return (3, bitLength, num) binary sharing array, corresponding to the binary sharing of x_1, x_2, x_3
     */
    private TripletRpZ2Vector[][] aWire2bWire(TripletRpLongVector data, int keepBit) {
        int num = data.getNum();

        BitVector[] d0 = MatrixUtils.transAvIntoBv(data.getVectors()[0], envType, parallel, keepBit);
        BitVector[] d1 = MatrixUtils.transAvIntoBv(data.getVectors()[1], envType, parallel, keepBit);
        TripletRpZ2Vector[][] res = new TripletRpZ2Vector[3][keepBit];
        for (int i = 0; i < keepBit; i++) {
            res[ownParty().getPartyId()][i] = TripletRpZ2Vector.create(d0[i], BitVectorFactory.createZeros(num));
            res[rightParty().getPartyId()][i] = TripletRpZ2Vector.create(BitVectorFactory.createZeros(num), d1[i]);
            res[leftParty().getPartyId()][i] = TripletRpZ2Vector.create(BitVectorFactory.createZeros(num), BitVectorFactory.createZeros(num));
        }
        return res;
    }

    @Override
    public TripletRpLongVector b2a(MpcZ2Vector[] data) throws MpcAbortException {
        int dataNum = data[0].bitNum();
        MpcZ2Vector[] input = new TripletRpZ2Vector[64];
        if (data.length < 64) {
            IntStream.range(0, 64 - data.length).forEach(i -> input[i] = TripletRpZ2Vector.createEmpty(dataNum));
            System.arraycopy(data, 0, input, 64 - data.length, data.length);
        } else {
            MathPreconditions.checkEqual("data.length", "64", data.length, 64);
            System.arraycopy(data, 0, input, 0, data.length);
        }

        LongVector x2Plain = null, x3Plain = null;
        TripletRpZ2Vector[] x2BinaryShare, x3BinaryShare;
        if (selfId == 0) {
            x2Plain = LongVector.create(crProvider.getRandLongArray(dataNum, rightParty()));
            BitVector[] x2Binary = MatrixUtils.transAvIntoBv(x2Plain, envType, parallel, 64);
            x2BinaryShare = Arrays.stream(x2Binary).map(x -> TripletRpZ2Vector.create(BitVectorFactory.createZeros(x.bitNum()), x))
                .toArray(TripletRpZ2Vector[]::new);
            x3BinaryShare = Arrays.stream(x2Binary).map(x ->
                    TripletRpZ2Vector.create(BitVectorFactory.createZeros(x.bitNum()), BitVectorFactory.createZeros(x.bitNum())))
                .toArray(TripletRpZ2Vector[]::new);
        } else if (selfId == 1) {
            x2Plain = LongVector.create(crProvider.getRandLongArray(dataNum, leftParty()));
            x3Plain = LongVector.create(crProvider.getRandLongArray(dataNum, rightParty()));
            BitVector[] x2Binary = MatrixUtils.transAvIntoBv(x2Plain, envType, parallel, 64);
            BitVector[] x3Binary = MatrixUtils.transAvIntoBv(x3Plain, envType, parallel, 64);
            x2BinaryShare = Arrays.stream(x2Binary).map(x -> TripletRpZ2Vector.create(x, BitVectorFactory.createZeros(x.bitNum())))
                .toArray(TripletRpZ2Vector[]::new);
            x3BinaryShare = Arrays.stream(x3Binary).map(x -> TripletRpZ2Vector.create(BitVectorFactory.createZeros(x.bitNum()), x))
                .toArray(TripletRpZ2Vector[]::new);
        } else {
            x3Plain = LongVector.create(crProvider.getRandLongArray(dataNum, leftParty()));
            BitVector[] x3Binary = MatrixUtils.transAvIntoBv(x3Plain, envType, parallel, 64);
            x2BinaryShare = Arrays.stream(x3Binary).map(x ->
                    TripletRpZ2Vector.create(BitVectorFactory.createZeros(x.bitNum()), BitVectorFactory.createZeros(x.bitNum())))
                .toArray(TripletRpZ2Vector[]::new);
            x3BinaryShare = Arrays.stream(x3Binary).map(x -> TripletRpZ2Vector.create(x, BitVectorFactory.createZeros(x.bitNum())))
                .toArray(TripletRpZ2Vector[]::new);
        }

        // 计算出binary share的值 [x_3] = [x] + [-x_2] + [-x_3]
        MpcZ2Vector[][] oneBitAddResult = rippleCarryAdder.addOneBit(input, x2BinaryShare, x3BinaryShare);
        // re-format the result of rippleCarryAdder
        TripletRpZ2Vector[] cResult = new TripletRpZ2Vector[64];
        IntStream.range(0, 63).forEach(i -> cResult[i] = (TripletRpZ2Vector) oneBitAddResult[1][i + 1]);
        cResult[63] = TripletRpZ2Vector.createEmpty(dataNum);
        MpcZ2Vector[] tmpAddRes = adder.add(oneBitAddResult[0], cResult, false);
        TripletRpZ2Vector[] x1Binary = IntStream.range(1, tmpAddRes.length).mapToObj(i -> (TripletRpZ2Vector) tmpAddRes[i]).toArray(TripletRpZ2Vector[]::new);

        // reconstruct [x_1] to P0 and P2
        if (selfId == 0) {
            LongVector x1Share = MatrixUtils.transBvIntoAv(z2cParty.revealOwn(x1Binary), envType, parallel);
            z2cParty.revealOther(x1Binary, leftParty());
            return TripletRpLongVector.create(x1Share, x2Plain.neg());
        } else if (selfId == 1) {
            z2cParty.revealOther(x1Binary, rightParty());
            z2cParty.revealOther(x1Binary, leftParty());
            return TripletRpLongVector.create(x2Plain.neg(), x3Plain.neg());
        } else {
            z2cParty.revealOther(x1Binary, rightParty());
            LongVector x1Share = MatrixUtils.transBvIntoAv(z2cParty.revealOwn(x1Binary), envType, parallel);
            return TripletRpLongVector.create(x3Plain.neg(), x1Share);
        }
    }

    @Override
    public TripletRpLongVector bit2a(MpcZ2Vector data) {
        int num = data.bitNum();
        // 对于binary share的值 x = x_1 ^ x_2 ^ x_3
        // 先直接得到arithmetic share的值 [x_1], [x_2], [x_3]
        TripletRpLongVector[] tmpA3Wire = IntStream.range(0, 3).mapToObj(i ->
            TripletRpLongVector.createZeros(num)).toArray(TripletRpLongVector[]::new);
        boolean[][] binary = Arrays.stream(data.getBitVectors()).map(x ->
            BinaryUtils.byteArrayToBinary(x.getBytes(), num)).toArray(boolean[][]::new);

        long[][] twoLong = new long[2][num];
        for (int dim = 0; dim < 2; dim++) {
            for (int i = 0; i < num; i++) {
                twoLong[dim][i] = binary[dim][i] ? 1 : 0;
            }
        }
        tmpA3Wire[ownParty().getPartyId()].getVectors()[0] = LongVector.create(twoLong[0]);
        tmpA3Wire[rightParty().getPartyId()].getVectors()[1] = LongVector.create(twoLong[1]);

        // 然后通过 两次 x ^ y = x + y - 2xy 得到转化结果
        long[] v2Array = new long[num];
        Arrays.fill(v2Array, 2L);
        PlainLongVector v2 = PlainLongVector.create(v2Array);
        MpcLongVector xy = zl64cParty.mul(tmpA3Wire[0], tmpA3Wire[1]);
        zl64cParty.muli(xy, v2);
        zl64cParty.addi(tmpA3Wire[0], tmpA3Wire[1]);
        zl64cParty.subi(tmpA3Wire[0], xy);

        MpcLongVector xXorYz = zl64cParty.mul(tmpA3Wire[0], tmpA3Wire[2]);
        zl64cParty.muli(xXorYz, v2);
        zl64cParty.addi(tmpA3Wire[0], tmpA3Wire[2]);
        zl64cParty.subi(tmpA3Wire[0], xXorYz);

        return tmpA3Wire[0];
    }

    @Override
    public TripletRpLongVector aMulB(MpcLongVector a, MpcZ2Vector b) {
        return (TripletRpLongVector) zl64cParty.mul(a, bit2a(b));
    }
}
