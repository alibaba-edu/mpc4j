package edu.alibaba.mpc4j.work.scape.s3pc.db.group;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvOperations.ConvOp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;

import java.util.Arrays;

/**
 * abstract group party
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public abstract class AbstractGroupParty extends AbstractThreePartyDbPto implements GroupParty {
    /**
     * adder type
     */
    public final ComparatorType comparatorType;
    /**
     * z2 circuit
     */
    protected final Z2IntegerCircuit z2IntegerCircuit;

    protected AbstractGroupParty(PtoDesc ptoDesc, Abb3Party abb3Party, GroupConfig config) {
        super(ptoDesc, abb3Party, config);
        comparatorType = config.getComparatorTypes();
        z2IntegerCircuit = new Z2IntegerCircuit(z2cParty, new Z2CircuitConfig.Builder().setComparatorType(comparatorType).build());
    }

    @Override
    public long[] setUsage(GroupFnParam... params) {
        long[] tuple = new long[]{0, 0};
        if(isMalicious){
            for (GroupFnParam param : params) {
                int dataSize = CommonUtils.getByteLength(param.inputSize) << 3;
                switch (param.groupOp){
                    case A_GROUP_FLAG:
                        tuple[0] += (long) dataSize * (param.inputDim * 64L + 2);
                        long[] tmp1 = abb3Party.getConvParty().getTupleNum(ConvOp.A2B, dataSize, param.inputDim, 64);
                        long[] tmp2 = abb3Party.getConvParty().getTupleNum(ConvOp.BIT2A, dataSize, 1, 64);
                        tuple[0] += tmp1[0] + tmp2[0];
                        tuple[1] += tmp1[1] + tmp2[1];
                        break;
                    case B_GROUP_FLAG:
                        tuple[0] += (long) dataSize * (param.inputDim + 2);
                        break;
                    case EXTREME:
                    case SUM:
                        throw new IllegalArgumentException("should not invoke it in the AbstractGroupParty");
                }
            }
        }
        return tuple;
    }

    @Override
    public TripletLongVector getGroupFlag(TripletLongVector[] input, int[] keyIndex) throws MpcAbortException {
        Preconditions.checkArgument(input.length > keyIndex.length);
        logPhaseInfo(PtoState.PTO_BEGIN, "getGroupFlag");

        stopWatch.start();
        TripletZ2Vector[] keys = Arrays.stream(
                abb3Party.getConvParty().a2b(
                    Arrays.stream(keyIndex).mapToObj(index -> input[index]).toArray(TripletLongVector[]::new), 64))
            .flatMap(Arrays::stream)
            .toArray(TripletZ2Vector[]::new);
        TripletZ2Vector validFlag = abb3Party.getConvParty().a2b(input[input.length - 1], 1)[0];
        logStepInfo(PtoState.PTO_STEP, 1, 3, resetAndGetTime(), "A2B");

        stopWatch.start();
        TripletZ2Vector flag = getGroupFlagBinary(keys, validFlag);
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime(), "eq");

        stopWatch.start();
        TripletLongVector res = abb3Party.getConvParty().bit2a(flag);
        logStepInfo(PtoState.PTO_STEP, 3, 3, resetAndGetTime(), "bit2a");

        logPhaseInfo(PtoState.PTO_END, "getGroupFlag");
        return res;
    }

    @Override
    public TripletZ2Vector getGroupFlag(TripletZ2Vector[] input, int[] keyIndex) throws MpcAbortException {
        Preconditions.checkArgument(input.length > keyIndex.length);
        logPhaseInfo(PtoState.PTO_BEGIN, "binary getGroupFlag");

        stopWatch.start();
        TripletZ2Vector[] keys = Arrays.stream(keyIndex).mapToObj(index -> input[index]).toArray(TripletZ2Vector[]::new);
        TripletZ2Vector res = getGroupFlagBinary(keys, input[input.length - 1]);
        logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime(), "eq");

        logPhaseInfo(PtoState.PTO_END, "binary getGroupFlag");
        return res;
    }

    private TripletZ2Vector getGroupFlagBinary(TripletZ2Vector[] keyValues, TripletZ2Vector validFlag) throws MpcAbortException {
        TripletZ2Vector validFlagRight = (TripletZ2Vector) validFlag.copy();
        validFlagRight.reduce(validFlagRight.bitNum() - 1);
        TripletZ2Vector validFlagLeft = validFlag.reduceShiftRight(1);

        TripletZ2Vector allOneFlag = z2cParty.and(validFlagLeft, validFlagRight);

        TripletZ2Vector[] rightKey = Arrays.stream(keyValues)
            .map(keyValue -> keyValue.reduceShiftRight(1))
            .toArray(TripletZ2Vector[]::new);
        TripletZ2Vector[] leftKey = Arrays.stream(keyValues)
            .map(keyValue -> {
                TripletZ2Vector tmp = (TripletZ2Vector) keyValue.copy();
                tmp.reduce(keyValue.bitNum() - 1);
                return tmp;
            })
            .toArray(TripletZ2Vector[]::new);
        TripletZ2Vector equalFlag = (TripletZ2Vector) z2IntegerCircuit.eq(rightKey, leftKey);

        TripletZ2Vector res = z2cParty.and(equalFlag, allOneFlag);
        res.extendLength(keyValues[0].bitNum());
        return res;
    }
}
