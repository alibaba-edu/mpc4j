package edu.alibaba.mpc4j.common.tool.crypto.engine;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Rijndael256 engine test.
 *
 * @author Weiran Liu
 * @date 2023/9/7
 */
public class Rijndael256EngineTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rijndael256EngineTest.class);
    /**
     * block byte length
     */
    private static final int BLOCK_BYTE_LENGTH = 32;
    /**
     * key byte length
     */
    private static final int KEY_BYTE_LENGTH = 32;
    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Rijndael256EngineTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testParams() {
        Rijndael256Engine engine = new Rijndael256Engine();
        Assert.assertEquals(BLOCK_BYTE_LENGTH, engine.getBlockByteLength());
        Assert.assertEquals(KEY_BYTE_LENGTH, engine.getKeyByteLength());
    }

    @Test
    public void testConstantEnc() {
        byte[] in = new byte[BLOCK_BYTE_LENGTH];
        byte[] key = new byte[KEY_BYTE_LENGTH];
        // create two engines
        Rijndael256Engine jdkEngine = new Rijndael256Engine();
        RijndaelEngine bcEngine = new RijndaelEngine(BLOCK_BYTE_LENGTH * Byte.SIZE);
        // init key
        jdkEngine.init(true, key);
        bcEngine.init(true, new KeyParameter(key));
        // encrypt
        byte[] jdkOut = jdkEngine.doFinal(in);
        byte[] bcOut = new byte[BLOCK_BYTE_LENGTH];
        bcEngine.processBlock(in, 0, bcOut, 0);
        Assert.assertArrayEquals(bcOut, jdkOut);
    }

    @Test
    public void testConstantDnc() {
        byte[] in = new byte[BLOCK_BYTE_LENGTH];
        byte[] key = new byte[KEY_BYTE_LENGTH];
        // create two engines
        Rijndael256Engine jdkEngine = new Rijndael256Engine();
        RijndaelEngine bcEngine = new RijndaelEngine(BLOCK_BYTE_LENGTH * Byte.SIZE);
        // init key
        jdkEngine.init(false, key);
        bcEngine.init(false, new KeyParameter(key));
        // encrypt
        byte[] jdkOut = jdkEngine.doFinal(in);
        byte[] bcOut = new byte[BLOCK_BYTE_LENGTH];
        bcEngine.processBlock(in, 0, bcOut, 0);
        Assert.assertArrayEquals(bcOut, jdkOut);
    }

    @Test
    public void testParallelEnc() {
        testParallel(true);
    }

    @Test
    public void testParallelDec() {
        testParallel(false);
    }

    private void testParallel(boolean forEncryption) {
        int round = 1 << 20;
        // choose a random key and a random plaintext
        byte[] key = new byte[KEY_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        byte[] in = new byte[BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(in);
        // init engine
        Rijndael256Engine engine = new Rijndael256Engine();
        engine.init(forEncryption, key);
        // do final
        Set<ByteBuffer> outSet = IntStream.range(0, round)
            .parallel()
            .mapToObj(index -> engine.doFinal(in))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, outSet.size());
    }

    @Test
    public void testParallelEfficiency() {
        int round = 1 << 20;
        byte[] in = new byte[BLOCK_BYTE_LENGTH];
        byte[] key = new byte[KEY_BYTE_LENGTH];
        LOGGER.info("{}\t{}\t{}", "      type", "   enc(us)", "   dec(us)");
        StopWatch stopWatch = new StopWatch();
        // test BC engine encryption, need to recreate
        stopWatch.start();
        IntStream.range(0, round).parallel().forEach(index -> {
            RijndaelEngine bcEncEngine = new RijndaelEngine(BLOCK_BYTE_LENGTH * Byte.SIZE);
            bcEncEngine.init(true, new KeyParameter(key));
            byte[] out = new byte[BLOCK_BYTE_LENGTH];
            bcEncEngine.processBlock(in, 0, out, 0);
        });
        stopWatch.stop();
        double bcEncTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / round;
        stopWatch.reset();
        // test BC engine decryption
        stopWatch.start();
        IntStream.range(0, round).parallel().forEach(index -> {
            RijndaelEngine bcDecEngine = new RijndaelEngine(BLOCK_BYTE_LENGTH * Byte.SIZE);
            bcDecEngine.init(false, new KeyParameter(key));
            byte[] out = new byte[BLOCK_BYTE_LENGTH];
            bcDecEngine.processBlock(in, 0, out, 0);
        });
        stopWatch.stop();
        double bcDecTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / round;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("BC", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bcEncTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(bcDecTime), 10)
        );
        // test JDK engine encryption
        Rijndael256Engine jdkEncEngine = new Rijndael256Engine();
        jdkEncEngine.init(true, key);
        stopWatch.start();
        IntStream.range(0, round).parallel().forEach(index -> jdkEncEngine.doFinal(in));
        stopWatch.stop();
        double jdkEncTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / round;
        stopWatch.reset();
        // test JDK engine decryption
        Rijndael256Engine jdkDecEngine = new Rijndael256Engine();
        jdkDecEngine.init(false, key);
        stopWatch.start();
        IntStream.range(0, round).parallel().forEach(index -> jdkEncEngine.doFinal(in));
        stopWatch.stop();
        double jdkDecTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / round;
        stopWatch.reset();
        LOGGER.info("{}\t{}\t{}",
            StringUtils.leftPad("JDK", 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(jdkEncTime), 10),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(jdkDecTime), 10)
        );
    }
}
