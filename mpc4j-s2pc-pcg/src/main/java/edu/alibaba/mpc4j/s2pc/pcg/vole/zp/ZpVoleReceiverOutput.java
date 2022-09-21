package edu.alibaba.mpc4j.s2pc.pcg.vole.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Zp-VOLE协议接收方输出。接收方得到(Δ, q)，满足t = q + Δ · x（x和t由发送方持有）。
 *
 * @author Hanwen Feng
 * @date 2022/06/08
 */
public class ZpVoleReceiverOutput {
    /**
     * 素数域Zp
     */
    private BigInteger prime;
    /**
     * 关联值Δ
     */
    private BigInteger delta;
    /**
     * Q数组
     */
    private BigInteger[] q;

    /**
     * 构建接收方输出。
     *
     * @param prime 素数域。
     * @param delta 关联值Δ。
     * @param q     q。
     * @return 接收方输出。
     */
    public static ZpVoleReceiverOutput create(BigInteger prime, BigInteger delta, BigInteger[] q) {
        ZpVoleReceiverOutput receiverOutput = new ZpVoleReceiverOutput();
        assert q.length > 0;
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        assert BigIntegerUtils.greaterOrEqual(delta, BigInteger.ZERO) && delta.bitLength() <= prime.bitLength() - 1
            : "Δ must be in range [0, " + BigInteger.ONE.shiftLeft(prime.bitLength() - 1) + "): " + delta;
        receiverOutput.delta = delta;
        receiverOutput.prime = prime;
        receiverOutput.q = Arrays.stream(q)
            .peek(qi -> {
                assert BigIntegerUtils.greaterOrEqual(qi, BigInteger.ZERO) && BigIntegerUtils.less(qi, prime)
                    : "qi must be in range [0, " + prime + "): " + qi;
            })
            .toArray(BigInteger[]::new);
        return receiverOutput;
    }

    /**
     * 创建长度为0的接收方输出。
     *
     * @param prime 素数域。
     * @param delta 关联值Δ。
     * @return 接收方输出。
     */
    public static ZpVoleReceiverOutput createEmpty(BigInteger prime, BigInteger delta) {
        ZpVoleReceiverOutput receiverOutput = new ZpVoleReceiverOutput();
        assert prime.isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        assert BigIntegerUtils.greaterOrEqual(delta, BigInteger.ZERO) && delta.bitLength() <= prime.bitLength() - 1
            : "Δ must be in range [0, " + BigInteger.ONE.shiftLeft(prime.bitLength() - 1) + "): " + delta;
        receiverOutput.prime = prime;
        receiverOutput.delta = delta;
        receiverOutput.q = new BigInteger[0];

        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private ZpVoleReceiverOutput() {
        // empty
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
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public ZpVoleReceiverOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        BigInteger[] subQ = new BigInteger[length];
        BigInteger[] remainQ = new BigInteger[num - length];
        System.arraycopy(q, 0, subQ, 0, length);
        System.arraycopy(q, length, remainQ, 0, num - length);
        q = remainQ;

        return ZpVoleReceiverOutput.create(prime, delta, subQ);
    }

    /**
     * 将当前输出结果数量减少至给定的数量。
     *
     * @param length 给定的数量。
     */
    public void reduce(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "reduce length = " + length + " must be in range (0, " + num + "]";
        if (length < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
            BigInteger[] remainQ = new BigInteger[length];
            System.arraycopy(q, 0, remainQ, 0, length);
            q = remainQ;
        }
    }

    /**
     * 合并两个接收方输出。
     *
     * @param that 另一个接收方输出。
     */
    public void merge(ZpVoleReceiverOutput that) {
        assert this.prime.equals(that.prime) : "merged outputs must have the same prime";
        assert this.delta.equals(that.delta) : "merged outputs must have the same Δ";
        BigInteger[] mergeQ = new BigInteger[this.q.length + that.q.length];
        System.arraycopy(this.q, 0, mergeQ, 0, this.q.length);
        System.arraycopy(that.q, 0, mergeQ, this.q.length, that.q.length);
        q = mergeQ;
    }

    /**
     * 返回关联值Δ。
     *
     * @return 关联值Δ。
     */
    public BigInteger getDelta() {
        return delta;
    }

    /**
     * 返回q_i。
     *
     * @param index 索引值。
     * @return q_i。
     */
    public BigInteger getQ(int index) {
        return q[index];
    }

    /**
     * 返回q。
     *
     * @return q。
     */
    public BigInteger[] getQ() {
        return q;
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return q.length;
    }
}
