package edu.alibaba.work.femur.redis;

import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Redis header map test.
 *
 * @author Weiran Liu
 * @date 2024/12/9
 */
public class RedisHeaderMapTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisHeaderMapTest.class);
    /**
     * test header
     */
    private static final String HEADER = "HEADER_MAP_TEST";
    /**
     * test host
     */
    private static final String HOST = "127.0.0.1";
    /**
     * test port
     */
    private static final int PORT = 6379;

    @Test
    public void testRedisHeaderMap() {
        try {
            // remove elements before testing
            RedisHeaderMap redisHeaderMap = new RedisHeaderMap(HOST, PORT, HEADER);
            redisHeaderMap.clear();

            String key = "Key1";
            byte[] value1 = IntUtils.intToByteArray(1);
            byte[] value2 = IntUtils.intToByteArray(2);
            // add a key-value pair
            Assert.assertTrue(redisHeaderMap.put(key, value1));
            Assert.assertTrue(redisHeaderMap.contains(key));
            Assert.assertArrayEquals(value1, redisHeaderMap.get(key));
            // add a duplicate key-value pair
            Assert.assertFalse(redisHeaderMap.put(key, value2));
            Assert.assertTrue(redisHeaderMap.contains(key));
            Assert.assertArrayEquals(value2, redisHeaderMap.get(key));

            // remove the key
            Assert.assertTrue(redisHeaderMap.remove(key));
            Assert.assertFalse(redisHeaderMap.contains(key));
            Assert.assertNull(redisHeaderMap.get(key));

            // clear and close
            redisHeaderMap.clear();
            redisHeaderMap.close();
        } catch (JedisConnectionException e) {
            LOGGER.error("Cannot connect to Redis server (host = {}, port = {}), running Redis server before testing", HOST, PORT);
        }
    }
}
