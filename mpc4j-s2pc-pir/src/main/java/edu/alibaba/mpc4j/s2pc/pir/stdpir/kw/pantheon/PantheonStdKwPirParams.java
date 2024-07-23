package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon;

import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.StdKwPirParams;

import java.util.Arrays;

/**
 * Pantheon standard keyword PIR params.
 *
 * @author Liqiang Peng
 * @date 2024/7/19
 */
public class PantheonStdKwPirParams implements StdKwPirParams {
    /**
     * plain modulus bit length
     */
    private final int plainModulusBitLength;
    /**
     * poly modulus degree
     */
    private final int polyModulusDegree;
    /**
     * coeffs modulus bits
     */
    private final int[] coeffModulusBits;
    /**
     * column num
     */
    public int colNum;
    /**
     * row num
     */
    public int rowNum;
    /**
     * query ciphertext num
     */
    public int queryCiphertextNum;
    /**
     * PIR object num
     */
    public int pirObjectNum;
    /**
     * PIR column num per object
     */
    public int pirColumnNumPerObj;
    /**
     * PIR database row num
     */
    public int pirDbRowNum;
    /**
     * keyword byte length
     */
    public int keywordPrfByteLength;
    /**
     * encryption params
     */
    public byte[] encryptionParams;

    private PantheonStdKwPirParams(int polyModulusDegree, int plainModulusBitLength, int[] coeffModulusBits,
                                   int keywordPrfByteLength) {
        this.polyModulusDegree = polyModulusDegree;
        assert plainModulusBitLength == 16;
        this.plainModulusBitLength = plainModulusBitLength;
        this.coeffModulusBits = coeffModulusBits;
        this.encryptionParams = PantheonStdKwPirNativeUtils.genEncryptionParameters(
            polyModulusDegree, (1L << plainModulusBitLength) + 1, coeffModulusBits
        );
        this.keywordPrfByteLength = keywordPrfByteLength;
        assert CommonUtils.getUnitNum(Byte.SIZE * keywordPrfByteLength, plainModulusBitLength) % 2 == 0;
    }

    /**
     * default params
     */
    public static PantheonStdKwPirParams DEFAULT_PARAMS = new PantheonStdKwPirParams(
        32768, 16, new int[]{60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60}, 8
    );

    /**
     * initialize PIR params.
     *
     * @param num            database size.
     * @param labelBitLength label bit length.
     */
    public void initPirParams(int num, int labelBitLength) {
        this.colNum = CommonUtils.getUnitNum(keywordPrfByteLength * Byte.SIZE , 2 * plainModulusBitLength);
        this.rowNum = CommonUtils.getUnitNum(num, polyModulusDegree / 2);
        this.pirObjectNum = rowNum * (polyModulusDegree / 2);
        this.queryCiphertextNum = rowNum;
        if (CommonUtils.getUnitNum(labelBitLength, plainModulusBitLength) % 2 == 1) {
            labelBitLength = plainModulusBitLength + labelBitLength;
        }
        this.pirColumnNumPerObj = 2 * CommonUtils.getUnitNum((labelBitLength / 2), plainModulusBitLength);
        this.pirDbRowNum = CommonUtils.getUnitNum(pirObjectNum, polyModulusDegree) * pirColumnNumPerObj;
    }

    /**
     * return plain modulus bit length.
     *
     * @return plain modulus bit length.
     */
    public int getPlainModulusSize() {
        return plainModulusBitLength;
    }

    /**
     * return poly modulus degree.
     *
     * @return poly modulus degree.
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * return coeffs modulus bit.
     *
     * @return coeffs modulus bit.
     */
    public int[] getCoeffModulusBits() {
        return coeffModulusBits;
    }

    @Override
    public int maxRetrievalSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return
            " Encryption parameters: {" + "\n" +
            "     - plain_modulus_size : " + getPlainModulusSize() + "\n" +
            "     - poly_modulus_degree : " + getPolyModulusDegree() + "\n" +
            "     - coeff_modulus_bits : " + Arrays.toString(getCoeffModulusBits()) + "\n" +
            "  }" + "\n";
    }
}