package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.mtp;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProvider;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.FilePtoWorkType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.MtProviderType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtp;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.buffer.RpLongBufferMtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.file.RpLongFileMtpConfig;
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
 * basic s3 zLong multiplication tuple provider test.
 *
 * @author Feng Han
 * @date 2024/01/30
 */
@RunWith(Parameterized.class)
public class RpLongMtpTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpLongMtpTest.class);
    /**
     * max batch num
     */
    private static final int MAX_BATCH_NUM = 128;
    /**
     * small num
     */
    private static final long SMALL_TOTAL = 1L << 12;
    /**
     * middle num
     */
    private static final long MIDDLE_TOTAL = 1L << 16;
    /**
     * large num
     */
    private static final long LARGE_TOTAL = 1L << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            MtProviderType.BUFFER.name(),
            new RpLongBufferMtpConfig.Builder().build()
        });

        configurations.add(new Object[]{
            MtProviderType.FILE.name() + "_READ_WRITE",
            new RpLongFileMtpConfig.Builder(FilePtoWorkType.READ_WRITE, "./").build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final RpLongMtpConfig config;

    public RpLongMtpTest(String name, RpLongMtpConfig config) {
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

    private void testGenerate(boolean parallel, long totalData) {
        Rpc[] rpcAll = new Rpc[]{firstRpc, secondRpc, thirdRpc};
        S3pcCrProviderConfig crProviderConfig = new S3pcCrProviderConfig.Builder().build();
        S3pcCrProvider[] crProviders = Arrays.stream(rpcAll).map(eachRpc ->
            new S3pcCrProvider(eachRpc, crProviderConfig)).toArray(S3pcCrProvider[]::new);
        RpLongMtp[] providers = IntStream.range(0, 3).mapToObj(i ->
            RpMtProviderFactory.createRpZl64MtParty(rpcAll[i], config, crProviders[i])).toArray(RpLongMtp[]::new);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        Arrays.stream(providers).forEach(p -> {
            p.setParallel(parallel);
            p.setTaskId(randomTaskId);
        });

        FilePtoWorkType workType = config instanceof RpLongFileMtpConfig ? ((RpLongFileMtpConfig)config).getPtoWorkType() : null;

        List<int[]> bitLists = getRequirement(totalData);
        try {
            LOGGER.info("-----test {} (totalBit = {}) start-----", providers[0].getPtoDesc().getPtoName(), totalData);
            RpZl64MtpThread[] threads = Arrays.stream(providers).map(p ->
                new RpZl64MtpThread(p, bitLists, totalData, workType)).toArray(RpZl64MtpThread[]::new);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            Arrays.stream(threads).forEach(Thread::start);
            for(RpZl64MtpThread t : threads){
                t.join();
            }
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            // destroy
            Arrays.stream(providers).forEach(p -> new Thread(p::destroy).start());
            LOGGER.info("-----test {} (totalBit = {}) end, total time : {}-----", providers[0].getPtoDesc().getPtoName(), totalData, time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static List<int[]> getRequirement(long totalData) {
        long lastNum = totalData;
        int upperBoundInEachOne = (int) Math.min(totalData / MAX_BATCH_NUM, Integer.MAX_VALUE / MAX_BATCH_NUM);
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
        }
        return bitLists;
    }
}
