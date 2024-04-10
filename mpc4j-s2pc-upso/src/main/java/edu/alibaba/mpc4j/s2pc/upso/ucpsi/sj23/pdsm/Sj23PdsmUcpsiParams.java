package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm;

import java.util.stream.IntStream;

/**
 * SJ23 UCPSI params.
 *
 * @author Liqiang Peng
 * @date 2023/7/21
 */
public class Sj23PdsmUcpsiParams {
    /**
     * bin num
     */
    public final int binNum;
    /**
     * max partition size per bin
     */
    public final int maxPartitionSizePerBin;
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
    /**
     * alpha upper bound
     */
    public final int alphaUpperBound;

    private Sj23PdsmUcpsiParams(int binNum, int maxPartitionSizePerBin, int alphaUpperBound) {
        this.binNum = binNum;
        this.maxPartitionSizePerBin = maxPartitionSizePerBin;
        this.queryPowers = IntStream.range(0, maxPartitionSizePerBin).map(i -> i + 1).toArray();
        this.plainModulusSize = 59;
        this.plainModulus = 1152921504606748673L;
        this.polyModulusDegree = 1 << 13;
        this.itemPerCiphertext = polyModulusDegree;
        this.ciphertextNum = binNum / itemPerCiphertext;
        this.l = plainModulusSize;
        this.alphaUpperBound = alphaUpperBound;
        this.encryptionParams = Sj23PdsmUcpsiNativeUtils.genEncryptionParameters(polyModulusDegree, plainModulus);
    }

    /**
     * serve log size 20, client log size 12.
     */
    public static final Sj23PdsmUcpsiParams SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_12 = new Sj23PdsmUcpsiParams(
        1 << 13, 24, 4
    );

    /**
     * serve log size 20, client log size 13.
     */
    public static final Sj23PdsmUcpsiParams SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_13 = new Sj23PdsmUcpsiParams(
        1 << 14, 18,  4
    );

    /**
     * serve log size 20, client log size 16.
     */
    public static final Sj23PdsmUcpsiParams SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_16 = new Sj23PdsmUcpsiParams(
        3 * (1 << 15), 8, 3
    );

    /**
     * serve log size 24, client log size 12.
     */
    public static final Sj23PdsmUcpsiParams SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_12 = new Sj23PdsmUcpsiParams(
        1 << 13, 83, 3
    );

    /**
     * serve log size 24, client log size 13.
     */
    public static final Sj23PdsmUcpsiParams SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_13 = new Sj23PdsmUcpsiParams(
        1 << 14, 55,  3
    );

    /**
     * serve log size 24, client log size 16.
     */
    public static final Sj23PdsmUcpsiParams SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_16 = new Sj23PdsmUcpsiParams(
        3 * (1 << 15), 24, 2
    );

    /**
     * serve log size 26, client log size 12.
     */
    public static final Sj23PdsmUcpsiParams SERVER_LOG_SIZE_26_CLIENT_LOG_SIZE_12 = new Sj23PdsmUcpsiParams(
        1 << 13, 158, 4
    );

    /**
     * serve log size 26, client log size 16.
     */
    public static final Sj23PdsmUcpsiParams SERVER_LOG_SIZE_26_CLIENT_LOG_SIZE_16 = new Sj23PdsmUcpsiParams(
        3 * (1 << 15), 55, 2
    );

    /**
     * get SJ23 UCPSI params.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return SJ23 UCPSI params.
     */
    static public Sj23PdsmUcpsiParams getParams(int serverElementSize, int clientElementSize) {
        if (serverElementSize <= 1 << 20) {
            if (clientElementSize <= 1 << 12) {
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
