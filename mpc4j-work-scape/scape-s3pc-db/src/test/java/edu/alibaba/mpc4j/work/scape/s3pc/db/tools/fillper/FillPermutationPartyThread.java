package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPerOperations.FillPerFnParam;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPerOperations.FillPerOp;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * filling permutation party thread
 *
 * @author Feng Han
 * @date 2025/2/18
 */
public class FillPermutationPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(FillPermutationPartyThread.class);
    /**
     * secure random
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * operations
     */
    private final FillPerOp[] ops;
    /**
     * fillPermutation party
     */
    private final FillPermutationParty fillPermutationParty;
    /**
     * rpc
     */
    private final Rpc rpc;
    /**
     * output num
     */
    private final int outputNum;
    /**
     * input parameters for oneFill test and twoFill test
     * inputNum: input data num
     * validNum: valid data num
     */
    private int[] inputNum, validNum, inputNum1, validNum1, inputNum2, validNum2;
    /**
     * input vectors for fill one permutation test
     */
    private LongVector[] fillOneInput;
    /**
     * output vectors for fill one permutation test
     */
    private LongVector[] fillOneOutput;
    /**
     * input vectors for fill two permutation test
     */
    private LongVector[][] fillTwoInput;
    /**
     * output vectors for fill two permutation test
     */
    private LongVector[][] fillTwoOutput;

    public FillPermutationPartyThread(FillPermutationParty fillPermutationParty, int outputNum, FillPerOp[] ops) {
        this.ops = ops;
        this.fillPermutationParty = fillPermutationParty;
        rpc = fillPermutationParty.getRpc();
        this.outputNum = outputNum;
    }

    public void setParam(int[] inputNum, int[] validNum, int[] inputNum1, int[] validNum1, int[] inputNum2, int[] validNum2) {
        this.inputNum = inputNum;
        this.validNum = validNum;
        this.inputNum1 = inputNum1;
        this.validNum1 = validNum1;
        this.inputNum2 = inputNum2;
        this.validNum2 = validNum2;
    }

    public LongVector[] getFillOneInput() {
        return fillOneInput;
    }

    public LongVector[] getFillOneOutput() {
        return fillOneOutput;
    }

    public LongVector[][] getFillTwoInput() {
        return fillTwoInput;
    }

    public LongVector[][] getFillTwoOutput() {
        return fillTwoOutput;
    }

    public static LongVector[] getExample(int inputNum, int outputNum, int validNum) {
        assert outputNum >= inputNum && validNum <= inputNum;
        // how many point are placed nearby each other in the last
        int dummyNum = inputNum - validNum;
        long[] res = new long[inputNum], flag = new long[inputNum];

        int[] random = IntStream.range(0, outputNum).map(i -> SECURE_RANDOM.nextInt()).toArray();
        int[] pai = ShuffleUtils.permutationGeneration(random);
        long[] targetIndex = Arrays.stream(Arrays.copyOf(pai, inputNum)).sorted().mapToLong(i -> (long) i).toArray();

        int[] random1 = IntStream.range(0, inputNum).map(i -> SECURE_RANDOM.nextInt()).toArray();
        int[] pai1 = ShuffleUtils.permutationGeneration(random1);
        if (validNum > 0) {
            int[] validIndex = Arrays.stream(Arrays.copyOf(pai1, validNum)).sorted().toArray();
            IntStream.range(0, validNum).forEach(i -> {
                res[validIndex[i]] = targetIndex[i];
                flag[validIndex[i]] = 1L;
            });
        }
        if (dummyNum > 0) {
            int[] dummyIndex = Arrays.stream(Arrays.copyOfRange(pai1, validNum, inputNum)).sorted().toArray();
            IntStream.range(0, dummyNum).forEach(i -> res[dummyIndex[i]] = targetIndex[i + validNum]);
        }
        return new LongVector[]{LongVector.create(res), LongVector.create(flag)};
    }

    private void testOneFill() throws MpcAbortException {
        LOGGER.info("testing fill one permutation");
        TripletLongVector[] shareInput;

        fillOneInput = new LongVector[inputNum.length];
        fillOneOutput = new LongVector[inputNum.length];
        for (int i = 0; i < inputNum.length; i++) {
            int cInputN = inputNum[i], cValidN = validNum[i];
            if (rpc.ownParty().getPartyId() == 0) {
                LongVector[] indexAndFlag = getExample(cInputN, outputNum, cValidN);
                fillOneInput[i] = indexAndFlag[0];
                shareInput = (TripletLongVector[]) fillPermutationParty.getAbb3Party().getLongParty().shareOwn(indexAndFlag);
            } else {
                shareInput = (TripletLongVector[]) fillPermutationParty.getAbb3Party().getLongParty().shareOther(new int[]{cInputN, cInputN}, rpc.getParty(0));
            }

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            TripletLongVector outShare = fillPermutationParty.permutationCompletion(shareInput[0], shareInput[1], outputNum);
            LongVector output = fillPermutationParty.getAbb3Party().getLongParty().open(outShare)[0];
            fillOneOutput[i] = output;
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            if (rpc.ownParty().getPartyId() == 0) {
                LOGGER.info("one fill permutation (inputNum:{}, outputNum:{}) process time: {}ms", cInputN, outputNum, time);
            }
        }
    }

    private void testTwoFill() throws MpcAbortException {
        LOGGER.info("testing fill two permutation");
        TripletLongVector[] shareInput1;
        TripletLongVector[] shareInput2;
        fillTwoInput = new LongVector[inputNum.length][2];
        fillTwoOutput = new LongVector[inputNum.length][2];
        for (int i = 0; i < inputNum1.length; i++) {
            if (rpc.ownParty().getPartyId() == 0) {
                LongVector[] indexAndFlag1 = getExample(inputNum1[i], outputNum, validNum1[i]);
                LongVector[] indexAndFlag2 = getExample(inputNum2[i], outputNum, validNum2[i]);
                fillTwoInput[i][0] = indexAndFlag1[0];
                fillTwoInput[i][1] = indexAndFlag2[0];
                shareInput1 = (TripletLongVector[]) fillPermutationParty.getAbb3Party().getLongParty().shareOwn(indexAndFlag1);
                shareInput2 = (TripletLongVector[]) fillPermutationParty.getAbb3Party().getLongParty().shareOwn(indexAndFlag2);
            } else {
                shareInput1 = (TripletLongVector[]) fillPermutationParty.getAbb3Party().getLongParty().shareOther(
                    new int[]{inputNum1[i], inputNum1[i]}, rpc.getParty(0));
                shareInput2 = (TripletLongVector[]) fillPermutationParty.getAbb3Party().getLongParty().shareOther(
                    new int[]{inputNum2[i], inputNum2[i]}, rpc.getParty(0));
            }

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            TripletLongVector[] outShare = fillPermutationParty.twoPermutationCompletion(
                shareInput1[0], shareInput1[1], shareInput2[0], shareInput2[1], outputNum);
            LongVector[] output = fillPermutationParty.getAbb3Party().getLongParty().open(outShare);
            fillTwoOutput[i] = output;
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            if (rpc.ownParty().getPartyId() == 0) {
                LOGGER.info("two fill permutation (inputNum1:{}, inputNum2:{}, outputNum:{}) process time: {}ms",
                    inputNum1[i], inputNum2[i], outputNum, time);
            }
        }
    }

    public FillPerFnParam[] getParam() {
        List<FillPerFnParam> p = new LinkedList<>();
        for (FillPerOp op : ops) {
            switch (op) {
                case FILL_ONE_PER_A:
                    for (int in : inputNum) {
                        p.add(new FillPerFnParam(FillPerOp.FILL_ONE_PER_A, outputNum, in));
                    }
                    break;
                case FILL_TWO_PER_A:
                    for (int i = 0; i < inputNum1.length; i++) {
                        p.add(new FillPerFnParam(FillPerOp.FILL_TWO_PER_A, outputNum, inputNum1[i], inputNum2[i]));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("illegal FillPerOp: " + op.name());
            }
        }
        return p.toArray(new FillPerFnParam[0]);
    }

    @Override
    public void run() {
        FillPerFnParam[] params = getParam();
        try {
            long[] es = fillPermutationParty.setUsage(params);
            fillPermutationParty.init();

            for (FillPerOp op : ops) {
                switch (op) {
                    case FILL_ONE_PER_A:
                        testOneFill();
                        break;
                    case FILL_TWO_PER_A:
                        testTwoFill();
                        break;
                    default:
                        throw new IllegalArgumentException(op + " is not a fill permutation operation");
                }
            }
            RpZ2Mtp z2Mtp = fillPermutationParty.getAbb3Party().getTripletProvider().getZ2MtProvider();
            RpLongMtp zl64Mtp = fillPermutationParty.getAbb3Party().getTripletProvider().getZl64MtProvider();
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
