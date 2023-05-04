package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.Arrays;

/**
 * Zp64 triple.
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Zp64Triple implements MergedPcgPartyOutput {
    /**
     * the Zp64 instance
     */
    private Zp64 zp64;
    /**
     * num
     */
    private int num;
    /**
     * a
     */
    private long[] as;
    /**
     * b
     */
    private long[] bs;
    /**
     * c
     */
    private long[] cs;

    /**
     * Creates a triple.
     *
     * @param zp64 the Zp64 instance.
     * @param num  num.
     * @param as   a.
     * @param bs   b.
     * @param cs   c.
     * @return a triple.
     */
    public static Zp64Triple create(Zp64 zp64, int num, long[] as, long[] bs, long[] cs) {
        assert num > 0 : "num must be greater than 0: " + num;
        assert as.length == num : "a.length must be equal to num = " + num + ": " + as.length;
        assert bs.length == num : "b.length must be equal to num = " + num + ": " + bs.length;
        assert cs.length == num : "c.length must be equal to num = " + num + ": " + cs.length;
        Zp64Triple triple = new Zp64Triple();
        triple.zp64 = zp64;
        triple.num = num;
        triple.as = Arrays.stream(as)
            .peek(a -> {
                assert triple.zp64.validateElement(a);
            })
            .toArray();
        triple.bs = Arrays.stream(bs)
            .peek(b -> {
                assert triple.zp64.validateElement(b);
            })
            .toArray();
        triple.cs = Arrays.stream(cs)
            .peek(c -> {
                assert triple.zp64.validateElement(c);
            })
            .toArray();

        return triple;
    }

    /**
     * Creates an empty triple.
     *
     * @param zp64 the Zp64 instance.
     * @return an empty triple.
     */
    public static Zp64Triple createEmpty(Zp64 zp64) {
        Zp64Triple emptyTriple = new Zp64Triple();
        emptyTriple.zp64 = zp64;
        emptyTriple.num = 0;
        emptyTriple.as = new long[0];
        emptyTriple.bs = new long[0];
        emptyTriple.cs = new long[0];

        return emptyTriple;
    }

    /**
     * private constructor.
     */
    private Zp64Triple() {
        // empty
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public Zp64Triple split(int splitNum) {
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        // split a
        long[] aSubs = new long[splitNum];
        long[] aRemains = new long[num - splitNum];
        System.arraycopy(as, 0, aSubs, 0, splitNum);
        System.arraycopy(as, splitNum, aRemains, 0, num - splitNum);
        as = aRemains;
        // split b
        long[] bSubs = new long[splitNum];
        long[] bRemains = new long[num - splitNum];
        System.arraycopy(bs, 0, bSubs, 0, splitNum);
        System.arraycopy(bs, splitNum, bRemains, 0, num - splitNum);
        bs = bRemains;
        // split c
        long[] cSubs = new long[splitNum];
        long[] cRemains = new long[num - splitNum];
        System.arraycopy(cs, 0, cSubs, 0, splitNum);
        System.arraycopy(cs, splitNum, cRemains, 0, num - splitNum);
        cs = cRemains;
        // update num
        num = num - splitNum;

        return Zp64Triple.create(zp64, splitNum, aSubs, bSubs, cSubs);
    }

    @Override
    public void reduce(int reduceNum) {
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        // if the reduced num is less than num, split the triple. If not, keep the current state.
        if (reduceNum < num) {
            // reduce a
            long[] aRemains = new long[reduceNum];
            System.arraycopy(as, 0, aRemains, 0, reduceNum);
            as = aRemains;
            // reduce b
            long[] bRemains = new long[reduceNum];
            System.arraycopy(bs, 0, bRemains, 0, reduceNum);
            bs = bRemains;
            // reduce c
            long[] cRemains = new long[reduceNum];
            System.arraycopy(cs, 0, cRemains, 0, reduceNum);
            cs = cRemains;
            // reduce num
            num = reduceNum;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Zp64Triple that = (Zp64Triple) other;
        assert this.zp64.equals(that.zp64) : "merged " + this.getClass().getSimpleName()
            + " must have the same " + zp64.getClass().getSimpleName() + " instance:"
            + " (" + this.zp64 + " : " + that.zp64 + ")";
        // merge a
        long[] mergeAs = new long[this.as.length + that.as.length];
        System.arraycopy(this.as, 0, mergeAs, 0, this.as.length);
        System.arraycopy(that.as, 0, mergeAs, this.as.length, that.as.length);
        as = mergeAs;
        // merge b
        long[] mergeBs = new long[this.bs.length + that.bs.length];
        System.arraycopy(this.bs, 0, mergeBs, 0, this.bs.length);
        System.arraycopy(that.bs, 0, mergeBs, this.bs.length, that.bs.length);
        bs = mergeBs;
        // merge c
        long[] mergeCs = new long[this.cs.length + that.cs.length];
        System.arraycopy(this.cs, 0, mergeCs, 0, this.cs.length);
        System.arraycopy(that.cs, 0, mergeCs, this.cs.length, that.cs.length);
        cs = mergeCs;
        // update num
        num += that.num;
    }

    /**
     * Gets the Zp64 instance.
     *
     * @return the Zp64 instance.
     */
    public Zp64 getZp64() {
        return zp64;
    }

    /**
     * Gets a[i]。
     *
     * @param index the index.
     * @return a[i].
     */
    public long getA(int index) {
        return as[index];
    }

    /**
     * Gets a.
     *
     * @return a.
     */
    public long[] getA() {
        return as;
    }

    /**
     * Gets b[i]。
     *
     * @param index the index.
     * @return b[i].
     */
    public long getB(int index) {
        return bs[index];
    }

    /**
     * Gets b.
     *
     * @return b.
     */
    public long[] getB() {
        return bs;
    }

    /**
     * Gets c[i]。
     *
     * @param index the index.
     * @return c[i].
     */
    public long getC(int index) {
        return cs[index];
    }

    /**
     * Gets c.
     *
     * @return c.
     */
    public long[] getC() {
        return cs;
    }
}
