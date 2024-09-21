package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j;

import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.serialization.Serialization;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * SEAL PIR params.
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Seal4jStdIdxPirParams implements StdIdxPirParams {

    /**
     * plain modulus size
     */
    private final int plainModulusBitLength;
    /**
     * poly modulus degree
     */
    private final int polyModulusDegree;
    /**
     * dimension
     */
    private final int dimension;
    /**
     * SEAL encryption params
     */
    private final EncryptionParameters encryptionParams;
    /**
     * expansion ratio
     */
    private final int expansionRatio;

    public Seal4jStdIdxPirParams(int polyModulusDegree, int plainModulusBitLength, int dimension) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        this.dimension = dimension;
        this.encryptionParams = Seal4jStdIdxPirUtils.generateEncryptionParams(
            polyModulusDegree, (1L << plainModulusBitLength) + 1
        );
        this.expansionRatio = Seal4jStdIdxPirUtils.expansionRatio(this.encryptionParams);
    }

    /**
     * default params
     */
    public static Seal4jStdIdxPirParams DEFAULT_PARAMS = new Seal4jStdIdxPirParams(4096, 20, 2);

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
        return dimension;
    }

    @Override
    public byte[] getEncryptionParams() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            encryptionParams.save(outputStream, Serialization.COMPR_MODE_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    /**
     * returns deserialized encryption parameters
     *
     * @return encryption parameters.
     */
    public EncryptionParameters getEncryptionParameters() {
        return encryptionParams;
    }

    /**
     * return expansion ratio.
     *
     * @return expansion ratio.
     */
    public int getExpansionRatio() {
        return expansionRatio;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength + "\n" +
            " - dimension : " + dimension;
    }
}
