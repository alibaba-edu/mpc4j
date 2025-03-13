package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.hzf22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.AbstractThreePartyDbPto;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.SemiJoinFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.general.GeneralSemiJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.InputProcessUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteOperations.PermuteOp;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.TraversalFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalParty;

import java.util.Arrays;

/**
 * HZF22 general semi-join party.
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class Hzf22GeneralSemiJoinParty extends AbstractThreePartyDbPto implements GeneralSemiJoinParty {
    /**
     * permute party
     */
    protected final PermuteParty permuteParty;
    /**
     * oblivious traversal party
     */
    protected final TraversalParty traversalParty;
    /**
     * the party to fill the injective function into a permutation
     */
    protected final SortSignParty sortSignParty;

    public Hzf22GeneralSemiJoinParty(Abb3Party abb3Party, Hzf22GeneralSemiJoinConfig config){
        super(Hzf22GeneralSemiJoinPtoDesc.getInstance(), abb3Party, config);
        permuteParty = PermuteFactory.createParty(abb3Party, config.getPermuteConfig());
        traversalParty = TraversalFactory.createParty(abb3Party, config.getTraversalConfig());
        sortSignParty = SortSignFactory.createParty(abb3Party, config.getSortSignConfig());
        addMultiSubPto(permuteParty, traversalParty, sortSignParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        permuteParty.init();
        traversalParty.init();
        sortSignParty.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(SemiJoinFnParam... params) {
        long[] tuples = new long[]{0, 0};
        if (isMalicious) {
            for (SemiJoinFnParam param : params) {
                int totalNum = param.leftDataNum + param.rightDataNum;
                long[] traversalTuple = traversalParty.setUsage(new TraversalFnParam(TraversalOperations.TraversalOp.TRAVERSAL_A, totalNum, 2));
                long[] permuteTuple = permuteParty.setUsage(new PermuteFnParam(PermuteOp.COMPOSE_A_A, param.rightDataNum, 1, 64));
                long[] sortSignTuple = sortSignParty.setUsage(new SortSignFnParam(param.isInputSorted, param.keyDim, param.leftDataNum, param.rightDataNum));
                tuples[0] += traversalTuple[0] + permuteTuple[0] + sortSignTuple[0];
                tuples[1] += traversalTuple[1] + permuteTuple[1] + sortSignTuple[1];
                abb3Party.updateNum(0, param.rightDataNum);
                tuples[1] += param.rightDataNum;
            }
        }
        return tuples;
    }

    @Override
    public TripletLongVector semiJoin(TripletLongVector[] x, TripletLongVector[] y,
                                      int[] xKeyIndex, int[] yKeyIndex, boolean inputIsSorted) throws MpcAbortException {
        TripletLongVector[] newLeft = InputProcessUtils.reshapeInput(x, xKeyIndex);
        TripletLongVector[] newRight = InputProcessUtils.reshapeInput(y, yKeyIndex);
        logPhaseInfo(PtoState.PTO_BEGIN, "general semi-join");

        stopWatch.start();
        TripletLongVector[] sortRes = sortSignParty.preSort(
            Arrays.copyOf(newLeft, xKeyIndex.length), Arrays.copyOf(newRight, yKeyIndex.length),
            newLeft[newLeft.length - 1], newRight[newRight.length - 1], inputIsSorted);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime(), "preSort");

        stopWatch.start();
        TripletLongVector kPai = sortRes[4];
        TripletLongVector eSign4Right, rightEqualSign;
        eSign4Right = traversalParty.traversalPrefix(new TripletLongVector[]{sortRes[0], sortRes[1]}, false, false, true, false)[0];
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime(), "traversalPrefix");

        stopWatch.start();
        // 如果没有预先排序，那么可以直接用pai将得到的结果置换回原来的位置
        TripletLongVector eSignIntoOrigin = permuteParty.composePermutation(kPai, eSign4Right)[0];
        rightEqualSign = eSignIntoOrigin.copyOfRange(x[0].getNum(), x[0].getNum() + y[0].getNum());
        TripletLongVector res = abb3Party.getLongParty().mul(y[y.length - 1], rightEqualSign);
        logStepInfo(PtoState.PTO_STEP, 3, 3, resetAndGetTime(), "composePermutation");

        logPhaseInfo(PtoState.PTO_END, "general semi-join");
        return res;
    }
}
