package edu.alibaba.mpc4j.common.tool.utils;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Based on our performance tests, we find that operations for <code>byte[]</code> in Java is much slower than C/C++.
 * Here we try several ways to test performances. All tests are for 128-bit operations.
 *
 * @author Weiran Liu
 * @date 2024/10/18
 */
public class BytesEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BytesEfficiencyTest.class);
    /**
     * default byte length
     */
    private static final int BYTE_LENGTH = ByteVector.SPECIES_128.vectorByteSize();
    /**
     * default long length
     */
    private static final int LONG_LENGTH = BYTE_LENGTH / Long.BYTES;
    /**
     * unsafe API.
     * See <a href="https://howtodoinjava.com/java-examples/usage-of-class-sun-misc-unsafe/">Usage of class sun.misc.Unsafe</a>
     * for a good explanations with examples. We also note that <code>Unsafe</code> locates in different places in JDK 8
     * and JDK 17.
     */
    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * random state
     */
    private final SecureRandom secureRandom;
    /**
     * stop watch
     */
    private final StopWatch stopWatch;

    public BytesEfficiencyTest() {
        secureRandom = new SecureRandom();
        stopWatch = new StopWatch();
    }

    @Test
    public void testSingle() {
        LOGGER.info("----------report single operations for byte[]----------");
        int n = 1 << 22;
        byte[][] as = BytesUtils.randomByteArrayVector(n, BYTE_LENGTH, secureRandom);
        byte[][] bs = BytesUtils.randomByteArrayVector(n, BYTE_LENGTH, secureRandom);

        // warm up and get correct result.
        byte[][] correct = IntStream.range(0, n).mapToObj(i -> naiveSingle(as[i], bs[i])).toArray(byte[][]::new);

        // naive XOR
        stopWatch.start();
        byte[][] naive = IntStream.range(0, n).mapToObj(i -> naiveSingle(as[i], bs[i])).toArray(byte[][]::new);
        stopWatch.stop();
        IntStream.range(0, n).forEach(i -> Assert.assertArrayEquals(correct[i], naive[i]));
        LOGGER.info(" Naive: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();

        // simd
        stopWatch.start();
        byte[][] simd = IntStream.range(0, n).mapToObj(i -> simdSingle(as[i], bs[i])).toArray(byte[][]::new);
        stopWatch.stop();
        IntStream.range(0, n).forEach(i -> Assert.assertArrayEquals(correct[i], simd[i]));
        LOGGER.info("  SIMD: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();

        // unsafe
        stopWatch.start();
        byte[][] unsafe = IntStream.range(0, n).mapToObj(i -> unsafeSingle(as[i], bs[i])).toArray(byte[][]::new);
        stopWatch.stop();
        IntStream.range(0, n).forEach(i -> Assert.assertArrayEquals(correct[i], unsafe[i]));
        LOGGER.info("UNSAFE: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();
    }

    private byte[] naiveSingle(byte[] a, byte[] b) {
        byte[] result = new byte[BYTE_LENGTH];
        for (int j = 0; j < BYTE_LENGTH; j++) {
            result[j] = (byte) (a[j] ^ b[j]);
        }
        return result;
    }

    private byte[] simdSingle(byte[] a, byte[] b) {
        ByteVector vectorA = ByteVector.fromArray(ByteVector.SPECIES_128, a, 0);
        ByteVector vectorB = ByteVector.fromArray(ByteVector.SPECIES_128, b, 0);
        return vectorA.lanewise(VectorOperators.XOR, vectorB).toArray();
    }

    private byte[] unsafeSingle(byte[] a, byte[] b) {
        byte[] result = new byte[BYTE_LENGTH];
        for (int j = 0; j < LONG_LENGTH; j++) {
            int offset = Long.BYTES * j;
            long temp = UNSAFE.getLong(a, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset)
                ^ UNSAFE.getLong(b, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset);
            UNSAFE.putLong(result, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset, temp);
        }
        return result;
    }

    @Test
    public void testBatch() {
        LOGGER.info("----------report batch operations for byte[]----------");
        int n = 1 << 24;
        byte[][] data = BytesUtils.randomByteArrayVector(n, BYTE_LENGTH, secureRandom);

        // warm up and get correct result.
        byte[] correct = naiveBatch(data);

        // naive XOR
        stopWatch.start();
        byte[] naive = naiveBatch(data);
        stopWatch.stop();
        Assert.assertArrayEquals(correct, naive);
        LOGGER.info(" Naive: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();

        // simd
        stopWatch.start();
        byte[] simd = simdBatch(data);
        stopWatch.stop();
        Assert.assertArrayEquals(correct, simd);
        LOGGER.info("  SIMD: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();

        // unsafe
        stopWatch.start();
        byte[] unsafe = unsafeBatch(data);
        stopWatch.stop();
        Assert.assertArrayEquals(correct, unsafe);
        LOGGER.info("UNSAFE: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();
    }

    private byte[] naiveBatch(byte[][] bs) {
        byte[] result = new byte[BYTE_LENGTH];
        for (byte[] b : bs) {
            for (int j = 0; j < BYTE_LENGTH; j++) {
                result[j] ^= b[j];
            }
        }
        return result;
    }

    private byte[] simdBatch(byte[][] bs) {
        ByteVector result = ByteVector.zero(ByteVector.SPECIES_128);
        for (byte[] b : bs) {
            ByteVector data = ByteVector.fromArray(ByteVector.SPECIES_128, b, 0);
            result = result.lanewise(VectorOperators.XOR, data);
        }
        return result.toArray();
    }

    private byte[] unsafeBatch(byte[][] bs) {
        int longLength = BYTE_LENGTH / Long.BYTES;
        long[] longResult = new long[longLength];
        for (byte[] b : bs) {
            for (int j = 0; j < longLength; j++) {
                // directly get long value from byte array, avoiding copy operations.
                longResult[j] ^= UNSAFE.getLong(b, Unsafe.ARRAY_BYTE_BASE_OFFSET + Long.BYTES * j);
            }
        }
        byte[] result = new byte[BYTE_LENGTH];
        UNSAFE.copyMemory(longResult, Unsafe.ARRAY_LONG_BASE_OFFSET, result, Unsafe.ARRAY_BYTE_BASE_OFFSET, BYTE_LENGTH);
        return result;
    }

    @Test
    public void testRandomByteArrayToIntArray() {
        LOGGER.info("----------report random byte[] -> int[] ----------");
        // we use two ways to do conversions. Note that two ways get different results so that we do not verify output.
        int n = 1 << 22;
        byte[][] inputs = BytesUtils.randomByteArrayVector(n, BYTE_LENGTH, secureRandom);

        // naive conversion, warmup then test
        IntStream.range(0, n).forEach(i -> IntUtils.byteArrayToIntArray(inputs[i]));
        stopWatch.start();
        IntStream.range(0, n).forEach(i -> IntUtils.byteArrayToIntArray(inputs[i]));
        stopWatch.stop();
        LOGGER.info(" NAIVE: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();

        // UNSAFE
        IntStream.range(0, n).forEach(i -> IntUtils.randomByteArrayToIntArray(inputs[i]));
        stopWatch.start();
        IntStream.range(0, n).forEach(i -> IntUtils.randomByteArrayToIntArray(inputs[i]));
        stopWatch.stop();
        LOGGER.info("UNSAFE: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();
    }

    @Test
    public void testExtractLsb() {
        LOGGER.info("----------report extract LSB ----------");
        // we use two ways to do conversions.
        int n = 1 << 22;
        byte[][] data = BlockUtils.randomBlocks(n, secureRandom);

        // naive conversion, warmup then test
        BytesUtils.naiveExtractLsb(data);
        stopWatch.start();
        BytesUtils.naiveExtractLsb(data);
        stopWatch.stop();
        LOGGER.info(" NAIVE: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();

        // SIMD
        BytesUtils.simdExtractLsb(data);
        stopWatch.start();
        BytesUtils.simdExtractLsb(data);
        stopWatch.stop();
        LOGGER.info("  SIMD: {} (us)", StringUtils.leftPad(String.valueOf(stopWatch.getTime(TimeUnit.MICROSECONDS)), 10));
        stopWatch.reset();
    }
}
