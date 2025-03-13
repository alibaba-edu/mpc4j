package edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.hzf22;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AbstractAggParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFnParam.AggOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggParty;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * HZF22 aggregate functions
 *
 * @author Feng Han
 * @date 2025/2/26
 */
public class Hzf22AggParty extends AbstractAggParty implements AggParty {

    public Hzf22AggParty(Abb3Party abb3Party, Hzf22AggConfig config) {
        super(Hzf22AggPtoDesc.getInstance(), abb3Party, config);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(AggFnParam... params) {
        long[] tuples = new long[]{0, 0};
        List<long[]> allList = new LinkedList<>();
        for (AggFnParam param : params) {
            switch (param.aggOp) {
                case SUM: {
                    if (param.isBinaryInput) {
                        allList.add(new long[]{(long) param.inputSize * param.inputDim, 0});
                        allList.add(abb3Party.getConvParty().getTupleNum(ConvOp.B2A, param.inputSize, 1, param.inputDim));
                        allList.add(abb3Party.getConvParty().getTupleNum(ConvOp.A2B, 1, 1, 64));
                    } else {
                        allList.add(new long[]{0, param.inputSize});
                    }
                    break;
                }
                case MAX, MIN: {
                    int inputDim = param.isBinaryInput ? param.inputDim : 64;
                    allList.add(new long[]{
                        8L * inputDim + (3L + inputDim + ComparatorFactory.getAndGateNum(comparatorType, inputDim)) * param.inputSize,
                        0});
                    if (!param.isBinaryInput) {
                        allList.add(abb3Party.getConvParty().getTupleNum(ConvOp.A2B, param.inputSize, 1, 64));
                        allList.add(abb3Party.getConvParty().getTupleNum(ConvOp.A2B, param.inputSize, 1, 1));
                        allList.add(abb3Party.getConvParty().getTupleNum(ConvOp.B2A, 1, 1, 64));
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid AggType: " + param.aggOp.name());
            }
        }
        tuples[0] += allList.stream().mapToLong(tuple -> tuple[0]).sum();
        tuples[1] += allList.stream().mapToLong(tuple -> tuple[1]).sum();
        abb3Party.updateNum(tuples[0], tuples[1]);
        return tuples;
    }

    @Override
    public TripletLongVector agg(TripletLongVector input, TripletLongVector validFlag, AggOp aggOp) throws MpcAbortException {
        checkInput(input, validFlag);
        logPhaseInfo(PtoState.PTO_BEGIN, "arithmetic agg:" + aggOp.name());
        TripletLongVector res;

        if (validFlag.getNum() == 1) {
            res = zl64cParty.mul(input, validFlag);
        } else {
            switch (aggOp) {
                case SUM: {
                    stopWatch.start();
                    TripletLongVector mulRes = zl64cParty.mul(input, validFlag);
                    LongVector[] sums = Arrays.stream(mulRes.getVectors())
                        .map(LongVector::sum)
                        .map(num -> LongVector.create(new long[]{num}))
                        .toArray(LongVector[]::new);
                    res = (TripletLongVector) zl64cParty.create(false, sums);
                    logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime(), "mul and sum");
                    break;
                }
                case MAX:
                case MIN: {
                    stopWatch.start();
                    TripletZ2Vector[] bInput = abb3Party.getConvParty().a2b(input, 64);
                    TripletZ2Vector bFlag = abb3Party.getConvParty().a2b(validFlag, 1)[0];
                    logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "type conversion");

                    stopWatch.start();
                    TripletZ2Vector[] bRes = extreme(bInput, bFlag, aggOp);
                    res = abb3Party.getConvParty().b2a(bRes);
                    logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "extreme and b2a");
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid AggType: " + aggOp.name());
            }
        }
        logPhaseInfo(PtoState.PTO_END, "arithmetic agg:" + aggOp.name());

        return res;
    }

    @Override
    public TripletZ2Vector[] agg(TripletZ2Vector[] input, TripletZ2Vector validFlag, AggOp aggOp) throws MpcAbortException {
        checkInput(input, validFlag);
        logPhaseInfo(PtoState.PTO_BEGIN, "binary agg:" + aggOp.name());
        TripletZ2Vector[] res;

        if (validFlag.getNum() == 1) {
            res = z2cParty.and(input, IntStream.range(0, input.length).mapToObj(i -> validFlag).toArray(TripletZ2Vector[]::new));
        } else {
            switch (aggOp) {
                case SUM: {
                    stopWatch.start();
                    TripletZ2Vector[] andRes = z2cParty.and(input,
                        IntStream.range(0, input.length).mapToObj(i -> validFlag).toArray(TripletZ2Vector[]::new));
                    TripletLongVector longTrans = abb3Party.getConvParty().b2a(andRes);
                    logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "mul and type conversion");

                    stopWatch.start();
                    LongVector[] sums = Arrays.stream(longTrans.getVectors())
                        .map(LongVector::sum)
                        .map(num -> LongVector.create(new long[]{num}))
                        .toArray(LongVector[]::new);
                    TripletLongVector aRes = (TripletLongVector) zl64cParty.create(false, sums);
                    res = abb3Party.getConvParty().a2b(aRes, 64);
                    logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "sum and a2b");
                    break;
                }
                case MAX:
                case MIN: {
                    stopWatch.start();
                    res = extreme(input, validFlag, aggOp);
                    logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime(), "extreme");
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid AggType: " + aggOp.name());
            }
        }
        logPhaseInfo(PtoState.PTO_END, "binary agg:" + aggOp.name());

        return res;
    }

    private TripletZ2Vector[] extreme(TripletZ2Vector[] input, TripletZ2Vector validFlag, AggOp aggOp) throws MpcAbortException {
        TripletZ2Vector tmpValidFlag = (TripletZ2Vector) validFlag.copy();
        TripletZ2Vector[] tmpInput = Arrays.copyOf(input, input.length);
        int dim = input.length;
        while (tmpValidFlag.getNum() > 1) {
            int halfNum = tmpValidFlag.getNum() >> 1;
            TripletZ2Vector[] leftCompareInput = new TripletZ2Vector[dim];
            TripletZ2Vector[] rightCompareInput = new TripletZ2Vector[dim];
            for (int i = 0; i < dim; i++) {
                leftCompareInput[i] = tmpInput[i].reduceShiftRight(halfNum);
                leftCompareInput[i].reduce(halfNum);
                rightCompareInput[i] = (TripletZ2Vector) tmpInput[i].copy();
                rightCompareInput[i].reduce(halfNum);
            }
            TripletZ2Vector leftFlag = tmpValidFlag.reduceShiftRight(halfNum);
            leftFlag.reduce(halfNum);
            TripletZ2Vector rightFlag = (TripletZ2Vector) tmpValidFlag.copy();
            rightFlag.reduce(halfNum);

            TripletZ2Vector keepLeftFlag = (TripletZ2Vector) z2IntegerCircuit.leq(leftCompareInput, rightCompareInput);
            if (aggOp.equals(AggOp.MAX)) {
                z2cParty.noti(keepLeftFlag);
            }
            // left.valid = 1 and (keepLeftFlag || right.valid = 0), keep left
            TripletZ2Vector[] orRes = z2cParty.or(
                new TripletZ2Vector[]{keepLeftFlag, leftFlag},
                new TripletZ2Vector[]{(TripletZ2Vector) z2cParty.not(rightFlag), rightFlag});

            keepLeftFlag = z2cParty.and(orRes[0], leftFlag);
            TripletZ2Vector nextFlag = orRes[1];
            TripletZ2Vector[] res = z2cParty.mux(rightCompareInput, leftCompareInput, keepLeftFlag);

            // deal with the first data if the input number is odd
            if (tmpValidFlag.getNum() % 2 == 1) {
                nextFlag.extendLength(halfNum + 1);
                nextFlag.setPointsWithFixedSpace(tmpValidFlag, 0, 1, 1);
                for (int i = 0; i < dim; i++) {
                    res[i].extendLength(halfNum + 1);
                    res[i].setPointsWithFixedSpace(tmpInput[i], 0, 1, 1);
                }
            }
            tmpInput = res;
            tmpValidFlag = nextFlag;
        }
        TripletZ2Vector[] allZero = (TripletZ2Vector[]) z2cParty.setPublicValues(
            IntStream.range(0, dim).mapToObj(i -> BitVectorFactory.createZeros(1)).toArray(BitVector[]::new));
        return z2cParty.mux(allZero, tmpInput, tmpValidFlag);
    }

}
