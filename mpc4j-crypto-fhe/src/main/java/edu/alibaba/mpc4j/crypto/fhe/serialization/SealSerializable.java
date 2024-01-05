package edu.alibaba.mpc4j.crypto.fhe.serialization;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Serializable SEAL parameter.
 *
 * @author Weiran Liu
 * @date 2023/12/14
 */
public class SealSerializable<T extends SealCloneable> {
    /**
     * cloneable parameter
     */
    private final T cloneable;

    public SealSerializable(T cloneable) {
        this.cloneable = cloneable;
    }

    /**
     * Saves the SEALCloneable and compresses the output according to the given
     * compr_mode_type. The resulting data is written to stream and is prepended
     * by the given compr_mode_type and the total size of the data to facilitate
     * deserialization. In typical use-cases save_members in SEALCloneable would
     * be a function that serializes the member variables of an object to the
     * given stream.
     *
     * @param outputStream the stream to save the SEALCloneable to.
     * @return the number of bytes saved.
     * @throws IOException if I/O operations failed.
     */
    public int save(OutputStream outputStream) throws IOException {
        return save(outputStream, Serialization.COMPR_MODE_DEFAULT);
    }

    /**
     * Saves the SEALCloneable and compresses the output according to the given
     * compr_mode_type. The resulting data is written to stream and is prepended
     * by the given compr_mode_type and the total size of the data to facilitate
     * deserialization. In typical use-cases save_members in SEALCloneable would
     * be a function that serializes the member variables of an object to the
     * given stream.
     *
     * @param outputStream the stream to save the SEALCloneable to.
     * @param comprMode    the desired compression mode.
     * @return the number of bytes saved.
     * @throws IOException if I/O operations failed.
     */
    public int save(OutputStream outputStream, ComprModeType comprMode) throws IOException {
        return cloneable.save(outputStream, comprMode);
    }

    /**
     * Saves the SealCloneable to a byte array. The output is in binary format and not human-readable.
     *
     * @return the resulting byte array.
     * @throws IOException if I/O operations failed.
     */
    public byte[] save() throws IOException {
        return save(Serialization.COMPR_MODE_DEFAULT);
    }

    /**
     * Saves the SealCloneable to a byte array. The output is in binary format and not human-readable.
     *
     * @param comprMode the desired compression mode.
     * @return the resulting byte array.
     * @throws IOException if I/O operations failed.
     */
    public byte[] save(ComprModeType comprMode) throws IOException {
        return cloneable.save(comprMode);
    }
}
