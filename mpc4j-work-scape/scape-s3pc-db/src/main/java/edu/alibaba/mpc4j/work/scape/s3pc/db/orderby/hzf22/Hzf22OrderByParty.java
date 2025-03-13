package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.hzf22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.AbstractOrderByParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.orderby.OrderByFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortParty;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * HZF22 order-by protocol, where the payload is not involved during sorting
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class Hzf22OrderByParty extends AbstractOrderByParty {
    /**
     * permute party
     */
    public final PermuteParty permuteParty;
    /**
     * sort party
     */
    public final PgSortParty pgSortParty;

    public Hzf22OrderByParty(Abb3Party abb3Party, Hzf22OrderByConfig config) {
        super(Hzf22OrderByPtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        pgSortParty = PgSortFactory.createParty(abb3Party, config.getSortConfig());
        addMultiSubPto(permuteParty, pgSortParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        permuteParty.init();
        pgSortParty.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(OrderByFnParam... params) {
        if (!isMalicious) {
            return new long[]{0, 0};
        }
        long bitTupleNum = 0, longTupleNum = 0;
        List<long[]> cost = new LinkedList<>();
        for (OrderByFnParam funParam : params) {
            int dataNum = CommonUtils.getByteLength(funParam.inputSize) << 3;
            int logDataNum = LongUtils.ceilLog2(dataNum);
            if (funParam.inputInBinaryForm) {
                cost.add(pgSortParty.setUsage(new PgSortFnParam(PgSortOp.SORT_PERMUTE_B, dataNum, funParam.keyDim + 1)));
                cost.add(permuteParty.setUsage(new PermuteFnParam(PermuteOp.APPLY_INV_B_B, dataNum, funParam.totalDim, logDataNum)));
            } else {
                int[] dims = new int[funParam.keyDim + 1];
                dims[0] = 1;
                Arrays.fill(dims, 1, dims.length, 64);
                cost.add(pgSortParty.setUsage(new PgSortFnParam(PgSortOp.SORT_A, dataNum, dims)));
                cost.add(permuteParty.setUsage(new PermuteFnParam(PermuteOp.APPLY_INV_A_A, dataNum, funParam.totalDim, 1)));
            }
        }
        for (long[] oneCost : cost) {
            bitTupleNum += oneCost[0];
            longTupleNum += oneCost[1];
        }
        return new long[]{bitTupleNum, longTupleNum};
    }

    @Override
    public void orderBy(TripletZ2Vector[] table, int[] keyIndex) throws MpcAbortException {
        preprocess(table, keyIndex);
        logPhaseInfo(PtoState.PTO_BEGIN, "orderBy for binary input");

        stopWatch.start();
        TripletZ2Vector[] sortInput = new TripletZ2Vector[keyIndex.length + 1];
        sortInput[0] = bInput[bInput.length - 1];
        z2cParty.noti(sortInput[0]);
        System.arraycopy(bInput, 0, sortInput, 1, keyIndex.length);
        TripletZ2Vector[] perm = pgSortParty.perGenAndSortOrigin(sortInput);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "sort to gen perm");

        stopWatch.start();
        TripletZ2Vector[] payload = permuteParty.applyInvPermutation(
            perm, Arrays.copyOfRange(bInput, keyIndex.length, bInput.length - 1));
        z2cParty.noti(sortInput[0]);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "permute payload");

        postprocess(sortInput, payload, table);
        logPhaseInfo(PtoState.PTO_END, "orderBy for binary input");
    }

    @Override
    public void orderBy(TripletLongVector[] table, int[] keyIndex) throws MpcAbortException {
        int keyDim = keyIndex.length;
        if(table[0].getNum() == 1){
            return;
        }
        logPhaseInfo(PtoState.PTO_BEGIN, "orderBy for arithmetic input");

        zl64cParty.open(table);

        stopWatch.start();
        TripletLongVector[] sortKeys = new TripletLongVector[keyDim + 1];
        sortKeys[0] = (TripletLongVector) zl64cParty.neg(table[table.length - 1]);
        zl64cParty.addi(sortKeys[0], zl64cParty.setPublicValue(LongVector.createOnes(sortKeys[0].getNum())));
        IntStream.range(0, keyDim).forEach(i -> sortKeys[i + 1] = table[keyIndex[i]]);
        int[] bitLen = new int[keyDim + 1];
        bitLen[0] = 1;
        Arrays.fill(bitLen, 1, keyDim + 1, 64);
        TripletLongVector perm = pgSortParty.perGen4MultiDim(sortKeys, bitLen);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "sort");

        zl64cParty.open(perm);

        stopWatch.start();
        TripletLongVector[] tmp = permuteParty.applyInvPermutation(perm, table);
        System.arraycopy(tmp, 0, table, 0, table.length);
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime(), "permutation");

        logPhaseInfo(PtoState.PTO_END, "orderBy for arithmetic input");
    }
}
