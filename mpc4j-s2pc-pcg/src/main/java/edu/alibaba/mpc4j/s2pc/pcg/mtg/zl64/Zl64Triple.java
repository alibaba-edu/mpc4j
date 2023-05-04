package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl64;

import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.Arrays;

/**
 * Zl64 triple.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
public class Zl64Triple implements MergedPcgPartyOutput {
    /**
     * the Zl64 instance
     */
    private Zl64 zl64;
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
     * @param zl64 the Zl64 instance.
     * @param num  num.
     * @param as   a.
     * @param bs   b.
     * @param cs   c.
     * @return a triple.
     */
    public static Zl64Triple create(Zl64 zl64, int num, long[] as, long[] bs, long[] cs) {
        assert num > 0 : "num must be greater than 0: " + num;
        assert as.length == num : "a.length must be equal to num = " + num + ": " + as.length;
        assert bs.length == num : "b.length must be equal to num = " + num + ": " + bs.length;
        assert cs.length == num : "c.length must be equal to num = " + num + ": " + cs.length;

        Zl64Triple triple = new Zl64Triple();
        triple.zl64 = zl64;
        triple.num = num;
        triple.as = Arrays.stream(as)
            .peek(a -> {
                assert triple.zl64.validateElement(a);
            })
            .toArray();
        triple.bs = Arrays.stream(bs)
            .peek(b -> {
                assert triple.zl64.validateElement(b);
            })
            .toArray();
        triple.cs = Arrays.stream(cs)
            .peek(c -> {
                assert triple.zl64.validateElement(c);
            })
            .toArray();

        return triple;
    }

    /**
     * Creates an empty triple.
     *
     * @param zl64 the Zl64 instance.
     * @return an empty triple.
     */
    public static Zl64Triple createEmpty(Zl64 zl64) {
        Zl64Triple emptyTriple = new Zl64Triple();
        emptyTriple.zl64 = zl64;
        emptyTriple.num = 0;
        emptyTriple.as = new long[0];
        emptyTriple.bs = new long[0];
        emptyTriple.cs = new long[0];

        return emptyTriple;
    }

    /**
     * private constructor.
     */
    private Zl64Triple() {
        // empty
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public Zl64Triple split(int splitNum) {
        assert splitNum > 0 && splitNum <= num : "split num must be in range (0, " + num + "]: " + splitNum;
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
        // update the num
        num = num - splitNum;

        return Zl64Triple.create(zl64, splitNum, aSubs, bSubs, cSubs);
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
            // reduce the num
            num = reduceNum;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Zl64Triple that = (Zl64Triple) other;
        assert this.zl64.equals(that.zl64) : "merged " + this.getClass().getSimpleName()
            + " must have the same " + zl64.getClass().getSimpleName() + " instance:"
            + " (" + this.zl64 + " : " + that.zl64 + ")";
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
        // update the num
        num += that.num;
    }

    /**
     * Gets the Zl64 instance.
     *
     * @return the Zl64 instance.
     */
    public Zl64 getZl64() {
        return zl64;
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
     * Gets b[i].
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
     * Gets c[i].
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
