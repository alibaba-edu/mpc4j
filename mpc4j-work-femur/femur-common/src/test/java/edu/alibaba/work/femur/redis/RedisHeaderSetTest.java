package edu.alibaba.work.femur.redis;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Redis header hash set test.
 *
 * @author Weiran Liu
 * @date 2024/12/9
 */
public class RedisHeaderSetTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisHeaderSetTest.class);
    /**
     * test header
     */
    private static final String HEADER = "HEADER_SET_TEST";
    /**
     * test host
     */
    private static final String HOST = "127.0.0.1";
    /**
     * test port
     */
    private static final int PORT = 6379;

    @Test
    public void testRedisHeaderSet() {
        try {
            // remove elements before testing
            RedisHeaderSet redisHeaderSet = new RedisHeaderSet(HOST, PORT, HEADER);
            redisHeaderSet.clear();

            String key = "Key1";
            // add an element
            Assert.assertTrue(redisHeaderSet.add(key));
            Assert.assertTrue(redisHeaderSet.contains(key));
            // add a duplicate element
            Assert.assertFalse(redisHeaderSet.add(key));
            Assert.assertTrue(redisHeaderSet.contains(key));

            // remove the element
            Assert.assertTrue(redisHeaderSet.remove(key));
            Assert.assertFalse(redisHeaderSet.contains(key));

            // clear and close
            redisHeaderSet.clear();
            redisHeaderSet.close();
        } catch (JedisConnectionException e) {
            LOGGER.error("Cannot connect to Redis server (host = {}, port = {}), running Redis server before testing", HOST, PORT);
        }
    }
}
