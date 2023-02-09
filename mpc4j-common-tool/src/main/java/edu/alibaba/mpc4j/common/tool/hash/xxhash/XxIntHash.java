package edu.alibaba.mpc4j.common.tool.hash.xxhash;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;

/**
 * XXHash is a non-cryptographic, extremely fast and high-quality hash function. We use XXHash32 for IntHash.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
public class XxIntHash implements IntHash {
    /**
     * XXHash32
     */
    private final XXHash32 xxHash32;

    public XxIntHash() {
        xxHash32 = XXHashFactory.fastestInstance().hash32();
    }

    @Override
    public IntHashFactory.IntHashType getType() {
        return IntHashFactory.IntHashType.XX_HASH_32;
    }

    @Override
    public int hash(byte[] data) {
        MathPreconditions.checkPositive("data.length", data.length);
        return xxHash32.hash(data, 0, data.length, 0);
    }

    @Override
    public int hash(byte[] data, int seed) {
        MathPreconditions.checkPositive("data.length", data.length);
        return xxHash32.hash(data, 0, data.length, seed);
    }
}
