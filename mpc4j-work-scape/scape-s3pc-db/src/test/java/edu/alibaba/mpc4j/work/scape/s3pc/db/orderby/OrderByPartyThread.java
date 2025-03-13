package edu.alibaba.mpc4j.work.scape.s3pc.db.orderby;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * order-by party thread
 *
 * @author Feng Han
 * @date 2025/3/4
 */
public class OrderByPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderByPartyThread.class);
    /**
     * order-by party
     */
    private final OrderByParty orderByParty;
    /**
     * binary input
     */
    private final BitVector[] bInput;
    /**
     * key dimension for binary input
     */
    private final int[] bKeyInd;
    /**
     * arithmetic input
     */
    private final LongVector[] aInput;
    /**
     * key dimension for arithmetic input
     */
    private final int[] aKeyInd;
    /**
     * the binary output
     */
    private BitVector[] bOutput;
    /**
     * the arithmetic output
     */
    private LongVector[] aOutput;

    public OrderByPartyThread(OrderByParty orderByParty, BitVector[] bInput, int[] bKeyInd,
                              LongVector[] aInput, int[] aKeyInd) {
        this.orderByParty = orderByParty;
        this.bInput = bInput;
        this.bKeyInd = bKeyInd;
        this.aInput = aInput;
        this.aKeyInd = aKeyInd;
    }

    public BitVector[] getbOutput() {
        return bOutput;
    }

    public LongVector[] getaOutput() {
        return aOutput;
    }

    @Override
    public void run() {
        List<OrderByFnParam> params = new LinkedList<>();
        if (aInput != null) {
            params.add(new OrderByFnParam(false, aInput[0].getNum(), aKeyInd.length, aInput.length));
        }
        if (bInput != null) {
            params.add(new OrderByFnParam(true, bInput[0].bitNum(), bKeyInd.length, bInput.length));
        }
        try {
            long[] costTuple = orderByParty.setUsage(params.toArray(new OrderByFnParam[0]));
            orderByParty.init();

            TripletLongVector[] aShare = null;
            TripletZ2Vector[] bShare = null;
            if (aInput != null) {
//                if(orderByParty.getRpc().ownParty().getPartyId() == 0){
//                    aShare = (TripletLongVector[]) orderByParty.getAbb3Party().getLongParty().shareOwn(aInput);
//                }else{
//                    aShare = (TripletLongVector[]) orderByParty.getAbb3Party().getLongParty().shareOther(
//                        IntStream.range(0, aInput.length).map(i -> aInput[0].getNum()).toArray(), orderByParty.getRpc().getParty(0));
//                }

                aShare = Arrays.stream(aInput)
                    .map(ea -> (TripletLongVector) orderByParty.getAbb3Party().getLongParty().setPublicValue(ea))
                    .toArray(TripletLongVector[]::new);
            }
            if (bInput != null) {
                bShare = (TripletZ2Vector[]) orderByParty.getAbb3Party().getZ2cParty().setPublicValues(bInput);
            }

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            if (aInput != null) {
                orderByParty.orderBy(aShare, aKeyInd);
                aOutput = orderByParty.getAbb3Party().getLongParty().open(aShare);
            }
            if (bInput != null) {
                orderByParty.orderBy(bShare, bKeyInd);
                bOutput = orderByParty.getAbb3Party().getZ2cParty().open(bShare);
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info("P{} order-by process time: {}ms", orderByParty.getRpc().ownParty().getPartyId(), time);

            long usedBitTuple = costTuple[0] == 0 ? 0 : orderByParty.getAbb3Party().getTripletProvider().getZ2MtProvider().getAllTupleNum();
            long usedLongTuple = costTuple[1] == 0 ? 0 : orderByParty.getAbb3Party().getTripletProvider().getZl64MtProvider().getAllTupleNum();
            LOGGER.info("computed bitTupleNum:{}, actually used bitTupleNum:{} | computed longTupleNum:{}, actually used longTupleNum:{}",
                costTuple[0], usedBitTuple, costTuple[1], usedLongTuple);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
