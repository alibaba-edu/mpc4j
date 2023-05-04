package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

/**
 * Vectorized PIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23IndexPirParams implements IndexPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 明文模数比特长度
     */
    private final int plainModulusBitLength;
    /**
     * 多项式阶
     */
    private final int polyModulusDegree;
    /**
     * 加密方案参数
     */
    private final byte[] encryptionParams;
    /**
     * 前两维长度
     */
    private final int firstTwoDimensionSize;
    /**
     * 第三维长度
     */
    private final int thirdDimensionSize;

    public Mr23IndexPirParams(int polyModulusDegree, int plainModulusBitLength, int firstTwoDimensionSize,
                              int thirdDimensionSize) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        assert firstTwoDimensionSize == PirUtils.getNextPowerOfTwo(firstTwoDimensionSize);
        this.firstTwoDimensionSize = firstTwoDimensionSize;
        this.thirdDimensionSize = thirdDimensionSize;
        // 生成加密方案参数
        this.encryptionParams = Mr23IndexPirNativeUtils.generateSealContext(
            polyModulusDegree, plainModulusBitLength
        );
    }

    /**
     * 默认参数
     */
    public static Mr23IndexPirParams DEFAULT_PARAMS = new Mr23IndexPirParams(
        8192, 20, 128, 64
    );

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
        return 3;
    }

    @Override
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    public int getFirstTwoDimensionSize() {
        return firstTwoDimensionSize;
    }

    public int getThirdDimensionSize() {
        return thirdDimensionSize;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength;
    }
}
