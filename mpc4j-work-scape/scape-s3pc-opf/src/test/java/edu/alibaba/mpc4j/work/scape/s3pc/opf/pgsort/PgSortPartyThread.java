package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortOperations.PgSortOp;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 3p sorting party thread
 *
 * @author Feng Han
 * @date 2024/03/01
 */
public class PgSortPartyThread extends Thread{
    private static final Logger LOGGER = LoggerFactory.getLogger(PgSortPartyThread.class);
    /**
     * max group number of our test
     */
    private static final int GROUP_UPPER_NUM = 4;
    /**
     * sort party
     */
    private final PgSortParty sortParty;
    /**
     * input data size
     */
    private final int dataNum;
    /**
     * the sum of the bits for one input data
     */
    private final int totalBitDim;
    /**
     * the original input should also be sorted
     */
    private final boolean needSortRes;
    /**
     * the input data
     */
    private BigInteger[] input;
    /**
     * the input dimension
     */
    private int[] inputDim;
    /**
     * the permutation
     */
    private int[] pai;
    /**
     * the sorted output
     */
    private BigInteger[] output;

    public PgSortPartyThread(PgSortParty sortParty, int dataNum, int totalBitDim, boolean needSortRes) {
        this.sortParty = sortParty;
        this.dataNum = dataNum;
        MathPreconditions.checkPositiveInRangeClosed("totalBitDim", totalBitDim, 64 + GROUP_UPPER_NUM);
        this.totalBitDim = totalBitDim;
        this.needSortRes = needSortRes;
        output = null;
    }

    public BigInteger[] getInput() {
        return input;
    }

    public BigInteger[] getOutput() {
        return output;
    }

    public int[] getPai() {
        return pai;
    }

    private LongVector[] genInputData(){
        SecureRandom secureRandom = new SecureRandom();
        BigInteger upper = BigInteger.ONE.shiftLeft(totalBitDim);
        input = IntStream.range(0 ,dataNum).mapToObj(i ->
            BigIntegerUtils.randomPositive(upper, secureRandom)).toArray(BigInteger[]::new);
        LinkedList<Integer> dim = new LinkedList<>();
        int count = 0, resBit = totalBitDim;
        while (count <= GROUP_UPPER_NUM - 1 && resBit > 0){
            int r = resBit == 1 ? 1 : secureRandom.nextInt(Math.min(resBit - 1, 63)) + 1;
            resBit -= r;
            count++;
            dim.add(r);
        }
        if(resBit > 0){
            dim.add(resBit);
        }
        inputDim = dim.stream().mapToInt(x -> x).toArray();
        LongVector[] res = new LongVector[inputDim.length];
        int shiftLen = 0;
        for(int i = inputDim.length - 1; i >= 0; i--){
            long[] tmp = new long[dataNum];
            BigInteger andNum = BigInteger.ONE.shiftLeft(inputDim[i]).subtract(BigInteger.ONE);
            for(int j = 0; j < dataNum; j++){
                tmp[j] = input[j].shiftRight(shiftLen).and(andNum).longValue();
            }
            shiftLen += inputDim[i];
            res[i] = LongVector.create(tmp);
        }
        return res;
    }

    @Override
    public void run() {
        LongVector[] input = null;
        if(sortParty.getRpc().ownParty().getPartyId() == 0){
            input = genInputData();
            List<byte[]> send = Collections.singletonList(IntUtils.intArrayToByteArray(inputDim));
            DataPacketHeader to1 = new DataPacketHeader(0, 0, 0, 0, 1);
            DataPacketHeader to2 = new DataPacketHeader(0, 0, 0, 0, 2);
            sortParty.getRpc().send(DataPacket.fromByteArrayList(to1, send));
            sortParty.getRpc().send(DataPacket.fromByteArrayList(to2, send));
        }else{
            DataPacketHeader from0 = new DataPacketHeader(0, 0, 0, 0, sortParty.getRpc().ownParty().getPartyId());
            inputDim = IntUtils.byteArrayToIntArray(sortParty.getRpc().receive(from0).getPayload().get(0));
        }
        try{
            long[] costTuple = sortParty.setUsage(new PgSortFnParam(needSortRes ? PgSortOp.SORT_PERMUTE_A : PgSortOp.SORT_A, dataNum, inputDim));
            sortParty.getAbb3Party().init();
            sortParty.init();
            TripletLongVector[] inputShare;
            if(sortParty.getRpc().ownParty().getPartyId() == 0){
                inputShare = (TripletLongVector[]) sortParty.getAbb3Party().getLongParty().shareOwn(input);
            }else{
                inputShare = (TripletLongVector[]) sortParty.getAbb3Party().getLongParty().shareOther(
                    Arrays.stream(inputDim).map(x -> dataNum).toArray(), sortParty.getRpc().getParty(0));
            }
            TripletZ2Vector[] sortRes = needSortRes ? new TripletZ2Vector[totalBitDim] : null;
            TripletLongVector paiShare;

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            if(needSortRes){
                paiShare = sortParty.perGen4MultiDimWithOrigin(inputShare, inputDim, sortRes);
            }else{
                paiShare = sortParty.perGen4MultiDim(inputShare, inputDim);
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            LOGGER.info("P{} with sort origin:[{}], inputDim:{}, process time: {}ms",
                sortParty.getRpc().ownParty().getPartyId(), needSortRes, Arrays.toString(inputDim), time);

            long[] paiLong = sortParty.getAbb3Party().getLongParty().open(paiShare)[0].getElements();
            pai = Arrays.stream(paiLong).mapToInt(each -> (int) each).toArray();
            if(needSortRes){
                BitVector[] res = sortParty.getAbb3Party().getZ2cParty().open(sortRes);
                if(sortParty.getRpc().ownParty().getPartyId() == 0){
                    output = ZlDatabase.create(EnvType.STANDARD, true, res).getBigIntegerData();
                }
            }
            long usedBitTuple = costTuple[0] == 0 ? 0 : sortParty.getAbb3Party().getTripletProvider().getZ2MtProvider().getAllTupleNum();
            long usedLongTuple = costTuple[1] == 0 ? 0 : sortParty.getAbb3Party().getTripletProvider().getZl64MtProvider().getAllTupleNum();
            LOGGER.info("computed bitTupleNum:{}, actually used bitTupleNum:{} | computed longTupleNum:{}, actually used longTupleNum:{}",
                costTuple[0], usedBitTuple, costTuple[1], usedLongTuple);
        }catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

