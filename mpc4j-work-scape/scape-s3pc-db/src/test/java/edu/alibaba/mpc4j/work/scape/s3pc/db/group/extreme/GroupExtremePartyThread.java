package edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam.GroupOp;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory.ExtremeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * group extreme party thread
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class GroupExtremePartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupExtremePartyThread.class);
    /**
     * join party
     */
    private final GroupExtremeParty groupExtremeParty;
    /**
     * abb3 party
     */
    private final Abb3Party abb3Party;
    /**
     * input table
     */
    private final BitVector[] plainTable;
    /**
     * input table
     */
    private final BitVector groupFlag;
    /**
     * input table
     */
    private final ExtremeType extremeType;
    /**
     * result
     */
    private BitVector[] result;

    public GroupExtremePartyThread(GroupExtremeParty groupExtremeParty, BitVector[] plainTable, BitVector groupFlag, ExtremeType extremeType) {
        this.groupExtremeParty = groupExtremeParty;
        abb3Party = groupExtremeParty.getAbb3Party();
        this.plainTable = plainTable;
        this.groupFlag = groupFlag;
        this.extremeType = extremeType;
    }

    public BitVector[] getResult() {
        return result;
    }

    @Override
    public void run() {
        GroupFnParam param = new GroupFnParam(GroupOp.EXTREME, plainTable.length, plainTable[0].bitNum());
        try {
            long[] esTupleNums = groupExtremeParty.setUsage(param);
            groupExtremeParty.init();

            TripletZ2Vector[] shareTab;
            TripletZ2Vector flag;
            if (abb3Party.ownParty().getPartyId() == 0) {
                shareTab = abb3Party.getZ2cParty().shareOwn(plainTable);
                flag = abb3Party.getZ2cParty().shareOwn(groupFlag);
            } else {
                shareTab = abb3Party.getZ2cParty().shareOther(
                    IntStream.range(0, plainTable.length).map(e -> plainTable[0].bitNum()).toArray(), abb3Party.getRpc().getParty(0));
                flag = abb3Party.getZ2cParty().shareOther(new int[]{groupFlag.bitNum()}, abb3Party.getRpc().getParty(0))[0];
            }
            TripletZ2Vector[] res = groupExtremeParty.groupExtreme(shareTab, flag, extremeType);
            result = abb3Party.getZ2cParty().open(res);

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
