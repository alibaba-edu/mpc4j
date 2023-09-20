package edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;
import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.lang.Math;

/**
 * Constant-weight PIR params
 *
 * @author Qixian Zhou
 * @date 2023/6/18
 */
public class Mk22SingleIndexPirParams implements SingleIndexPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    public enum EqualityType {
        /**
         * folklore
         */
        FOLKLORE,
        /**
         * constant weight
         */
        CONSTANT_WEIGHT,
    }

    /**
     * plain modulus size
     */
    private final int plainModulusBitLength;
    /**
     * log2 of poly modulus degree
     */
    private final int logPolyModulusDegree;
    /**
     * poly modulus degree
     */
    private final int polyModulusDegree;
    /**
     * EqualityType
     */
    private final EqualityType equalityType;
    /**
     * hamming weight: k
     */
    private final int hammingWeight;
    /**
     * Codewords Bit length: m
     */
    private int codewordsBitLength;
    /**
     * num input cipher:  h = ceil(m/2^c)
     */
    private int numInputCiphers;
    /**
     * used slots per BFV PT: 2^c
     */
    private int usedSlotsPerPlain;
    /**
     * SEAL encryption params
     */
    private final byte[] encryptionParams;

    public Mk22SingleIndexPirParams(int hammingWeight, int polyModulusDegree, int plainModulusBitLength,
                                    EqualityType equalityType) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        assert equalityType.equals(EqualityType.FOLKLORE) || equalityType.equals(EqualityType.CONSTANT_WEIGHT);
        this.equalityType = equalityType;
        this.hammingWeight = hammingWeight;
        this.logPolyModulusDegree = (int) DoubleUtils.log2(polyModulusDegree);
        this.encryptionParams = Mk22SingleIndexPirNativeUtils.generateEncryptionParams(
            this.polyModulusDegree, (1L << this.plainModulusBitLength) + 1
        );
    }

    /**
     * default params
     */
    public static Mk22SingleIndexPirParams DEFAULT_PARAMS = new Mk22SingleIndexPirParams(
        2, 8192, 21, EqualityType.CONSTANT_WEIGHT
    );

    /**
     * set query params.
     *
     * @param serverElementSize server element size.
     */
    public void setQueryParams(int serverElementSize) {
        int indexBitLength = (int) Math.ceil(DoubleUtils.log2(serverElementSize));
        switch (this.equalityType) {
            case FOLKLORE:
                this.codewordsBitLength = indexBitLength;
                break;
            case CONSTANT_WEIGHT:
                // m must be larger than k
                int codewordBitLength = hammingWeight;
                // C_m^k >= n
                while ((int) DoubleUtils.estimateCombinatorial(codewordBitLength, hammingWeight)
                    < (1 << indexBitLength)) {
                    codewordBitLength += 1;
                }
                this.codewordsBitLength = codewordBitLength;
                break;
            default:
                throw new IllegalStateException("Invalid Equality Operator Type");
        }
        // log2(m)
        int log2CodewordBitLength = (int) Math.ceil(DoubleUtils.log2(this.codewordsBitLength));
        // compression factor: c \in {0, 1, ..., log2(N)}, N is the polyModulusDegree
        int compressionFactor = Math.min(log2CodewordBitLength, this.logPolyModulusDegree);
        // 2^c
        this.usedSlotsPerPlain = 1 << compressionFactor;
        // m/2^c
        this.numInputCiphers = (int) Math.ceil((double) this.codewordsBitLength / this.usedSlotsPerPlain);
    }

    @Override
    public int getPlainModulusBitLength() {
        return plainModulusBitLength;
    }

    @Override
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    public EqualityType getEqualityType() {
        return equalityType;
    }

    public int getCodewordsBitLength() {
        return codewordsBitLength;
    }

    public int getNumInputCiphers() {
        return numInputCiphers;
    }

    public int getUsedSlotsPerPlain() {
        return usedSlotsPerPlain;
    }

    public int getHammingWeight() {
        return hammingWeight;
    }

    @Override
    public String toString() {
        return
            "Constant-Weight PIR encryption parameters : " + "\n" +
                " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
                " - size of plaintext modulus : " + plainModulusBitLength + "\n" +
                " - EqualityType : " + equalityType + "\n" +
                " - Hamming Weight : " + hammingWeight + "\n";
    }
}
