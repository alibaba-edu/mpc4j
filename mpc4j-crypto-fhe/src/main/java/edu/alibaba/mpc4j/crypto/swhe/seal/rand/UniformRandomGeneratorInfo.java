package edu.alibaba.mpc4j.crypto.swhe.seal.rand;

import edu.alibaba.mpc4j.crypto.swhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.swhe.seal.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.swhe.seal.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.swhe.seal.serialization.Serialization;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;
import java.util.Arrays;

/**
 * Uniform random generator information.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L55">
 * UniformRandomGeneratorInfo in randomgen.h
 * </a>
 *
 * @author Weiran Liu
 * @date 2023/12/13
 */
public class UniformRandomGeneratorInfo implements SealCloneable {
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
    private PrngType type;
    /**
     * seed
     */
    private final long[] seed;

    /**
     * Creates a new UniformRandomGeneratorInfo.
     */
    public UniformRandomGeneratorInfo() {
        type = PrngType.UNKNOWN;
        seed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
    }

    /**
     * Creates a new UniformRandomGeneratorInfo.
     *
     * @param type the PRNG type.
     * @param seed the PRNG seed.
     */
    public UniformRandomGeneratorInfo(PrngType type, long[] seed) {
        this.type = type;
        this.seed = seed;
    }

    /**
     * Creates a new UniformRandomGeneratorInfo by copying a given one.
     *
     * @param copy the UniformRandomGeneratorInfo to copy from.
     */
    public UniformRandomGeneratorInfo(UniformRandomGeneratorInfo copy) {
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
            case SHA1PRNG -> new Sha1Prng(seed);
            // TODO: implement black2xb and Shake256
            case BLAKE2XB, SHAKE256, UNKNOWN -> null;
        };
    }

    /**
     * Returns whether this object holds a valid PRNG type.
     *
     * @return true if holding a valid PRNG type; false otherwise.
     */
    public boolean hasValidPrngType() {
        return switch (type) {
            case SHA1PRNG, UNKNOWN -> true;
            // TODO: implement black2xb and Shake256
            case BLAKE2XB, SHAKE256 -> false;
        };
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UniformRandomGeneratorInfo that)) {
            return false;
        }
        return new EqualsBuilder()
            .append(this.type, that.type)
            .append(this.seed, that.seed)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(type)
            .append(seed)
            .toHashCode();
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        DataOutputStream stream = new DataOutputStream(outputStream);
        stream.writeByte(type.getValue());
        for (int i = 0; i < UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT; i++) {
            stream.writeLong(seed[i]);
        }
        stream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        DataInputStream stream = new DataInputStream(inputStream);
        type = PrngType.getByValue(stream.readByte());
        if (!hasValidPrngType()) {
            throw new IllegalArgumentException("prng_type is invalid");
        }

        // Read the seed data
        for (int i = 0; i < UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT; i++) {
            seed[i] = stream.readLong();
        }
        stream.close();
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        return unsafeLoad(context, inputStream);
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        unsafeLoad(context, in);
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
