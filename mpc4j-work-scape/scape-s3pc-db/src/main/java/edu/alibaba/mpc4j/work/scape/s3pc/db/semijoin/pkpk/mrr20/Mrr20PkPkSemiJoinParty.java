package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.mrr20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinFactory;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.AbstractPkPkSemiJoinParty;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.SemiJoinFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.PkPkSemiJoinParty;

import java.util.stream.IntStream;

/**
 * MRR20 pk-pk semi-join party.
 * directly invoking the PkPk join protocol without payload is sufficient
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class Mrr20PkPkSemiJoinParty extends AbstractPkPkSemiJoinParty implements PkPkSemiJoinParty {
    /**
     * semi-join party
     */
    protected final PkPkJoinParty joinParty;

    public Mrr20PkPkSemiJoinParty(Abb3Party abb3Party, Mrr20PkPkSemiJoinConfig config) {
        super(Mrr20PkPkSemiJoinPtoDesc.getInstance(), abb3Party, config);
        this.joinParty = PkPkJoinFactory.createParty(abb3Party, config.getJoinConfig());
        addMultiSubPto(joinParty);
    }

    @Override
    public void init() throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        abb3Party.init();
        joinParty.init();
        initState();
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public long[] setUsage(SemiJoinFnParam... params) {
        long[] tupleNum = new long[]{0, 0};
        for (SemiJoinFnParam param : params) {
            long[] tmpNum = joinParty.setUsage(
                new PkPkJoinFnParam(param.leftDataNum, param.rightDataNum, param.keyDim, 0, 0, param.isInputSorted));
            tupleNum[0] += tmpNum[0];
            tupleNum[1] += tmpNum[1];
        }
        return tupleNum;
    }

    @Override
    public TripletZ2Vector semiJoin(TripletZ2Vector[] x, TripletZ2Vector[] y, int[] xKeyIndex, int[] yKeyIndex, boolean withDummy, boolean inputIsSorted) throws MpcAbortException {
        inputProcess(x, y, xKeyIndex, yKeyIndex, withDummy, inputIsSorted);
        int[] keyDims = IntStream.range(0, xKeyIndex.length).toArray();
        TripletZ2Vector[] res = joinParty.primaryKeyInnerJoin(newLeft, newRight, keyDims, keyDims, withDummy, inputIsSorted);
        return res[res.length - 1];
    }

}
