package edu.alibaba.mpc4j.common.tool.hash.xxhash;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.LongHash;
import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory;
import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

/**
 * XXHash is a non-cryptographic, extremely fast and high-quality hash function. We use XXHash64 for LongHash.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public class XxLongHash implements LongHash {
    /**
     * XXHash64
     */
    private final XXHash64 xxHash64;

    public XxLongHash() {
        xxHash64 = XXHashFactory.fastestInstance().hash64();
    }

    @Override
    public LongHashFactory.LongHashType getType() {
        return LongHashFactory.LongHashType.XX_HASH_64;
    }

    @Override
    public long hash(byte[] data) {
        MathPreconditions.checkPositive("data.length", data.length);
        return xxHash64.hash(data, 0, data.length, 0);
    }

    @Override
    public long hash(byte[] data, long seed) {
        MathPreconditions.checkPositive("data.length", data.length);
        return xxHash64.hash(data, 0, data.length, seed);
    }
}
