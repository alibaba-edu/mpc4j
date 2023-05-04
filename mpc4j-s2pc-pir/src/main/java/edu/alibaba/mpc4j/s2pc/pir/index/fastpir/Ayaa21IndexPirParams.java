package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

import java.math.BigInteger;

/**
 * FastPIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21IndexPirParams implements IndexPirParams {

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

    public Ayaa21IndexPirParams(int polyModulusDegree, long plainModulus, long[] coeffModulus) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = BigInteger.valueOf(plainModulus).bitLength() - 1;
        this.encryptionParams = Ayaa21IndexPirNativeUtils.generateSealContext(
            polyModulusDegree, plainModulus, coeffModulus
        );
    }

    /**
     * 默认参数
     */
    public static Ayaa21IndexPirParams DEFAULT_PARAMS = new Ayaa21IndexPirParams(
        4096,
        1073153L,
        new long[]{1152921504606830593L, 562949953216513L}
    );

    @Override
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public int getPlainModulusBitLength() {
        return plainModulusBitLength;
    }

    @Override
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength;
    }
}
