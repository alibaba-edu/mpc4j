package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.Arrays;

/**
 * Zp64-VOLE sender output. The sender gets (x, t) with t = q + Δ · x, where Δ and q are owned by the receiver.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public class Zp64VoleSenderOutput implements MergedPcgPartyOutput {
    /**
     * the Zp64 instance
     */
    private Zp64 zp64;
    /**
     * x_i
     */
    private long[] x;
    /**
     * t_i
     */
    private long[] t;

    /**
     * Creates a sender output.
     *
     * @param zp64 the Zp64 instance.
     * @param x    x.
     * @param t    t.
     * @return a sender output.
     */
    public static Zp64VoleSenderOutput create(Zp64 zp64, long[] x, long[] t) {
        Zp64VoleSenderOutput senderOutput = new Zp64VoleSenderOutput();
        assert x.length > 0 : "# of x must be greater than 0: " + x.length;
        assert x.length == t.length : "# of x must be equal to # of t (" + x.length + " : " + t.length + ")";
        senderOutput.zp64 = zp64;
        senderOutput.x = Arrays.stream(x)
            .peek(xi -> {
                assert zp64.validateElement(xi) : "xi must be in range [0, " + zp64.getPrime() + "): " + xi;
            })
            .toArray();
        senderOutput.t = Arrays.stream(t)
            .peek(ti -> {
                assert zp64.validateElement(ti) : "ti must be in range [0, " + zp64.getPrime() + "): " + ti;
            })
            .toArray();
        return senderOutput;
    }

    /**
     * Creates an empty sender output.
     *
     * @param zp64 the Zp64 instance.
     * @return an empty sender output.
     */
    public static Zp64VoleSenderOutput createEmpty(Zp64 zp64) {
        Zp64VoleSenderOutput senderOutput = new Zp64VoleSenderOutput();
        senderOutput.zp64 = zp64;
        senderOutput.x = new long[0];
        senderOutput.t = new long[0];
        return senderOutput;
    }

    /**
     * private constructor.
     */
    private Zp64VoleSenderOutput() {
        // empty
    }

    @Override
    public int getNum() {
        return x.length;
    }

    @Override
    public Zp64VoleSenderOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]:" + splitNum;
        // split x
        long[] subX = new long[splitNum];
        long[] remainX = new long[num - splitNum];
        System.arraycopy(x, 0, subX, 0, splitNum);
        System.arraycopy(x, splitNum, remainX, 0, num - splitNum);
        x = remainX;
        // split t
        long[] subT = new long[splitNum];
        long[] remainT = new long[num - splitNum];
        System.arraycopy(t, 0, subT, 0, splitNum);
        System.arraycopy(t, splitNum, remainT, 0, num - splitNum);
        t = remainT;

        return Zp64VoleSenderOutput.create(zp64, subX, subT);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            long[] remainX = new long[reduceNum];
            System.arraycopy(x, 0, remainX, 0, reduceNum);
            x = remainX;
            long[] remainT = new long[reduceNum];
            System.arraycopy(t, 0, remainT, 0, reduceNum);
            t = remainT;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Zp64VoleSenderOutput that = (Zp64VoleSenderOutput) other;
        assert this.zp64.equals(that.zp64) : "merged " + this.getClass().getSimpleName()
            + " must have the same " + zp64.getClass().getSimpleName() + " instance:"
            + " (" + this.zp64 + " : " + that.zp64 + ")";
        // merge x
        long[] mergeX = new long[this.x.length + that.x.length];
        System.arraycopy(this.x, 0, mergeX, 0, this.x.length);
        System.arraycopy(that.x, 0, mergeX, this.x.length, that.x.length);
        x = mergeX;
        // merge t
        long[] mergeT = new long[this.t.length + that.t.length];
        System.arraycopy(this.t, 0, mergeT, 0, this.t.length);
        System.arraycopy(that.t, 0, mergeT, this.t.length, that.t.length);
        t = mergeT;
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
     * Gets x_i.
     *
     * @param index the index.
     * @return x_i.
     */
    public long getX(int index) {
        return x[index];
    }

    /**
     * Gets x.
     *
     * @return x.
     */
    public long[] getX() {
        return x;
    }

    /**
     * Gets t_i.
     *
     * @param index the index.
     * @return t_i.
     */
    public long getT(int index) {
        return t[index];
    }

    /**
     * Gets t.
     *
     * @return t.
     */
    public long[] getT() {
        return t;
    }
}
