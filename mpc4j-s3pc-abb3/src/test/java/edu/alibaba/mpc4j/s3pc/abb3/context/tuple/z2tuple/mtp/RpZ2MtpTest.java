package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.mtp;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.FilePtoWorkType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.MtProviderType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2Mtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2MtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.buffer.RpZ2BufferMtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.file.RpZ2FileMtpConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * basic s3 z2 multiplication tuple provider test.
 *
 * @author Feng Han
 * @date 2024/01/29
 */
@RunWith(Parameterized.class)
public class RpZ2MtpTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpZ2MtpTest.class);
    /**
     * max batch num
     */
    private static final int MAX_BATCH_NUM = 128;
    /**
     * small num
     */
    private static final long SMALL_TOTAL = 1L << 16;
    /**
     * middle num
     */
    private static final long MIDDLE_TOTAL = 1L << 20;
    /**
     * large num
     */
    private static final long LARGE_TOTAL = 1L << 24;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            MtProviderType.BUFFER.name(),
            new RpZ2BufferMtpConfig.Builder().build()
        });

        configurations.add(new Object[]{
            MtProviderType.FILE.name() + "_READ_WRITE",
            new RpZ2FileMtpConfig.Builder(FilePtoWorkType.READ_WRITE, "./").build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final RpZ2MtpConfig config;

    public RpZ2MtpTest(String name, RpZ2MtpConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testSmall(){
        testGenerate(true, SMALL_TOTAL);
        testGenerate(false, SMALL_TOTAL);
    }

    @Test
    public void testMiddle(){
        testGenerate(true, MIDDLE_TOTAL);
        testGenerate(false, MIDDLE_TOTAL);
    }

    @Test
    public void testLarge(){
        testGenerate(true, LARGE_TOTAL);
        testGenerate(false, LARGE_TOTAL);
    }

    private void testGenerate(boolean parallel, long totalBit) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        S3pcCrProviderConfig crProviderConfig = new S3pcCrProviderConfig.Builder().build();
        S3pcCrProvider[] crProviders = Arrays.stream(rpcAll).map(eachRpc ->
            new S3pcCrProvider(eachRpc, crProviderConfig)).toArray(S3pcCrProvider[]::new);
        RpZ2Mtp[] providers = IntStream.range(0, 3).mapToObj(i ->
            RpMtProviderFactory.createRpZ2MtParty(rpcAll[i], config, crProviders[i])).toArray(RpZ2Mtp[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(providers).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });

        FilePtoWorkType workType = config instanceof RpZ2FileMtpConfig ? ((RpZ2FileMtpConfig)config).getPtoWorkType() : null;

        List<int[]> bitLists = getRequirement(totalBit);
        try {
            LOGGER.info("-----test {} (totalBit = {}) start-----", providers[0].getPtoDesc().getPtoName(), totalBit);
            RpZ2MtpThread[] threads = Arrays.stream(providers).map(p ->
                new RpZ2MtpThread(p, bitLists, totalBit, workType)).toArray(RpZ2MtpThread[]::new);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for(RpZ2MtpThread t : threads){
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            // destroy
            Arrays.stream(providers).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {} (totalBit = {}) end, total time : {}-----", providers[0].getPtoDesc().getPtoName(), totalBit, time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static List<int[]> getRequirement(long totalBit) {
        long lastNum = totalBit;
        int upperBoundInEachOne = (int) Math.min(totalBit / MAX_BATCH_NUM, Integer.MAX_VALUE / MAX_BATCH_NUM);
        List<int[]> bitLists = new LinkedList<>();
        while (lastNum > 0) {
            int[] bitNums = new int[MAX_BATCH_NUM];
            int needBatch = 0;
            for (int i = 0; i < bitNums.length; i++, needBatch++) {
                bitNums[i] = SECURE_RANDOM.nextInt(upperBoundInEachOne) + 1;
                lastNum -= bitNums[i];
                if (lastNum < 0) {
                    break;
                }
            }
            if (needBatch <= 0) {
                break;
            }
            bitLists.add(Arrays.copyOf(bitNums, needBatch));
            lastNum -= (long) needBatch << 3;
        }
        return bitLists;
    }
}
