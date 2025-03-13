package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22;

import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.AbstractGroupParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.TraversalFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalParty;

/**
 * group sum with the prefix tree
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class Hzf22GroupSumParty extends AbstractGroupParty implements GroupSumParty {
    /**
     * oblivious traversal party
     */
    protected final TraversalParty traversalParty;

    public Hzf22GroupSumParty(Abb3Party abb3Party, Hzf22GroupSumConfig config) {
        super(Hzf22GroupSumPtoDesc.getInstance(), abb3Party, config);
        traversalParty = TraversalFactory.createParty(abb3Party, config.getTraversalConfig());
        addMultiSubPto(traversalParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        traversalParty.init();
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
                    case EXTREME:
                        throw new IllegalArgumentException("should not invoke EXTREME in the GroupSumParty");
                    case B_GROUP_FLAG:
                    case A_GROUP_FLAG:
                        long[] tmp = super.setUsage(param);
                        abb3Party.updateNum(tmp[0], tmp[1]);
                        tuple[0] += tmp[0];
                        tuple[1] += tmp[1];
                        break;
                    case SUM:
                        long[] traversalCost = traversalParty.setUsage(new TraversalFnParam(
                            TraversalOperations.TraversalOp.TRAVERSAL_A, param.inputSize, param.inputDim));
                        tuple[0] += traversalCost[0];
                        tuple[1] += traversalCost[1];
                        break;
                }
            }
        }
        return tuple;
    }

    @Override
    public TripletLongVector[] groupSum(TripletLongVector[] input, TripletLongVector groupFlag) {
        logPhaseInfo(PtoState.PTO_BEGIN, "group sum");

        stopWatch.start();
        TripletLongVector[] traversalInput = new TripletLongVector[input.length + 1];
        System.arraycopy(input, 0, traversalInput, 0, input.length);
        traversalInput[input.length] = groupFlag;
        TripletLongVector[] tmp = traversalParty.traversalPrefix(traversalInput, false, false, true, false);

        TripletLongVector[] result = new TripletLongVector[input.length + 1];
        System.arraycopy(tmp, 0, result, 0, input.length);
        result[input.length] = groupFlag.shiftLeft(1, groupFlag.getNum());
        PlainLongVector plainOne = PlainLongVector.createOnes(groupFlag.getNum());
        zl64cParty.negi(result[input.length]);
        zl64cParty.addi(result[input.length], plainOne);
        logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END, "group sum");
        return result;
    }
}
