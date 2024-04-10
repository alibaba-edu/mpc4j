package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * basic s3 zLong circuit test.
 *
 * @author Feng Han
 * @date 2024/01/30
 */
@RunWith(Parameterized.class)
public class RpZl64EnvTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpZl64EnvTest.class);

    private static final int BATCH_NUM = 8;

    private static final int SMALL_SIZE = 1 << 10;

    private static final int MIDDLE_SIZE = 1 << 14;

    private static final int LARGE_SIZE = 1 << 18;
    public enum DyadicOperator {
        MUL,
        MULI,
        SUBI,
        SUB,
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            RpLongEnvPtoDesc.getInstance().getPtoName(),
            new RpLongEnvConfig.Builder().build()
        });

        return configurations;
    }

    private final RpLongEnvConfig config;

    public RpZl64EnvTest(String name, RpLongEnvConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testSmallSize(){
        testOpi(false, SMALL_SIZE, DyadicOperator.MUL);
        testOpi(false, SMALL_SIZE, DyadicOperator.MULI);
        testOpi(false, SMALL_SIZE, DyadicOperator.SUB);
        testOpi(false, SMALL_SIZE, DyadicOperator.SUBI);
    }

    @Test
    public void testMiddleSize(){
        testOpi(true, MIDDLE_SIZE, DyadicOperator.MUL);
        testOpi(true, MIDDLE_SIZE, DyadicOperator.MULI);
        testOpi(true, MIDDLE_SIZE, DyadicOperator.SUB);
        testOpi(true, MIDDLE_SIZE, DyadicOperator.SUBI);
    }

    @Test
    public void testLargeSize(){
        testOpi(true, LARGE_SIZE, DyadicOperator.MUL);
        testOpi(true, LARGE_SIZE, DyadicOperator.MULI);
        testOpi(true, LARGE_SIZE, DyadicOperator.SUB);
        testOpi(true, LARGE_SIZE, DyadicOperator.SUBI);
    }

    private RpLongEnvParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        S3pcCrProviderConfig crProviderConfig = new S3pcCrProviderConfig.Builder().build();
        S3pcCrProvider[] crProviders = Arrays.stream(rpcAll).map(eachRpc ->
            new S3pcCrProvider(eachRpc, crProviderConfig)).toArray(S3pcCrProvider[]::new);
        RpLongEnvParty[] parties = IntStream.range(0, 3).mapToObj(i ->
            new RpLongEnvParty(rpcAll[i], config, crProviders[i])).toArray(RpLongEnvParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private TripletRpLongVector[][] generateData4Op(LongVector[] data) {
        TripletRpLongVector[][] res = new TripletRpLongVector[3][data.length];
        IntStream intStream = IntStream.range(0, data.length).parallel();
        intStream.forEach(i -> {
            LongVector tmp1 = LongVector.createRandom(data[i].getNum(), SECURE_RANDOM);
            LongVector tmp2 = LongVector.createRandom(data[i].getNum(), SECURE_RANDOM);
            LongVector tmp3 = data[i].sub(tmp1).sub(tmp2);
            res[0][i] = TripletRpLongVector.create(tmp1, tmp2.copy());
            res[1][i] = TripletRpLongVector.create(tmp2, tmp3.copy());
            res[2][i] = TripletRpLongVector.create(tmp3, tmp1.copy());
        });
        return res;
    }

    public LongVector[] getRealRes(LongVector[] inputData, DyadicOperator op){
        int dim = inputData.length / 2;
        if(op.equals(DyadicOperator.SUB) || op.equals(DyadicOperator.SUBI)){
            return IntStream.range(0, dim).mapToObj(i -> inputData[i].sub(inputData[i + dim])).toArray(LongVector[]::new);
        }else{
            return IntStream.range(0, dim).mapToObj(i -> inputData[i].mul(inputData[i + dim])).toArray(LongVector[]::new);
        }
    }

    private void testOpi(boolean parallel, int eachNum, DyadicOperator op) {
        RpLongEnvParty[] parties = getParties(parallel);
        LongVector[] inputData = IntStream.range(0, BATCH_NUM * 2).mapToObj(i ->
            LongVector.createRandom(eachNum, SECURE_RANDOM)).toArray(LongVector[]::new);
        TripletRpLongVector[][] shareData = generateData4Op(inputData);
        LongVector[] realRes = getRealRes(inputData, op);
        try {
            LOGGER.info("-----test {}, {} (Bit number = {}) start-----", parties[0].getPtoDesc().getPtoName(), op.name(), eachNum);
            RpZl64EnvPartyThread[] threads = Arrays.stream(parties).map(p ->
                new RpZl64EnvPartyThread(p, op)).toArray(RpZl64EnvPartyThread[]::new);
            IntStream.range(0, parties.length).forEach(i -> threads[i].setData(inputData, shareData[i]));

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for(RpZl64EnvPartyThread t : threads){
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            LongVector[][] compRes = threads[0].getResult();
            for(int i = 0; i < compRes.length; i++){
                LOGGER.info("verify {}-th op result", i);
                Assert.assertArrayEquals(compRes[i], realRes);
            }
            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, {} (Bit number = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(), op.name(), eachNum, time);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
