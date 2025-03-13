package edu.alibaba.mpc4j.work.scape.s3pc.opf.agg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.agg.AggFnParam.AggOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * agg party thread
 *
 * @author Feng Han
 * @date 2025/2/27
 */
public class AggPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggPartyThread.class);
    /**
     * merge party
     */
    private final AggParty aggParty;
    /**
     * binary input data
     */
    private final BitVector[] plainBinaryInput;
    /**
     * binary flag data
     */
    private final BitVector plainBinaryFlag;
    /**
     * arithmetic input data
     */
    private final LongVector plainArithmeticInput;
    /**
     * arithmetic flag data
     */
    private final LongVector plainArithmeticFlag;
    /**
     * agg type
     */
    private final AggOp aggOp;
    /**
     * binary protocol output
     */
    private BitVector[] bOutput;
    /**
     * arithmetic protocol output
     */
    private LongVector aOutput;

    public AggPartyThread(AggParty aggParty, BitVector[] plainBinaryInput, BitVector plainBinaryFlag,
                          LongVector plainArithmeticInput, LongVector plainArithmeticFlag, AggOp aggOp) {
        this.aggParty = aggParty;
        this.plainBinaryInput = plainBinaryInput;
        this.plainBinaryFlag = plainBinaryFlag;
        this.plainArithmeticInput = plainArithmeticInput;
        this.plainArithmeticFlag = plainArithmeticFlag;
        this.aggOp = aggOp;
    }

    public BitVector[] bOutput() {
        return bOutput;
    }

    public LongVector aOutput() {
        return aOutput;
    }

    @Override
    public void run() {
        List<AggFnParam> params = new LinkedList<>();
        if (plainBinaryFlag != null) {
            params.add(new AggFnParam(true, aggOp, plainBinaryInput.length, plainBinaryFlag.bitNum()));
        }
        if (plainArithmeticFlag != null) {
            params.add(new AggFnParam(false, aggOp, 1, plainArithmeticFlag.getNum()));
        }
        try {
            long[] es = aggParty.setUsage(params.toArray(new AggFnParam[0]));
            aggParty.init();

            LOGGER.info("testing agg");
            if (plainBinaryFlag != null) {
                TripletZ2Vector[] input = (TripletZ2Vector[]) aggParty.getAbb3Party().getZ2cParty().setPublicValues(plainBinaryInput);
                TripletZ2Vector flag = (TripletZ2Vector) aggParty.getAbb3Party().getZ2cParty().setPublicValues(
                    new BitVector[]{plainBinaryFlag})[0];
                TripletZ2Vector[] outShare = aggParty.agg(input, flag, aggOp);
                bOutput = aggParty.getAbb3Party().getZ2cParty().open(outShare);
            }
            if (plainArithmeticFlag != null) {
                TripletLongVector input = (TripletLongVector) aggParty.getAbb3Party().getLongParty().setPublicValue(plainArithmeticInput);
                TripletLongVector flag = (TripletLongVector) aggParty.getAbb3Party().getLongParty().setPublicValue(plainArithmeticFlag);
                TripletLongVector outShare = aggParty.agg(input, flag, aggOp);
                aOutput = aggParty.getAbb3Party().getLongParty().open(outShare)[0];
            }

            RpZ2Mtp z2Mtp = aggParty.getAbb3Party().getTripletProvider().getZ2MtProvider();
            RpLongMtp zl64Mtp = aggParty.getAbb3Party().getTripletProvider().getZl64MtProvider();
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
