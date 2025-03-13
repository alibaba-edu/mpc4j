package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * sortSign test thread
 *
 * @author Feng Han
 * @date 2025/2/20
 */
public class SortSignPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SortSignPartyThread.class);
    /**
     * fillPermutation party
     */
    private final SortSignParty sortSignParty;
    /**
     * input is sorted
     */
    private final boolean inputIsSorted;
    /**
     * input table
     */
    private final LongVector[][] plainInputTable;
    /**
     * the dimension of key
     */
    private final int keyDim;
    /**
     * protocol output
     */
    private LongVector[] output;

    public SortSignPartyThread(SortSignParty sortSignParty, boolean inputIsSorted, LongVector[][] plainInputTable, int keyDim) {
        this.sortSignParty = sortSignParty;
        this.inputIsSorted = inputIsSorted;
        this.plainInputTable = plainInputTable;
        this.keyDim = keyDim;
    }

    public LongVector[] getOutput() {
        return output;
    }

    private void testSortSign() throws MpcAbortException {
        LOGGER.info("testing sortSign");
        TripletLongVector[] leftInput, rightInput;

        if (sortSignParty.ownParty().getPartyId() == 0) {
            leftInput = (TripletLongVector[]) sortSignParty.getAbb3Party().getLongParty().shareOwn(plainInputTable[0]);
            rightInput = (TripletLongVector[]) sortSignParty.getAbb3Party().getLongParty().shareOwn(plainInputTable[1]);
        } else {
            int[] leftSize = new int[keyDim + 1];
            Arrays.fill(leftSize, plainInputTable[0][0].getNum());
            leftInput = (TripletLongVector[]) sortSignParty.getAbb3Party().getLongParty().shareOther(
                leftSize, sortSignParty.getRpc().getParty(0));
            int[] rightSize = new int[keyDim + 1];
            Arrays.fill(rightSize, plainInputTable[1][0].getNum());
            rightInput = (TripletLongVector[]) sortSignParty.getAbb3Party().getLongParty().shareOther(
                rightSize, sortSignParty.getRpc().getParty(0));
        }
        TripletLongVector[] outShare = sortSignParty.preSort(
            Arrays.copyOf(leftInput, keyDim), Arrays.copyOf(rightInput, keyDim),
            leftInput[keyDim], rightInput[keyDim], inputIsSorted);
        output = sortSignParty.getAbb3Party().getLongParty().open(outShare);
    }

    @Override
    public void run() {
        try {
            long[] es = sortSignParty.setUsage(new SortSignFnParam(inputIsSorted, keyDim, plainInputTable[0][0].getNum(), plainInputTable[1][0].getNum()));
            sortSignParty.init();
            testSortSign();

            RpZ2Mtp z2Mtp = sortSignParty.getAbb3Party().getTripletProvider().getZ2MtProvider();
            RpLongMtp zl64Mtp = sortSignParty.getAbb3Party().getTripletProvider().getZl64MtProvider();
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
