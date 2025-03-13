package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.hzf22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.AbstractTraversalParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.TraversalFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalParty;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 3p traversal party
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class Hzf22TraversalParty extends AbstractTraversalParty implements TraversalParty {
    /**
     * z2c party
     */
    public TripletZ2cParty z2cParty;
    /**
     * zl64c party
     */
    public TripletLongParty zl64cParty;

    public Hzf22TraversalParty(Abb3Party abb3Party, Hzf22TraversalConfig config) {
        super(Hzf22TraversalPtoDesc.getInstance(), abb3Party, config);
        z2cParty = abb3Party.getZ2cParty();
        zl64cParty = abb3Party.getLongParty();
    }

    @Override
    public long[] setUsage(TraversalFnParam... params) {
        if (!isMalicious) {
            return new long[]{0, 0};
        }
        long bitTupleNum = 0;
        long longTupleNum = 0;
        for (TraversalFnParam param : params) {
            int dataNum = CommonUtils.getByteLength(param.dataNum) << 3;
            switch (param.op) {
                case TRAVERSAL_A:{
                    longTupleNum += zl64cParty instanceof Cgh18RpLongParty ? 0 : (2L * param.dim + 1) * dataNum;
                    break;
                }
                case TRAVERSAL_B: {
                    bitTupleNum += (4L * param.dim + 1) * dataNum + 24L * (4L * param.dim + 1);
                    break;
                }
                default:
                    throw new IllegalArgumentException("illegal TraversalOp: " + param.op.name());
            }
        }
        abb3Party.updateNum(bitTupleNum, longTupleNum);
        return new long[]{bitTupleNum, longTupleNum};
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        initState();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }


    @Override
    public TripletLongVector[] traversalPrefix(TripletLongVector[] input, boolean isInv, boolean keepFlag,
                                               boolean keepOrigin, boolean theta) {
        logPhaseInfo(PtoState.PTO_BEGIN, "traverse arithmetic shares");

        stopWatch.start();
        // 首先先将所有的原始值复制。
        // 然后如果keepFlag,在down的过程中也一起更新flag的值，否则down的时候不用更新
        TripletLongVector[] prefixInput = keepOrigin ?
            IntStream.range(0, input.length - (keepFlag ? 0 : 1)).mapToObj(i -> (TripletLongVector) input[i].copy()).toArray(TripletLongVector[]::new)
            : Arrays.copyOf(input, input.length - (keepFlag ? 0 : 1));
        TripletLongVector flag = (keepOrigin | keepFlag) ? (TripletLongVector) input[input.length - 1].copy() : input[input.length - 1];
        int logDim = LongUtils.ceilLog2(input[0].getNum());
        int upLevel = (1 << logDim) > input[0].getNum() ? logDim - 1 : logDim;
        int downLevel = (3 << (logDim - 2)) > input[0].getNum() ? logDim - 2 : logDim - 1;
        // 对于上行的过程中
        for (int i = 0; i < upLevel; i++) {
            int step = 1 << i;
            int sepDistance = step << 1;
            int num = input[0].getNum() / sepDistance;
            int belowStartPos, aboveStartPos;
            if (isInv) {
                belowStartPos = input[0].getNum() - num * sepDistance;
                aboveStartPos = belowStartPos + step;
            } else {
                belowStartPos = sepDistance - 1;
                aboveStartPos = step - 1;
            }
            TripletLongVector aboveFlag = flag.getPointsWithFixedSpace(aboveStartPos, num, sepDistance);
            TripletLongVector belowFlag = this.commonPartOfTraversal(prefixInput, flag, sepDistance, num, aboveStartPos, belowStartPos, theta);
            flag.setPointsWithFixedSpace(zl64cParty.mul(aboveFlag, belowFlag), belowStartPos, num, sepDistance);
        }
        stopWatch.stop();
        long upStageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, upStageTime);

        stopWatch.start();
        // 对于下行的过程中
        for (int i = 0; i < downLevel; i++) {
            int sepDistance = 1 << (downLevel - i);
            int step = sepDistance >> 1;
            int num = input[0].getNum() / step - (input[0].getNum() / sepDistance) - 1;
            int belowStartPos, aboveStartPos;
            if (isInv) {
                aboveStartPos = input[0].getNum() - num * sepDistance;
                belowStartPos = aboveStartPos - step;
            } else {
                belowStartPos = sepDistance + step - 1;
                aboveStartPos = sepDistance - 1;
            }
            this.commonPartOfTraversal(prefixInput, flag, sepDistance, num, aboveStartPos, belowStartPos, theta);
        }
        stopWatch.stop();
        long downStageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, downStageTime);

        logPhaseInfo(PtoState.PTO_END, "traverse arithmetic shares");
        return prefixInput;
    }

    private TripletLongVector commonPartOfTraversal(TripletLongVector[] prefixInput, TripletLongVector flag,
                                                    int sepDistance, int num, int aboveStartPos, int belowStartPos, boolean theta) {
        TripletLongVector belowFlag = flag.getPointsWithFixedSpace(belowStartPos, num, sepDistance);
        TripletLongVector[] extendBelowFlag = new TripletLongVector[prefixInput.length];
        Arrays.fill(extendBelowFlag, belowFlag);
        TripletLongVector[] aboveValues = new TripletLongVector[prefixInput.length];
        IntStream intStream = parallel ? IntStream.range(0, prefixInput.length).parallel() : IntStream.range(0, prefixInput.length);
        TripletLongVector[] belowValues = intStream.mapToObj(j -> prefixInput[j].getPointsWithFixedSpace(belowStartPos, num, sepDistance)).toArray(TripletLongVector[]::new);
        intStream = parallel ? IntStream.range(0, prefixInput.length).parallel() : IntStream.range(0, prefixInput.length);
        if (theta) {
            intStream.forEach(j -> aboveValues[j] = zl64cParty.sub(prefixInput[j].getPointsWithFixedSpace(aboveStartPos, num, sepDistance), belowValues[j]));
        } else {
            intStream.forEach(j -> aboveValues[j] = prefixInput[j].getPointsWithFixedSpace(aboveStartPos, num, sepDistance));
        }
        TripletLongVector[] mulRes = zl64cParty.mul(extendBelowFlag, aboveValues);
        intStream = parallel ? IntStream.range(0, prefixInput.length).parallel() : IntStream.range(0, prefixInput.length);
        intStream.forEach(j -> prefixInput[j].setPointsWithFixedSpace(zl64cParty.add(mulRes[j], belowValues[j]), belowStartPos, num, sepDistance));
        return belowFlag;
    }

    @Override
    public TripletZ2Vector[] traversalPrefix(TripletZ2Vector[] dummyFlag, TripletZ2Vector groupFlag,
                                             boolean keepOrigin, boolean isInv) {
        logPhaseInfo(PtoState.PTO_BEGIN, "traverse binary shares");

        stopWatch.start();
        int dataNum = dummyFlag[0].getNum();
        TripletZ2Vector[] valid = keepOrigin ? Arrays.copyOf(dummyFlag, dummyFlag.length) : dummyFlag;
        TripletZ2Vector flag = keepOrigin ? (TripletZ2Vector) groupFlag.copy() : groupFlag;
        int logDim = LongUtils.ceilLog2(dataNum);
        int upLevel = (1 << logDim) > dataNum ? logDim - 1 : logDim;
        int downLevel = (3 << (logDim - 2)) > dataNum ? logDim - 2 : logDim - 1;
        // 对于上行的过程中
        for (int i = 0; i < upLevel; i++) {
            int step = 1 << i;
            int sepDistance = step << 1;
            int num = dataNum / sepDistance;
            int belowStartPos, aboveStartPos;
            if (isInv) {
                belowStartPos = dataNum - num * sepDistance;
                aboveStartPos = belowStartPos + step;
            } else {
                belowStartPos = sepDistance - 1;
                aboveStartPos = step - 1;
            }

            TripletZ2Vector aboveFlag = (TripletZ2Vector) flag.getPointsWithFixedSpace(aboveStartPos, num, sepDistance);
            TripletZ2Vector belowFlag = this.commonPartOfTraversal(valid, flag, sepDistance, num, aboveStartPos, belowStartPos);
            flag.setPointsWithFixedSpace(z2cParty.and(aboveFlag, belowFlag), belowStartPos, num, sepDistance);
        }
        stopWatch.stop();
        long upStageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, upStageTime);

        stopWatch.start();
        // 对于下行的过程中
        for (int i = 0; i < downLevel; i++) {
            int sepDistance = 1 << (downLevel - i);
            int step = sepDistance >> 1;
            int num = dataNum / step - (dataNum / sepDistance) - 1;
            int belowStartPos, aboveStartPos;
            if (isInv) {
                aboveStartPos = dataNum - num * sepDistance;
                belowStartPos = aboveStartPos - step;
            } else {
                belowStartPos = sepDistance + step - 1;
                aboveStartPos = sepDistance - 1;
            }

            this.commonPartOfTraversal(valid, flag, sepDistance, num, aboveStartPos, belowStartPos);
        }
        stopWatch.stop();
        long downStageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, downStageTime);

        logPhaseInfo(PtoState.PTO_END, "traverse binary shares");
        return valid;
    }

    private TripletZ2Vector commonPartOfTraversal(TripletZ2Vector[] prefixInput, TripletZ2Vector flag,
                                                  int sepDistance, int num, int aboveStartPos, int belowStartPos) {
        TripletZ2Vector belowFlag = (TripletZ2Vector) flag.getPointsWithFixedSpace(belowStartPos, num, sepDistance);
        TripletZ2Vector[] belowValues = new TripletZ2Vector[prefixInput.length], aboveValues = new TripletZ2Vector[prefixInput.length];
        IntStream intStream = parallel ? IntStream.range(0, prefixInput.length).parallel() : IntStream.range(0, prefixInput.length);
        intStream.forEach(i -> {
            belowValues[i] = (TripletZ2Vector) z2cParty.not(prefixInput[i].getPointsWithFixedSpace(belowStartPos, num, sepDistance));
            aboveValues[i] = (TripletZ2Vector) z2cParty.not(prefixInput[i].getPointsWithFixedSpace(aboveStartPos, num, sepDistance));
        });
        TripletZ2Vector[] haveOne = z2cParty.and(belowValues, aboveValues);
        IntStream.range(0, haveOne.length).forEach(i -> haveOne[i] = (TripletZ2Vector) z2cParty.not(haveOne[i]));
        TripletZ2Vector[] extend = new TripletZ2Vector[haveOne.length];
        Arrays.fill(extend, belowFlag);

        IntStream.range(0, haveOne.length).forEach(i -> belowValues[i] = (TripletZ2Vector) z2cParty.not(belowValues[i]));
        TripletZ2Vector[] newRes = z2cParty.xor(z2cParty.and(extend, z2cParty.xor(haveOne, belowValues)), belowValues);
        intStream = parallel ? IntStream.range(0, prefixInput.length).parallel() : IntStream.range(0, prefixInput.length);
        intStream.forEach(i -> prefixInput[i].setPointsWithFixedSpace(newRes[i], belowStartPos, num, sepDistance));

        return belowFlag;
    }
}

