package edu.alibaba.mpc4j.crypto.fhe.serialization;

import com.github.luben.zstd.Zstd;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * Class to provide functionality for serialization. Most users of the library
 * should never have to call these functions explicitly, as they are called
 * internally by functions such as Ciphertext::save and Ciphertext::load.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/src/seal/serialization.h
 *
 * @author Weiran Liu
 * @date 2023/12/11
 */
public class Serialization {
    /**
     * The compression mode used by default; prefer Zstandard
     */
    public static ComprModeType COMPR_MODE_DEFAULT = ComprModeType.ZSTD;
    /**
     * The magic value indicating a Microsoft SEAL header. SEAL uses '0xA15E' while we use a different '0xA15F'.
     */
    public static final short SEAL_MAGIC = (short) 0xA15F;
    /**
     * The size in bytes of the SEALHeader.
     */
    public static final short SEAL_HEADER_SIZE = 0x10;
    /**
     * Microsoft SEAL's major version number
     */
    public static final byte SEAL_MAJOR_VERSION = 4;
    /**
     * Microsoft SEAL's minor version number
     */
    public static final byte SEAL_MINOR_VERSION = 0;
    /**
     * Microsoft SEAL's patch version number
     */
    public static final byte SEAL_VERSION_PATCH = 0;

    /**
     * Returns true if the given index corresponds to a supported compression mode.
     *
     * @param comprMode the compression mode to validate.
     * @return true if the given index corresponds to a supported compression mode.
     */
    public static boolean isSupportedComprMode(int comprMode) {
        // Java does not support zlib compression mode
        ComprModeType comprModeType = ComprModeType.getByValue(comprMode);
        return isSupportedComprMode(comprModeType);
    }

    /**
     * Returns true if the given value corresponds to a supported compression mode.
     *
     * @param comprMode the compression mode to validate.
     * @return true if the given index corresponds to a supported compression mode.
     */
    public static boolean isSupportedComprMode(ComprModeType comprMode) {
        switch (comprMode) {
            case NONE:
            case ZLIB:
            case ZSTD:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true if the SEALHeader has a version number compatible with this version of Microsoft SEAL.
     *
     * @param header the SEALHeader.
     * @return true if the SEALHeader has a version number compatible with this version; false otherwise.
     */
    public static boolean isCompatibleVersion(SealHeader header) {
        // Java only support exact the same version
        return header.majorVersion == SEAL_MAJOR_VERSION && header.minorVersion == SEAL_MINOR_VERSION;
    }

    /**
     * Returns true if the given SEALHeader is valid for this version of Microsoft SEAL.
     *
     * @param header the SEALHeader.
     * @return true if the given SEALHeader is valid; false otherwise.
     */
    public static boolean isValidHeader(SealHeader header) {
        if (header.magic != SEAL_MAGIC) {
            return false;
        }
        if (header.headerSize != SEAL_HEADER_SIZE) {
            return false;
        }
        if (!isCompatibleVersion(header)) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (!isSupportedComprMode(header.comprMode.getValue())) {
            return false;
        }
        return true;
    }

    /**
     * Saves a SEALHeader to a given stream. The output is in binary format and
     * not human-readable. The output stream must have the "binary" flag set.
     *
     * @param header       The SEALHeader to save to the stream.
     * @param outputStream The stream to save the SEALHeader to.
     * @return the number of bytes saved.
     * @throws IOException if I/O operations failed.
     */
    public static int saveHeader(SealHeader header, OutputStream outputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeShort(header.magic);
        dataOutputStream.writeByte(header.headerSize);
        dataOutputStream.writeByte(header.majorVersion);
        dataOutputStream.writeByte(header.minorVersion);
        dataOutputStream.writeByte(header.comprMode.getValue());
        dataOutputStream.writeShort(header.reserved);
        dataOutputStream.writeLong(header.size);
        dataOutputStream.close();
        // Return the size of the SEALHeader
        return SEAL_HEADER_SIZE;
    }

    /**
     * Loads a SEALHeader from a given stream.
     *
     * @param inputStream The stream to load the SEALHeader from.
     * @param header      The SEALHeader to populate with the loaded data.
     * @return the number of bytes loaded.
     * @throws IOException if I/O operations failed.
     */
    public static int loadHeader(InputStream inputStream, SealHeader header) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        header.magic = dataInputStream.readShort();
        header.headerSize = dataInputStream.readByte();
        header.majorVersion = dataInputStream.readByte();
        header.minorVersion = dataInputStream.readByte();
        header.comprMode = ComprModeType.getByValue(dataInputStream.readByte());
        header.reserved = dataInputStream.readShort();
        header.size = dataInputStream.readLong();
        dataInputStream.close();
        if (!isValidHeader(header)) {
            throw new IllegalArgumentException("Invalid header");
        }
        // Return the size of the SEALHeader
        return SEAL_HEADER_SIZE;
    }

    /**
     * Saves the SEALCloneable and compresses the output according to the given
     * compr_mode_type. The resulting data is written to stream and is prepended
     * by the given compr_mode_type and the total size of the data to facilitate
     * deserialization. In typical use-cases save_members in SEALCloneable would
     * be a function that serializes the member variables of an object to the
     * given stream.
     *
     * @param cloneable    the given SEALCloneable.
     * @param outputStream the stream to save the SEALCloneable to.
     * @param comprMode    the desired compression mode.
     * @return the number of bytes saved.
     * @throws IOException if I/O operations failed.
     */
    public static int save(SealCloneable cloneable, OutputStream outputStream, ComprModeType comprMode)
        throws IOException {
        if (!isSupportedComprMode(comprMode)) {
            throw new IllegalArgumentException("unsupported compression mode");
        }
        int outSize;
        // Create the header
        SealHeader header = new SealHeader();
        header.comprMode = comprMode;
        // First save_members to a temporary byte stream; set the size of the temporary stream to be right from
        // the start to avoid extra reallocs.
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
        cloneable.saveMembers(tempStream);
        switch (comprMode) {
            case NONE:
                // We set the compression mode and size here, and save the header
                outSize = SEAL_HEADER_SIZE + tempStream.size();
                header.size = outSize;
                saveHeader(header, outputStream);
                // Write rest of the data
                tempStream.writeTo(outputStream);
                break;
            case ZLIB:
                // First save_members to a temporary byte stream; set the size of the temporary stream to be right from
                // the start to avoid extra reallocs.
                byte[] zlibData = tempStream.toByteArray();
                ByteArrayOutputStream zlibByteArrayOutputStream = new ByteArrayOutputStream();
                DeflaterOutputStream zlibOutputStream = new DeflaterOutputStream(zlibByteArrayOutputStream);
                zlibOutputStream.write(zlibData);
                zlibOutputStream.flush();
                zlibOutputStream.finish();
                zlibOutputStream.close();
                byte[] zlibCompress = zlibByteArrayOutputStream.toByteArray();
                zlibByteArrayOutputStream.close();
                // After compression, write_header_deflate_buffer will write the final size to the given header and
                // write the header to stream, before writing the compressed output.
                outSize = SEAL_HEADER_SIZE + zlibCompress.length;
                header.size = outSize;
                saveHeader(header, outputStream);
                outputStream.write(zlibCompress);
                break;
            case ZSTD:
                // First save_members to a temporary byte stream; set the size of the temporary stream to be right from
                // the start to avoid extra reallocs.
                byte[] zstdData = tempStream.toByteArray();
                byte[] zstdCompress = Zstd.compress(zstdData);
                // After compression, write_header_deflate_buffer will write the final size to the given header and
                // write the header to stream, before writing the compressed output.
                outSize = SEAL_HEADER_SIZE + zstdCompress.length;
                header.size = outSize;
                saveHeader(header, outputStream);
                outputStream.write(zstdCompress);
                break;
            default:
                throw new IllegalArgumentException("unsupported compression mode");
        }
        tempStream.close();
        return outSize;
    }

    /**
     * Deserializes data from stream that was serialized by Save. Once stream has
     * been decompressed (depending on compression mode), load_members in
     * SEALCloneable is applied to the decompressed stream. In typical use-cases
     * load_members in SEALCloneable would be a function that deserializes the
     * member variables of an object from the given stream.
     *
     * @param context     the SEALContext.
     * @param cloneable   the result that writes into.
     * @param inputStream the stream to read from.
     * @return the number of bytes loaded.
     * @throws IOException if I/O operations failed.
     */
    public static int load(SealContext context, SealCloneable cloneable, InputStream inputStream) throws IOException {
        int inSize = 0;
        SealHeader header = new SealHeader();

        inSize += loadHeader(inputStream, header);
        if (!isCompatibleVersion(header)) {
            throw new IllegalArgumentException("incompatible version");
        }
        if (!isValidHeader(header)) {
            throw new IllegalArgumentException("loaded SEALHeader is invalid");
        }

        // Read header version information so we can call, if necessary, the correct variant of load_members.
        SealVersion version = new SealVersion(header.majorVersion, header.minorVersion, (byte) 0, (byte) 0);

        int memberSize;
        byte[] member;
        switch (header.comprMode) {
            case NONE:
                // read rest of the data
                memberSize = (int) header.size - SEAL_HEADER_SIZE;
                member = new byte[memberSize];
                inSize += inputStream.read(member);
                if (inSize != (int) header.size) {
                    throw new IllegalArgumentException("invalid data size");
                }
                break;
            case ZLIB:
                int zlibSize = (int) header.size - SEAL_HEADER_SIZE;
                byte[] zlibCompress = new byte[zlibSize];
                inSize += inputStream.read(zlibCompress);
                if (inSize != (int) header.size) {
                    throw new IllegalArgumentException("invalid data size");
                }
                ByteArrayOutputStream zlibByteArrayOutputStream = new ByteArrayOutputStream();
                InflaterOutputStream zlibOutputStream = new InflaterOutputStream(zlibByteArrayOutputStream);
                zlibOutputStream.write(zlibCompress);
                zlibOutputStream.flush();
                zlibOutputStream.finish();
                zlibOutputStream.close();
                member = zlibByteArrayOutputStream.toByteArray();
                zlibByteArrayOutputStream.close();
                break;
            case ZSTD:
                int zstdSize = (int) header.size - SEAL_HEADER_SIZE;
                byte[] zstdCompress = new byte[zstdSize];
                inSize += inputStream.read(zstdCompress);
                if (inSize != (int) header.size) {
                    throw new IllegalArgumentException("invalid data size");
                }
                int zstdMemberSize = (int) Zstd.getFrameContentSize(zstdCompress);
                member = Zstd.decompress(zstdCompress, zstdMemberSize);
                break;
            default:
                throw new IllegalArgumentException("unsupported compression mode");
        }
        ByteArrayInputStream tempStream = new ByteArrayInputStream(member);
        cloneable.loadMembers(context, tempStream, version);
        tempStream.close();
        return inSize;
    }

    /**
     * Saves the SEALCloneable and compresses the output according to the given
     * compr_mode_type. The resulting data is written to a given memory location
     * and is prepended by the given compr_mode_type and the total size of the
     * data to facilitate deserialization. In typical use-cases in SEALCloneable
     * would be a function that serializes the member variables of an object to
     * the given stream.
     *
     * @param cloneable the given SEALCloneable.
     * @param comprMode the desired compression mode.
     * @return the serialized result.
     * @throws IOException if I/O operations failed.
     */
    public static byte[] save(SealCloneable cloneable, ComprModeType comprMode) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        save(cloneable, outputStream, comprMode);
        outputStream.close();
        return outputStream.toByteArray();
    }

    /**
     * Deserializes data from a memory location that was serialized by Save.
     * Once the data has been decompressed (depending on compression mode),
     * load_members in SealCloneable is applied to the decompressed stream.
     * In typical use-cases load_members in SealCloneable would be a function
     * that deserializes the member variables of an object from the given stream.
     *
     * @param context   the SEALContext.
     * @param cloneable the result that writes into.
     * @param in        the data to read from.
     * @throws IOException if I/O operations failed.
     */
    public static void load(SealContext context, SealCloneable cloneable, byte[] in) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(in);
        load(context, cloneable, inputStream);
        inputStream.close();
    }
}
