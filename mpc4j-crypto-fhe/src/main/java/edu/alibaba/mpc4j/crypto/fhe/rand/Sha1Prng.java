package edu.alibaba.mpc4j.crypto.fhe.rand;

import edu.alibaba.mpc4j.crypto.fhe.zq.Common;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * SHA1 pseudo-random generator.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/2
 */
public class Sha1Prng implements UniformRandomGenerator {
    /**
     * the random state
     */
    private SecureRandom secureRandom;
    /**
     * seed
     */
    private long[] seed;

    /**
     * Creates a uniform random generator.
     */
    public Sha1Prng() {
        secureRandom = new SecureRandom();
    }

    /**
     * Creates a uniform random generator.
     *
     * @param seed the seed.
     */
    public Sha1Prng(long[] seed) {
        assert seed.length == UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT;
        try {
            // only SHA1PRNG support random generation with seed
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
            // we cannot directly use a for loop to set seed, otherwise the generated randomness is not fixed.
            byte[] byteSeed = Common.uint64ArrayToByteArray(seed, seed.length);
            secureRandom.setSeed(byteSeed);
            this.seed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
            System.arraycopy(seed, 0, this.seed, 0, UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long[] getSeed() {
        return Arrays.copyOf(seed, seed.length);
    }

    @Override
    public int nextInt() {
        return secureRandom.nextInt(Integer.MAX_VALUE);
    }

    @Override
    public int nextInt(int bound) {
        return secureRandom.nextInt(bound);
    }

    @Override
    public long nextLong() {
        return secureRandom.nextLong();
    }

    @Override
    public double nextGaussian() {
        return secureRandom.nextGaussian();
    }

    @Override
    public void generate(byte[] destination) {
        secureRandom.nextBytes(destination);
    }

    @Override
    public void generate(long[] destination) {
        for (int i = 0; i < destination.length; i++) {
            destination[i] = secureRandom.nextLong();
        }
    }

    @Override
    public void generate(int byteCount, long[] destination, int startIndex) {
        assert byteCount % Common.BYTES_PER_UINT64 == 0;
        int longCount = byteCount / Common.BYTES_PER_UINT64;
        for (int i = startIndex; i < startIndex + longCount; i++) {
            destination[i] = secureRandom.nextLong();
        }
    }

    @Override
    public PrngType getType() {
        return PrngType.SHA1PRNG;
    }
}
