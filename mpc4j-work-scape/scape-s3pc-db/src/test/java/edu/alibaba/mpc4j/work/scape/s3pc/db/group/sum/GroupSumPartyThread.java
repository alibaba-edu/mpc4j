package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam.GroupOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * group sum party thread
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class GroupSumPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupSumPartyThread.class);
    /**
     * join party
     */
    private final GroupSumParty groupSumParty;
    /**
     * abb3 party
     */
    private final Abb3Party abb3Party;
    /**
     * input table
     */
    private final LongVector[] plainTable;
    /**
     * input table
     */
    private final LongVector groupFlag;
    /**
     * result
     */
    private LongVector[] result;

    public GroupSumPartyThread(GroupSumParty groupSumParty, LongVector[] plainTable, LongVector groupFlag) {
        this.groupSumParty = groupSumParty;
        abb3Party = groupSumParty.getAbb3Party();
        this.plainTable = plainTable;
        this.groupFlag = groupFlag;
    }

    public LongVector[] getResult() {
        return result;
    }

    @Override
    public void run() {
        GroupFnParam param = new GroupFnParam(GroupOp.SUM, plainTable.length, plainTable[0].getNum());
        try {
            long[] esTupleNums = groupSumParty.setUsage(param);
            groupSumParty.init();

            TripletLongVector[] shareTab;
            TripletLongVector flag;
            if (abb3Party.ownParty().getPartyId() == 0) {
                shareTab = (TripletLongVector[]) abb3Party.getLongParty().shareOwn(plainTable);
                flag = (TripletLongVector) abb3Party.getLongParty().shareOwn(groupFlag);
            } else {
                shareTab = (TripletLongVector[]) abb3Party.getLongParty().shareOther(
                    IntStream.range(0, plainTable.length).map(e -> plainTable[0].getNum()).toArray(), abb3Party.getRpc().getParty(0));
                flag = (TripletLongVector) abb3Party.getLongParty().shareOther(new int[]{groupFlag.getNum()}, abb3Party.getRpc().getParty(0))[0];
            }
            TripletLongVector[] res = groupSumParty.groupSum(shareTab, flag);
            result = abb3Party.getLongParty().open(res);

            RpZ2Mtp z2Mtp = abb3Party.getTripletProvider().getZ2MtProvider();
            RpLongMtp zl64Mtp = abb3Party.getTripletProvider().getZl64MtProvider();
            long usedBitTuple = z2Mtp == null ? 0 : z2Mtp.getAllTupleNum();
            long usedLongTuple = zl64Mtp == null ? 0 : zl64Mtp.getAllTupleNum();
            LOGGER.info("computed bitTupleNum:{}, actually used bitTupleNum:{} | computed longTupleNum:{}, actually used longTupleNum:{}",
                esTupleNums[0], usedBitTuple, esTupleNums[1], usedLongTuple);
        } catch (MpcAbortException e) {
            e.printStackTrace();
            throw new RuntimeException("error");
        }
    }
}
