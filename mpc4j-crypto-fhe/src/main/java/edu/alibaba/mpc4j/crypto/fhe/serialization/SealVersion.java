package edu.alibaba.mpc4j.crypto.fhe.serialization;

/**
 * Holds Microsoft SEAL version information. A SEALVersion contains four values:
 * <li>The major version number;</li>
 * <li>The minor version number;</li>
 * <li>The patch version number;</li>
 * <li>The tweak version number.</li>
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/src/seal/version.h
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/12/11
 */
public class SealVersion {
    /**
     * Holds the major version number.
     */
    final byte major;
    /**
     * Holds the minor version number.
     */
    final byte minor;
    /**
     * Holds the patch version number;
     */
    final byte patch;
    /**
     * Holds the tweak
     */
    final byte tweak;

    public SealVersion(byte major, byte minor, byte patch, byte tweak) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.tweak = tweak;
    }

    private SealVersion() {
        major = Serialization.SEAL_MAJOR_VERSION;
        minor = Serialization.SEAL_MINOR_VERSION;
        patch = Serialization.SEAL_VERSION_PATCH;
        tweak = 0;
    }

    /**
     * singleton mode
     */
    private static final SealVersion SEAL_VERSION = new SealVersion();

    /**
     * Gets the SEAL version.
     *
     * @return the SEAL version.
     */
    public static SealVersion getInstance() {
        return SEAL_VERSION;
    }
}
