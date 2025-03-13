package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * thread for random encoding party
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public class RandomEncodingPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomEncodingPartyThread.class);
    /**
     * join party
     */
    private final RandomEncodingParty encodingParty;
    /**
     * abb3 party
     */
    private final Abb3Party abb3Party;
    /**
     * input table
     */
    private final BitVector[][] plainTable;
    /**
     * input is sorted
     */
    private final boolean withDummy;
    /**
     * result
     */
    private BitVector[][] result;

    public RandomEncodingPartyThread(RandomEncodingParty encodingParty, BitVector[][] plainTable, boolean withDummy) {
        this.encodingParty = encodingParty;
        abb3Party = encodingParty.getAbb3Party();
        this.plainTable = plainTable;
        this.withDummy = withDummy;
    }

    public BitVector[][] getResult() {
        return result;
    }

    @Override
    public void run() {
        RandomEncodingFnParam param = new RandomEncodingFnParam(plainTable[0].length - 1, plainTable[0][0].bitNum(), plainTable[1][0].bitNum(), withDummy);
        try {
            long[] esTupleNums = encodingParty.setUsage(param);
            encodingParty.init();

            TripletZ2Vector[] left, right;
            if (abb3Party.ownParty().getPartyId() == 0) {
                left = abb3Party.getZ2cParty().shareOwn(plainTable[0]);
                right = abb3Party.getZ2cParty().shareOwn(plainTable[1]);
            } else {
                left = abb3Party.getZ2cParty().shareOther(
                    IntStream.range(0, plainTable[0].length).map(e -> plainTable[0][0].bitNum()).toArray(), abb3Party.getRpc().getParty(0));
                right = abb3Party.getZ2cParty().shareOther(
                    IntStream.range(0, plainTable[1].length).map(e -> plainTable[1][0].bitNum()).toArray(), abb3Party.getRpc().getParty(0));
            }
            TripletZ2Vector[][] res = encodingParty.getEncodingForTwoKeys(
                Arrays.copyOf(left, left.length - 1), left[left.length - 1],
                Arrays.copyOf(right, right.length - 1), right[right.length - 1],
                withDummy);
            result = new BitVector[][]{
                abb3Party.getZ2cParty().open(res[0]), abb3Party.getZ2cParty().open(res[1])
            };

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
