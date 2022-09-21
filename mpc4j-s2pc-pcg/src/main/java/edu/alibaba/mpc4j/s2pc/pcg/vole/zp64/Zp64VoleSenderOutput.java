package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Zp64-VOLE发送方输出。发送方得到(x, t)，满足t = q + Δ · x（Δ和q由接收方持有）。
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public class Zp64VoleSenderOutput {
    /**
     * 素数域
     */
    private long prime;
    /**
     * x_i
     */
    private long[] x;
    /**
     * t_i
     */
    private long[] t;

    /**
     * 创建发送方输出。
     *
     * @param prime 素数域。
     * @param x     x。
     * @param t     t。
     * @return 发送方输出。
     */
    public static Zp64VoleSenderOutput create(long prime, long[] x, long[] t) {
        Zp64VoleSenderOutput senderOutput = new Zp64VoleSenderOutput();
        assert x.length > 0;
        assert x.length == t.length;
        assert BigInteger.valueOf(prime).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        senderOutput.prime = prime;
        senderOutput.x = Arrays.stream(x)
            .peek(xi -> {
                assert xi >= 0 && xi < prime
                    : "xi must be in range [0, " + prime + "): " + xi;
            })
            .toArray();
        senderOutput.t = Arrays.stream(t)
            .peek(ti -> {
                assert ti >= 0 && ti < prime
                    : "ti must be in range [0, " + prime + "): " + ti;
            })
            .toArray();
        return senderOutput;
    }

    /**
     * 创建长度为0的发送方输出。
     *
     * @param prime 素数域。
     * @return 长度为0的发送方输出。
     */
    public static Zp64VoleSenderOutput createEmpty(long prime) {
        Zp64VoleSenderOutput senderOutput = new Zp64VoleSenderOutput();
        assert BigInteger.valueOf(prime).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "input prime is not a prime: " + prime;
        senderOutput.prime = prime;
        senderOutput.x = new long[0];
        senderOutput.t = new long[0];
        return senderOutput;
    }

    /**
     * 私有构造函数。
     */
    private Zp64VoleSenderOutput() {
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
    public Zp64VoleSenderOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        // 拆分x
        long[] subX = new long[length];
        long[] remainX = new long[num - length];
        System.arraycopy(x, 0, subX, 0, length);
        System.arraycopy(x, length, remainX, 0, num - length);
        x = remainX;
        // 拆分t
        long[] subT = new long[length];
        long[] remainT = new long[num - length];
        System.arraycopy(t, 0, subT, 0, length);
        System.arraycopy(t, length, remainT, 0, num - length);
        t = remainT;

        return Zp64VoleSenderOutput.create(prime, subX, subT);
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
            long[] remainX = new long[length];
            System.arraycopy(x, 0, remainX, 0, length);
            x = remainX;
            long[] remainT = new long[length];
            System.arraycopy(t, 0, remainT, 0, length);
            t = remainT;
        }
    }

    /**
     * 合并两个输出。
     *
     * @param that 另一个输出。
     */
    public void merge(Zp64VoleSenderOutput that) {
        assert this.prime == that.prime : "merged outputs must have the same prime";
        // 拷贝x
        long[] mergeX = new long[this.x.length + that.x.length];
        System.arraycopy(this.x, 0, mergeX, 0, this.x.length);
        System.arraycopy(that.x, 0, mergeX, this.x.length, that.x.length);
        x = mergeX;
        // 拷贝t
        long[] mergeT = new long[this.t.length + that.t.length];
        System.arraycopy(this.t, 0, mergeT, 0, this.t.length);
        System.arraycopy(that.t, 0, mergeT, this.t.length, that.t.length);
        t = mergeT;
    }

    /**
     * 返回x_i。
     *
     * @param index 索引值。
     * @return x_i。
     */
    public long getX(int index) {
        return x[index];
    }

    /**
     * 返回x。
     *
     * @return x。
     */
    public long[] getX() {
        return x;
    }

    /**
     * 返回t_i。
     *
     * @param index 索引值。
     * @return t_i。
     */
    public long getT(int index) {
        return t[index];
    }

    /**
     * 返回t。
     *
     * @return t。
     */
    public long[] getT() {
        return t;
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return x.length;
    }
}
