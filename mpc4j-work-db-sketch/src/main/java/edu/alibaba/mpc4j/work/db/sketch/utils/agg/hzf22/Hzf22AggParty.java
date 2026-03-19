package edu.alibaba.mpc4j.work.db.sketch.utils.agg.hzf22;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.circuit.z2.utils.Z2VectorUtils;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AbstractAggParty;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggFnParam;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggFnParam.AggOp;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.AggParty;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * HZF22 aggregate functions
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
                    if (param.extremeInfo != null) {
                        if (param.extremeInfo.equals(AggFnParam.ExtremeInfo.INDEX)) {
                            allList.add(new long[]{(long) param.inputSize * LongUtils.ceilLog2(param.inputSize), 0});
                        } else {
                            allList.add(new long[]{2L * param.inputSize, 0});
                        }
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
        return commonPartOfExtreme(input, aggOp, false, validFlag).getLeft();
    }

    @Override
    public Pair<TripletZ2Vector[], TripletZ2Vector[]> extremeIndex(TripletZ2Vector[] input, AggOp aggOp, TripletZ2Vector... validFlag) throws MpcAbortException {
        Pair<TripletZ2Vector[], List<TripletZ2Vector>> commonRes = commonPartOfExtreme(input, aggOp, true, validFlag);
        TripletZ2Vector[] resValue = Arrays.copyOf(commonRes.getLeft(), input.length);
        TripletZ2Vector[] resIndex = Arrays.copyOfRange(commonRes.getLeft(), input.length, commonRes.getLeft().length);
        return Pair.of(resValue, resIndex);
    }

    @Override
    public Pair<TripletZ2Vector[], TripletZ2Vector> extremeIndicator(TripletZ2Vector[] input, AggOp aggOp, TripletZ2Vector... validFlag) throws MpcAbortException {
        Pair<TripletZ2Vector[], List<TripletZ2Vector>> commonRes = commonPartOfExtreme(input, aggOp, false, validFlag);
        // save the number of the valid data in this layers
        List<TripletZ2Vector> keepLeftFlagList = commonRes.getRight();
        // save the number of the valid data in this layers
        TIntList numList = new TIntLinkedList();
        int inputNum = input[0].getNum();
        while (inputNum > 1) {
            // save the number of the valid data in this layers
            numList.add(inputNum);
            inputNum = inputNum / 2 + inputNum % 2;
        }

        // up-down update the extreme indicator flag
        numList.reverse();
        TripletZ2Vector[] keepLeftFlags = keepLeftFlagList.toArray(new TripletZ2Vector[0]);
        TripletZ2Vector indicatorFlag = z2cParty.createShareZeros(1);
        z2cParty.noti(indicatorFlag);
        for (int levelIndex = 0; levelIndex < numList.size(); levelIndex++) {
            int dataNum = numList.get(levelIndex);
            TripletZ2Vector layerFlag = keepLeftFlags[keepLeftFlags.length - 1 - levelIndex];
            TripletZ2Vector firstFlag = null;
            if (dataNum % 2 == 1) {
                // if the number is odd, such as 3, then the length of keepLeftFlag is dataNum / 2, the first element does not involve in the computation
                firstFlag = indicatorFlag.reduceShiftRight(dataNum / 2);
                indicatorFlag.reduce(dataNum / 2);
            }
            // get (leftFlag · upperFlag) ((!leftFlag) · upperFlag), such that only the extreme value source can be 1
            TripletZ2Vector[] downLrFlag = z2cParty.and(
                new TripletZ2Vector[]{layerFlag, (TripletZ2Vector) z2cParty.not(layerFlag)},
                new TripletZ2Vector[]{indicatorFlag, indicatorFlag});
            downLrFlag[0].merge(downLrFlag[1]);
            indicatorFlag = downLrFlag[0];
            if (dataNum % 2 == 1) {
                // if the number is odd, pad the first element
                indicatorFlag.extendLength(dataNum);
                indicatorFlag.setPointsWithFixedSpace(firstFlag, 0, 1, 1);
            }
        }
        MathPreconditions.checkEqual("indicatorFlag.bitNum()", "input[0].bitNum()", indicatorFlag.bitNum(), input[0].bitNum());
        return Pair.of(commonRes.getLeft(), indicatorFlag);
    }

    /**
     * common part of extreme protocol
     *
     * @param input            input data
     * @param aggOp            operation
     * @param needExtremeIndex need extreme index
     * @param validFlag        valid flag
     * @return Pair<( extreme_value | index ), middle_layer_flag>
     */
    private Pair<TripletZ2Vector[], List<TripletZ2Vector>> commonPartOfExtreme(TripletZ2Vector[] input, AggOp aggOp, boolean needExtremeIndex, TripletZ2Vector... validFlag) throws MpcAbortException {
        Preconditions.checkArgument(aggOp.equals(AggOp.MIN) || aggOp.equals(AggOp.MAX));
        TripletZ2Vector tmpValidFlag = null;
        if (validFlag.length > 0 && validFlag[0] != null) {
            MathPreconditions.checkEqual("validFlag[0].bitNum()", "input[0].bitNum()", validFlag[0].bitNum(), input[0].bitNum());
            tmpValidFlag = (TripletZ2Vector) validFlag[0].copy();
        }
        int dataNum = input[0].bitNum();
        int keyDim = input.length;
        int indexDim = dataNum == 1 ? 1 : LongUtils.ceilLog2(dataNum);
        int totalDim = keyDim + (needExtremeIndex ? indexDim : 0);
        TripletZ2Vector[] inputAndIndex = new TripletZ2Vector[totalDim];
        System.arraycopy(input, 0, inputAndIndex, 0, input.length);
        if (needExtremeIndex) {
            TripletZ2Vector[] indexShare = (TripletZ2Vector[]) z2cParty.setPublicValues(Z2VectorUtils.getBinaryIndex(dataNum));
            System.arraycopy((TripletZ2Vector[]) indexShare,
                0, inputAndIndex, input.length, indexShare.length);
        }
        // save the keep left flag
        List<TripletZ2Vector> keepLeftFlagList = new LinkedList<>();

        while (inputAndIndex[0].getNum() > 1) {
            int halfNum = inputAndIndex[0].getNum() >> 1;
            TripletZ2Vector[] leftInput = new TripletZ2Vector[totalDim];
            TripletZ2Vector[] rightInput = new TripletZ2Vector[totalDim];
            for (int i = 0; i < totalDim; i++) {
                leftInput[i] = inputAndIndex[i].reduceShiftRight(halfNum);
                leftInput[i].reduce(halfNum);
                rightInput[i] = (TripletZ2Vector) inputAndIndex[i].copy();
                rightInput[i].reduce(halfNum);
            }

            // compute the keep left flag
            TripletZ2Vector keepLeftFlag;
            if (needExtremeIndex) {
                keepLeftFlag = aggOp.equals(AggOp.MIN)
                    ? (TripletZ2Vector) z2IntegerCircuit.leq(Arrays.copyOf(leftInput, keyDim), Arrays.copyOf(rightInput, keyDim))
                    : (TripletZ2Vector) z2IntegerCircuit.leq(Arrays.copyOf(rightInput, keyDim), Arrays.copyOf(leftInput, keyDim));
            } else {
                keepLeftFlag = aggOp.equals(AggOp.MIN)
                    ? (TripletZ2Vector) z2IntegerCircuit.leq(leftInput, rightInput)
                    : (TripletZ2Vector) z2IntegerCircuit.leq(rightInput, leftInput);
            }
            TripletZ2Vector nextFlag = null;
            if (tmpValidFlag != null) {
                TripletZ2Vector leftFlag = tmpValidFlag.reduceShiftRight(halfNum);
                leftFlag.reduce(halfNum);
                TripletZ2Vector rightFlag = (TripletZ2Vector) tmpValidFlag.copy();
                rightFlag.reduce(halfNum);
                // right.valid = 0 || (keepLeftFlag · (right.valid == left.valid)), keep left
                TripletZ2Vector sameLr = z2cParty.xor(leftFlag, rightFlag);
                z2cParty.noti(sameLr);
                sameLr = z2cParty.and(sameLr, keepLeftFlag);
                TripletZ2Vector[] orRes = z2cParty.or(
                    new TripletZ2Vector[]{sameLr, leftFlag},
                    new TripletZ2Vector[]{(TripletZ2Vector) z2cParty.not(rightFlag), rightFlag});
                keepLeftFlag = orRes[0];
                nextFlag = orRes[1];
            }
            TripletZ2Vector[] res = z2cParty.mux(rightInput, leftInput, keepLeftFlag);
            // save the keep left flag
            keepLeftFlagList.add(keepLeftFlag);

            // deal with the first data if the input number is odd
            if (inputAndIndex[0].getNum() % 2 == 1) {
                if (nextFlag != null) {
                    nextFlag.extendLength(halfNum + 1);
                    nextFlag.setPointsWithFixedSpace(tmpValidFlag, 0, 1, 1);
                }
                for (int i = 0; i < inputAndIndex.length; i++) {
                    res[i].extendLength(halfNum + 1);
                    res[i].setPointsWithFixedSpace(inputAndIndex[i], 0, 1, 1);
                }
            }
            inputAndIndex = res;
            tmpValidFlag = nextFlag;
        }
        if (tmpValidFlag != null) {
            TripletZ2Vector[] allZero = IntStream.range(0, totalDim)
                .mapToObj(i -> z2cParty.createShareZeros(1))
                .toArray(TripletZ2Vector[]::new);
            inputAndIndex = z2cParty.mux(allZero, inputAndIndex, tmpValidFlag);
        }
        return Pair.of(inputAndIndex, keepLeftFlagList);
    }
}
