package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * XPIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16IndexPirParams implements IndexPirParams {

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
     * 维数
     */
    private final int dimension;
    /**
     * 加密方案参数
     */
    private final byte[] encryptionParams;
    /**
     * 多项式里的元素数量
     */
    private final int elementSizeOfPlaintext;
    /**
     * 多项式数量
     */
    private final int plaintextSize;
    /**
     * 各维度的向量长度
     */
    private final int[] dimensionsLength;

    public Mbfk16IndexPirParams(int serverElementSize, int elementByteLength, Mbfk16IndexPirConfig config, int dimension) {
        this.polyModulusDegree = config.getPolyModulusDegree();
        this.plainModulusBitLength = config.getPlainModulusSize();
        this.dimension = dimension;
        // 生成加密方案参数
        this.encryptionParams = Mbfk16IndexPirNativeUtils.generateSealContext(polyModulusDegree, (1L << plainModulusBitLength) + 1);
        // 一个多项式可以包含的元素数量
        this.elementSizeOfPlaintext = elementSizeOfPlaintext(elementByteLength);
        // 多项式数量
        this.plaintextSize = (int) Math.ceil((double) serverElementSize / this.elementSizeOfPlaintext);
        // 各维度的向量长度
        this.dimensionsLength = computeDimensionLength();
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
     * 返回多项式阶。
     *
     * @return 多项式阶。
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * 返回维数。
     *
     * @return 维数。
     */
    public int getDimension() {
        return dimension;
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
     * 返回各维度的向量长度。
     *
     * @return 各维度的向量长度。
     */
    public int[] getDimensionsLength() {
        return dimensionsLength;
    }

    /**
     * 返回多项式数量。
     *
     * @return 多项式数量。
     */
    public int getPlaintextSize() {
        return plaintextSize;
    }

    /**
     * 返回多项式里的元素数量。
     *
     * @return 多项式里的元素数量。
     */
    public int getElementSizeOfPlaintext() {
        return elementSizeOfPlaintext;
    }

    /**
     * 返回多项式包含的数据库元素数量。
     *
     * @param elementByteLength 元素字节长度。
     * @return 明文多项式包含的数据库元素数量。
     */
    private int elementSizeOfPlaintext(int elementByteLength) {
        int coeffSizeOfElement = coeffSizeOfElement(elementByteLength);
        int elementSizeOfPlaintext = polyModulusDegree / coeffSizeOfElement;
        assert elementSizeOfPlaintext > 0 : "N should be larger than the of coefficients needed to represent a database element";
        return elementSizeOfPlaintext;
    }

    /**
     * 返回数据库编码后每个维度的长度。
     *
     * @return 数据库编码后每个维度的长度。
     */
    private int[] computeDimensionLength() {
        int[] dimensionLength = IntStream.range(0, dimension)
            .map(i -> (int) Math.max(2, Math.floor(Math.pow(plaintextSize, 1.0 / dimension))))
            .toArray();
        int product = 1;
        int j = 0;
        // if plaintext_num is not a d-power
        if (dimensionLength[0] != Math.pow(plaintextSize, 1.0 / dimension)) {
            while (product < plaintextSize && j < dimension) {
                product = 1;
                dimensionLength[j++]++;
                for (int i = 0; i < dimension; i++) {
                    product *= dimensionLength[i];
                }
            }
        }
        return dimensionLength;
    }

    /**
     * 返回元素的系数数量。
     *
     * @param elementByteLength 元素字节长度。
     * @return 元素的系数数量。
     */
    private int coeffSizeOfElement(int elementByteLength) {
        return (int) Math.ceil(Byte.SIZE * elementByteLength / (double) plainModulusBitLength);
    }

    @Override
    public String toString() {
        int product = Arrays.stream(dimensionsLength).reduce(1, (a, b) -> a * b);
        return "PIR Parameters :" + "\n" +
            "  - elements per BFV plaintext : " + elementSizeOfPlaintext + "\n" +
            "  - dimensions for d-dimensional hyperrectangle : " + dimension + "\n" +
            "  - number of BFV plaintexts (before padding) : " + plaintextSize + "\n" +
            "  - number of BFV plaintexts after padding (to fill d-dimensional hyperrectangle) : " + product + "\n" +
            "\n" +
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength + "\n";
    }
}
