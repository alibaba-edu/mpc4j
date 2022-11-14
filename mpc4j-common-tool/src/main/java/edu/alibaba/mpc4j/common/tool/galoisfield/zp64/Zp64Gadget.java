package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.stream.IntStream;

/**
 * p为64bit以下长度的Zp小工具。来自下述论文：
 * <p>
 * Keller, Marcel, Emmanuela Orsini, and Peter Scholl. MASCOT: faster malicious arithmetic secure computation with
 * oblivious transfer. CCS 2016, pp. 830-842. 2016.
 * </p>
 * Section 2，Notation部分。 按照long数据类型存储Zp元素。由于采用大端表示，小工具向量（Gadget Array）的元素顺序和原论文相反。即：
 * <p>
 * gadget = (2^{k-1}, 2^{k-2}, ...., 2^1, 2^0)
 * </p>
 *
 * @author Hanwen Feng
 * @date 2022/06/07
 */
public class Zp64Gadget {
    /**
     * Zp64运算
     */
    private final Zp64 zp64;
    /**
     * 素数域比特长度
     */
    private final int l;
    /**
     * 小工具向量
     */
    private final long[] gadgetArray;

    /**
     * 构造Zp64域小工具。
     *
     * @param zp64 Zp64运算。
     */
    public Zp64Gadget(Zp64 zp64) {
        this.zp64 = zp64;
        // p = 2^k + µ
        l = zp64.getL();
        gadgetArray = IntStream.range(0, l)
            .mapToLong(i -> 1L << (l - i - 1))
            .toArray();
    }

    /**
     * 计算输入向量和小工具向量的内积。
     *
     * @param inputArray 输入向量。
     * @return 内积。
     */
    public long innerProduct(long[] inputArray) {
        assert inputArray.length == l : "input array length must equal to " + l + ": " + inputArray.length;
        long result = 0;
        for (int i = 0; i < l; i++) {
            long product = zp64.mul(gadgetArray[i], inputArray[i]);
            result = zp64.add(result, product);
        }
        return result;
    }

    /**
     * 将比特向量组合为Zp64域的元素。
     *
     * @param binary 比特向量。
     * @return 组合结果。
     */
    public long bitComposition(boolean[] binary) {
        assert binary.length == l : "binary length must equal to " + l + ": " + binary.length;
        long result = 0;
        for (int i = 0; i < l; i++) {
            result = binary[i] ? zp64.add(result, gadgetArray[i]) : result;
        }
        return result;
    }

    /**
     * 将Zp64域中的元素分解为比特向量，大端表示。
     *
     * @param element Zp域元素。
     * @return 分解结果。
     */
    public boolean[] bitDecomposition(long element) {
        assert zp64.validateRangeElement(element) : "element must be in range [0, " + zp64.getRangeBound() + ")";
        byte[] elementByteArray = LongUtils.longToByteArray(element);
        return BinaryUtils.byteArrayToBinary(elementByteArray, l);
    }
}
