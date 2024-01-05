package edu.alibaba.mpc4j.crypto.fhe.rand;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.fhe.serialization.Serialization;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;
import java.util.Arrays;

/**
 * Uniform random generator information.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/src/seal/randomgen.h#L55
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
        switch (type) {
            case SHA1PRNG:
                return new Sha1Prng(seed);
            case BLAKE2XB:
            case SHAKE256:
            case UNKNOWN:
            default:
                return null;
        }
    }

    /**
     * Returns whether this object holds a valid PRNG type.
     *
     * @return true if holding a valid PRNG type; false otherwise.
     */
    public boolean hasValidPrngType() {
        switch (type) {
            case SHA1PRNG:
            case UNKNOWN:
                return true;
            case BLAKE2XB:
            case SHAKE256:
            default:
                return false;
        }
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
        if (!(o instanceof UniformRandomGeneratorInfo)) {
            return false;
        }
        UniformRandomGeneratorInfo that = (UniformRandomGeneratorInfo) o;
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
