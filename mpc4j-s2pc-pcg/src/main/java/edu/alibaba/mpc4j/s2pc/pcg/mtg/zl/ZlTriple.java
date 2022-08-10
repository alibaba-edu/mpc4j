package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * Z_{2^l}乘法三元组。
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
     * 模数字节长度
     */
    private int byteL;
    /**
     * 乘法三元组数量
     */
    private int n;
    /**
     * 随机分量a
     */
    private byte[][] as;
    /**
     * 随机分量b
     */
    private byte[][] bs;
    /**
     * 随机分量c
     */
    private byte[][] cs;

    /**
     * 创建Z_{2^l}乘法三元组。
     *
     * @param l 模数比特长度。
     * @param n 乘法三元组数量。
     * @param as 随机分量a。
     * @param bs 随机分量b。
     * @param cs 随机分量c。
     */
    public static ZlTriple create(int l, int n, byte[][] as, byte[][] bs, byte[][] cs) {
        assert n > 0 : "n must be greater than 0";
        assert as.length == n : "a.length must be equal to n = " + n;
        assert bs.length == n : "b.length must be equal to n = " + n;
        assert cs.length == n : "c.length must be equal to n = " + n;
        // 验证随机分量的长度
        assert l > 0 : "l must be greater than 0";
        int byteL = CommonUtils.getByteLength(l);

        ZlTriple zlTriple = new ZlTriple();
        zlTriple.l = l;
        zlTriple.byteL = byteL;
        zlTriple.n = n;
        zlTriple.as = Arrays.stream(as)
            .peek(a -> {
                assert a.length == byteL && BytesUtils.isReduceByteArray(a, l);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        zlTriple.bs = Arrays.stream(bs)
            .peek(b -> {
                assert b.length == byteL && BytesUtils.isReduceByteArray(b, l);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        zlTriple.cs = Arrays.stream(cs)
            .peek(c -> {
                assert c.length == byteL && BytesUtils.isReduceByteArray(c, l);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);

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
        int byteL = CommonUtils.getByteLength(l);

        ZlTriple emptyTriple = new ZlTriple();
        emptyTriple.l = l;
        emptyTriple.byteL = byteL;
        emptyTriple.n = 0;
        emptyTriple.as = new byte[0][];
        emptyTriple.bs = new byte[0][];
        emptyTriple.cs = new byte[0][];

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
    public int getN() {
        return n;
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
     * 返回乘法三元组模数字节长度。
     *
     * @return 乘法三元组模数字节长度。
     */
    public int getByteL() {
        return byteL;
    }

    /**
     * 返回随机分量a。
     *
     * @param index 索引值。
     * @return 随机分量a。
     */
    public byte[] getA(int index) {
        return as[index];
    }

    /**
     * 返回所有随机分量a。
     *
     * @return 所有随机分量a。
     */
    public byte[][] getAs() {
        return as;
    }

    /**
     * 返回随机分量b。
     *
     * @param index 索引值。
     * @return 随机分量b。
     */
    public byte[] getB(int index) {
        return bs[index];
    }

    /**
     * 返回所有随机分量b。
     *
     * @return 所有随机分量b。
     */
    public byte[][] getBs() {
        return bs;
    }

    /**
     * 返回随机分量c。
     *
     * @param index 索引值。
     * @return 随机分量c。
     */
    public byte[] getC(int index) {
        return cs[index];
    }

    /**
     * 返回所有随机分量c。
     *
     * @return 所有随机分量c。
     */
    public byte[][] getCs() {
        return cs;
    }

    /**
     * 从乘法三元组中切分出指定长度的子乘法三元组。
     *
     * @param length 指定切分长度。
     */
    public ZlTriple split(int length) {
        assert length > 0 && length <= n : "split length must be in range (0, " + n + "]";
        // 切分a
        byte[][] aSubs = new byte[length][];
        byte[][] aRemains = new byte[n - length][];
        System.arraycopy(as, 0, aSubs, 0, length);
        System.arraycopy(as, length, aRemains, 0, n - length);
        as = aRemains;
        // 切分b
        byte[][] bSubs = new byte[length][];
        byte[][] bRemains = new byte[n - length][];
        System.arraycopy(bs, 0, bSubs, 0, length);
        System.arraycopy(bs, length, bRemains, 0, n - length);
        bs = bRemains;
        // 切分c
        byte[][] cSubs = new byte[length][];
        byte[][] cRemains = new byte[n - length][];
        System.arraycopy(cs, 0, cSubs, 0, length);
        System.arraycopy(cs, length, cRemains, 0, n - length);
        cs = cRemains;

        return ZlTriple.create(l, length, aSubs, bSubs, cSubs);
    }

    /**
     * 将布尔三元组长度缩减为指定长度。
     *
     * @param length 指定缩减长度。
     */
    public void reduce(int length) {
        assert length > 0 && length <= n : "reduce length = " + length + " must be in range (0, " + n + "]";
        // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
        if (length < n) {
            // 减小a
            byte[][] aRemains = new byte[length][];
            System.arraycopy(as, 0, aRemains, 0, length);
            as = aRemains;
            // 减小b
            byte[][] bRemains = new byte[length][];
            System.arraycopy(bs, 0, bRemains, 0, length);
            bs = bRemains;
            // 减小c
            byte[][] cRemains = new byte[length][];
            System.arraycopy(cs, 0, cRemains, 0, length);
            cs = cRemains;
            // 减小长度
            n = length;
        }
    }

    /**
     * 合并两个乘法三元组。
     *
     * @param that 另一个乘法三元组。
     */
    public void merge(ZlTriple that) {
        assert this.l == that.l : "merged triples must have the same l";
        // 合并a
        byte[][] mergeAs = new byte[this.as.length + that.as.length][];
        System.arraycopy(this.as, 0, mergeAs, 0, this.as.length);
        System.arraycopy(that.as, 0, mergeAs, this.as.length, that.as.length);
        as = mergeAs;
        // 合并b
        byte[][] mergeBs = new byte[this.bs.length + that.bs.length][];
        System.arraycopy(this.bs, 0, mergeBs, 0, this.bs.length);
        System.arraycopy(that.bs, 0, mergeBs, this.bs.length, that.bs.length);
        bs = mergeBs;
        // 合并c
        byte[][] mergeCs = new byte[this.cs.length + that.cs.length][];
        System.arraycopy(this.cs, 0, mergeCs, 0, this.cs.length);
        System.arraycopy(that.cs, 0, mergeCs, this.cs.length, that.cs.length);
        cs = mergeCs;
        // 更新长度
        n += that.n;
    }
}
