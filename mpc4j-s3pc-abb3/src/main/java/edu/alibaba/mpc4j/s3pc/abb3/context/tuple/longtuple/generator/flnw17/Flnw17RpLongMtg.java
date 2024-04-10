package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.flnw17;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.PrpUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.flnw17.Flnw17RpZ2Mtg;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.AbstractRpLongMtg;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * FLNW17 replicated 3p sharing zl64 mt generator
 *
 * @author Feng Han
 * @date 2024/01/24
 */
public class Flnw17RpLongMtg extends AbstractRpLongMtg {
    private static final Logger LOGGER = LoggerFactory.getLogger(Flnw17RpZ2Mtg.class);
    /**
     * how many bits in the same ball
     */
    private final int elementInEachBall;
    /**
     * how many balls can be generated in one generation
     */
    private int numOfResultBalls;
    /**
     * all the tuples should be generated in 2^n rounds
     */
    int logOfTripleRounds;
    /**
     * the bucket size, one ball will use the other balls in the same bucket for verification
     */
    private int bucketSize;

    /**
     * the current index in generating tuples
     */
    private int currentRoundIndex;

    public Flnw17RpLongMtg(Rpc rpc, Flnw17RpLongMtgConfig config, RpLongEnvParty rpLongEnvParty) {
        super(Flnw17RpLongMtgPtoDesc.getInstance(), rpc, config, rpLongEnvParty);

        numOfResultBalls = config.getNumOfResultBalls();
        elementInEachBall = config.getElementInEachBall();
        currentRoundIndex = 0;
    }

    /**
     * pre-compute the parameters for generating tuples
     *
     * @param totalData the total required tuples
     */
    @Override
    protected void initTripleParam(long totalData) {
        int logOfTripleNum = Math.max(LongUtils.ceilLog2(totalData, 1) - LongUtils.ceilLog2(elementInEachBall, 1), 1);
        int logOfTupleInOneGen = LongUtils.ceilLog2(numOfResultBalls, 1);
        if (logOfTripleNum <= logOfTupleInOneGen) {
            logOfTupleInOneGen = logOfTripleNum;
            numOfResultBalls = 1 << logOfTupleInOneGen;
        }
        logOfTripleRounds = Math.max(0, logOfTripleNum - logOfTupleInOneGen);
        bucketSize = (int) Math.ceil((double) (CommonConstants.STATS_BIT_LENGTH + logOfTripleRounds) / logOfTupleInOneGen + 1);
        while (((long) numOfResultBalls) * bucketSize + bucketSize > Integer.MAX_VALUE) {
            // if the generation can not be realized
            logOfTupleInOneGen--;
            logOfTripleRounds = Math.max(0, logOfTripleNum - logOfTupleInOneGen);
            bucketSize = (int) Math.ceil((double) (CommonConstants.STATS_BIT_LENGTH + logOfTripleRounds) / logOfTupleInOneGen + 1);
            numOfResultBalls = 1 << logOfTupleInOneGen;
        }
        numOfResultBalls = 1 << logOfTupleInOneGen;
        // verify whether satisfy the requirement
        Preconditions.checkArgument(CommonConstants.STATS_BIT_LENGTH + logOfTripleRounds <= (bucketSize - 1) * logOfTupleInOneGen);
        LOGGER.info("bucketSize:{}, logOfTupleInOneGen:{}, logOfTripleRounds:{}, totalData:{}", bucketSize, logOfTupleInOneGen, logOfTripleRounds, totalData);
    }

    @Override
    public int getLogOfRound() {
        return logOfTripleRounds;
    }

    @Override
    public RpLongEnvParty getEnv() {
        return envParty;
    }

    @Override
    public TripletRpLongVector[][] genMtOnline() throws MpcAbortException {
        if(totalData == 0){
            throw new MpcAbortException("the required number of tuples are initialized as 0, so mtg can not generate tuples!!!");
        }
        if (currentRoundIndex >= (1 << logOfTripleRounds)) {
            throw new MpcAbortException("too many triples are needed!!!");
        }
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. 先生成两个 M=NB+B的随机bit数组
        stopWatch.start();
        int totalLength = numOfResultBalls * bucketSize + bucketSize;
        int[] bitNums = new int[totalLength];
        Arrays.fill(bitNums, elementInEachBall);
        TripletRpLongVector[] left = crProvider.randRpShareZl64Vector(bitNums);
        TripletRpLongVector[] right = crProvider.randRpShareZl64Vector(bitNums);
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 1, 5, resetAndGetTime());

        // 2. 这两个随机bit数组相乘
        stopWatch.start();
        TripletRpLongVector[] resultOfMul = envParty.mul(left, right);
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 2, 5, resetAndGetTime());

        // 3. 随机shuffling
        stopWatch.start();
        TripletRpLongVector rand = crProvider.randRpShareZl64Vector(new int[]{CommonConstants.BLOCK_BIT_LENGTH>>6})[0];
        long[] plainRand = envParty.open(rand)[0].getElements();
        int[] pai = PrpUtils.genCorRandomPerm(LongUtils.longArrayToByteArray(plainRand), totalLength, parallel, envType);
        TripletRpLongVector[][] tuple = new TripletRpLongVector[][]{left, right, resultOfMul};
        for(int i = 0; i < tuple.length; i++){
            tuple[i] = ShuffleUtils.applyPermutation(tuple[i], pai);
        }
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 3, 5, resetAndGetTime());

        // 4. 验证前B个的正确性
        stopWatch.start();
        TripletRpLongVector[] openAddition = new TripletRpLongVector[3 * bucketSize];
        for (int i = 0, destStart = 0, srcStart = totalLength - bucketSize; i < 3; i++, destStart += bucketSize) {
            System.arraycopy(tuple[i], srcStart, openAddition, destStart, bucketSize);
        }
        LongVector[] openValue = envParty.open(openAddition);
        for (int i = 0; i < bucketSize; i++) {
            if (!openValue[i + bucketSize * 2].equals(openValue[i + bucketSize].mul(openValue[i]))) {
                throw new MpcAbortException("error happens when verifying the last B triple " + i);
            }
        }
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 4, 5, resetAndGetTime());

        // 5. 每B个验证一个的正确性
        stopWatch.start();
        int eachVerGroupNum = bucketSize - 1;
        TripletRpLongVector[][] first = new TripletRpLongVector[3][eachVerGroupNum * numOfResultBalls];
        TripletRpLongVector[][] second = new TripletRpLongVector[3][];
        for (int dim = 0; dim < 3; dim++) {
            for (int i = 0; i < eachVerGroupNum; i++) {
                System.arraycopy(tuple[dim], 0, first[dim], i * numOfResultBalls, numOfResultBalls);
            }
            second[dim] = Arrays.copyOfRange(tuple[dim], numOfResultBalls, bucketSize * numOfResultBalls);
        }
        envParty.verifyMultipleGroup(first, second);
        currentRoundIndex++;
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 5, 5, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return Arrays.stream(first).map(each -> Arrays.copyOf(each, numOfResultBalls)).toArray(TripletRpLongVector[][]::new);
    }
}
