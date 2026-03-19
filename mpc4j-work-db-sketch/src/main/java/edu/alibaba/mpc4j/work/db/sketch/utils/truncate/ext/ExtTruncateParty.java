package edu.alibaba.mpc4j.work.db.sketch.utils.truncate.ext;

import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortParty;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateFnParam;
import edu.alibaba.mpc4j.work.db.sketch.utils.truncate.TruncateParty;

import java.util.Arrays;

public class ExtTruncateParty extends AbstractThreePartyDbPto implements TruncateParty {
    /**
     * oblivious permutation party
     */
    protected final PermuteParty permuteParty;
    /**
     * oblivious permutation party
     */
    protected final PgSortParty pgSortParty;

    public ExtTruncateParty(Abb3Party abb3Party, ExtTruncateConfig config) {
        super(ExtTruncatePtoDesc.getInstance(), abb3Party, config);
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
    public long[] setUsage(TruncateFnParam... params) {
        long[] tuple = new long[]{0, 0};
        if (isMalicious) {
            for (TruncateFnParam param : params) {
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
        return tuple;
    }

    /**
     * obtain group sum result and truncate all valid values into the front
     *
     * @param payload      Input group payload
     * @param groupFlag    group flag, sucha as [0,1,1,...1,0,...], where 0 represents the first element in each group
     * @param truncateSize target truncate size, to make sure that all valid rows are saved, this value should be large enough
     * @param keys         group key: may be null
     * @return [group_key(maybe nothing in result if input keys is null), group_agg_result, valid_flag]
     */
    @Override
    public TripletLongVector[] groupSumAndTruncate(TripletLongVector[] payload, TripletLongVector groupFlag, int truncateSize, TripletLongVector... keys) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN, "group sum and truncate");
        MathPreconditions.checkGreaterOrEqual("num >= truncateSize", groupFlag.getNum(), truncateSize);
        for (TripletLongVector key : keys) {
            MathPreconditions.checkEqual("key.getNum()", "groupFlag.getNum()", key.getNum(), groupFlag.getNum());
        }
        int keyDim = keys.length;

        stopWatch.start();
        TripletLongVector[] sumRows = Arrays.stream(payload)
            .map(ea -> zl64cParty.rowAdderWithPrefix(ea, (TripletLongVector) zl64cParty.setPublicValue(LongVector.createZeros(1)), false))
            .toArray(TripletLongVector[]::new);
        TripletLongVector invFlag = groupFlag.shiftLeft(1, groupFlag.getNum());
        TripletLongVector perm = pgSortParty.perGen4MultiDim(new TripletLongVector[]{invFlag}, new int[]{1});

        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "row add and permutation computation");

        stopWatch.start();
        TripletLongVector[] permInput = new TripletLongVector[sumRows.length + 1 + keyDim];
        if (keyDim > 0) {
            System.arraycopy(keys, 0, permInput, 0, keyDim);
        }
        System.arraycopy(sumRows, 0, permInput, keyDim, sumRows.length);
        permInput[sumRows.length + keyDim] = invFlag;
        TripletLongVector[] sumRowsAndInvFlag = permuteParty.applyInvPermutation(perm, permInput);
        for (int i = keyDim; i < sumRowsAndInvFlag.length - 1; i++) {
            TripletLongVector tmp = sumRowsAndInvFlag[i].shiftRight(1, sumRowsAndInvFlag[i].getNum());
            zl64cParty.subi(sumRowsAndInvFlag[i], tmp);
        }
        PlainLongVector plainOne = PlainLongVector.createOnes(groupFlag.getNum());
        zl64cParty.negi(sumRowsAndInvFlag[sumRowsAndInvFlag.length - 1]);
        zl64cParty.addi(sumRowsAndInvFlag[sumRowsAndInvFlag.length - 1], plainOne);
        for (int i = 0; i < sumRowsAndInvFlag.length; i++) {
            sumRowsAndInvFlag[i] = sumRowsAndInvFlag[i].shiftRight(sumRowsAndInvFlag[i].getNum() - truncateSize, truncateSize);
        }
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "inverse permutation and local sub");

        logPhaseInfo(PtoState.PTO_END, "group sum and truncate");
        return sumRowsAndInvFlag;
    }
}
