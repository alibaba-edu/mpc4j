package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * FastPIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21IndexPirParams extends AbstractIndexPirParams {

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
     * 单个元素的列长度
     */
    private final int[] elementColumnLength;
    /**
     * 查询向量的密文个数
     */
    private final int querySize;
    /**
     * 数据库行数
     */
    private final int[] databaseRowNum;
    /**
     * 数据库分块数量
     */
    private final int binNum;

    public Ayaa21IndexPirParams(int serverElementSize, int elementByteLength, int polyModulusDegree,
                                long plainModulus, long[] coeffModulus) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = BigInteger.valueOf(plainModulus).bitLength() - 1;
        this.encryptionParams = Ayaa21IndexPirNativeUtils.generateSealContext(
            polyModulusDegree, plainModulus, coeffModulus
        );
        this.querySize = (int) Math.ceil(serverElementSize / (polyModulusDegree / 2.0));
        int binMaxByteLength = plainModulusBitLength * polyModulusDegree / Byte.SIZE;
        // 数据库分块数量
        this.binNum = (elementByteLength + binMaxByteLength - 1) / binMaxByteLength;
        int lastBinByteLength = elementByteLength % binMaxByteLength == 0 ?
            binMaxByteLength : elementByteLength % binMaxByteLength;
        this.elementColumnLength = new int[binNum];
        this.databaseRowNum = new int[binNum];
        IntStream.range(0, binNum).forEach(i -> {
            int byteLength = i == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            elementColumnLength[i] = (int) Math.ceil((byteLength / 2.0) * Byte.SIZE / plainModulusBitLength);
            databaseRowNum[i] = querySize * elementColumnLength[i];
        });
    }

    /**
     * 返回数据库行数。
     *
     * @return 数据库行数。
     */
    public int[] getDatabaseRowNum() {
        return databaseRowNum;
    }

    /**
     * 返回多项式阶。
     *
     * @return 多项式阶。
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * 返回明文模数比特长度。
     *
     * @return 明文模数比特长度。
     */
    public int getPlainModulusBitLength() {
        return plainModulusBitLength;
    }

    /**
     * 返回元素的列长度。
     *
     * @return 元素的列长度。
     */
    public int[] getElementColumnLength() {
        return elementColumnLength;
    }

    /**
     * 返回查询信息的密文数目。
     *
     * @return 查询信息的密文数。
     */
    public int getQuerySize() {
        return querySize;
    }

    /**
     * 返回加密方案参数。
     *
     * @return 加密方案参数。
     */
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    /**
     * 返回分块数目。
     *
     * @return RGSW密文参数。
     */
    public int getBinNum() {
        return this.binNum;
    }

    @Override
    public String toString() {
        return "FastPIR Parameters :" + "\n" +
            "  - query ciphertext size : " + querySize + "\n" +
            "  - element column length : " + Arrays.toString(elementColumnLength) + "\n" +
            "  - database row size : " + Arrays.toString(databaseRowNum) + "\n" +
            "\n" +
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength + "\n";
    }
}
