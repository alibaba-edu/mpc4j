package edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.hzf22;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.Vector;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.AbstractGroupParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory.ExtremeType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeParty;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * HZF22 group extreme protocol
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class Hzf22GroupExtremeParty extends AbstractGroupParty implements GroupExtremeParty {

    public Hzf22GroupExtremeParty(Abb3Party abb3Party, Hzf22GroupExtremeConfig config) {
        super(Hzf22GroupExtremePtoDesc.getInstance(), abb3Party, config);
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
    public long[] setUsage(GroupFnParam... params) {
        long[] tuple = new long[]{0, 0};
        if (isMalicious) {
            for (GroupFnParam param : params) {
                switch (param.groupOp){
                    case SUM:
                        throw new IllegalArgumentException("should not invoke SUM in the GroupExtremeParty");
                    case B_GROUP_FLAG:
                    case A_GROUP_FLAG:
                        long[] tmp = super.setUsage(param);
                        tuple[0] += tmp[0];
                        tuple[1] += tmp[1];
                        break;
                    case EXTREME:
                        tuple[0] += 2L * (2L + param.inputDim + ComparatorFactory.getAndGateNum(comparatorType, param.inputDim)) * param.inputSize;
                        // for the operations for small-bit input
                        tuple[0] += 8L * (param.inputDim + ComparatorFactory.getAndGateNum(comparatorType, param.inputDim));
                        break;
                }
            }
        }
        abb3Party.updateNum(tuple[0], tuple[1]);
        return tuple;
    }

    @Override
    public TripletZ2Vector[] groupExtreme(TripletZ2Vector[] shouldExtreme, TripletZ2Vector groupFlag, ExtremeType ops) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN, "groupExtreme");

        stopWatch.start();
        int dataNum = shouldExtreme[0].bitNum();
        int logDim = LongUtils.ceilLog2(dataNum);
        int upLevel = (1 << logDim) > dataNum ? logDim - 1 : logDim;
        int downLevel = (3 << (logDim - 2)) > dataNum ? logDim - 2 : logDim - 1;
        TripletZ2Vector[] prefixInput = Arrays.stream(shouldExtreme)
            .map(Vector::copy)
            .map(ea -> (TripletZ2Vector) ea)
            .toArray(TripletZ2Vector[]::new);
        TripletZ2Vector flag = (TripletZ2Vector) groupFlag.copy();
        // 对于上行的过程中
        for (int i = 0; i < upLevel; i++) {
            int step = 1 << i;
            int sepDistance = step << 1;
            int num = dataNum / sepDistance;
            int belowStartPos = sepDistance - 1, aboveStartPos = step - 1;
            TripletZ2Vector aboveFlag = (TripletZ2Vector) flag.getPointsWithFixedSpace(aboveStartPos, num, sepDistance);
            TripletZ2Vector belowFlag = this.commonPartOfPrefix(prefixInput, flag, ops, sepDistance, num, aboveStartPos, belowStartPos);
            flag.setPointsWithFixedSpace(z2cParty.and(aboveFlag, belowFlag), belowStartPos, num, sepDistance);
        }
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "upper phase");

        stopWatch.start();
        // 对于下行的过程中
        for (int i = 0; i < downLevel; i++) {
            int sepDistance = 1 << (downLevel - i);
            int step = sepDistance >> 1;
            int num = dataNum / step - (dataNum / sepDistance) - 1;
            int belowStartPos = sepDistance + step - 1, aboveStartPos = sepDistance - 1;
            this.commonPartOfPrefix(prefixInput, flag, ops, sepDistance, num, aboveStartPos, belowStartPos);
        }
        TripletZ2Vector[] finalRes = new TripletZ2Vector[shouldExtreme.length + 1];
        finalRes[shouldExtreme.length] = (TripletZ2Vector) groupFlag.copy();
        finalRes[shouldExtreme.length].fixShiftLefti(1);
        z2cParty.noti(finalRes[shouldExtreme.length]);
        System.arraycopy(prefixInput, 0, finalRes, 0, shouldExtreme.length);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "down phase");

        logPhaseInfo(PtoState.PTO_END, "groupExtreme");
        return finalRes;
    }

    /**
     * groupBy 极值计算中循环的共同部分
     */
    private TripletZ2Vector commonPartOfPrefix(TripletZ2Vector[] prefixInput, TripletZ2Vector flag, ExtremeType ops,
                                               int sepDistance, int num, int aboveStartPos, int belowStartPos) throws MpcAbortException {
        int totalDim = prefixInput.length;
        TripletZ2Vector belowFlag = (TripletZ2Vector) flag.getPointsWithFixedSpace(belowStartPos, num, sepDistance);
        IntStream intStream = parallel ? IntStream.range(0, totalDim).parallel() : IntStream.range(0, totalDim);
        TripletZ2Vector[] belowValues = new TripletZ2Vector[totalDim], aboveValues = new TripletZ2Vector[totalDim];
        intStream.forEach(j -> {
            belowValues[j] = (TripletZ2Vector) prefixInput[j].getPointsWithFixedSpace(belowStartPos, num, sepDistance);
            aboveValues[j] = (TripletZ2Vector) prefixInput[j].getPointsWithFixedSpace(aboveStartPos, num, sepDistance);
        });
        TripletZ2Vector tmpCompRes = (TripletZ2Vector) z2IntegerCircuit.leq(aboveValues, belowValues);
        // 如果是计算的MIN，则bit翻转
        if (ops.equals(ExtremeType.MAX)) {
            z2cParty.noti(tmpCompRes);
        }
        // 如果左边的极值和右边的极值相比起来更是极值，且当前节点的左边不是分界点，那么就采用top的值，否则就采用自己的值
        TripletZ2Vector chooseLeft = z2cParty.and(belowFlag, tmpCompRes);
        TripletZ2Vector[] chooseLeftExtend = new TripletZ2Vector[totalDim];
        Arrays.fill(chooseLeftExtend, chooseLeft);
        TripletZ2Vector[] midResFlat = z2cParty.and(chooseLeftExtend, z2cParty.xor(aboveValues, belowValues));
        intStream = parallel ? IntStream.range(0, prefixInput.length).parallel() : IntStream.range(0, prefixInput.length);
        intStream.forEach(j -> prefixInput[j].setPointsWithFixedSpace(z2cParty.xor(midResFlat[j], belowValues[j]), belowStartPos, num, sepDistance));
        return belowFlag;
    }

}
