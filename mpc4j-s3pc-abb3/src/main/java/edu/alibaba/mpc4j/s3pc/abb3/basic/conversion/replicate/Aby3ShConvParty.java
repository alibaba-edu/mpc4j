package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.MatrixUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * The semi-honest version of Replicated-sharing type conversion party
 *
 * @author Feng Han
 * @date 2024/01/17
 */
public class Aby3ShConvParty extends AbstractAby3ConvParty implements Aby3ConvParty {
    protected Aby3ShConvParty(TripletZ2cParty z2cParty, TripletLongParty zl64cParty, Aby3ConvConfig config) {
        super(z2cParty, zl64cParty, config);
    }

    public long[] getTupleNum(ConvOp op, int inputDataNum, int dataDim, int bitLen) {
        return new long[]{0, 0};
    }

    @Override
    protected TripletRpZ2Vector[][] transIntoSumOfTwoBinary(MpcLongVector data, int bitNum) throws MpcAbortException {
        // in the semi-honest setting, we compute [x1 + x2]_b + [x3]_b
        TripletRpZ2Vector[] x12, x3;
        if (selfId == 0) {
            LongVector sumX12 = data.getVectors()[0].add(data.getVectors()[1]);
            BitVector[] x21Plain = MatrixUtils.transAvIntoBv(sumX12, envType, parallel, bitNum);
            x12 = (TripletRpZ2Vector[]) z2cParty.shareOwn(x21Plain);
            x3 = Arrays.stream(x21Plain).map(x -> TripletRpZ2Vector.create(
                BitVectorFactory.createZeros(x.bitNum()), BitVectorFactory.createZeros(x.bitNum()))).toArray(TripletRpZ2Vector[]::new);
        } else if (selfId == 1) {
            BitVector[] x3Plain = MatrixUtils.transAvIntoBv(data.getVectors()[1], envType, parallel, bitNum);
            x3 = Arrays.stream(x3Plain).map(x -> TripletRpZ2Vector.create(
                BitVectorFactory.createZeros(x.bitNum()), x)).toArray(TripletRpZ2Vector[]::new);
            x12 = (TripletRpZ2Vector[]) z2cParty.shareOther(IntStream.range(0, bitNum).map(i -> data.getNum()).toArray(), leftParty());
        } else {
            BitVector[] x3Plain = MatrixUtils.transAvIntoBv(data.getVectors()[0], envType, parallel, bitNum);
            x3 = Arrays.stream(x3Plain).map(x -> TripletRpZ2Vector.create(
                x, BitVectorFactory.createZeros(x.bitNum()))).toArray(TripletRpZ2Vector[]::new);
            x12 = (TripletRpZ2Vector[]) z2cParty.shareOther(IntStream.range(0, bitNum).map(i -> data.getNum()).toArray(), rightParty());
        }
        return new TripletRpZ2Vector[][]{x12, x3};
    }

    @Override
    public TripletRpLongVector b2a(MpcZ2Vector[] data) throws MpcAbortException {
        int dataNum = data[0].bitNum();
        MpcZ2Vector[] input = new TripletRpZ2Vector[64];
        int[] dataNums = IntStream.range(0, 64).map(i -> dataNum).toArray();
        if (data.length < 64) {
            IntStream.range(0, 64 - data.length).forEach(i -> input[i] = TripletRpZ2Vector.createEmpty(dataNum));
            System.arraycopy(data, 0, input, 64 - data.length, data.length);
        } else {
            MathPreconditions.checkEqual("data.length", "64", data.length, 64);
            System.arraycopy(data, 0, input, 0, data.length);
        }

        LongVector x2Plain, x3Plain;
        TripletRpZ2Vector[] x23;
        if (selfId == 0) {
            x2Plain = LongVector.create(crProvider.getRandLongArray(dataNum, rightParty()));
            x23 = (TripletRpZ2Vector[]) z2cParty.shareOther(dataNums, rightParty());
            MpcZ2Vector[] res = adder.add(input, x23, false);
            TripletRpZ2Vector[] x1Binary = IntStream.range(1, res.length).mapToObj(i -> (TripletRpZ2Vector) res[i]).toArray(TripletRpZ2Vector[]::new);
            LongVector x1Plain = MatrixUtils.transBvIntoAv(z2cParty.revealOwn(x1Binary), envType, parallel);
            z2cParty.revealOther(x1Binary, leftParty());
            return TripletRpLongVector.create(x1Plain, x2Plain);
        } else if (selfId == 1) {
            x2Plain = LongVector.create(crProvider.getRandLongArray(dataNum, leftParty()));
            x3Plain = LongVector.create(crProvider.getRandLongArray(dataNum, rightParty()));
            BitVector[] x23Plain = MatrixUtils.transAvIntoBv(x2Plain.add(x3Plain).neg(), envType, parallel, 64);
            x23 = (TripletRpZ2Vector[]) z2cParty.shareOwn(x23Plain);
            MpcZ2Vector[] res = adder.add(input, x23, false);
            TripletRpZ2Vector[] x1Binary = IntStream.range(1, res.length).mapToObj(i -> (TripletRpZ2Vector) res[i]).toArray(TripletRpZ2Vector[]::new);
            z2cParty.revealOther(x1Binary, leftParty());
            z2cParty.revealOther(x1Binary, rightParty());
            return TripletRpLongVector.create(x2Plain, x3Plain);
        } else {
            x3Plain = LongVector.create(crProvider.getRandLongArray(dataNum, leftParty()));
            x23 = (TripletRpZ2Vector[]) z2cParty.shareOther(dataNums, leftParty());
            MpcZ2Vector[] res = adder.add(input, x23, false);
            TripletRpZ2Vector[] x1Binary = IntStream.range(1, res.length).mapToObj(i -> (TripletRpZ2Vector) res[i]).toArray(TripletRpZ2Vector[]::new);
            z2cParty.revealOther(x1Binary, rightParty());
            LongVector x1Plain = MatrixUtils.transBvIntoAv(z2cParty.revealOwn(x1Binary), envType, parallel);
            return TripletRpLongVector.create(x3Plain, x1Plain);
        }
    }

    @Override
    public TripletRpLongVector bit2a(MpcZ2Vector data) {
        return aMulB(PlainLongVector.createOnes(data.bitNum()), data, rpc.getParty(2));
    }

    @Override
    public TripletRpLongVector aMulB(MpcLongVector a, MpcZ2Vector b) {
        TripletRpLongVector x, y;
        if (selfId == 0) {
            x = aMulB(null, b, leftParty());
            y = aMulB(null, b, rightParty());
        } else if (selfId == 1) {
            x = aMulB(null, b, rightParty());
            y = aMulB(PlainLongVector.create(a.getVectors()[0]), b, ownParty());
        } else {
            PlainLongVector a23 = PlainLongVector.create(a.getVectors()[0].add(a.getVectors()[1]));
            x = aMulB(a23, b, ownParty());
            y = aMulB(null, b, leftParty());
        }
        zl64cParty.addi(x, y);
        return x;
    }

    public TripletRpLongVector aMulB(PlainLongVector a, MpcZ2Vector b, Party sender) {
        int dataNum = b.bitNum();
        if (ownParty().equals(sender)) {
            // P3 current party is sender, aider is left party, receiver is right party
            LongVector c1 = LongVector.create(crProvider.getRandLongArray(dataNum, rightParty()));
            LongVector c3 = LongVector.create(crProvider.getRandLongArray(dataNum, leftParty()));
            LongVector c13 = c1.add(c3);
            BitVector b13 = b.getBitVectors()[0].xor(b.getBitVectors()[1]);
            LongVector r0 = choiceData(b13, a.getVectors()[0]);
            LongVector r1 = a.getVectors()[0].sub(r0);
            r0.subi(c13);
            r0.subi(LongVector.create(crProvider.getRandLongArray(dataNum, rightParty())));
            r1.subi(c13);
            r1.subi(LongVector.create(crProvider.getRandLongArray(dataNum, rightParty())));
            sendLongVectors(PtoStep.A_MUL_BIT.ordinal(), leftParty(), r0, r1);
            return TripletRpLongVector.create(c3, c1);
        } else if (leftParty().equals(sender)) {
            // P1 current party is aider
            LongVector[] c1AndW = IntStream.range(0, 3).mapToObj(i ->
                LongVector.create(crProvider.getRandLongArray(dataNum, leftParty()))).toArray(LongVector[]::new);
            sendLongVectors(PtoStep.A_MUL_BIT.ordinal(), rightParty(), choiceData(b.getBitVectors()[1], c1AndW[2], c1AndW[1]));
            LongVector c2 = receiveLongVectors(PtoStep.A_MUL_BIT.ordinal(), rightParty())[0];
            return TripletRpLongVector.create(c1AndW[0], c2);
        } else {
            // P2 current party is receiver
            LongVector c3 = LongVector.create(crProvider.getRandLongArray(dataNum, rightParty()));
            LongVector wb2 = receiveLongVectors(PtoStep.A_MUL_BIT.ordinal(), leftParty())[0];
            LongVector[] mi = receiveLongVectors(PtoStep.A_MUL_BIT.ordinal(), rightParty());
            LongVector c2 = choiceData(b.getBitVectors()[0], mi[1], mi[0]);
            c2.addi(wb2);
            sendLongVectors(PtoStep.A_MUL_BIT.ordinal(), leftParty(), c2);
            return TripletRpLongVector.create(c2, c3);
        }
    }

    /**
     * return flag[i] ? x[i] : y[i]
     *
     * @param flag the indicator value
     * @param x    the first value
     * @param y    the second value
     * @return a plain Long vector.
     */
    private LongVector choiceData(BitVector flag, LongVector x, LongVector... y) {
        MathPreconditions.checkEqual("x.getNum()", "flag.bitNum()", x.getNum(), flag.bitNum());
        boolean[] flagB = BinaryUtils.byteArrayToBinary(flag.getBytes(), flag.bitNum());
        long[] data = x.getElements();
        long[] other = (y == null || y.length == 0) ? new long[x.getNum()] : y[0].getElements();
        return LongVector.create(IntStream.range(0, flagB.length).mapToLong(i -> flagB[i] ? data[i] : other[i]).toArray());
    }

}
