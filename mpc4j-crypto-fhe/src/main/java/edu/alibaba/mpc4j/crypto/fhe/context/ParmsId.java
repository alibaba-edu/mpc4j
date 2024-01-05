package edu.alibaba.mpc4j.crypto.fhe.context;

import edu.alibaba.mpc4j.crypto.fhe.utils.HashFunction;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.*;
import java.util.Arrays;

/**
 * The data type to store unique identifiers of encryption parameters.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/encryptionparams.h#L43
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/30
 */
public class ParmsId {
    /**
     * parms_id length
     */
    private static final int PARMS_ID_UINT64_COUNT = HashFunction.HASH_BLOCK_UINT64_COUNT;

    /**
     * Creates a parms_id consisting of zeros.
     *
     * @return a parms_id consisting of zeros.
     */
    public static ParmsId parmsIdZero() {
        return new ParmsId();
    }

    /**
     * value of parms_id
     */
    public long[] value;

    /**
     * Creates a parms_id consisting of zeros.
     */
    public ParmsId() {
        value = new long[PARMS_ID_UINT64_COUNT];
    }

    /**
     * Creates a parms_id by deep-copying given value.
     *
     * @param value the given value.
     */
    public ParmsId(long[] value) {
        assert value.length == PARMS_ID_UINT64_COUNT;
        this.value = new long[PARMS_ID_UINT64_COUNT];
        System.arraycopy(value, 0, this.value, 0, HashFunction.HASH_BLOCK_UINT64_COUNT);
    }

    /**
     * Creates a parms_id by copying a parms_id.
     *
     * @param other the other parms_id.
     */
    public ParmsId(ParmsId other) {
        this.value = new long[other.value.length];
        System.arraycopy(other.value, 0, value, 0, value.length);
    }

    /**
     * Sets parms_id to given value.
     *
     * @param value the given value.
     */
    public void set(long[] value) {
        assert value.length == HashFunction.HASH_BLOCK_UINT64_COUNT;
        this.value = value;
    }

    /**
     * Sets parms_id to zero.
     */
    public void setZero() {
        Arrays.fill(value, 0);
    }

    /**
     * Returns whether parms_id is zero.
     *
     * @return true if parms_id is zero; false otherwise.
     */
    public boolean isZero() {
        return (value[0] == 0) && (value[1] == 0) && (value[2] == 0) && (value[3] == 0);
    }

    /**
     * Saves members to an output stream. The output is in binary format and
     * not human-readable. The output stream must have the "binary" flag set.
     *
     * @param outputStream the stream to save members to.
     * @throws IOException if I/O operations failed.
     */
    public void saveMembers(DataOutputStream outputStream) throws IOException {
        for (int i = 0; i < PARMS_ID_UINT64_COUNT; i++) {
            outputStream.writeLong(value[i]);
        }
    }

    /**
     * Loads members from an input stream overwriting the current parms_id.
     *
     * @param inputStream the stream to load members from.
     * @throws IOException if I/O operations failed.
     */
    public void loadMembers(DataInputStream inputStream) throws IOException {
        for (int i = 0; i < PARMS_ID_UINT64_COUNT; i++) {
            value[i] = inputStream.readLong();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ParmsId)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ParmsId that = (ParmsId) obj;
        return new EqualsBuilder()
            .append(this.value, that.value)
            .isEquals();
    }

    @Override
    public int hashCode() {
        // see struct hash<seal::parms_id_type>
        long result = 17;
        result = 31 * result + value[0];
        result = 31 * result + value[1];
        result = 31 * result + value[2];
        result = 31 * result + value[3];
        return (int) result;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle()).toString();
    }
}
