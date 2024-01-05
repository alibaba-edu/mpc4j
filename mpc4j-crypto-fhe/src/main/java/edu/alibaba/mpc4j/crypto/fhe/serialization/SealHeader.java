package edu.alibaba.mpc4j.crypto.fhe.serialization;

/**
 * Struct to contain metadata for serialization comprising the following fields:
 * <li>a magic number identifying this is a SEALHeader struct (2 bytes)</li>
 * <li>size in bytes of the SEALHeader struct (1 byte)</li>
 * <li>Microsoft SEAL's major version number (1 byte)</li>
 * <li>Microsoft SEAL's minor version number (1 byte)</li>
 * <li>a compr_mode_type indicating whether data after the header is compressed (1 byte)</li>
 * <li>reserved for future use and data alignment (2 bytes)</li>
 * <li>the size in bytes of the entire serialized object, including the header (8 bytes)</li>
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/src/seal/serialization.h#76
 *
 * @author Weiran Liu
 * @date 2023/12/11
 */
public class SealHeader {
    /**
     * a magic number identifying this is a SEALHeader struct (2 bytes)
     */
    short magic = Serialization.SEAL_MAGIC;
    /**
     * size in bytes of the SEALHeader struct (1 byte)
     */
    byte headerSize = Serialization.SEAL_HEADER_SIZE;
    /**
     * Microsoft SEAL's major version number (1 byte)
     */
    byte majorVersion = Serialization.SEAL_MAJOR_VERSION;
    /**
     * Microsoft SEAL's minor version number (1 byte)
     */
    byte minorVersion = Serialization.SEAL_MINOR_VERSION;
    /**
     * a compr_mode_type indicating whether data after the header is compressed (1 byte)
     */
    ComprModeType comprMode = ComprModeType.NONE;
    /**
     * reserved for future use and data alignment (2 bytes)
     */
    short reserved = 0;
    /**
     * the size in bytes of the entire serialized object, including the header (8 bytes)
     */
    public long size = 0;
}
