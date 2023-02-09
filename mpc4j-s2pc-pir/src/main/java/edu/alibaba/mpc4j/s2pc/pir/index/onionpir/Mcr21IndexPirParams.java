package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;


/**
 * OnionPIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class Mcr21IndexPirParams extends AbstractIndexPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 第一维度向量长度
     */
    private final int firstDimensionSize;
    /**
     * 其余维度向量长度
     */
    private static final int SUBSEQUENT_DIMENSION_SIZE = 4;
    /**
     * 明文模数比特长度
     */
    private final int plainModulusBitLength = 54;
    /**
     * 多项式阶
     */
    private final int polyModulusDegree = 4096;
    /**
     * 维数
     */
    private final int[] dimension;
    /**
     * 加密方案参数
     */
    private final byte[] encryptionParams;
    /**
     * 多项式里的元素数量
     */
    private final int[] elementSizeOfPlaintext;
    /**
     * 多项式数量
     */
    private final int[] plaintextSize;
    /**
     * 各维度的向量长度
     */
    private final int[][] dimensionsLength;
    /**
     * 数据库分块数量
     */
    private final int binNum;

    public Mcr21IndexPirParams(int serverElementSize, int elementByteLength, int firstDimensionSize) {
        this.firstDimensionSize = firstDimensionSize;
        // 生成加密方案参数
        this.encryptionParams = Mcr21IndexPirNativeUtils.generateSealContext(polyModulusDegree, plainModulusBitLength);
        // 一个多项式可表示的字节长度
        int binMaxByteLength = polyModulusDegree * plainModulusBitLength / Byte.SIZE;
        // 数据库分块数量
        this.binNum = (elementByteLength + binMaxByteLength - 1) / binMaxByteLength;
        int lastBinByteLength = elementByteLength % binMaxByteLength == 0 ?
            binMaxByteLength : elementByteLength % binMaxByteLength;
        this.elementSizeOfPlaintext = new int[this.binNum];
        this.plaintextSize = new int[this.binNum];
        this.dimensionsLength = new int[this.binNum][];
        this.dimension = new int[this.binNum];
        IntStream.range(0, this.binNum).forEach(i -> {
            int byteLength = i == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            // 一个多项式可以包含的元素数量
            elementSizeOfPlaintext[i] = elementSizeOfPlaintext(byteLength, polyModulusDegree, plainModulusBitLength);
            // 多项式数量
            plaintextSize[i] = (int) Math.ceil((double)serverElementSize / elementSizeOfPlaintext[i]);
            // 各维度的向量长度
            dimensionsLength[i] = computeDimensionLength(plaintextSize[i]);
            assert (dimensionsLength[i][0] <= 512) : "first dimension is too large";
            dimension[i] = dimensionsLength[i].length;
        });
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
    public int[][] getDimensionsLength() {
        return dimensionsLength;
    }

    /**
     * 返回多项式数量。
     *
     * @return 多项式数量。
     */
    public int[] getPlaintextSize() {
        return plaintextSize;
    }

    /**
     * 返回多项式里的元素数量。
     *
     * @return 多项式里的元素数量。
     */
    public int[] getElementSizeOfPlaintext() {
        return elementSizeOfPlaintext;
    }

    /**
     * 返回GSW密文参数。
     *
     * @return RGSW密文参数。
     */
    public int getGswDecompSize() {
        return 7;
    }

    /**
     * 返回分块数目。
     *
     * @return RGSW密文参数。
     */
    public int getBinNum() {
        return this.binNum;
    }

    /**
     * 返回数据库编码后每个维度的长度。
     *
     * @param elementSize 元素数量。
     * @return 数据库编码后每个维度的长度。
     */
    private int[] computeDimensionLength(int elementSize) {
        ArrayList<Integer> dimensionLength = new ArrayList<>();
        dimensionLength.add(firstDimensionSize);
        int product = firstDimensionSize;
        for (int i = elementSize / firstDimensionSize; i >= SUBSEQUENT_DIMENSION_SIZE; i /= SUBSEQUENT_DIMENSION_SIZE) {
            dimensionLength.add(SUBSEQUENT_DIMENSION_SIZE);
            product *= SUBSEQUENT_DIMENSION_SIZE;
        }
        int dimensionSize = dimensionLength.size();
        int[] dimensionArray = IntStream.range(0, dimensionSize).map(dimensionLength::get).toArray();
        while (product < elementSize) {
            dimensionArray[dimensionSize - 1]++;
            product = 1;
            product *= Arrays.stream(dimensionArray, 0, dimensionSize).reduce(1, (a, b) -> a * b);
        }
        if (dimensionSize == 1 && dimensionArray[0] > firstDimensionSize) {
            dimensionArray = new int[] {firstDimensionSize, SUBSEQUENT_DIMENSION_SIZE};
        }
        return dimensionArray;
    }

    @Override
    public String toString() {
        return "OnionPIR Parameters :" + "\n" +
            "  - elements per BFV plaintext : " + Arrays.toString(elementSizeOfPlaintext) + "\n" +
            "  - dimensions for d-dimensional hyperrectangle : " + Arrays.toString(dimension) + "\n" +
            "  - number of BFV plaintexts (before padding) : " + Arrays.toString(plaintextSize) + "\n" +
            "\n" +
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength + "\n";
    }
}