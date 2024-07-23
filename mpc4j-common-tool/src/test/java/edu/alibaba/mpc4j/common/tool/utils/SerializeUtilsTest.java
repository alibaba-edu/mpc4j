package edu.alibaba.mpc4j.common.tool.utils;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.IntStream;

/**
 * serialize utilities test.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
public class SerializeUtilsTest {
    /**
     * random round
     */
    private static final int RANDOM_ROUND = 1000;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public SerializeUtilsTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testCompressL1() {
        int unitLength = 1 << 10;
        for (int offset = -7; offset < 8; offset++) {
            testCompressL1(unitLength + offset);
        }
    }

    private void testCompressL1(int size) {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] origin = new byte[size];
            for (int index = 0; index < size; index++) {
                origin[index] = secureRandom.nextBoolean() ? (byte) 0b00000001 : (byte) 0b00000000;
            }
            byte[] compressed = SerializeUtils.compressL1(origin);
            // verify compressed length
            int maxCapLength = compressed.length * 8;
            Assert.assertTrue(maxCapLength >= size && maxCapLength < size + 8);
            byte[] decompressed = SerializeUtils.decompressL1(compressed, size);
            Assert.assertArrayEquals(origin, decompressed);
        }
    }

    @Test
    public void testCompressL2() {
        int unitLength = 1 << 10;
        for (int offset = -7; offset < 8; offset++) {
            testCompressL2(unitLength + offset);
        }
    }

    private void testCompressL2(int size) {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] origin = new byte[size];
            secureRandom.nextBytes(origin);
            for (int index = 0; index < size; index++) {
                origin[index] &= 0b00000011;
            }
            byte[] compressed = SerializeUtils.compressL2(origin);
            // verify compressed length
            int maxCapLength = compressed.length * 4;
            Assert.assertTrue(maxCapLength >= size && maxCapLength < size + 4);
            byte[] decompressed = SerializeUtils.decompressL2(compressed, size);
            Assert.assertArrayEquals(origin, decompressed);
        }
    }

    @Test
    public void testCompressL4() {
        int unitLength = 1 << 10;
        for (int offset = -7; offset < 8; offset++) {
            testCompressL4(unitLength + offset);
        }
    }

    private void testCompressL4(int size) {
        for (int i = 0; i < RANDOM_ROUND; i++) {
            byte[] origin = new byte[size];
            secureRandom.nextBytes(origin);
            for (int index = 0; index < size; index++) {
                origin[index] &= 0b00001111;
            }
            byte[] compressed = SerializeUtils.compressL4(origin);
            // verify compressed length
            int maxCapLength = compressed.length * 2;
            Assert.assertTrue(maxCapLength >= size && maxCapLength < size + 2);
            byte[] decompressed = SerializeUtils.decompressL4(compressed, size);
            Assert.assertArrayEquals(origin, decompressed);
        }
    }

    @Test
    public void testCompressEqual() {
        testCompressEqual(0, 0);
        testCompressEqual(0, 10);
        testCompressEqual(10, 0);
        testCompressEqual(10, 10);
        // very large
        testCompressEqual(1 << 6, (1 << 22) + 1);
        testCompressEqual(1 << 22, (1 << 6) + 1);
    }

    private void testCompressEqual(int size, int length) {
        List<byte[]> expect = IntStream.range(0, size)
            .mapToObj(i -> {
                if (length == 0) {
                    return new byte[0];
                } else {
                    return BytesUtils.randomByteArray(length, secureRandom);
                }
            })
            .toList();
        byte[] compress = SerializeUtils.compressEqual(expect, length);
        List<byte[]> actual = SerializeUtils.decompressEqual(compress, length);
        Assert.assertEquals(size, actual.size());
        for (int i = 0; i < size; i++) {
            Assert.assertArrayEquals(expect.get(i), actual.get(i));
        }
        // we need to allow modification
        if (size > 0) {
            actual.remove(0);
        }
    }
}
