package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22ext;

import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.AbstractGroupParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.hzf22.Hzf22GroupSumPtoDesc;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortParty;

import java.util.Arrays;

/**
 * HZF22 extension group sum party
 *
 * @author Feng Han
 * @date 2025/3/3
 */
public class Hzf22ExtGroupSumParty extends AbstractGroupParty implements GroupSumParty {
    /**
     * oblivious permutation party
     */
    protected final PermuteParty permuteParty;
    /**
     * oblivious permutation party
     */
    protected final PgSortParty pgSortParty;

    public Hzf22ExtGroupSumParty(Abb3Party abb3Party, Hzf22ExtGroupSumConfig config) {
        super(Hzf22GroupSumPtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        pgSortParty = new RadixPgSortParty(abb3Party, new RadixPgSortConfig.Builder(isMalicious()).build());
        addMultiSubPto(permuteParty, pgSortParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        permuteParty.init();
        pgSortParty.init();
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
                switch (param.groupOp) {
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
                        long[] sortCost = pgSortParty.setUsage(new PgSortFnParam(PgSortOp.SORT_A, param.inputSize, 1));
                        long[] permuteCost = permuteParty.setUsage(new PermuteFnParam(
                            PermuteOp.APPLY_INV_A_A, param.inputSize, param.inputDim, 1));
                        long[] permute2Cost = permuteParty.setUsage(new PermuteFnParam(
                            PermuteOp.COMPOSE_A_A, param.inputSize, param.inputDim, 1));
                        tuple[0] += sortCost[0] + permuteCost[0] + permute2Cost[0];
                        tuple[1] += sortCost[1] + permuteCost[1] + permute2Cost[1];
                        break;
                }
            }
        }
        return tuple;
    }

    @Override
    public TripletLongVector[] groupSum(TripletLongVector[] input, TripletLongVector groupFlag) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN, "group sum");

        stopWatch.start();
        TripletLongVector[] sumRows = Arrays.stream(input)
            .map(ea -> zl64cParty.rowAdderWithPrefix(ea, (TripletLongVector) zl64cParty.setPublicValue(LongVector.createZeros(1)), false))
            .toArray(TripletLongVector[]::new);
        TripletLongVector invFlag = groupFlag.shiftLeft(1, groupFlag.getNum());
        TripletLongVector perm = pgSortParty.perGen4MultiDim(new TripletLongVector[]{invFlag}, new int[]{1});
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "row add and permutation computation");

        stopWatch.start();
        sumRows = permuteParty.applyInvPermutation(perm, sumRows);
        Arrays.stream(sumRows).forEach(ea -> {
            TripletLongVector tmp = ea.shiftRight(1, ea.getNum());
            zl64cParty.subi(ea, tmp);
        });
        sumRows = permuteParty.composePermutation(perm, sumRows);
        TripletLongVector[] result = new TripletLongVector[input.length + 1];
        System.arraycopy(sumRows, 0, result, 0, input.length);
        PlainLongVector plainOne = PlainLongVector.createOnes(groupFlag.getNum());
        zl64cParty.negi(invFlag);
        zl64cParty.addi(invFlag, plainOne);
        result[input.length] = invFlag;
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "inverse permutation and compose");

        logPhaseInfo(PtoState.PTO_END, "group sum");
        return result;
    }
}
