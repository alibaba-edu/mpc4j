package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64Triple;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * l比特三元组。
 *
 * @author Weiran Liu
 * @date 2022/4/11
 */
public class ZlTriple {
    /**
     * 模数比特长度
     */
    private int l;
    /**
     * 乘法三元组数量
     */
    private int num;
    /**
     * 随机分量a
     */
    private BigInteger[] as;
    /**
     * 随机分量b
     */
    private BigInteger[] bs;
    /**
     * 随机分量c
     */
    private BigInteger[] cs;

    /**
     * 创建Z_{2^l}乘法三元组。
     *
     * @param l 模数比特长度。
     * @param num 乘法三元组数量。
     * @param as 随机分量a。
     * @param bs 随机分量b。
     * @param cs 随机分量c。
     */
    public static ZlTriple create(int l, int num, BigInteger[] as, BigInteger[] bs, BigInteger[] cs) {
        assert num > 0 : "num must be greater than 0";
        assert as.length == num : "a.length must be equal to num = " + num;
        assert bs.length == num : "b.length must be equal to num = " + num;
        assert cs.length == num : "c.length must be equal to num = " + num;
        // 验证随机分量的长度
        assert l > 0 : "l must be greater than 0";

        ZlTriple zlTriple = new ZlTriple();
        zlTriple.l = l;
        zlTriple.num = num;
        zlTriple.as = Arrays.stream(as)
            .peek(a -> {
                assert validElement(a, l);
            })
            .toArray(BigInteger[]::new);
        zlTriple.bs = Arrays.stream(bs)
            .peek(b -> {
                assert validElement(b, l);
            })
            .toArray(BigInteger[]::new);
        zlTriple.cs = Arrays.stream(cs)
            .peek(c -> {
                assert validElement(c, l);
            })
            .toArray(BigInteger[]::new);

        return zlTriple;
    }

    /**
     * 创建长度为0的Z_{2^l}三元组随机分量。
     *
     * @param l 模数比特长度。
     * @return 长度为0的布尔三元组随机分量。
     */
    public static ZlTriple createEmpty(int l) {
        // 验证随机分量的长度
        assert l > 0 : "l must be greater than 0";

        ZlTriple emptyTriple = new ZlTriple();
        emptyTriple.l = l;
        emptyTriple.num = 0;
        emptyTriple.as = new BigInteger[0];
        emptyTriple.bs = new BigInteger[0];
        emptyTriple.cs = new BigInteger[0];

        return emptyTriple;
    }

    /**
     * 私有构造函数。
     */
    private ZlTriple() {
        // empty
    }

    /**
     * 返回乘法三元组数量。
     *
     * @return 乘法三元组数量。
     */
    public int getNum() {
        return num;
    }

    /**
     * 返回乘法三元组模数比特长度。
     *
     * @return 乘法三元组模数比特长度。
     */
    public int getL() {
        return l;
    }

    /**
     * 返回随机分量a。
     *
     * @param index 索引值。
     * @return 随机分量a。
     */
    public BigInteger getA(int index) {
        return as[index];
    }

    /**
     * 返回所有随机分量a。
     *
     * @return 所有随机分量a。
     */
    public BigInteger[] getA() {
        return as;
    }

    /**
     * 返回随机分量b。
     *
     * @param index 索引值。
     * @return 随机分量b。
     */
    public BigInteger getB(int index) {
        return bs[index];
    }

    /**
     * 返回所有随机分量b。
     *
     * @return 所有随机分量b。
     */
    public BigInteger[] getB() {
        return bs;
    }

    /**
     * 返回随机分量c。
     *
     * @param index 索引值。
     * @return 随机分量c。
     */
    public BigInteger getC(int index) {
        return cs[index];
    }

    /**
     * 返回所有随机分量c。
     *
     * @return 所有随机分量c。
     */
    public BigInteger[] getC() {
        return cs;
    }

    /**
     * 从乘法三元组中切分出指定长度的子乘法三元组。
     *
     * @param length 指定切分长度。
     */
    public ZlTriple split(int length) {
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        // 切分a
        BigInteger[] aSubs = new BigInteger[length];
        BigInteger[] aRemains = new BigInteger[num - length];
        System.arraycopy(as, 0, aSubs, 0, length);
        System.arraycopy(as, length, aRemains, 0, num - length);
        as = aRemains;
        // 切分b
        BigInteger[] bSubs = new BigInteger[length];
        BigInteger[] bRemains = new BigInteger[num - length];
        System.arraycopy(bs, 0, bSubs, 0, length);
        System.arraycopy(bs, length, bRemains, 0, num - length);
        bs = bRemains;
        // 切分c
        BigInteger[] cSubs = new BigInteger[length];
        BigInteger[] cRemains = new BigInteger[num - length];
        System.arraycopy(cs, 0, cSubs, 0, length);
        System.arraycopy(cs, length, cRemains, 0, num - length);
        cs = cRemains;
        // 更新长度
        num = num - length;

        return ZlTriple.create(l, length, aSubs, bSubs, cSubs);
    }

    /**
     * 将布尔三元组长度缩减为指定长度。
     *
     * @param length 指定缩减长度。
     */
    public void reduce(int length) {
        assert length > 0 && length <= num : "reduce length = " + length + " must be in range (0, " + num + "]";
        // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
        if (length < num) {
            // 减小a
            BigInteger[] aRemains = new BigInteger[length];
            System.arraycopy(as, 0, aRemains, 0, length);
            as = aRemains;
            // 减小b
            BigInteger[] bRemains = new BigInteger[length];
            System.arraycopy(bs, 0, bRemains, 0, length);
            bs = bRemains;
            // 减小c
            BigInteger[] cRemains = new BigInteger[length];
            System.arraycopy(cs, 0, cRemains, 0, length);
            cs = cRemains;
            // 减小长度
            num = length;
        }
    }

    /**
     * 合并两个乘法三元组。
     *
     * @param that 另一个乘法三元组。
     */
    public void merge(ZlTriple that) {
        assert this.l == that.l : "merged " + ZlTriple.class.getSimpleName() + " must have the same l";
        // 合并a
        BigInteger[] mergeAs = new BigInteger[this.as.length + that.as.length];
        System.arraycopy(this.as, 0, mergeAs, 0, this.as.length);
        System.arraycopy(that.as, 0, mergeAs, this.as.length, that.as.length);
        as = mergeAs;
        // 合并b
        BigInteger[] mergeBs = new BigInteger[this.bs.length + that.bs.length];
        System.arraycopy(this.bs, 0, mergeBs, 0, this.bs.length);
        System.arraycopy(that.bs, 0, mergeBs, this.bs.length, that.bs.length);
        bs = mergeBs;
        // 合并c
        BigInteger[] mergeCs = new BigInteger[this.cs.length + that.cs.length];
        System.arraycopy(this.cs, 0, mergeCs, 0, this.cs.length);
        System.arraycopy(that.cs, 0, mergeCs, this.cs.length, that.cs.length);
        cs = mergeCs;
        // 更新长度
        num += that.num;
    }

    private static boolean validElement(BigInteger element, int l) {
        return BigIntegerUtils.greaterOrEqual(element, BigInteger.ZERO) && element.bitLength() <= l;
    }
}
