package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkfk;

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
 * PkFk join party thread
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class PkFkJoinPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkFkJoinPartyThread.class);
    /**
     * PkFk join party
     */
    private final PkFkJoinParty joinParty;
    /**
     * abb3 party
     */
    private final Abb3Party abb3Party;
    /**
     * left table
     */
    private final LongVector[] uTable;
    /**
     * right table
     */
    private final LongVector[] nuTable;
    /**
     * left key index
     */
    private final int[] uKeyIndex;
    /**
     * right key index
     */
    private final int[] nuKeyIndex;
    /**
     * the input is sorted in the order of join_key and valid_flag
     */
    private final boolean inputIsSorted;
    /**
     * result
     */
    private LongVector[] resPlain;

    public PkFkJoinPartyThread(PkFkJoinParty joinParty, LongVector[] uTable, LongVector[] nuTable,
                               int[] uKeyIndex, int[] nuKeyIndex, boolean inputIsSorted) {
        this.joinParty = joinParty;
        abb3Party = joinParty.getAbb3Party();
        this.uTable = uTable;
        this.nuTable = nuTable;
        this.uKeyIndex = uKeyIndex;
        this.nuKeyIndex = nuKeyIndex;
        this.inputIsSorted = inputIsSorted;
    }

    public LongVector[] getPlainRes() {
        return resPlain;
    }

    @Override
    public void run() {
        PkFkJoinFnParam param = new PkFkJoinFnParam(inputIsSorted, uTable[0].getNum(), nuTable[0].getNum(),
            uKeyIndex.length,
            uTable.length - 1 - uKeyIndex.length,
            nuTable.length - 1 - nuKeyIndex.length);
        try {
            long[] es = joinParty.setUsage(param);
            joinParty.init();

            TripletLongVector[] left, right;
            if (abb3Party.getRpc().ownParty().getPartyId() == 0) {
                left = (TripletLongVector[]) abb3Party.getLongParty().shareOwn(uTable);
                right = (TripletLongVector[]) abb3Party.getLongParty().shareOwn(nuTable);
            } else {
                left = (TripletLongVector[]) abb3Party.getLongParty().shareOther(
                    IntStream.range(0, uTable.length).map(e -> uTable[0].getNum()).toArray(), abb3Party.getRpc().getParty(0));
                right = (TripletLongVector[]) abb3Party.getLongParty().shareOther(
                    IntStream.range(0, nuTable.length).map(e -> nuTable[0].getNum()).toArray(), abb3Party.getRpc().getParty(0));
            }
            TripletLongVector[] res = joinParty.innerJoin(left, right, uKeyIndex, nuKeyIndex, inputIsSorted);
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

