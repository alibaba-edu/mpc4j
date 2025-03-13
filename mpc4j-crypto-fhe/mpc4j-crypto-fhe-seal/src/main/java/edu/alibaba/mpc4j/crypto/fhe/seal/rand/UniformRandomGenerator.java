package edu.alibaba.mpc4j.crypto.fhe.seal.rand;

import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Common;

/**
 * Uniform random generator.
 *
 * @author Weiran Liu
 * @date 2023/12/13
 */
public interface UniformRandomGenerator {
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
     * Generates randomness into the destination.
     *
     * @param destination the destination.
     */
    default void generate(long[] destination) {
        generate(destination.length * Common.BYTES_PER_UINT64, destination, 0);
    }

    /**
     * Generates randomness with the assigned length into the destination.
     *
     * @param byteCount   the randomness length in byte.
     * @param destination the destination.
     * @param startIndex  the start index in the destination.
     */
    void generate(int byteCount, long[] destination, int startIndex);

    /**
     * Returns the next pseudorandom, uniformly distributed {@code int}
     * value from this random number generator's sequence. The general
     * contract of {@code nextInt} is that one {@code int} value is
     * pseudorandomly generated and returned. All 2<sup>32</sup> possible
     * {@code int} values are produced with (approximately) equal probability.
     *
     * @return the next pseudorandom, uniformly distributed {@code int}
     * value from this random number generator's sequence
     * @implSpec The method {@code nextInt} is
     * implemented by class {@code Random} as if by:
     * <pre>{@code
     * public int nextInt() {
     *   return next(32);
     * }}</pre>
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
     * value between zero (inclusive) and {@code bound} (exclusive)
     * from this random number generator's sequence
     * @throws IllegalArgumentException if bound is not positive
     * @implSpec The method {@code nextInt(int bound)} is implemented by
     * class {@code Random} as if by:
     * <pre>{@code
     * public int nextInt(int bound) {
     *   if (bound <= 0)
     *     throw new IllegalArgumentException("bound must be positive");
     *
     *   if ((bound & -bound) == bound)  // i.e., bound is a power of 2
     *     return (int)((bound * (long)next(31)) >> 31);
     *
     *   int bits, val;
     *   do {
     *       bits = next(31);
     *       val = bits % bound;
     *   } while (bits - val + (bound-1) < 0);
     *   return val;
     * }}</pre>
     *
     * <p>The hedge "approximately" is used in the foregoing description only
     * because the next method is only approximately an unbiased source of
     * independently chosen bits.  If it were a perfect source of randomly
     * chosen bits, then the algorithm shown would choose {@code int}
     * values from the stated range with perfect uniformity.
     * <p>
     * The algorithm is slightly tricky.  It rejects values that would result
     * in an uneven distribution (due to the fact that 2^31 is not divisible
     * by n). The probability of a value being rejected depends on n.  The
     * worst case is n=2^30+1, for which the probability of a reject is 1/2,
     * and the expected number of iterations before the loop terminates is 2.
     * <p>
     * The algorithm treats the case where n is a power of two specially: it
     * returns the correct number of high-order bits from the underlying
     * pseudo-random number generator.  In the absence of special treatment,
     * the correct number of <i>low-order</i> bits would be returned.  Linear
     * congruential pseudo-random number generators such as the one
     * implemented by this class are known to have short periods in the
     * sequence of values of their low-order bits.  Thus, this special case
     * greatly increases the length of the sequence of values returned by
     * successive calls to this method if n is a small power of two.
     * @since 1.2
     */
    int nextInt(int bound);

    /**
     * Returns the next pseudorandom, uniformly distributed {@code long}
     * value from this random number generator's sequence. The general
     * contract of {@code nextLong} is that one {@code long} value is
     * pseudorandomly generated and returned.
     *
     * @return the next pseudorandom, uniformly distributed {@code long}
     * value from this random number generator's sequence
     * @implSpec The method {@code nextLong} is implemented by class {@code Random}
     * as if by:
     * <pre>{@code
     * public long nextLong() {
     *   return ((long)next(32) << 32) + next(32);
     * }}</pre>
     * <p>
     * Because class {@code Random} uses a seed with only 48 bits,
     * this algorithm will not return all possible {@code long} values.
     */
    long nextLong();

    /**
     * Returns the next pseudorandom, uniformly distributed
     * {@code double} value between {@code 0.0} and
     * {@code 1.0} from this random number generator's sequence.
     *
     * <p>The general contract of {@code nextDouble} is that one
     * {@code double} value, chosen (approximately) uniformly from the
     * range {@code 0.0d} (inclusive) to {@code 1.0d} (exclusive), is
     * pseudorandomly generated and returned.
     *
     * @return the next pseudorandom, uniformly distributed {@code double}
     * value between {@code 0.0} and {@code 1.0} from this
     * random number generator's sequence
     * @implSpec The method {@code nextDouble} is implemented by class
     * {@code Random} as if by:
     * <pre>{@code
     * public double nextDouble() {
     *   return (((long)next(26) << 27) + next(27))
     *     / (double)(1L << 53);
     * }}</pre>
     * <p>The hedge "approximately" is used in the foregoing description only
     * because the {@code next} method is only approximately an unbiased source
     * of independently chosen bits. If it were a perfect source of randomly
     * chosen bits, then the algorithm shown would choose {@code double} values
     * from the stated range with perfect uniformity.
     * <p>[In early versions of Java, the result was incorrectly calculated as:
     * <pre> {@code return (((long)next(27) << 27) + next(27)) / (double)(1L << 54);}</pre>
     * This might seem to be equivalent, if not better, but in fact it
     * introduced a large nonuniformity because of the bias in the rounding of
     * floating-point numbers: it was three times as likely that the low-order
     * bit of the significand would be 0 than that it would be 1! This
     * nonuniformity probably doesn't matter much in practice, but we strive
     * for perfection.]
     * @see Math#random
     */
    double nextDouble();

    /**
     * Returns the next pseudorandom, Gaussian ("normally") distributed
     * {@code double} value with mean {@code 0.0} and standard
     * deviation {@code 1.0} from this random number generator's sequence.
     * <p>
     * The general contract of {@code nextGaussian} is that one
     * {@code double} value, chosen from (approximately) the usual
     * normal distribution with mean {@code 0.0} and standard deviation
     * {@code 1.0}, is pseudorandomly generated and returned.
     *
     * @return the next pseudorandom, Gaussian ("normally") distributed
     * {@code double} value with mean {@code 0.0} and
     * standard deviation {@code 1.0} from this random number
     * generator's sequence
     * @implSpec The method {@code nextGaussian} is implemented by class
     * {@code Random} as if by a threadsafe version of the following:
     * <pre>{@code
     * private double nextNextGaussian;
     * private boolean haveNextNextGaussian = false;
     *
     * public double nextGaussian() {
     *   if (haveNextNextGaussian) {
     *     haveNextNextGaussian = false;
     *     return nextNextGaussian;
     *   } else {
     *     double v1, v2, s;
     *     do {
     *       v1 = 2 * nextDouble() - 1;   // between -1.0 and 1.0
     *       v2 = 2 * nextDouble() - 1;   // between -1.0 and 1.0
     *       s = v1 * v1 + v2 * v2;
     *     } while (s >= 1 || s == 0);
     *     double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s)/s);
     *     nextNextGaussian = v2 * multiplier;
     *     haveNextNextGaussian = true;
     *     return v1 * multiplier;
     *   }
     * }}</pre>
     * <p>
     * This uses the <i>polar method</i> of G. E. P. Box, M. E. Muller, and
     * G. Marsaglia, as described by Donald E. Knuth in <cite>The Art of
     * Computer Programming, Volume 2, third edition: Seminumerical Algorithms</cite>,
     * section 3.4.1, subsection C, algorithm P. Note that it generates two
     * independent values at the cost of only one call to {@code StrictMath.log}
     * and one call to {@code StrictMath.sqrt}.
     */
    double nextGaussian();
}
