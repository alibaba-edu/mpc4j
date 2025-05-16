package edu.alibaba.mpc4j.crypto.fhe.seal.modulus;

import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.seal.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintArithmeticSmallMod;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.*;
import java.util.Arrays;

/**
 * Represent an integer modulus of up to 61 bits. An instance of the Modulus class represents a non-negative integer
 * modulus up to 61 bits. In particular, the encryption parameter plain_modulus, and the primes in coeff_modulus, are
 * represented by instances of Modulus. The purpose of this class is to perform and store the pre-computation required
 * by Barrett reduction.
 * <p>
 * The implementation is from <code>Modulus</code> in
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/modulus.h#L33">
 * modulus.h
 * </a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/3
 */
public class Modulus extends AbstractModulus implements SealCloneable {
    /**
     * Creates modulus for a long array.
     *
     * @param values a long array.
     * @return a modulus array.
     */
    public static Modulus[] createModulus(long[] values) {
        return Arrays.stream(values).mapToObj(Modulus::new).toArray(Modulus[]::new);
    }

    /**
     * Creates a Modulus instance. The value of the Modulus is set to zero by default.
     */
    public Modulus() {
        super();
    }

    /**
     * Creates a Modulus instance. The value of the Modulus is set to the given value.
     *
     * @param value a given value.
     */
    public Modulus(long value) {
        super(value);
    }

    /**
     * Creates a new Modulus by copying a given one.
     *
     * @param other the Modulus to copy from.
     */
    public Modulus(Modulus other) {
        super(other);
    }

    @Override
    public long reduce(long input) {
        if (value == 0) {
            throw new IllegalArgumentException("cannot reduce modulo a zero modulus");
        }
        return UintArithmeticSmallMod.barrettReduce64(input, this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Modulus that)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return new EqualsBuilder()
            .append(this.value, that.value)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        DataOutputStream stream = new DataOutputStream(outputStream);
        stream.writeLong(value);
        stream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        DataInputStream stream = new DataInputStream(inputStream);
        setValue(stream.readLong());
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
