package edu.alibaba.work.femur.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;

/**
 * Redis header map.
 *
 * @author Weiran Liu
 * @date 2024/12/9
 */
public class RedisHeaderMap {
    /**
     * Java Redis API
     */
    private final Jedis jedis;
    /**
     * header
     */
    private final String header;

    public RedisHeaderMap(String host, int port, String header) {
        this(host, port, Protocol.DEFAULT_TIMEOUT, header);
    }

    public RedisHeaderMap(String host, int port, int timeOut, String header) {
        jedis = new Jedis(host, port, timeOut);
        this.header = header;
    }

    /**
     * Set the specified hash field to the specified value.
     *
     * @param key   key.
     * @param value value.
     * @return If the field already exists, and put just produced an update of the value, false is returned, otherwise
     * if a new field is created true is returned.
     */
    public boolean put(String key, byte[] value) {
        long result = jedis.hset(header.getBytes(), key.getBytes(), value);
        return result > 0;
    }

    /**
     * Test for existence of a specified field in a hash.
     *
     * @param key key.
     * @return true if the hash stored at key contains the specified field, false if the key is not found or the field
     * is not present.
     */
    public boolean contains(String key) {
        return jedis.hexists(header.getBytes(), key.getBytes());
    }

    /**
     * If key holds a hash, retrieve the value associated to the specified field. If the field is not found or the key
     * does not exist, null is returned.
     *
     * @param key key.
     * @return Bulk reply.
     */
    public byte[] get(String key) {
        return jedis.hget(header.getBytes(), key.getBytes());
    }

    /**
     * Remove the specified field from an hash stored at key.
     *
     * @param key key.
     * @return If the field was present in the hash it is deleted and true is returned, otherwise false is returned and
     * no operation is performed.
     */
    public boolean remove(String key) {
        long result = jedis.hdel(header.getBytes(), key.getBytes());
        return result > 0;
    }

    /**
     * Remove the specified header map. If a given header does not exist, no operation is performed.
     *
     * @return true if the key was removed, false if the key does not exist.
     */
    public boolean clear() {
        long result = jedis.del(header);
        return result > 0;
    }

    /**
     * Closes this stream and releases any system resources associated with it. If the stream is already closed then
     * invoking this method has no effect.
     */
    public void close() {
        jedis.close();
    }
}
