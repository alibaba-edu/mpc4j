package edu.alibaba.mpc4j.crypto.fhe.seal.rand;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * Uniform random generator factory.
 * <p>
 * The implementation is from <code>UniformRandomGeneratorFactory</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L411">
 * </a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/19
 */
public class UniformRandomGeneratorFactory {
    /**
     * seed length
     */
    public static final int PRNG_SEED_UINT64_COUNT = 8;
    /**
     * use random seed
     */
    private final boolean useRandomSeed;
    /**
     * default seed
     */
    private long[] defaultSeed;

    public UniformRandomGeneratorFactory() {
        useRandomSeed = true;
    }

    public UniformRandomGeneratorFactory(long[] defaultSeed) {
        assert defaultSeed.length == UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT;
        this.defaultSeed = new long[defaultSeed.length];
        System.arraycopy(defaultSeed, 0, this.defaultSeed, 0, defaultSeed.length);
        useRandomSeed = false;
    }

    public static UniformRandomGeneratorFactory defaultFactory() {
        return new UniformRandomGeneratorFactory();
    }

    /**
     * Returns if the factory use a random seed.
     *
     * @return true if the factory use a random seed.
     */
    public boolean useRandomSeed() {
        return useRandomSeed;
    }

    /**
     * Gets a copy of the default seed.
     *
     * @return a copy of the default seed.
     */
    public long[] defaultSeed() {
        if (defaultSeed == null) {
            return null;
        }
        return Arrays.copyOf(defaultSeed, defaultSeed.length);
    }

    /**
     * Creates a uniform random generator.
     *
     * @return a uniform random generator.
     */
    public UniformRandomGenerator create() {
        // see https://github.com/microsoft/SEAL/blob/main/CMakeLists.txt#L262
        // set(SEAL_DEFAULT_PRNG "Blake2xb" CACHE STRING ${SEAL_DEFAULT_PRNG_STR} FORCE)
        return useRandomSeed ? create(PrngType.BLAKE2XB) : create(PrngType.BLAKE2XB, defaultSeed);
    }

    /**
     * Creates a uniform random generator.
     *
     * @param seed the seed.
     * @return a uniform random generator.
     */
    public UniformRandomGenerator create(long[] seed) {
        return create(PrngType.SHAKE256, seed);
    }

    /**
     * Creates a uniform random generator.
     *
     * @param prngType the PRNG type.
     * @return a uniform random generator.
     */
    public UniformRandomGenerator create(PrngType prngType) {
        return switch (prngType) {
            case BLAKE2XB -> new Blake2xbPrng();
            case SHAKE256 -> new Shake256Prng();
            default -> throw new IllegalArgumentException("unknown PRNG type");
        };
    }

    /**
     * Creates a uniform random generator.
     *
     * @param prngType the PRNG type.
     * @param seed the seed.
     * @return a uniform random generator.
     */
    public UniformRandomGenerator create(PrngType prngType, long[] seed) {
        return switch (prngType) {
            case BLAKE2XB -> new Blake2xbPrng(seed);
            case SHAKE256 -> new Shake256Prng(seed);
            default -> throw new IllegalArgumentException("unknown PRNG type");
        };
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(useRandomSeed)
            .append(defaultSeed)
            .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UniformRandomGeneratorFactory that = (UniformRandomGeneratorFactory) o;
        return new EqualsBuilder()
            .append(this.useRandomSeed, that.useRandomSeed)
            .append(this.defaultSeed, that.defaultSeed)
            .isEquals();
    }
}
