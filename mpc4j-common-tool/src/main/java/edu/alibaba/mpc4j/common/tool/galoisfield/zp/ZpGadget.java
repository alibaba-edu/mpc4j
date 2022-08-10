package edu.alibaba.mpc4j.common.tool.galoisfield.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

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
     * KDF
     */
    private Kdf kdf;
    /**
     * 伪随机数生成器
     */
    private Prg prg;
    /**
     * 素数域
     */
    private BigInteger prime;
    /**
     * 最大有效元素：2^k - 1
     */
    private BigInteger maxValidElement;
    /**
     * 素数域比特长度
     */
    private int k;
    /**
     * 素数域字节长度
     */
    private int byteK;
    /**
     * 小工具向量
     */
    private BigInteger[] gadgetArray;

    /**
     * 构造Zp域小工具。
     *
     * @param envType 环境类型。
     * @param prime   素数域。
     * @return Zp域小工具。
     */
    public static ZpGadget createFromPrime(EnvType envType, BigInteger prime) {
        ZpGadget zpGadget = new ZpGadget();
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        zpGadget.prime = prime;
        // p = 2^k + μ
        zpGadget.k = prime.bitLength() - 1;
        // 2^k - 1
        zpGadget.maxValidElement = BigInteger.ONE.shiftLeft(zpGadget.k);
        zpGadget.byteK = CommonUtils.getByteLength(zpGadget.k);
        zpGadget.kdf = KdfFactory.createInstance(envType);
        zpGadget.prg = PrgFactory.createInstance(envType, zpGadget.byteK);
        // (g^{k-1}, ..., g^1, 1) = (2^{k-1}, ..., 2, 1)，需要反过来生成，参见类注释
        zpGadget.gadgetArray = IntStream.range(0, zpGadget.k)
            .mapToObj(i -> BigInteger.ONE.shiftLeft(zpGadget.k - i - 1))
            .toArray(BigInteger[]::new);
        return zpGadget;
    }

    /**
     * 构造Zp域小工具。
     *
     * @param envType 环境类型。
     * @param k       要求素数域比特长度大于k。
     * @return Zp域小工具。
     */
    public static ZpGadget createFromK(EnvType envType, int k) {
        ZpGadget zpGadget = new ZpGadget();
        assert k > 0 : "k must be greater than 0: " + k;
        zpGadget.k = k;
        zpGadget.prime = ZpManager.getPrime(k);
        // 2^k - 1
        zpGadget.maxValidElement = BigInteger.ONE.shiftLeft(zpGadget.k);
        zpGadget.byteK = CommonUtils.getByteLength(zpGadget.k);
        zpGadget.kdf = KdfFactory.createInstance(envType);
        zpGadget.prg = PrgFactory.createInstance(envType, zpGadget.byteK);
        // (g^{k-1}, ..., g^1, 1) = (2^{k-1}, ..., 2, 1)，需要反过来生成，参见类注释
        zpGadget.gadgetArray = IntStream.range(0, zpGadget.k)
            .mapToObj(i -> BigInteger.ONE.shiftLeft(zpGadget.k - i - 1))
            .toArray(BigInteger[]::new);
        return zpGadget;
    }

    /**
     * 私有构造函数
     */
    private ZpGadget() {
        // empty
    }

    /**
     * 计算输入向量和小工具向量的内积。
     *
     * @param inputArray 输入向量。
     * @return 内积。
     */
    public BigInteger composition(BigInteger[] inputArray) {
        assert inputArray.length == gadgetArray.length;
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < inputArray.length; i++) {
            result = result.add(gadgetArray[i].multiply(inputArray[i])).mod(prime);
        }
        return result;
    }

    /**
     * 计算输入向量和小工具向量的内积。
     *
     * @param inputArray 输入向量。
     * @return 内积。
     */
    public BigInteger composition(boolean[] inputArray) {
        assert inputArray.length == k;
        return BigIntegerUtils.innerProduct(gadgetArray, prime, inputArray);
    }

    /**
     * 将Zp域中的元素分解为长度和小工具向量一致的布尔向量，大端表示。
     *
     * @param element Zp域元素。
     * @return 分解得到的布尔向量。
     */
    public boolean[] decomposition(BigInteger element) {
        assert BigIntegerUtils.greaterOrEqual(element, BigInteger.ZERO) && BigIntegerUtils.less(element, maxValidElement);
        byte[] elementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(element, byteK);
        return BinaryUtils.byteArrayToBinary(elementByteArray, k);
    }

    /**
     * 返回k。
     *
     * @return k。
     */
    public int getK() {
        return k;
    }

    /**
     * 返回字节长度的k。
     *
     * @return 字节长度的k。
     */
    public int getByteK() {
        return byteK;
    }

    /**
     * 返回素数域。
     *
     * @return 素数域。
     */
    public BigInteger getPrime() {
        return prime;
    }

    /**
     * 生成Zp域中的元素。
     *
     * @param seed 种子。
     * @return Zp域中的元素。
     */
    public BigInteger randomElement(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return new BigInteger(1, elementByteArray).mod(prime);
    }
}
