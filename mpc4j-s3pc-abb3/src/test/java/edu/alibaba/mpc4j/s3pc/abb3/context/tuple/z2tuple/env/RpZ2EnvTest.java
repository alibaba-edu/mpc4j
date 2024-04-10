package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
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
 * basic s3 z2 circuit test.
 *
 * @author Feng Han
 * @date 2024/01/30
 */
@RunWith(Parameterized.class)
public class RpZ2EnvTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpZ2EnvTest.class);

    private static final int BATCH_NUM = 16;

    private static final int SMALL_SIZE = 1 << 10;

    private static final int MIDDLE_SIZE = 1 << 15;

    private static final int LARGE_SIZE = 1 << 20;

    public enum DyadicOperator {
        XOR,
        XORI,
        ANDI,
        AND,
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            RpZ2EnvPtoDesc.getInstance().getPtoName(),
            new RpZ2EnvConfig.Builder().build()
        });

        return configurations;
    }

    private final RpZ2EnvConfig config;

    public RpZ2EnvTest(String name, RpZ2EnvConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testSmallSize(){
        testOpi(false, SMALL_SIZE, DyadicOperator.AND);
        testOpi(false, SMALL_SIZE, DyadicOperator.ANDI);
        testOpi(false, SMALL_SIZE, DyadicOperator.XORI);
        testOpi(false, SMALL_SIZE, DyadicOperator.XOR);
    }

    @Test
    public void testMiddleSize(){
        testOpi(true, MIDDLE_SIZE, DyadicOperator.AND);
        testOpi(true, MIDDLE_SIZE, DyadicOperator.ANDI);
        testOpi(true, MIDDLE_SIZE, DyadicOperator.XORI);
        testOpi(true, MIDDLE_SIZE, DyadicOperator.XOR);
    }

    @Test
    public void testLargeSize(){
        testOpi(true, LARGE_SIZE, DyadicOperator.AND);
        testOpi(true, LARGE_SIZE, DyadicOperator.ANDI);
        testOpi(true, LARGE_SIZE, DyadicOperator.XORI);
        testOpi(true, LARGE_SIZE, DyadicOperator.XOR);
    }

    private RpZ2EnvParty[] getParties(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        S3pcCrProviderConfig crProviderConfig = new S3pcCrProviderConfig.Builder().build();
        S3pcCrProvider[] crProviders = Arrays.stream(rpcAll).map(eachRpc ->
            new S3pcCrProvider(eachRpc, crProviderConfig)).toArray(S3pcCrProvider[]::new);
        RpZ2EnvParty[] parties = IntStream.range(0, 3).mapToObj(i ->
            new RpZ2EnvParty(rpcAll[i], config, crProviders[i])).toArray(RpZ2EnvParty[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(parties).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });
        return parties;
    }

    private TripletRpZ2Vector[][] generateData4Op(BitVector[] data) {
        TripletRpZ2Vector[][] res = new TripletRpZ2Vector[3][data.length];
        IntStream intStream = IntStream.range(0, data.length).parallel();
        intStream.forEach(i -> {
            BitVector tmp1 = BitVectorFactory.createRandom(data[i].bitNum(), SECURE_RANDOM);
            BitVector tmp2 = BitVectorFactory.createRandom(data[i].bitNum(), SECURE_RANDOM);
            BitVector tmp3 = data[i].xor(tmp1).xor(tmp2);
            res[0][i] = TripletRpZ2Vector.create(tmp1, tmp2.copy());
            res[1][i] = TripletRpZ2Vector.create(tmp2, tmp3.copy());
            res[2][i] = TripletRpZ2Vector.create(tmp3, tmp1.copy());
        });
        return res;
    }

    public BitVector[] getRealRes(BitVector[] inputData, DyadicOperator op){
        int dim = inputData.length / 2;
        if(op.equals(DyadicOperator.XORI) || op.equals(DyadicOperator.XOR)){
            return IntStream.range(0, dim).mapToObj(i -> inputData[i].xor(inputData[i + dim])).toArray(BitVector[]::new);
        }else{
            return IntStream.range(0, dim).mapToObj(i -> inputData[i].and(inputData[i + dim])).toArray(BitVector[]::new);
        }
    }

    private void testOpi(boolean parallel, int eachBitNum, DyadicOperator op) {
        RpZ2EnvParty[] parties = getParties(parallel);

        BitVector[] inputData = IntStream.range(0, BATCH_NUM * 2).mapToObj(i ->
            BitVectorFactory.createRandom(eachBitNum, SECURE_RANDOM)).toArray(BitVector[]::new);

        TripletRpZ2Vector[][] shareData = generateData4Op(inputData);
        BitVector[] realRes = getRealRes(inputData, op);
        try {
            LOGGER.info("-----test {}, {} (Bit number = {}) start-----", parties[0].getPtoDesc().getPtoName(), op.name(), eachBitNum);
            RpZ2EnvPartyThread[] threads = Arrays.stream(parties).map(p ->
                new RpZ2EnvPartyThread(p, op)).toArray(RpZ2EnvPartyThread[]::new);
            IntStream.range(0, parties.length).forEach(i -> threads[i].setData(inputData, shareData[i]));

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for(RpZ2EnvPartyThread t : threads){
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            BitVector[][] compRes = threads[0].getResult();
            for(int i = 0; i < compRes.length; i++){
                LOGGER.info("verify {}-th op result", i);
                Assert.assertArrayEquals(compRes[i], realRes);
            }

            // destroy
            Arrays.stream(parties).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {}, {} (Bit number = {}) end, time:{}-----", parties[0].getPtoDesc().getPtoName(), op.name(), eachBitNum, time);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
