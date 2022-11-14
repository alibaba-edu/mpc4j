package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * zp64三元组。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Zp64Triple {
    /**
     * 三元组数量
     */
    private int num;
    /**
     * 模数
     */
    private long p;
    /**
     * 随机分量a
     */
    private long[] as;
    /**
     * 随机分量b
     */
    private long[] bs;
    /**
     * 随机分量c
     */
    private long[] cs;

    /**
     * 创建zp64乘法三元组。
     *
     * @param p   模数。
     * @param num 乘法三元组数量。
     * @param as  随机分量a。
     * @param bs  随机分量b。
     * @param cs  随机分量c。
     */
    public static Zp64Triple create(long p, int num, long[] as, long[] bs, long[] cs) {
        assert num > 0 : "num must be greater than 0";
        assert p >= 2 && BigInteger.valueOf(p).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "p must be a prime: " + p;
        assert as.length == num && bs.length == num && cs.length == num;
        IntStream.range(0, num).forEach(i -> {
            assert as[i] >= 0 && as[i] < p;
            assert bs[i] >= 0 && bs[i] < p;
            assert cs[i] >= 0 && cs[i] < p;
        });

        Zp64Triple zpTriple = new Zp64Triple();
        zpTriple.num = num;
        zpTriple.p = p;
        zpTriple.as = as;
        zpTriple.bs = bs;
        zpTriple.cs = cs;

        return zpTriple;
    }

    /**
     * 创建长度为0的zp64三元组随机分量。
     *
     * @return 长度为0的zp64三元组随机分量。
     */
    public static Zp64Triple createEmpty(long p) {
        assert p >= 2 && BigInteger.valueOf(p).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "p must be a prime: " + p;
        Zp64Triple emptyTriple = new Zp64Triple();
        emptyTriple.num = 0;
        emptyTriple.p = p;
        emptyTriple.as = new long[0];
        emptyTriple.bs = new long[0];
        emptyTriple.cs = new long[0];

        return emptyTriple;
    }

    /**
     * 私有构造函数。
     */
    private Zp64Triple() {
        // empty
    }

    /**
     * 返回zp64乘法三元组数量。
     *
     * @return 乘法三元组数量。
     */
    public int getNum() {
        return num;
    }

    /**
     * 返回zp64乘法三元组模数。
     *
     * @return 乘法三元组模数。
     */
    public long getP() {
        return p;
    }

    /**
     * 返回随机分量a。
     *
     * @param index 索引值。
     * @return 随机分量a。
     */
    public long getA(int index) {
        return as[index];
    }

    /**
     * 返回所有随机分量a。
     *
     * @return 所有随机分量a。
     */
    public long[] getA() {
        return as;
    }

    /**
     * 返回随机分量b。
     *
     * @param index 索引值。
     * @return 随机分量b。
     */
    public long getB(int index) {
        return bs[index];
    }

    /**
     * 返回所有随机分量b。
     *
     * @return 所有随机分量b。
     */
    public long[] getB() {
        return bs;
    }

    /**
     * 返回随机分量c。
     *
     * @param index 索引值。
     * @return 随机分量c。
     */
    public long getC(int index) {
        return cs[index];
    }

    /**
     * 返回所有随机分量c。
     *
     * @return 所有随机分量c。
     */
    public long[] getC() {
        return cs;
    }

    /**
     * 从zp64乘法三元组中切分出指定长度的子zp64乘法三元组。
     *
     * @param length 指定切分长度。
     */
    public Zp64Triple split(int length) {
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        // 切分a
        long[] aSubs = new long[length];
        long[] aRemains = new long[num - length];
        System.arraycopy(as, 0, aSubs, 0, length);
        System.arraycopy(as, length, aRemains, 0, num - length);
        as = aRemains;
        // 切分b
        long[] bSubs = new long[length];
        long[] bRemains = new long[num - length];
        System.arraycopy(bs, 0, bSubs, 0, length);
        System.arraycopy(bs, length, bRemains, 0, num - length);
        bs = bRemains;
        // 切分c
        long[] cSubs = new long[length];
        long[] cRemains = new long[num - length];
        System.arraycopy(cs, 0, cSubs, 0, length);
        System.arraycopy(cs, length, cRemains, 0, num - length);
        cs = cRemains;
        // 更新长度
        num = num - length;

        return Zp64Triple.create(p, length, aSubs, bSubs, cSubs);
    }

    /**
     * 将zp64乘法三元组长度缩减为指定长度。
     *
     * @param length 指定缩减长度。
     */
    public void reduce(int length) {
        assert length > 0 && length <= num : "reduce length = " + length + " must be in range (0, " + num + "]";
        // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
        if (length < num) {
            // 减小a
            long[] aRemains = new long[length];
            System.arraycopy(as, 0, aRemains, 0, length);
            as = aRemains;
            // 减小b
            long[] bRemains = new long[length];
            System.arraycopy(bs, 0, bRemains, 0, length);
            bs = bRemains;
            // 减小c
            long[] cRemains = new long[length];
            System.arraycopy(cs, 0, cRemains, 0, length);
            cs = cRemains;
            // 减小长度
            num = length;
        }
    }

    /**
     * 合并两个zp64乘法三元组。
     *
     * @param that 另一个乘法三元组。
     */
    public void merge(Zp64Triple that) {
        assert this.p == that.p : "merged " + Zp64Triple.class.getSimpleName() + " must have the same l";
        // 合并a
        long[] mergeAs = new long[this.as.length + that.as.length];
        System.arraycopy(this.as, 0, mergeAs, 0, this.as.length);
        System.arraycopy(that.as, 0, mergeAs, this.as.length, that.as.length);
        as = mergeAs;
        // 合并b
        long[] mergeBs = new long[this.bs.length + that.bs.length];
        System.arraycopy(this.bs, 0, mergeBs, 0, this.bs.length);
        System.arraycopy(that.bs, 0, mergeBs, this.bs.length, that.bs.length);
        bs = mergeBs;
        // 合并c
        long[] mergeCs = new long[this.cs.length + that.cs.length];
        System.arraycopy(this.cs, 0, mergeCs, 0, this.cs.length);
        System.arraycopy(that.cs, 0, mergeCs, this.cs.length, that.cs.length);
        cs = mergeCs;
        // 更新长度
        num += that.num;
    }
}
