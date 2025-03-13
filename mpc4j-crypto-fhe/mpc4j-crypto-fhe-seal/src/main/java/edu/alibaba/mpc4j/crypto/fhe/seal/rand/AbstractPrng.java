package edu.alibaba.mpc4j.crypto.fhe.seal.rand;

import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Common;
import org.bouncycastle.util.Pack;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * abstract uniform random generator.
 *
 * @author Weiran Liu
 * @date 2025/2/13
 */
abstract class AbstractPrng implements UniformRandomGenerator {
    /**
     * const std::size_t buffer_size_ = 4096
     */
    private static final int BUFFER_SIZE = 4096;
    /**
     * seed
     */
    protected final long[] seed;
    /**
     * AtomicLong seed.
     */
    private final AtomicLong atomicLongSeed;
    /**
     * multiplier used for updating seed.
     */
    private static final long multiplier = 0x5DEECE66DL;
    /**
     * addend used for updating seed.
     */
    private static final long addend = 0xBL;
    /**
     * mask used for updating seed.
     */
    private static final long mask = (1L << 48) - 1;
    /**
     * 1.0 / (1L << 53), used for <code>nextDouble()</code>.
     */
    private static final double DOUBLE_UNIT = 0x1.0p-53;
    /**
     * buffer head
     */
    private int bufferHead;
    /**
     * random buffer
     */
    protected final byte[] buffer;
    /**
     * counter
     */
    protected long counter;

    AbstractPrng() {
        this(randomSeed());
    }

    private static long[] randomSeed() {
        SecureRandom secureRandom = new SecureRandom();
        long[] seed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = secureRandom.nextLong();
        }
        return seed;
    }

    AbstractPrng(long[] seed) {
        assert seed.length == UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT;
        this.seed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
        System.arraycopy(seed, 0, this.seed, 0, UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT);
        atomicLongSeed = new AtomicLong();
        setSeed(seed[0]);
        buffer = new byte[BUFFER_SIZE];
        bufferHead = buffer.length;
        counter = 0L;
    }

    private void setSeed(long seed) {
        atomicLongSeed.set(initialScramble(seed));
    }

    private static long initialScramble(long seed) {
        return (seed ^ multiplier) & mask;
    }

    @Override
    public long[] getSeed() {
        return Arrays.copyOf(seed, seed.length);
    }

    @Override
    public void generate(final byte[] destination) {
        int byteCount = destination.length;
        int offset = 0;
        while (byteCount > 0) {
            int currentBytes = Math.min(byteCount, buffer.length - bufferHead);
            System.arraycopy(buffer, bufferHead, destination, offset, currentBytes);
            bufferHead += currentBytes;
            offset += currentBytes;
            byteCount -= currentBytes;

            if (bufferHead == buffer.length) {
                refillBuffer();
                bufferHead = 0;
            }
        }
    }

    @Override
    public void generate(int byteCount, long[] destination, int offset) {
        assert byteCount % Common.BYTES_PER_UINT64 == 0;
        int longCount = byteCount / Common.BYTES_PER_UINT64;
        byte[] temp = new byte[byteCount];
        generate(temp);
        for (int i = 0; i < longCount; i++) {
            destination[offset + i] = Pack.littleEndianToLong(temp, i * Common.BYTES_PER_UINT64);
        }
    }

    protected abstract void refillBuffer();

    /**
     * Generates the next pseudorandom number. Subclasses should
     * override this, as this is used by all other methods.
     *
     * <p>The general contract of {@code next} is that it returns an
     * {@code int} value and if the argument {@code bits} is between
     * {@code 1} and {@code 32} (inclusive), then that many low-order
     * bits of the returned value will be (approximately) independently
     * chosen bit values, each of which is (approximately) equally
     * likely to be {@code 0} or {@code 1}. The method {@code next} is
     * implemented by class {@code Random} by atomically updating the seed to
     * <pre>{@code (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)}</pre>
     * and returning
     * <pre>{@code (int)(seed >>> (48 - bits))}.</pre>
     * <p>
     * This is a linear congruential pseudorandom number generator, as
     * defined by D. H. Lehmer and described by Donald E. Knuth in
     * <cite>The Art of Computer Programming, Volume 2, Third edition:
     * Seminumerical Algorithms</cite>, section 3.2.1.
     *
     * @param bits random bits
     * @return the next pseudorandom value from this random number
     * generator's sequence
     * @since 1.1
     */
    private int next(int bits) {
        long oldseed, nextseed;
        AtomicLong atomicLongSeed = this.atomicLongSeed;
        do {
            oldseed = atomicLongSeed.get();
            nextseed = (oldseed * multiplier + addend) & mask;
        } while (!atomicLongSeed.compareAndSet(oldseed, nextseed));
        return (int) (nextseed >>> (48 - bits));
    }

    @Override
    public int nextInt() {
        return next(32);
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0)  // i.e., bound is a power of 2
            r = (int) ((bound * (long) r) >> 31);
        else { // reject over-represented candidates
            for (int u = r;
                 u - (r = u % bound) + m < 0;
                 u = next(31))
                ;
        }
        return r;
    }

    @Override
    public long nextLong() {
        // it's okay that the bottom word remains signed.
        return ((long) (next(32)) << 32) + next(32);
    }

    @Override
    public double nextDouble() {
        return (((long) (next(26)) << 27) + next(27)) * DOUBLE_UNIT;
    }

    /**
     * cached Gaussian
     */
    private double nextNextGaussian;
    /**
     * has cached Gaussian
     */
    private boolean haveNextNextGaussian = false;

    @Override
    public synchronized double nextGaussian() {
        // See Knuth, TAOCP, Vol. 2, 3rd edition, Section 3.4.1 Algorithm C.
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        } else {
            double v1, v2, s;
            do {
                v1 = 2 * nextDouble() - 1; // between -1 and 1
                v2 = 2 * nextDouble() - 1; // between -1 and 1
                s = v1 * v1 + v2 * v2;
            } while (s >= 1 || s == 0);
            double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
            nextNextGaussian = v2 * multiplier;
            haveNextNextGaussian = true;
            return v1 * multiplier;
        }
    }
}
