package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt;

/**
 * SJ23 UCPSI params.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23PeqtUcpsiParams {
    /**
     * bin num
     */
    public final int binNum;
    /**
     * max partition size per bin
     */
    public final int maxPartitionSizePerBin;
    /**
     * item encoded slot size
     */
    public final int itemEncodedSlotSize;
    /**
     * Paterson-Stockmeyer low degree
     */
    public final int psLowDegree;
    /**
     * query powers
     */
    public final int[] queryPowers;
    /**
     * plain modulus size
     */
    public final int plainModulusSize;
    /**
     * plain modulus
     */
    public final long plainModulus;
    /**
     * poly modulus degree
     */
    public final int polyModulusDegree;
    /**
     * coeff modulus bits
     */
    public final int[] coeffModulusBits;
    /**
     * item per ciphertext
     */
    public final int itemPerCiphertext;
    /**
     * ciphertext num
     */
    public final int ciphertextNum;
    /**
     * l bit length
     */
    public final int l;
    /**
     * encryption params
     */
    public final byte[] encryptionParams;

    private Sj23PeqtUcpsiParams(int binNum, int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                                int[] queryPowers, int plainModulusSize, long plainModulus, int polyModulusDegree,
                                int[] coeffModulusBits) {
        this.binNum = binNum;
        this.maxPartitionSizePerBin = maxPartitionSizePerBin;
        this.itemEncodedSlotSize = itemEncodedSlotSize;
        this.psLowDegree = psLowDegree;
        this.queryPowers = queryPowers;
        this.plainModulusSize = plainModulusSize;
        this.plainModulus = plainModulus;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusBits = coeffModulusBits;
        this.itemPerCiphertext = polyModulusDegree / itemEncodedSlotSize;
        this.ciphertextNum = binNum / itemPerCiphertext;
        this.l = itemEncodedSlotSize * plainModulusSize;
        this.encryptionParams = Sj23PeqtUcpsiNativeUtils.genEncryptionParameters(
            polyModulusDegree, plainModulus, coeffModulusBits
        );
    }

    /**
     * serve log size 20, client log size 8.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_8 = new Sj23PeqtUcpsiParams(
        1 << 12, 228, 2,
        4, new int[]{1, 2, 3, 4, 5, 10, 15, 35, 55, 75, 95, 115, 125, 130, 140},
        32, 8589852673L, 1 << 13, new int[]{50, 50, 50, 47}
    );

    /**
     * serve log size 20, client log size 12.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_12 = new Sj23PeqtUcpsiParams(
        1 << 13, 98, 2,
        8, new int[]{1, 3, 4, 9, 27},
        33, 17179672577L, 1 << 14, new int[]{50, 50, 50, 50, 46}
    );

    /**
     * serve log size 20, client log size 13.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_13 = new Sj23PeqtUcpsiParams(
        1 << 14, 98, 1,
        8, new int[]{1, 3, 4, 9, 27},
        59, 1152921504606748673L, 1 << 14, new int[]{48, 48, 48, 49, 49, 49, 49, 49, 49}
    );

    /**
     * serve log size 20, client log size 16.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_16 = new Sj23PeqtUcpsiParams(
        3 * (1 << 15), 40, 2,
        0, new int[]{1, 3, 4, 9, 11, 16, 17, 19, 20},
        34, 34359410689L, 1 << 13, new int[]{50, 50, 50, 50}
    );

    /**
     * serve log size 24, client log size 12.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_12 = new Sj23PeqtUcpsiParams(
        1 << 13, 1304, 2,
        44, new int[]{1, 3, 11, 18, 45, 225},
        37, 274877153281L, 1 << 14, new int[]{60, 60, 60, 50, 50, 36}
    );

    /**
     * serve log size 24, client log size 13.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_13 = new Sj23PeqtUcpsiParams(
        1 << 14, 1304, 2,
        44, new int[]{1, 3, 11, 18, 45, 225},
        38, 549755486209L, 1 << 14, new int[]{60, 60, 60, 50, 50, 40}
    );

    /**
     * serve log size 24, client log size 16.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_16 = new Sj23PeqtUcpsiParams(
        3 * (1 << 15), 98, 2,
        8, new int[]{1, 3, 4, 9, 27},
        35, 68718428161L, 1 << 14, new int[]{55, 50, 50, 50, 50}
    );

    /**
     * serve log size 26, client log size 12.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_26_CLIENT_LOG_SIZE_12 = new Sj23PeqtUcpsiParams(
        1 << 13, 1304, 2,
        44, new int[]{1, 3, 11, 18, 45, 225},
        37, 274877153281L, 1 << 14, new int[]{60, 56, 56, 40, 40, 40}
    );

    /**
     * serve log size 26, client log size 16.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_26_CLIENT_LOG_SIZE_16 = new Sj23PeqtUcpsiParams(
        3 * (1 << 15), 98, 2,
        8, new int[]{1, 3, 4, 9, 27},
        35, 68718428161L, 1 << 14, new int[]{55, 50, 50, 50, 50}
    );

    /**
     * get SJ23 UCPSI params.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return SJ23 UCPSI params.
     */
    static public Sj23PeqtUcpsiParams getParams(int serverElementSize, int clientElementSize) {
        if (serverElementSize <= 1 << 20) {
            if (clientElementSize <= 1 << 8) {
                return SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_8;
            } else if (clientElementSize <= 1 << 12) {
                return SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_12;
            } else if (clientElementSize <= 1 << 13) {
                return SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_13;
            } else if (clientElementSize <= 1 << 16) {
                return SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_16;
            }
        } else if (serverElementSize <= 1 << 24) {
            if (clientElementSize <= 1 << 12) {
                return SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_12;
            } else if (clientElementSize <= 1 << 13) {
                return SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_13;
            } else if (clientElementSize <= 1 << 16) {
                return SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_16;
            }
        } else if (serverElementSize <= 1 << 26) {
            if (clientElementSize <= 1 << 12) {
                return SERVER_LOG_SIZE_26_CLIENT_LOG_SIZE_12;
            } else if (clientElementSize <= 1 << 16) {
                return SERVER_LOG_SIZE_26_CLIENT_LOG_SIZE_16;
            }
        }
        throw new IllegalArgumentException("Invalid element size");
    }
}
