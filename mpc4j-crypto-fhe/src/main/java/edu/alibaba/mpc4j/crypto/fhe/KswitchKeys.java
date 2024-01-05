package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.ParmsId;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.serialization.ComprModeType;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealCloneable;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealVersion;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValCheck;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;
import java.util.Arrays;

/**
 * Class to store keyswitching keys. It should never be necessary for normal
 * users to create an instance of KSwitchKeys. This class is used strictly as
 * a base class for RelinKeys and GaloisKeys classes.
 * <p></p>
 * Concretely, keyswitching is used to change a ciphertext encrypted with one
 * key to be encrypted with another key. It is a general technique and is used
 * in relinearization and Galois rotations. A keyswitching key contains a sequence
 * (vector) of keys. In RelinKeys, each key is an encryption of a power of the
 * secret key. In GaloisKeys, each key corresponds to a type of rotation.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/kswitchkeys.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/9/14
 */
public class KswitchKeys implements SealCloneable {
    /**
     * key switching keys
     */
    protected PublicKey[][] keys = new PublicKey[0][0];
    /**
     * parms_id
     */
    private ParmsId parmsId = ParmsId.parmsIdZero();

    /**
     * Creates an empty KSwitchKeys.
     */
    protected KswitchKeys() {
        // empty
    }

    /**
     * Resizes the keyswitching keys.
     *
     * @param num number of keyswitching keys.
     */
    public void resize(int num) {
        // here we must set the second dimension to be zero, in case of NullPointerException.
        keys = new PublicKey[num][0];
    }

    /**
     * Returns the current number of keyswitching keys. Only keys that are
     * non-empty are counted.
     *
     * @return the current number of keyswitching keys.
     */
    public int size() {
        return Arrays.stream(keys)
            .mapToInt(key -> key.length > 0 ? 1 : 0)
            .sum();
    }

    /**
     * Returns a reference to the KSwitchKeys data.
     *
     * @return a reference to the KSwitchKeys data.
     */
    public PublicKey[][] data() {
        return keys;
    }

    /**
     * Returns a reference to a keyswitching key at a given index.
     *
     * @param index the index of the keyswitching key.
     * @return a key switching key.
     */
    public PublicKey[] data(int index) {
        // Java does not require checking index >= keys_.size()
        if (keys[index].length == 0) {
            throw new IllegalArgumentException("keyswitching key does not exist");
        }
        return keys[index];
    }

    /**
     * Returns a reference to parms_id.
     *
     * @return a reference to parms_id.
     */
    public ParmsId parmsId() {
        return parmsId;
    }

    /**
     * Sets a given parms_id to key switching keys.
     *
     * @param parmsId the given parms_id.
     */
    public void setParmsId(ParmsId parmsId) {
        this.parmsId = parmsId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KswitchKeys)) {
            return false;
        }
        KswitchKeys that = (KswitchKeys) o;
        return new EqualsBuilder()
            .append(keys, that.keys)
            .append(parmsId, that.parmsId)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(keys)
            .append(parmsId)
            .toHashCode();
    }

    @Override
    public void saveMembers(OutputStream outputStream) throws IOException {
        DataOutputStream stream = new DataOutputStream(outputStream);

        // Save the parms_id
        parmsId.saveMembers(stream);

        // Save the size of keys_
        stream.writeLong(keys.length);

        // Now loop again over keys_dim1
        //noinspection ForLoopReplaceableByForEach
        for (int index = 0; index < keys.length; index++) {
            // Save second dimension of keys_
            stream.writeLong(keys[index].length);

            // Loop over keys_dim2 and save all (or none)
            for (int j = 0; j < keys[index].length; j++) {
                // Save the key
                keys[index][j].save(outputStream, ComprModeType.NONE);
            }
        }
        stream.close();
    }

    @Override
    public void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException {
        // Verify parameters
        if (!context.isParametersSet()) {
            throw new IllegalArgumentException("encryption parameters are not set correctly");
        }

        DataInputStream stream = new DataInputStream(inputStream);

        // Read the parms_id
        parmsId.loadMembers(stream);

        // Read in the size of keys_
        int keysDim1 = (int) stream.readLong();

        // Reserve first for dimension of keys_
        keys = new PublicKey[keysDim1][];

        // Loop over the first dimension of keys_
        for (int index = 0; index < keysDim1; index++) {
            // Read the size of the second dimension
            int keysDim2 = (int) stream.readLong();

            // Don't resize; only reserve
            keys[index] = new PublicKey[keysDim2];
            for (int j = 0; j < keysDim2; j++) {
                PublicKey key = new PublicKey();
                key.unsafeLoad(context, stream);
                keys[index][j] = key;
            }
        }
        stream.close();
    }

    @Override
    public int load(SealContext context, InputStream inputStream) throws IOException {
        int inSize = unsafeLoad(context, inputStream);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("KSwitchKeys data is invalid");
        }
        return inSize;
    }

    @Override
    public void load(SealContext context, byte[] in) throws IOException {
        unsafeLoad(context, in);
        if (!ValCheck.isValidFor(this, context)) {
            throw new IllegalArgumentException("KSwitchKeys data is invalid");
        }
    }
}
