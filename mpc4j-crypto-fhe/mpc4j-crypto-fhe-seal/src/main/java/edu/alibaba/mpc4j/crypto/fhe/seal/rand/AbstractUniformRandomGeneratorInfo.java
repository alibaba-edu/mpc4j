package edu.alibaba.mpc4j.crypto.fhe.seal.rand;

import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.Serialization;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * Uniform random generator information.
 * <p>
 * The implementation is from <code>UniformRandomGeneratorInfo</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L55">randomgen.h</a>
 *
 * @author Weiran Liu
 * @date 2023/12/13
 */
public abstract class AbstractUniformRandomGeneratorInfo {
    /**
     * PRNG info indicator
     */
    public static final long PRNG_INFO_INDICATOR = 0xFFFFFFFFFFFFFFFFL;
    /**
     * Returns an upper bound on the size of the UniformRandomGeneratorInfo, as
     * if it was written to an output stream. The implementation is different
     * with SEAL. We know that the seed is random, so the upper bound is the
     * size of the SEALHeader plus prng_type (1 byte) plus seed (8 long).
     *
     *
     * @return an upper bound on the size of the UniformRandomGeneratorInfo.
     */
    public static int saveSize() {
        return Serialization.SEAL_HEADER_SIZE + 1 + UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT * Byte.SIZE;
    }

    /**
     * prng_type
     */
    protected PrngType type;
    /**
     * seed
     */
    protected final long[] seed;

    /**
     * Creates a new UniformRandomGeneratorInfo.
     */
    public AbstractUniformRandomGeneratorInfo() {
        type = PrngType.UNKNOWN;
        seed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
    }

    /**
     * Creates a new UniformRandomGeneratorInfo.
     *
     * @param type the PRNG type.
     * @param seed the PRNG seed.
     */
    public AbstractUniformRandomGeneratorInfo(PrngType type, long[] seed) {
        this.type = type;
        this.seed = seed;
    }

    /**
     * Creates a new UniformRandomGeneratorInfo by copying a given one.
     *
     * @param copy the UniformRandomGeneratorInfo to copy from.
     */
    public AbstractUniformRandomGeneratorInfo(AbstractUniformRandomGeneratorInfo copy) {
        this.type = copy.type;
        this.seed = Arrays.copyOf(copy.seed, copy.seed.length);
    }

    /**
     * Creates a new UniformRandomGenerator object of type indicated by the PRNG
     * type and seeded with the current seed. If the current PRNG type is not
     * an official Microsoft SEAL PRNG type, the return value is nullptr.
     *
     * @return a PRNG.
     */
    public UniformRandomGenerator makePrng() {
        return switch (type) {
            case BLAKE2XB -> new Blake2xbPrng(seed);
            case SHAKE256 -> new Shake256Prng(seed);
            case UNKNOWN -> null;
        };
    }

    /**
     * Returns whether this object holds a valid PRNG type.
     *
     * @return true if holding a valid PRNG type; false otherwise.
     */
    public boolean hasValidPrngType() {
        return true;
    }

    /**
     * Returns the PRNG type.
     *
     * @return the PRNG type.
     */
    public PrngType getType() {
        return type;
    }

    /**
     * Returns a reference to the PRNG seed.
     *
     * @return a reference to the PRNG seed.
     */
    public long[] getSeed() {
        return seed;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(type)
            .append(seed)
            .toHashCode();
    }

    /**
     * Saves the PRNG flag and seed into a long array.
     *
     * @param out the long array.
     * @param offset the offset.
     */
    public void save(long[] out, int offset) {
        out[offset] = type.getValue();
        for (int i = 0; i < UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT; i++) {
            out[offset + 1 + i] = seed[i];
        }
    }

    /**
     * Loads the PRNG from a long array.
     *
     * @param in the long array.
     * @param offset the offset.
     */
    public void load(long[] in, int offset) {
        type = PrngType.getByValue((int)in[offset]);
        for (int i = 0; i < UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT; i++) {
            seed[i] = in[offset + 1 + i];
        }
    }
}
