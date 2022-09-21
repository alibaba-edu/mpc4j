package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Zp64-VOLE协议接收方输出。接收方得到(Δ, q)，满足t = q + Δ · x（x和t由发送方持有）。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public class Zp64VoleReceiverOutput {
    /**
     * 素数域
     */
    private long prime;
    /**
     * 关联值Δ
     */
    private long delta;
    /**
     * Q数组
     */
    private long[] q;

    /**
     * 构建接收方输出。
     *
     * @param prime 素数域。
     * @param delta 关联值Δ。
     * @param q     q。
     * @return 接收方输出。
     */
    public static Zp64VoleReceiverOutput create(long prime, long delta, long[] q) {
        Zp64VoleReceiverOutput receiverOutput = new Zp64VoleReceiverOutput();
        assert q.length > 0;
        assert BigInteger.valueOf(prime).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        assert delta >= 0 && LongUtils.ceilLog2(delta) <= LongUtils.ceilLog2(prime) - 1
                : "Δ must be in range [0, " + (1L << (LongUtils.ceilLog2(prime) - 1)) + "): " + delta;
        receiverOutput.delta = delta;
        receiverOutput.prime = prime;
        receiverOutput.q = Arrays.stream(q)
                .peek(qi -> {
                    assert qi >= 0 && qi < prime
                            : "qi must be in range [0, " + prime + "): " + qi;
                })
                .toArray();
        return receiverOutput;
    }

    /**
     * 创建长度为0的接收方输出。
     *
     * @param prime 素数域。
     * @param delta 关联值Δ。
     * @return 接收方输出。
     */
    public static Zp64VoleReceiverOutput createEmpty(long prime, long delta) {
        Zp64VoleReceiverOutput receiverOutput = new Zp64VoleReceiverOutput();
        assert BigInteger.valueOf(prime).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        assert delta >= 0 && LongUtils.ceilLog2(delta) <= LongUtils.ceilLog2(prime) - 1
                : "Δ must be in range [0, " + (1L << (LongUtils.ceilLog2(prime) - 1)) + "): " + delta;
        receiverOutput.delta = delta;
        receiverOutput.prime = prime;
        receiverOutput.q = new long[0];

        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private Zp64VoleReceiverOutput() {
        // empty
    }

    /**
     * 返回素数域。
     *
     * @return 素数域。
     */
    public long getPrime() {
        return prime;
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public Zp64VoleReceiverOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        long[] subQ = new long[length];
        long[] remainQ = new long[num - length];
        System.arraycopy(q, 0, subQ, 0, length);
        System.arraycopy(q, length, remainQ, 0, num - length);
        q = remainQ;

        return Zp64VoleReceiverOutput.create(prime, delta, subQ);
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
            long[] remainQ = new long[length];
            System.arraycopy(q, 0, remainQ, 0, length);
            q = remainQ;
        }
    }

    /**
     * 合并两个接收方输出。
     *
     * @param that 另一个接收方输出。
     */
    public void merge(Zp64VoleReceiverOutput that) {
        assert this.prime == that.prime : "merged outputs must have the same prime";
        assert this.delta == that.delta : "merged outputs must have the same Δ";
        long[] mergeQ = new long[this.q.length + that.q.length];
        System.arraycopy(this.q, 0, mergeQ, 0, this.q.length);
        System.arraycopy(that.q, 0, mergeQ, this.q.length, that.q.length);
        q = mergeQ;
    }

    /**
     * 返回关联值Δ。
     *
     * @return 关联值Δ。
     */
    public long getDelta() {
        return delta;
    }

    /**
     * 返回q_i。
     *
     * @param index 索引值。
     * @return q_i。
     */
    public long getQ(int index) {
        return q[index];
    }

    /**
     * 返回q。
     *
     * @return q。
     */
    public long[] getQ() {
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
