package edu.alibaba.work.femur.redis;

import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Redis header bytes array test.
 *
 * @author Weiran Liu
 * @date 2024/12/9
 */
public class RedisHeaderBytesArrayTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisHeaderBytesArrayTest.class);
    /**
     * test header
     */
    private static final String HEADER = "BYTES_ARRAY_TEST";
    /**
     * test host
     */
    private static final String HOST = "127.0.0.1";
    /**
     * test port
     */
    private static final int PORT = 6379;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public RedisHeaderBytesArrayTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testSmallRedisHeaderBytesArray() {
        testRedisHeaderBytesArray(1 << 18);
    }

    @Test
    public void testSpecialRedisHeaderBytesArray() {
        testRedisHeaderBytesArray(2097152);
    }

    @Test
    public void testLargeRedisHeaderBytesArray() {
        testRedisHeaderBytesArray(1 << 22);
    }

    private void testRedisHeaderBytesArray(int size) {
        assert size > 100;
        try {
            // remove elements before testing
            RedisHeaderBytesArray redisHeaderBytesArray = new RedisHeaderBytesArray(HOST, PORT, HEADER);
            redisHeaderBytesArray.clear();

            // generate and add an array
            byte[][] bytesArray = IntStream.range(0, size)
                .mapToObj(IntUtils::intToByteArray)
                .toArray(byte[][]::new);
            redisHeaderBytesArray.putArray(bytesArray);
            int index = 100;
            byte[] value = IntUtils.intToByteArray(index);
            Assert.assertArrayEquals(value, redisHeaderBytesArray.get(index));
            // sorted batch queries
            int from = 10;
            int to = 100;
            byte[][] expectValues = IntStream.range(from, to)
                .mapToObj(IntUtils::intToByteArray)
                .toArray(byte[][]::new);
            int[] indexes = IntStream.range(from, to).toArray();
            byte[][] actualValues = redisHeaderBytesArray.gets(indexes);
            Assert.assertEquals(expectValues.length, actualValues.length);
            for (int i = 0; i < expectValues.length; i++) {
                Assert.assertArrayEquals(expectValues[i], actualValues[i]);
            }
            // unsorted batch queries
            TIntSet intSet = new TIntHashSet(100);
            do {
                intSet.add(secureRandom.nextInt(size));
            } while (intSet.size() < 100);
            indexes = intSet.toArray();
            expectValues = Arrays.stream(indexes)
                .mapToObj(IntUtils::intToByteArray)
                .toArray(byte[][]::new);
            actualValues = redisHeaderBytesArray.gets(indexes);
            Assert.assertEquals(expectValues.length, actualValues.length);
            for (int i = 0; i < expectValues.length; i++) {
                Assert.assertArrayEquals(expectValues[i], actualValues[i]);
            }
            // update a value
            byte[] updateValue = IntUtils.intToByteArray(-index);
            Assert.assertTrue(redisHeaderBytesArray.updateAt(100, updateValue));
            Assert.assertArrayEquals(updateValue, redisHeaderBytesArray.get(index));

            // generate and add another array
            bytesArray = IntStream.range(0, size)
                .mapToObj(i -> IntUtils.intToByteArray(-i))
                .toArray(byte[][]::new);
            redisHeaderBytesArray.putArray(bytesArray);
            value = IntUtils.intToByteArray(-index);
            Assert.assertArrayEquals(value, redisHeaderBytesArray.get(index));

            // clear and close
            Assert.assertTrue(redisHeaderBytesArray.clear());
            redisHeaderBytesArray.close();
        } catch (JedisConnectionException e) {
            LOGGER.error("Cannot connect to Redis server (host = {}, port = {}), running Redis server before testing", HOST, PORT);
        }
    }
}
