package edu.alibaba.mpc4j.s3pc.abb3.basic.utils;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * prp utils test.
 *
 * @author Feng Han
 * @date 2024/02/02
 */
public class PrpUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrpUtilsTest.class);
    @Test
    public void testGenerateRandBytesEfficiency(){
        int testByteNum = 1<<24;
        int parallelNum = ForkJoinPool.getCommonPoolParallelism();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Prp[] parallelPrp = IntStream.range(0, parallelNum).mapToObj(i -> {
            Prp tmp = PrpFactory.createInstance(EnvType.STANDARD_JDK);
            tmp.setKey(key);
            return tmp;
        }).toArray(Prp[]::new);
        stopWatch.stop();
        LOGGER.info("parallel prp init time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
        stopWatch.reset();

        stopWatch.start();
        byte[] r = PrpUtils.generateRandBytes(parallelPrp, 0, testByteNum);
        stopWatch.stop();
        LOGGER.info("parallel prp generation time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
        stopWatch.reset();

        stopWatch.start();
        Prp[] singlePrp = new Prp[]{PrpFactory.createInstance(EnvType.STANDARD_JDK)};
        singlePrp[0].setKey(key);
        stopWatch.stop();
        LOGGER.info("no parallel prp init time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
        stopWatch.reset();

        stopWatch.start();
        byte[] r1 = PrpUtils.generateRandBytes(singlePrp, 0, testByteNum);
        stopWatch.stop();
        LOGGER.info("no parallel prp generation time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
        stopWatch.reset();
    }

    @Test
    public void testGenCorRandomPerm() throws MpcAbortException {
        int testLen = 1<<22;
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        int[] perm2 = PrpUtils.genCorRandomPerm(key, testLen, false, EnvType.STANDARD);
        stopWatch.stop();
        LOGGER.info("no parallel generation time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
        stopWatch.reset();

        stopWatch.start();
        int[] perm1 = PrpUtils.genCorRandomPerm(key, testLen, true, EnvType.STANDARD);
        stopWatch.stop();
        LOGGER.info("parallel generation time: {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
        stopWatch.reset();

        ShuffleUtils.checkCorrectIngFun(perm1, testLen);
        Assert.assertArrayEquals(perm1, perm2);
    }

    @Test
    public void testTime() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Prp prp = PrpFactory.createInstance(EnvType.STANDARD_JDK);
        stopWatch.stop();
        long time00 = stopWatch.getTime(TimeUnit.MILLISECONDS);

        stopWatch.reset();
        stopWatch.start();
        prp.setKey(new byte[16]);
        stopWatch.stop();
        long time01 = stopWatch.getTime(TimeUnit.MILLISECONDS);

        stopWatch.reset();
        stopWatch.start();
        IntStream.range(0, (1<<20) * ForkJoinPool.getCommonPoolParallelism()).parallel().forEach(i -> prp.prp(new byte[16]));
        stopWatch.stop();
        long time1 = stopWatch.getTime(TimeUnit.MILLISECONDS);

        Prp[] multiplePrp = IntStream.range(0, ForkJoinPool.getCommonPoolParallelism()).mapToObj(i -> {
            Prp tmp = PrpFactory.createInstance(EnvType.STANDARD_JDK);
            tmp.setKey(new byte[16]);
            return tmp;
        }).toArray(Prp[]::new);
        stopWatch.reset();
        stopWatch.start();
        Arrays.stream(multiplePrp).parallel().forEach(x -> IntStream.range(0, 1<<20).forEach(i -> x.prp(new byte[16])));
        stopWatch.stop();
        long time2 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info(time00 + "_" + time01 + "_" + time1 + "_" + time2);
    }

    @Test
    public void testTime1() {
        StopWatch stopWatch = new StopWatch();
        Prp[] multiplePrp = IntStream.range(0, ForkJoinPool.getCommonPoolParallelism()).mapToObj(i -> {
            Prp tmp = PrpFactory.createInstance(EnvType.STANDARD_JDK);
            tmp.setKey(new byte[16]);
            return tmp;
        }).toArray(Prp[]::new);
        stopWatch.start();
        Arrays.stream(multiplePrp).parallel().forEach(x -> IntStream.range(0, 1<<20).forEach(i -> x.prp(new byte[16])));
        stopWatch.stop();
        long time1 = stopWatch.getTime(TimeUnit.MILLISECONDS);

        byte[] res = new byte[(1<<20) * ForkJoinPool.getCommonPoolParallelism()];
        SecureRandom secureRandom = new SecureRandom();
        stopWatch.reset();
        stopWatch.start();
        secureRandom.nextBytes(res);
        stopWatch.stop();
        long time2 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        LOGGER.info(time1 + "_" + time2);
    }
}
