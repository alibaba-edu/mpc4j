package edu.alibaba.mpc4j.work.scape.s3pc.db.join.general;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * general join party thread
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class GeneralJoinPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralJoinPartyThread.class);
    /**
     * general join party
     */
    private final GeneralJoinParty joinParty;
    /**
     * abb3 party
     */
    private final Abb3Party abb3Party;
    /**
     * left table
     */
    private final LongVector[] leftTable;
    /**
     * right table
     */
    private final LongVector[] rightTable;
    /**
     * left key index
     */
    private final int[] leftKeyIndex;
    /**
     * right key index
     */
    private final int[] rightKeyIndex;
    /**
     * the upper bound of the join result size
     */
    private final int resultUpperBound;
    /**
     * the input is sorted in the order of join_key and valid_flag
     */
    private final boolean inputIsSorted;
    /**
     * result
     */
    private LongVector[] resPlain;


    public GeneralJoinPartyThread(GeneralJoinParty joinParty,
                                  LongVector[] leftTable, LongVector[] rightTable, int[] leftKeyIndex, int[] rightKeyIndex,
                                  int resultUpperBound, boolean inputIsSorted) {
        this.joinParty = joinParty;
        abb3Party = joinParty.getAbb3Party();
        this.leftTable = leftTable;
        this.rightTable = rightTable;
        this.leftKeyIndex = leftKeyIndex;
        this.rightKeyIndex = rightKeyIndex;
        this.resultUpperBound = resultUpperBound;
        this.inputIsSorted = inputIsSorted;
    }

    public LongVector[] getPlainRes() {
        return resPlain;
    }

    @Override
    public void run() {
        GeneralJoinFnParam param = new GeneralJoinFnParam(
            inputIsSorted,
            leftTable[0].getNum(),
            rightTable[0].getNum(),
            leftKeyIndex.length,
            leftTable.length - 1 - leftKeyIndex.length,
            rightTable.length - 1 - rightKeyIndex.length,
            resultUpperBound
        );
        try {
            long[] es = joinParty.setUsage(param);
            joinParty.init();

            TripletLongVector[] left, right;
            if (abb3Party.getRpc().ownParty().getPartyId() == 0) {
                left = (TripletLongVector[]) abb3Party.getLongParty().shareOwn(leftTable);
                right = (TripletLongVector[]) abb3Party.getLongParty().shareOwn(rightTable);
            } else {
                left = (TripletLongVector[]) abb3Party.getLongParty().shareOther(
                    IntStream.range(0, leftTable.length).map(e -> leftTable[0].getNum()).toArray(), abb3Party.getRpc().getParty(0));
                right = (TripletLongVector[]) abb3Party.getLongParty().shareOther(
                    IntStream.range(0, rightTable.length).map(e -> rightTable[0].getNum()).toArray(), abb3Party.getRpc().getParty(0));
            }
            TripletLongVector[] res = joinParty.innerJoin(left, right, leftKeyIndex, rightKeyIndex, resultUpperBound, inputIsSorted);
            resPlain = abb3Party.getLongParty().open(res);

            RpZ2Mtp z2Mtp = abb3Party.getTripletProvider().getZ2MtProvider();
            RpLongMtp zl64Mtp = abb3Party.getTripletProvider().getZl64MtProvider();
            long usedBitTuple = z2Mtp == null ? 0 : z2Mtp.getAllTupleNum();
            long usedLongTuple = zl64Mtp == null ? 0 : zl64Mtp.getAllTupleNum();
            LOGGER.info("computed bitTupleNum:{}, actually used bitTupleNum:{} | computed longTupleNum:{}, actually used longTupleNum:{}",
                es[0], usedBitTuple, es[1], usedLongTuple);
        } catch (MpcAbortException e) {
            e.printStackTrace();
            throw new RuntimeException("error");
        }
    }

}

