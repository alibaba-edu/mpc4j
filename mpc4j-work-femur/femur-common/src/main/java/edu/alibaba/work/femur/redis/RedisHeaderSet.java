package edu.alibaba.work.femur.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

/**
 * Redis hash set.
 *
 * @author Weiran Liu
 * @date 2024/12/9
 */
public class RedisHeaderSet {
    /**
     * Java Redis API
     */
    private final Jedis jedis;
    /**
     * header
     */
    private final String header;

    public RedisHeaderSet(String host, int port, String header) {
        this(host, port, Protocol.DEFAULT_TIMEOUT, header);
    }

    /**
     * Creates a Redis hash set.
     *
     * @param host    Redis host.
     * @param port    Redis port.
     * @param timeOut Redis time out.
     * @param header  key header.
     */
    public RedisHeaderSet(String host, int port, int timeOut, String header) {
        jedis = new Jedis(host, port, timeOut);
        this.header = header;
    }

    /**
     * Return true if member is a member of the set stored at key, otherwise false is returned.
     *
     * @param key key.
     * @return true if member is a member of the set stored at key, otherwise false is returned.
     */
    public boolean contains(String key) {
        return jedis.sismember(header, key);
    }

    /**
     * Add the specified member to the set value stored at key. If member is already a member of the set no operation
     * is performed. If key does not exist a new set with the specified member as sole member is created. If the key
     * exists but does not hold a set value an error is returned.
     *
     * @param key key.
     * @return true if the new element was added, false if the element was already a member of the set.
     */
    public boolean add(String key) {
        long result = jedis.sadd(header, key);
        return result > 0;
    }

    /**
     * Remove the specified member from the set value stored at key. If member was not a member of the set no operation
     * is performed. If key does not hold a set value an error is returned.
     *
     * @param key key.
     * @return true if the new element was removed, false if the new element was not a member of the set.
     */
    public boolean remove(String key) {
        long result = jedis.srem(header, key);
        return result > 0;
    }

    /**
     * Delete all the keys of the currently selected DB. This command never fails.
     */
    public void clear() {
        jedis.flushDB();
    }

    /**
     * Closes this stream and releases any system resources associated with it. If the stream is already closed then
     * invoking this method has no effect.
     */
    public void close() {
        jedis.close();
    }
}
