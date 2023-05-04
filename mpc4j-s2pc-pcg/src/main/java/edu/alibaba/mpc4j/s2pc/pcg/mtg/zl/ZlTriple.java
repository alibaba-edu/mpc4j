package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * ZL triple.
 *
 * @author Weiran Liu
 * @date 2022/4/11
 */
public class ZlTriple implements MergedPcgPartyOutput {
    /**
     * the Zl instance
     */
    private Zl zl;
    /**
     * num
     */
    private int num;
    /**
     * a
     */
    private BigInteger[] as;
    /**
     * b
     */
    private BigInteger[] bs;
    /**
     * c
     */
    private BigInteger[] cs;

    /**
     * Creates a triple.
     *
     * @param zl  the Zl instance.
     * @param num num.
     * @param as  a.
     * @param bs  b.
     * @param cs  c.
     * @return a triple.
     */
    public static ZlTriple create(Zl zl, int num, BigInteger[] as, BigInteger[] bs, BigInteger[] cs) {
        assert num > 0 : "num must be greater than 0: " + num;
        assert as.length == num : "a.length must be equal to num = " + num + ": " + as.length;
        assert bs.length == num : "b.length must be equal to num = " + num + ": " + bs.length;
        assert cs.length == num : "c.length must be equal to num = " + num + ": " + cs.length;

        ZlTriple triple = new ZlTriple();
        triple.zl = zl;
        triple.num = num;
        triple.as = Arrays.stream(as)
            .peek(a -> {
                assert triple.zl.validateElement(a);
            })
            .toArray(BigInteger[]::new);
        triple.bs = Arrays.stream(bs)
            .peek(b -> {
                assert triple.zl.validateElement(b);
            })
            .toArray(BigInteger[]::new);
        triple.cs = Arrays.stream(cs)
            .peek(c -> {
                assert triple.zl.validateElement(c);
            })
            .toArray(BigInteger[]::new);

        return triple;
    }

    /**
     * Creates an empty triple.
     *
     * @param zl the Zl instance.
     * @return an empty triple.
     */
    public static ZlTriple createEmpty(Zl zl) {
        ZlTriple emptyTriple = new ZlTriple();
        emptyTriple.zl = zl;
        emptyTriple.num = 0;
        emptyTriple.as = new BigInteger[0];
        emptyTriple.bs = new BigInteger[0];
        emptyTriple.cs = new BigInteger[0];

        return emptyTriple;
    }

    /**
     * private constructor.
     */
    private ZlTriple() {
        // empty
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public ZlTriple split(int splitNum) {
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        // split a
        BigInteger[] aSubs = new BigInteger[splitNum];
        BigInteger[] aRemains = new BigInteger[num - splitNum];
        System.arraycopy(as, 0, aSubs, 0, splitNum);
        System.arraycopy(as, splitNum, aRemains, 0, num - splitNum);
        as = aRemains;
        // split b
        BigInteger[] bSubs = new BigInteger[splitNum];
        BigInteger[] bRemains = new BigInteger[num - splitNum];
        System.arraycopy(bs, 0, bSubs, 0, splitNum);
        System.arraycopy(bs, splitNum, bRemains, 0, num - splitNum);
        bs = bRemains;
        // split c
        BigInteger[] cSubs = new BigInteger[splitNum];
        BigInteger[] cRemains = new BigInteger[num - splitNum];
        System.arraycopy(cs, 0, cSubs, 0, splitNum);
        System.arraycopy(cs, splitNum, cRemains, 0, num - splitNum);
        cs = cRemains;
        // update num
        num = num - splitNum;

        return ZlTriple.create(zl, splitNum, aSubs, bSubs, cSubs);
    }

    @Override
    public void reduce(int reduceNum) {
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        // if the reduced num is less than num, split the triple. If not, keep the current state.
        if (reduceNum < num) {
            // reduce a
            BigInteger[] aRemains = new BigInteger[reduceNum];
            System.arraycopy(as, 0, aRemains, 0, reduceNum);
            as = aRemains;
            // reduce b
            BigInteger[] bRemains = new BigInteger[reduceNum];
            System.arraycopy(bs, 0, bRemains, 0, reduceNum);
            bs = bRemains;
            // reduce c
            BigInteger[] cRemains = new BigInteger[reduceNum];
            System.arraycopy(cs, 0, cRemains, 0, reduceNum);
            cs = cRemains;
            // reduce the num
            num = reduceNum;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        ZlTriple that = (ZlTriple) other;
        assert this.zl.equals(that.zl) : "merged " + this.getClass().getSimpleName()
            + " must have the same " + zl.getClass().getSimpleName() + " instance:"
            + " (" + this.zl + " : " + that.zl + ")";
        // merge a
        BigInteger[] mergeAs = new BigInteger[this.as.length + that.as.length];
        System.arraycopy(this.as, 0, mergeAs, 0, this.as.length);
        System.arraycopy(that.as, 0, mergeAs, this.as.length, that.as.length);
        as = mergeAs;
        // merge b
        BigInteger[] mergeBs = new BigInteger[this.bs.length + that.bs.length];
        System.arraycopy(this.bs, 0, mergeBs, 0, this.bs.length);
        System.arraycopy(that.bs, 0, mergeBs, this.bs.length, that.bs.length);
        bs = mergeBs;
        // merge c
        BigInteger[] mergeCs = new BigInteger[this.cs.length + that.cs.length];
        System.arraycopy(this.cs, 0, mergeCs, 0, this.cs.length);
        System.arraycopy(that.cs, 0, mergeCs, this.cs.length, that.cs.length);
        cs = mergeCs;
        // update the num
        num += that.num;
    }

    /**
     * Gets the Zl instance.
     *
     * @return the Zl instance.
     */
    public Zl getZl() {
        return zl;
    }

    /**
     * Gets a[i]。
     *
     * @param index the index.
     * @return a[i].
     */
    public BigInteger getA(int index) {
        return as[index];
    }

    /**
     * Gets a.
     *
     * @return a.
     */
    public BigInteger[] getA() {
        return as;
    }

    /**
     * Gets b[i].
     *
     * @param index the index.
     * @return b[i].
     */
    public BigInteger getB(int index) {
        return bs[index];
    }

    /**
     * Gets b.
     *
     * @return b.
     */
    public BigInteger[] getB() {
        return bs;
    }

    /**
     * Gets c[i].
     *
     * @param index the index.
     * @return c[i].
     */
    public BigInteger getC(int index) {
        return cs[index];
    }

    /**
     * Gets c.
     *
     * @return c.
     */
    public BigInteger[] getC() {
        return cs;
    }
}
