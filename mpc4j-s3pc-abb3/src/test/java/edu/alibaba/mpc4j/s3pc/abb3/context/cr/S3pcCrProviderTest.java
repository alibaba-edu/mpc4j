package edu.alibaba.mpc4j.s3pc.abb3.context.cr;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * s3 correlated randomness provider test.
 *
 * @author Feng Han
 * @date 2024/02/01
 */
@RunWith(Parameterized.class)
public class S3pcCrProviderTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3pcCrProviderTest.class);
    private static final int BATCH_NUM = 32;
    private static final int BIT_UPPER = 1 << 17;
    private static final int LONG_UPPER = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            S3pcCrProvider.class.getName(),
            new S3pcCrProviderConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final S3pcCrProviderConfig config;

    public S3pcCrProviderTest(String name, S3pcCrProviderConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testParallel(){
        testGenerate(true);
    }

    @Test
    public void testNoParallel(){
        testGenerate(false);
    }

    private void testGenerate(boolean parallel) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        S3pcCrProvider[] crProviders = Arrays.stream(rpcAll).map(eachRpc ->
            new S3pcCrProvider(eachRpc, config)).toArray(S3pcCrProvider[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(crProviders).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });

        int[] bits = getRequirement(BIT_UPPER);
        int[] longs = getRequirement(LONG_UPPER);

        try {
            LOGGER.info("-----test {} start-----", crProviders[0].getPtoDesc().getPtoName());
            S3pcCrProviderThread[] threads = Arrays.stream(crProviders).map(p ->
                new S3pcCrProviderThread(p, bits, longs)).toArray(S3pcCrProviderThread[]::new);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for(S3pcCrProviderThread t : threads){
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);

            // verify
            BitVector[][] bitWithLeft = Arrays.stream(threads).map(S3pcCrProviderThread::getBitWithLeft).toArray(BitVector[][]::new);
            BitVector[][] bitWithRight = Arrays.stream(threads).map(S3pcCrProviderThread::getBitWithRight).toArray(BitVector[][]::new);
            BitVector[][] bitRandZero = Arrays.stream(threads).map(S3pcCrProviderThread::getBitZero).toArray(BitVector[][]::new);

            LongVector[][] longWithLeft = Arrays.stream(threads).map(S3pcCrProviderThread::getLongWithLeft).toArray(LongVector[][]::new);
            LongVector[][] longWithRight = Arrays.stream(threads).map(S3pcCrProviderThread::getLongWithRight).toArray(LongVector[][]::new);
            LongVector[][] longRandZero = Arrays.stream(threads).map(S3pcCrProviderThread::getLongZero).toArray(LongVector[][]::new);
            for(int i = 0; i < bits.length; i++){
                for(int j = 0; j < 3; j++){
                    int leftIndex = (j + 2) % 3;
                    Assert.assertEquals(bitWithLeft[j][i], bitWithRight[leftIndex][i]);
                    Assert.assertEquals(longWithLeft[j][i], longWithRight[leftIndex][i]);
                }
                Assert.assertEquals(bitRandZero[0][i].xor(bitRandZero[1][i]).xor(bitRandZero[2][i]), BitVectorFactory.createZeros(bits[i]));
                Assert.assertEquals(longRandZero[0][i].add(longRandZero[1][i]).add(longRandZero[2][i]), LongVector.createZeros(longs[i]));
            }

            // destroy
            Arrays.stream(crProviders).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {} end, total time : {}-----", crProviders[0].getPtoDesc().getPtoName(), time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static int[] getRequirement(int upperBound) {
        int[] req = new int[BATCH_NUM];
        for(int i = 0; i < req.length; i++){
            req[i] = SECURE_RANDOM.nextInt(upperBound) + 1;
        }
        return req;
    }

}
