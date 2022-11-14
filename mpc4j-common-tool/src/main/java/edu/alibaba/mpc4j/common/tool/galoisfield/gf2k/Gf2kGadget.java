package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * GF2K域小工具。来自下述论文：
 * <p>
 * Keller, Marcel, Emmanuela Orsini, and Peter Scholl. MASCOT: faster malicious arithmetic secure computation with
 * oblivious transfer. CCS 2016, pp. 830-842. 2016.
 * </p>
 * Section 2，Notation部分。 由于元素采用大端表示，小工具向量（Gadget Array）的元素顺序和原论文相反。即：
 * <p>
 * gadget = (X^{127}, X^{126}, ...., X, 1)
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public class Gf2kGadget {
    /**
     * GF2K元素比特长度
     */
    private static final int L = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * GF2K元素字节长度
     */
    private static final int L_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * GF2K运算
     */
    private final Gf2k gf2k;
    /**
     * 小工具向量
     */
    private final byte[][] gadgetArray;

    /**
     * 构造GF2K域小工具。
     *
     * @param gf2k GF2K运算。
     */
    public Gf2kGadget(Gf2k gf2k) {
        this.gf2k = gf2k;
        gadgetArray = IntStream.range(0, L)
            .mapToObj(i -> BigInteger.ONE.shiftLeft(L - i - 1))
            .map(element -> BigIntegerUtils.nonNegBigIntegerToByteArray(element, L_BYTE_LENGTH))
            .toArray(byte[][]::new);
    }

    /**
     * 计算输入向量和小工具向量的内积。
     *
     * @param inputArray 输入向量。
     * @return 内积。
     */
    public byte[] innerProduct(byte[][] inputArray) {
        assert inputArray.length == L : "input array length must equal to " + L + ": " + inputArray.length;
        byte[] result = new byte[L_BYTE_LENGTH];
        for (int i = 0; i < L; i++) {
            byte[] product = gf2k.mul(gadgetArray[i], inputArray[i]);
            gf2k.addi(result, product);
        }
        return result;
    }

    /**
     * 将比特向量组合为GF2K域的元素。
     *
     * @param binary 比特向量。
     * @return 组合结果。
     */
    public byte[] bitComposition(boolean[] binary) {
        assert binary.length == L : "binary length must equal to " + L + ": " + binary.length;
        byte[] result = new byte[L_BYTE_LENGTH];
        for (int i = 0; i < L; i++) {
            if (binary[i]) {
                gf2k.addi(result, gadgetArray[i]);
            }
        }
        return result;
    }

    /**
     * 将GF2K域中的元素分解为比特向量，大端表示。
     *
     * @param element GF2K域元素。
     * @return 分解结果。
     */
    public boolean[] bitDecomposition(byte[] element) {
        assert element.length == L_BYTE_LENGTH : "element byte length must be " + L_BYTE_LENGTH + ": " + element.length;
        return BinaryUtils.byteArrayToBinary(element);
    }
}
