package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * merge party thread
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class MergePartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergePartyThread.class);
    /**
     * merge party
     */
    private final MergeParty mergeParty;
    /**
     * first input data
     */
    private final BitVector[] firstPlainInput;
    /**
     * second input data
     */
    private final BitVector[] secondPlainInput;
    /**
     * protocol output
     */
    private BitVector[] output;

    public MergePartyThread(MergeParty mergeParty, BitVector[] firstPlainInput, BitVector[] secondPlainInput) {
        this.mergeParty = mergeParty;
        this.firstPlainInput = firstPlainInput;
        this.secondPlainInput = secondPlainInput;
    }

    public BitVector[] getOutput() {
        return output;
    }

    @Override
    public void run() {
        try {
            long[] es = mergeParty.setUsage(new MergeFnParam(firstPlainInput[0].bitNum(), secondPlainInput[0].bitNum(), firstPlainInput.length));
            mergeParty.init();

            LOGGER.info("testing merge");
            TripletZ2Vector[] leftInput = (TripletZ2Vector[]) mergeParty.getAbb3Party().getZ2cParty().setPublicValues(firstPlainInput);
            TripletZ2Vector[] rightInput = (TripletZ2Vector[]) mergeParty.getAbb3Party().getZ2cParty().setPublicValues(secondPlainInput);
            TripletZ2Vector[] outShare = mergeParty.merge(leftInput, rightInput);
            output = mergeParty.getAbb3Party().getZ2cParty().open(outShare);

            RpZ2Mtp z2Mtp = mergeParty.getAbb3Party().getTripletProvider().getZ2MtProvider();
            RpLongMtp zl64Mtp = mergeParty.getAbb3Party().getTripletProvider().getZl64MtProvider();
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
