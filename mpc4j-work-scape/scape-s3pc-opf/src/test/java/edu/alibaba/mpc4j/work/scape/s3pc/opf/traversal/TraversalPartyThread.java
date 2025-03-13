package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.AcTraversalRes;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.BcTraversalRes;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.TraversalFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalOperations.TraversalOp;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 3p traversal party thread
 *
 * @author Feng Han
 * @date 2024/03/05
 */
public class TraversalPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraversalPartyThread.class);
    /**
     * traverse party
     */
    private final TraversalParty traversalParty;
    /**
     * operations to be tested
     */
    private final TraversalOp[] ops;
    /**
     * test data size
     */
    private final int dataNum;
    /**
     * dimension length in bit
     */
    private final int bitDim;
    /**
     * dimension length of input long element
     */
    private final int longDim;
    /**
     * result of arithmetic permute
     */
    private final List<AcTraversalRes> acRes = new LinkedList<>();
    /**
     * result of binary permute
     */
    private final List<BcTraversalRes> bcRes = new LinkedList<>();

    public TraversalPartyThread(TraversalParty traversalParty, TraversalOp[] ops, int dataNum, int bitDim, int longDim) {
        this.traversalParty = traversalParty;
        this.ops = ops;
        this.dataNum = dataNum;
        this.bitDim = bitDim;
        this.longDim = longDim;
    }

    public List<AcTraversalRes> getAcRes() {
        return acRes;
    }

    public List<BcTraversalRes> getBcRes() {
        return bcRes;
    }

    public void testAcTraversal(TraversalOp op) throws MpcAbortException {
        Assert.assertEquals(op, TraversalOp.TRAVERSAL_A);
        SecureRandom secureRandom = new SecureRandom();

        for(int i = 0; i < 4; i++){
            boolean isInv = (i & 1) == 0;
            boolean theta = i >= 2;
            LongVector[] inputPlain = null;
            LongVector flag = null;
            TripletLongVector[] inputShare = new TripletLongVector[longDim + 1];

            if(traversalParty.getRpc().ownParty().getPartyId() == 0){
                inputPlain = IntStream.range(0, longDim).mapToObj(j -> LongVector.createRandom(dataNum, secureRandom)).toArray(LongVector[]::new);
                long[] tmp = IntStream.range(0, dataNum).mapToLong(j -> secureRandom.nextBoolean() ? 1L : 0L).toArray();
                if(isInv){
                    tmp[tmp.length - 1] = 0L;
                }else{
                    tmp[0] = 0L;
                }
                flag = LongVector.create(tmp);
                System.arraycopy(traversalParty.getAbb3Party().getLongParty().shareOwn(inputPlain), 0, inputShare, 0, longDim);
                inputShare[longDim] = (TripletLongVector) traversalParty.getAbb3Party().getLongParty().shareOwn(flag);
            }else{
                System.arraycopy(traversalParty.getAbb3Party().getLongParty().shareOther(
                        IntStream.range(0, longDim).map(x -> dataNum).toArray(), traversalParty.getRpc().getParty(0)),
                    0, inputShare, 0, longDim);
                inputShare[longDim] = (TripletLongVector) traversalParty.getAbb3Party().getLongParty().shareOther(dataNum, traversalParty.getRpc().getParty(0));
            }

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            TripletLongVector[] output = traversalParty.traversalPrefix(inputShare, isInv, false, false, theta);
            LongVector[] outputPlain = traversalParty.getAbb3Party().getLongParty().open(output);
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info("P{} op:[{}] process time: {}ms", traversalParty.getRpc().ownParty().getPartyId(), op.name(), time);

            if(traversalParty.getRpc().ownParty().getPartyId() == 0){
                acRes.add(new AcTraversalRes(isInv, theta, flag, inputPlain, outputPlain));
            }
        }
    }

    public void testBcTraversal(TraversalOp op) throws MpcAbortException{
        Assert.assertEquals(op, TraversalOp.TRAVERSAL_B);
        SecureRandom secureRandom = new SecureRandom();

        boolean[] isInvArray = new boolean[]{true, false};
        for(boolean isInv : isInvArray){
            BitVector[] inputPlain = null;
            BitVector flag = null;
            TripletZ2Vector[] inputShare;
            TripletZ2Vector flagShare;

            if(traversalParty.getRpc().ownParty().getPartyId() == 0){
                inputPlain = IntStream.range(0, bitDim).mapToObj(j -> BitVectorFactory.createRandom(dataNum, secureRandom)).toArray(BitVector[]::new);
                flag = BitVectorFactory.createRandom(dataNum, secureRandom);
                if(isInv){
                    flag.set(dataNum - 1, false);
                }else{
                    flag.set(0, false);
                }
                inputShare = traversalParty.getAbb3Party().getZ2cParty().shareOwn(inputPlain);
                flagShare = traversalParty.getAbb3Party().getZ2cParty().shareOwn(flag);
            }else{
                inputShare = traversalParty.getAbb3Party().getZ2cParty().shareOther(
                    IntStream.range(0, bitDim).map(x -> dataNum).toArray(), traversalParty.getRpc().getParty(0));
                flagShare = traversalParty.getAbb3Party().getZ2cParty().shareOther(
                    dataNum, traversalParty.getRpc().getParty(0));
            }

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            TripletZ2Vector[] output = traversalParty.traversalPrefix(inputShare, flagShare, false, isInv);
            BitVector[] outputPlain = traversalParty.getAbb3Party().getZ2cParty().open(output);
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info("P{} op:[{}] process time: {}ms", traversalParty.getRpc().ownParty().getPartyId(), op.name(), time);

            if(traversalParty.getRpc().ownParty().getPartyId() == 0){
                bcRes.add(new BcTraversalRes(isInv, flag, inputPlain, outputPlain));
            }
        }
    }

    @Override
    public void run() {
        try {
            List<TraversalFnParam> params = new LinkedList<>();
            for(TraversalOp op : ops){
                if(op.equals(TraversalOp.TRAVERSAL_A)){
                    IntStream.range(0, 4).forEach(i -> params.add(new TraversalFnParam(op, dataNum, longDim)));
                }else{
                    IntStream.range(0, 2).forEach(i -> params.add(new TraversalFnParam(op, dataNum, bitDim)));
                }
            }
            long[] costTuple = traversalParty.setUsage(params.toArray(new TraversalFnParam[0]));
            traversalParty.getAbb3Party().init();
            traversalParty.init();

            for (TraversalOp op : ops) {
                switch (op) {
                    case TRAVERSAL_A:
                        testAcTraversal(op);
                        break;
                    case TRAVERSAL_B:
                        testBcTraversal(op);
                        break;
                    default:
                        throw new IllegalArgumentException(op + " is not a TraversalOp");
                }
            }
            long usedBitTuple = costTuple[0] == 0 ? 0 : traversalParty.getAbb3Party().getTripletProvider().getZ2MtProvider().getAllTupleNum();
            long usedLongTuple = costTuple[1] == 0 ? 0 : traversalParty.getAbb3Party().getTripletProvider().getZl64MtProvider().getAllTupleNum();
            LOGGER.info("computed bitTupleNum:{}, actually used bitTupleNum:{} | computed longTupleNum:{}, actually used longTupleNum:{}",
                costTuple[0], usedBitTuple, costTuple[1], usedLongTuple);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
