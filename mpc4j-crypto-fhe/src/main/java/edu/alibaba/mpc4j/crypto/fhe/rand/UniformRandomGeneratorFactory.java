package edu.alibaba.mpc4j.crypto.fhe.rand;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * Uniform random generator factory.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L411
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
        return useRandomSeed ? create(PrngType.SHA1PRNG) : create(PrngType.SHA1PRNG, defaultSeed);
    }

    /**
     * Creates a uniform random generator.
     *
     * @param seed the seed.
     * @return a uniform random generator.
     */
    public UniformRandomGenerator create(long[] seed) {
        return create(PrngType.SHA1PRNG, seed);
    }

    /**
     * Creates a uniform random generator.
     *
     * @param prngType the PRNG type.
     * @return a uniform random generator.
     */
    public UniformRandomGenerator create(PrngType prngType) {
        switch (prngType) {
            case BLAKE2XB:
                // TODO: implement blake2xb
            case SHAKE256:
                // TODO: implement Shake256
                throw new IllegalArgumentException("unsupported PRNG type");
            case SHA1PRNG:
                return new Sha1Prng();
            default:
                throw new IllegalArgumentException("unknown PRNG type");
        }
    }

    /**
     * Creates a uniform random generator.
     *
     * @param prngType the PRNG type.
     * @param seed the seed.
     * @return a uniform random generator.
     */
    public UniformRandomGenerator create(PrngType prngType, long[] seed) {
        switch (prngType) {
            case BLAKE2XB:
                // TODO: implement blake2xb
            case SHAKE256:
                // TODO: implement Shake256
                throw new IllegalArgumentException("unsupported PRNG type");
            case SHA1PRNG:
                return new Sha1Prng(seed);
            default:
                throw new IllegalArgumentException("unknown PRNG type");
        }
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
