package edu.alibaba.mpc4j.crypto.fhe.seal.rand;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.Serialization;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.*;

/**
 * Uniform random generator information.
 * <p>
 * The implementation is from <code>UniformRandomGeneratorInfo</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L55">randomgen.h</a>
 *
 * @author Weiran Liu
 * @date 2023/12/13
 */
public class UniformRandomGeneratorInfo extends AbstractUniformRandomGeneratorInfo implements SealCloneable {
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
     * Creates a new UniformRandomGeneratorInfo.
     */
    public UniformRandomGeneratorInfo() {
        super();
    }

    /**
     * Creates a new UniformRandomGeneratorInfo.
     *
     * @param type the PRNG type.
     * @param seed the PRNG seed.
     */
    public UniformRandomGeneratorInfo(PrngType type, long[] seed) {
        super(type, seed);
    }

    /**
     * Creates a new UniformRandomGeneratorInfo by copying a given one.
     *
     * @param copy the UniformRandomGeneratorInfo to copy from.
     */
    public UniformRandomGeneratorInfo(UniformRandomGeneratorInfo copy) {
        super(copy);
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
    public void saveMembers(OutputStream outputStream) throws IOException {
        LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(outputStream);
        stream.writeByte(type.getValue());
        for (int i = 0; i < UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT; i++) {
            stream.writeLong(seed[i]);
        }
        stream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        LittleEndianDataInputStream stream = new LittleEndianDataInputStream(inputStream);
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
}
