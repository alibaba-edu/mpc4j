package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PkPk join party thread
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class PkPkJoinPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkPkJoinPartyThread.class);
    /**
     * join party
     */
    private final PkPkJoinParty joinParty;
    /**
     * abb3 party
     */
    private final Abb3Party abb3Party;
    /**
     * input table
     */
    private final BitVector[][] plainTable;
    /**
     * left key index
     */
    private final int[] leftKeyIndex;
    /**
     * right key index
     */
    private final int[] rightKeyIndex;
    /**
     * input is sorted
     */
    private final boolean isInputSorted;
    /**
     * result
     */
    private BitVector[] result;

    public PkPkJoinPartyThread(PkPkJoinParty joinParty, BitVector[][] plainTable, int[] leftKeyIndex, int[] rightKeyIndex, boolean isInputSorted) {
        this.joinParty = joinParty;
        abb3Party = joinParty.getAbb3Party();
        this.plainTable = plainTable;
        this.leftKeyIndex = leftKeyIndex;
        this.rightKeyIndex = rightKeyIndex;
        this.isInputSorted = isInputSorted;
    }

    public BitVector[] getResult() {
        return result;
    }

    @Override
    public void run() {
        PkPkJoinFnParam param = new PkPkJoinFnParam(
            plainTable[0][0].bitNum(), plainTable[1][0].bitNum(),
            leftKeyIndex.length,
            plainTable[0].length - 1 - leftKeyIndex.length,
            plainTable[1].length - 1 - rightKeyIndex.length, isInputSorted);
        try {
            long[] esTupleNums = joinParty.setUsage(param);
            joinParty.init();

            TripletZ2Vector[] left = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(plainTable[0]);
            TripletZ2Vector[] right = (TripletZ2Vector[]) abb3Party.getZ2cParty().setPublicValues(plainTable[1]);
            TripletZ2Vector[] res = joinParty.primaryKeyInnerJoin(left, right, leftKeyIndex, rightKeyIndex, true, isInputSorted);
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
