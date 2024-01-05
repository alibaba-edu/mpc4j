package edu.alibaba.mpc4j.crypto.fhe.rand;

/**
 * Uniform random generator.
 *
 * @author Weiran Liu
 * @date 2023/12/13
 */
public interface UniformRandomGenerator {
    /**
     * Gets a copy of the seed.
     *
     * @return a copy of the seed.
     */
    long[] getSeed();

    /**
     * Generates randomness into the destination.
     *
     * @param destination the destination.
     */
    void generate(byte[] destination);

    /**
     * Generates a random integer value in range [0, Integer.MAX_VALUE).
     *
     * @return a random integer value in range [0, Integer.MAX_VALUE).
     */
    int nextInt();

    /**
     * Returns a pseudorandom, uniformly distributed {@code int} value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence.  The general contract of
     * {@code nextInt} is that one {@code int} value in the specified range
     * is pseudorandomly generated and returned.  All {@code bound} possible
     * {@code int} values are produced with (approximately) equal
     * probability.
     *
     * @param bound the upper bound (exclusive).  Must be positive.
     * @return the next pseudorandom, uniformly distributed {@code int}
     * value from this random number generator's sequence.
     */
    int nextInt(int bound);

    /**
     * Returns the next pseudorandom, uniformly distributed {@code long}
     * value from this random number generator's sequence. The general
     * contract of {@code nextLong} is that one {@code long} value is
     * pseudorandomly generated and returned.
     *
     * @return the next pseudorandom, uniformly distributed {@code long}
     * value from this random number generator's sequence.
     */
    long nextLong();

    /**
     * Generates randomness into the destination.
     *
     * @param destination the destination.
     */
    void generate(long[] destination);

    /**
     * Generates randomness with the assigned length into the destination.
     *
     * @param byteCount   the randomness length in byte.
     * @param destination the destination.
     * @param startIndex  the start index in the destination.
     */
    void generate(int byteCount, long[] destination, int startIndex);

    /**
     * Returns the next pseudorandom, Gaussian ("normally") distributed
     * {@code double} value with mean {@code 0.0} and standard
     * deviation {@code 1.0} from this random number generator's sequence.
     *
     * @return the next pseudorandom, Gaussian ("normally") distributed
     * {@code double} value with mean {@code 0.0} and
     * standard deviation {@code 1.0} from this random number
     * generator's sequence
     */
    double nextGaussian();

    /**
     * Returns a UniformRandomGeneratorInfo object representing this PRNG.
     *
     * @return a UniformRandomGeneratorInfo object representing this PRNG.
     */
    default UniformRandomGeneratorInfo getInfo() {
        return new UniformRandomGeneratorInfo(getType(), getSeed());
    }

    /**
     * Gets the PRNG type.
     *
     * @return the PRNG type.
     */
    PrngType getType();
}
