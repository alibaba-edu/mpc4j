package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.flnw17;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.PrpUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.AbstractRpZ2Mtg;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * FLNW17 replicated 3p sharing z2 mt generator
 *
 * @author Feng Han
 * @date 2024/01/24
 */
public class Flnw17RpZ2Mtg extends AbstractRpZ2Mtg {
    private static final Logger LOGGER = LoggerFactory.getLogger(Flnw17RpZ2Mtg.class);
    /**
     * how many bits in the same ball
     */
    private final int bitInEachBall;
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

    public Flnw17RpZ2Mtg(Rpc rpc, Flnw17RpZ2MtgConfig config, RpZ2EnvParty rpZ2EnvParty) {
        super(Flnw17RpZ2MtgPtoDesc.getInstance(), rpc, config, rpZ2EnvParty);

        numOfResultBalls = config.getNumOfResultBalls();
        bitInEachBall = config.getBitInEachBall();
        currentRoundIndex = 0;
    }

    /**
     * pre-compute the parameters for generating tuples (B-1)logN <= logRound + param
     *
     * @param totalBit the total required tuples
     */
    @Override
    protected void initTripleParam(long totalBit) {
        int logOfTripleNum = Math.max(LongUtils.ceilLog2(totalBit, 1) - LongUtils.ceilLog2(bitInEachBall, 1), 1);
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
        LOGGER.info("bucketSize:{}, logOfTupleInOneGen:{}, logOfTripleRounds:{}, totalBit:{}", bucketSize, logOfTupleInOneGen, logOfTripleRounds, totalBit);
    }

    @Override
    public RpZ2EnvParty getEnv() {
        return envParty;
    }

    @Override
    public int getLogOfRound(){
        return logOfTripleRounds;
    }

    /**
     * generate a batch of multiplication tuples
     */
    @Override
    public TripletRpZ2Vector[][] genMtOnline() throws MpcAbortException {
        if(totalBitNum == 0){
            throw new MpcAbortException("the required number of tuples are initialized as 0, so mtg can not generate tuples!!!");
        }
        if (currentRoundIndex >= (1 << logOfTripleRounds)) {
            throw new MpcAbortException("too many triples are needed!!!");
        }

        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 1. 先生成两个 M=NB+B的随机bit数组
        int totalLength = numOfResultBalls * bucketSize + bucketSize;
        int[] bitNums = new int[totalLength];
        Arrays.fill(bitNums, bitInEachBall);
        TripletRpZ2Vector[] left = crProvider.randRpShareZ2Vector(bitNums);
        TripletRpZ2Vector[] right = crProvider.randRpShareZ2Vector(bitNums);
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 1, 5, resetAndGetTime());

        stopWatch.start();
        // 2. 这两个随机bit数组相乘
        TripletRpZ2Vector[] resultOfMul = envParty.and(left, right);
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 2, 5, resetAndGetTime());

        stopWatch.start();
        // 3. 随机shuffling
        TripletRpZ2Vector rand = crProvider.randRpShareZ2Vector(new int[]{CommonConstants.BLOCK_BIT_LENGTH})[0];
        byte[] plainRand = envParty.open(rand)[0].getBytes();
        int[] pai = PrpUtils.genCorRandomPerm(plainRand, totalLength, parallel, envType);
        left = ShuffleUtils.applyPermutation(left, pai);
        right = ShuffleUtils.applyPermutation(right, pai);
        resultOfMul = ShuffleUtils.applyPermutation(resultOfMul, pai);
        TripletRpZ2Vector[][] tuple = new TripletRpZ2Vector[][]{left, right, resultOfMul};
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 3, 5, resetAndGetTime());

        stopWatch.start();
        // 4. 验证前B个的正确性
        TripletRpZ2Vector[] openAddition = new TripletRpZ2Vector[3 * bucketSize];
        for (int i = 0, destStart = 0, srcStart = totalLength - bucketSize; i < 3; i++, destStart += bucketSize) {
            System.arraycopy(tuple[i], srcStart, openAddition, destStart, bucketSize);
        }
        BitVector[] openValue = envParty.open(openAddition);
        for (int i = 0; i < bucketSize; i++) {
            if (!openValue[i + bucketSize * 2].equals(openValue[i + bucketSize].and(openValue[i]))) {
                throw new MpcAbortException("error happens when verifying the last B triple " + i);
            }
        }
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 4, 5, resetAndGetTime());

        stopWatch.start();
        // 5. 每B个验证一个的正确性
        int eachVerGroupNum = bucketSize - 1;
        TripletRpZ2Vector[][] first = new TripletRpZ2Vector[3][eachVerGroupNum * numOfResultBalls];
        TripletRpZ2Vector[][] second = new TripletRpZ2Vector[3][];
        for (int dim = 0; dim < 3; dim++) {
            for (int i = 0; i < eachVerGroupNum; i++) {
                System.arraycopy(tuple[dim], 0, first[dim], i * numOfResultBalls, numOfResultBalls);
            }
            second[dim] = Arrays.copyOfRange(tuple[dim], numOfResultBalls, bucketSize * numOfResultBalls);
        }
        envParty.verifyMultipleGroup(first, second);
        currentRoundIndex++;
        TripletRpZ2Vector[][] res = Arrays.stream(first).map(each -> Arrays.copyOf(each, numOfResultBalls)).toArray(TripletRpZ2Vector[][]::new);
        logStepInfo(PtoState.PTO_STEP, "genMtOnline", 5, 5, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }
}
