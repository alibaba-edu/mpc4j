package edu.alibaba.mpc4j.work.scape.s3pc.db.group;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupFnParam.GroupOp;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * party thread for group flag computation
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public class GroupPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupPartyThread.class);
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
    private final BitVector[] plainTableBinary;
    /**
     * input table
     */
    private final int[] binaryKeyDim;
    /**
     * input table
     */
    private final LongVector[] plainTableArithmetic;
    /**
     * input table
     */
    private final int[] arithmeticKeyDim;
    /**
     * result
     */
    private BitVector resultBinary;

    /**
     * result
     */
    private LongVector arithmeticResult;

    public GroupPartyThread(GroupExtremeParty groupExtremeParty,
                            BitVector[] plainTableBinary, int[] binaryKeyDim,
                            LongVector[] plainTableArithmetic, int[] arithmeticKeyDim) {
        this.groupExtremeParty = groupExtremeParty;
        abb3Party = groupExtremeParty.getAbb3Party();
        this.plainTableBinary = plainTableBinary;
        this.binaryKeyDim = binaryKeyDim;
        this.plainTableArithmetic = plainTableArithmetic;
        this.arithmeticKeyDim = arithmeticKeyDim;
    }

    public BitVector getBinaryResult() {
        return resultBinary;
    }

    public LongVector getArithmeticResult() {
        return arithmeticResult;
    }

    @Override
    public void run() {
        List<GroupFnParam> paramList = new LinkedList<>();
        if (arithmeticKeyDim != null && arithmeticKeyDim.length > 0) {
            paramList.add(new GroupFnParam(GroupOp.A_GROUP_FLAG, arithmeticKeyDim.length, plainTableArithmetic[0].getNum()));
        }
        if (binaryKeyDim != null && binaryKeyDim.length > 0) {
            paramList.add(new GroupFnParam(GroupOp.B_GROUP_FLAG, binaryKeyDim.length, plainTableBinary[0].bitNum()));
        }
        GroupFnParam[] params = paramList.toArray(GroupFnParam[]::new);
        try {
            long[] esTupleNums = groupExtremeParty.setUsage(params);
            groupExtremeParty.init();

            if (arithmeticKeyDim != null && arithmeticKeyDim.length > 0) {
                TripletLongVector[] shareTab;
                if (abb3Party.ownParty().getPartyId() == 0) {
                    shareTab = (TripletLongVector[]) abb3Party.getLongParty().shareOwn(plainTableArithmetic);
                } else {
                    shareTab = (TripletLongVector[]) abb3Party.getLongParty().shareOther(
                        IntStream.range(0, plainTableArithmetic.length).map(e -> plainTableArithmetic[0].getNum()).toArray(), abb3Party.getRpc().getParty(0));
                }
                TripletLongVector res = groupExtremeParty.getGroupFlag(shareTab, arithmeticKeyDim);
                arithmeticResult = abb3Party.getLongParty().open(res)[0];
            }

            if (binaryKeyDim != null && binaryKeyDim.length > 0) {
                TripletZ2Vector[] shareTab;
                if (abb3Party.ownParty().getPartyId() == 0) {
                    shareTab = abb3Party.getZ2cParty().shareOwn(plainTableBinary);
                } else {
                    shareTab = abb3Party.getZ2cParty().shareOther(
                        IntStream.range(0, plainTableBinary.length).map(e -> plainTableBinary[0].bitNum()).toArray(), abb3Party.getRpc().getParty(0));
                }
                TripletZ2Vector res = groupExtremeParty.getGroupFlag(shareTab, binaryKeyDim);
                resultBinary = abb3Party.getZ2cParty().open(new TripletZ2Vector[]{res})[0];
            }

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
