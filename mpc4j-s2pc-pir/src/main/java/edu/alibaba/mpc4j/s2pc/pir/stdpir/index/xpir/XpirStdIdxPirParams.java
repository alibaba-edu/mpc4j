package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirParams;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * XPIR params.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class XpirStdIdxPirParams implements StdIdxPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * default params
     */
    public static final XpirStdIdxPirParams DEFAULT_PARAMS = new XpirStdIdxPirParams(4096, 20, 2);
    /**
     * plaintext modulus bit length
     */
    private final int plainModulusBitLength;
    /**
     * polynomial modulus degree
     */
    private final int polyModulusDegree;
    /**
     * dimension
     */
    private final int dimension;
    /**
     * SEAL encryption params
     */
    private final byte[] encryptionParams;
    /**
     * expansion ratio
     */
    private final int expansionRatio;

    public XpirStdIdxPirParams(int polyModulusDegree, int plainModulusBitLength, int dimension) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        this.dimension = dimension;
        this.encryptionParams = XpirStdIdxPirNativeUtils.generateEncryptionParams(
            polyModulusDegree, (1L << plainModulusBitLength) + 1
        );
        this.expansionRatio = XpirStdIdxPirNativeUtils.expansionRatio(this.encryptionParams);
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
        return dimension;
    }

    @Override
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    public int getExpansionRatio() {
        return expansionRatio;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new MultilineRecursiveToStringStyle())
            .setExcludeFieldNames("encryptionParams")
            .toString();
    }
}
