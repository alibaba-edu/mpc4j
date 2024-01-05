package edu.alibaba.mpc4j.crypto.fhe.serialization;

import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Cloneable SEAL parameter.
 *
 * @author Weiran Liu
 * @date 2023/12/11
 */
public interface SealCloneable {
    /**
     * Saves members to an output stream. The output is in binary format and
     * not human-readable. The output stream must have the "binary" flag set.
     *
     * @param outputStream the stream to save members to.
     * @throws IOException if I/O operations failed.
     */
    void saveMembers(OutputStream outputStream) throws IOException;

    /**
     * Loads members from an input stream overwriting the current SealCloneable.
     *
     * @param context     the SEALContext, can be null if useless when loading members.
     * @param inputStream the stream to load members from.
     * @param version     the SEAL version, can be null if useless when loading members.
     * @throws IOException if I/O operations failed.
     */
    void loadMembers(SealContext context, InputStream inputStream, SealVersion version) throws IOException;

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
    default int save(OutputStream outputStream) throws IOException {
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
    default int save(OutputStream outputStream, ComprModeType comprMode) throws IOException {
        return Serialization.save(this, outputStream, comprMode);
    }

    /**
     * Loads a SealCloneable from an input stream overwriting the current SealCloneable.
     * No checking if the validity of the SealCloneable is performed. This function
     * should not be used unless the SealCloneable comes from a fully trusted source.
     *
     * @param context     the SEALContext, can be null if useless when loading members.
     * @param inputStream the stream to read from.
     * @return the number of bytes loaded.
     * @throws IOException if I/O operations failed.
     */
    default int unsafeLoad(SealContext context, InputStream inputStream) throws IOException {
        return Serialization.load(context, this, inputStream);
    }

    /**
     * Loads a SealCloneable from an input stream overwriting the current SealCloneable.
     * The loaded SealCloneable is verified to be valid for the given SEALContext.
     *
     * @param context     the SEALContext, can be null if useless when loading members.
     * @param inputStream the stream to load the SealCloneable from.
     * @return the number of bytes loaded.
     * @throws IOException if I/O operations failed.
     */
    int load(SealContext context, InputStream inputStream) throws IOException;

    /**
     * Saves the SealCloneable to a byte array. The output is in binary format and not human-readable.
     *
     * @return the resulting byte array.
     * @throws IOException if I/O operations failed.
     */
    default byte[] save() throws IOException {
        return save(Serialization.COMPR_MODE_DEFAULT);
    }

    /**
     * Saves the SealCloneable to a byte array. The output is in binary format and not human-readable.
     *
     * @param comprMode the desired compression mode.
     * @return the resulting byte array.
     * @throws IOException if I/O operations failed.
     */
    default byte[] save(ComprModeType comprMode) throws IOException {
        return Serialization.save(this, comprMode);
    }

    /**
     * Loads a SealCloneable from a byte array overwriting the current SealCloneable.
     * No checking of the validity of the SealCloneable is performed. The function
     * should not be used unless the SealCloneable comes from a fully trusted source.
     *
     * @param context the SEALContext, can be null if useless when loading members.
     * @param in      the byte array to load the SealCloneable from.
     * @throws IOException if I/O operations failed.
     */
    default void unsafeLoad(SealContext context, byte[] in) throws IOException {
        Serialization.load(context, this, in);
    }

    /**
     * Loads a SealCloneable from an input stream overwriting the current SealCloneable.
     * The loaded SealCloneable is verified to be valid for the given SEALContext.
     *
     * @param context the SEALContext, can be null if useless when loading members.
     * @param in      the byte array to load the SealCloneable from.
     * @throws IOException if I/O operations failed.
     */
    void load(SealContext context, byte[] in) throws IOException;
}
