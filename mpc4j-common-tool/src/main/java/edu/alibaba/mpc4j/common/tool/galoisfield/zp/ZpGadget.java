package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * Zp小工具。来自下述论文：
 * <p>
 * Keller, Marcel, Emmanuela Orsini, and Peter Scholl. MASCOT: faster malicious arithmetic secure computation with
 * oblivious transfer. CCS 2016, pp. 830-842. 2016.
 * </p>
 * Section 2，Notation部分。 由于BigInteger转ByteArray采用大端表示，小工具向量（Gadget Array）的元素顺序和原论文相反。即：
 * <p>
 * gadget = (2^{k-1}, 2^{k-2}, ...., 2^1, 2^0)
 * </p>
 *
 * @author Hanwen Feng
 * @date 2022/06/07
 */
public class ZpGadget {
    /**
     * Zp运算
     */
    private final Zp zp;
    /**
     * 素数域比特长度
     */
    private final int l;
    /**
     * 素数域字节长度
     */
    private final int byteL;
    /**
     * 小工具向量
     */
    private final BigInteger[] gadgetArray;

    /**
     * 构造Zp域小工具。
     *
     * @param zp Zp有限域。
     */
    public ZpGadget(Zp zp) {
        this.zp = zp;
        l = zp.getL();
        byteL = zp.getByteL();
        gadgetArray = IntStream.range(0, l)
            .mapToObj(i -> BigInteger.ONE.shiftLeft(l - i - 1))
            .toArray(BigInteger[]::new);
    }

    /**
     * 计算输入向量和小工具向量的内积。
     *
     * @param inputArray 输入向量。
     * @return 内积。
     */
    public BigInteger innerProduct(BigInteger[] inputArray) {
        assert inputArray.length == l : "input array length must equal to " + l + ": " + inputArray.length;
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < inputArray.length; i++) {
            result = zp.add(result, zp.mul(gadgetArray[i], inputArray[i]));
        }
        return result;
    }

    /**
     * 将比特向量组合为Zp域的元素。
     *
     * @param binary 比特向量。
     * @return 组合结果。
     */
    public BigInteger composition(boolean[] binary) {
        assert binary.length == l : "binary length must equal to " + l + ": " + binary.length;
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < l; i++) {
            result = binary[i] ? zp.add(result, gadgetArray[i]) : result;
        }
        return result;
    }

    /**
     * 将Zp域中的元素分解为比特向量，大端表示。
     *
     * @param element Zp域元素。
     * @return 分解结果。
     */
    public boolean[] decomposition(BigInteger element) {
        assert zp.validateRangeElement(element) : "element must be in range [0, " + zp.getRangeBound() + ")";
        byte[] elementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteL);
        return BinaryUtils.byteArrayToBinary(elementByteArray, l);
    }
}
