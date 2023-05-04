package edu.alibaba.mpc4j.s2pc.pcg.vole.zp;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Zp-VOLE sender output. The sender gets (x, t) with t = q + Δ·x, where Δ and q are owned by the receiver.
 *
 * @author Hanwen Feng
 * @date 2022/06/07
 */
public class ZpVoleSenderOutput implements MergedPcgPartyOutput {
    /**
     * the Zp instance
     */
    private Zp zp;
    /**
     * x_i
     */
    private BigInteger[] x;
    /**
     * t_i
     */
    private BigInteger[] t;

    /**
     * Creates a sender output.
     *
     * @param zp the Zp instance.
     * @param x  x.
     * @param t  t.
     * @return a sender output.
     */
    public static ZpVoleSenderOutput create(Zp zp, BigInteger[] x, BigInteger[] t) {
        ZpVoleSenderOutput senderOutput = new ZpVoleSenderOutput();
        assert x.length > 0 : "# of x must be greater than 0: " + x.length;
        assert x.length == t.length : "# of x must be equal to # of t (" + x.length + " : " + t.length + ")";
        senderOutput.zp = zp;
        senderOutput.x = Arrays.stream(x)
            .peek(xi -> {
                assert zp.validateElement(xi) : "xi must be in range [0, " + zp.getPrime() + "): " + xi;
            })
            .toArray(BigInteger[]::new);
        senderOutput.t = Arrays.stream(t)
            .peek(ti -> {
                assert zp.validateElement(ti) : "ti must be in range [0, " + zp.getPrime() + "): " + ti;
            })
            .toArray(BigInteger[]::new);
        return senderOutput;
    }

    /**
     * Creates an empty sender output.
     *
     * @param zp the Zp instance.
     * @return an empty sender output.
     */
    public static ZpVoleSenderOutput createEmpty(Zp zp) {
        ZpVoleSenderOutput senderOutput = new ZpVoleSenderOutput();
        senderOutput.zp = zp;
        senderOutput.x = new BigInteger[0];
        senderOutput.t = new BigInteger[0];
        return senderOutput;
    }

    /**
     * private constructor.
     */
    private ZpVoleSenderOutput() {
        // empty
    }

    @Override
    public int getNum() {
        return x.length;
    }

    @Override
    public ZpVoleSenderOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        // split x
        BigInteger[] subX = new BigInteger[splitNum];
        BigInteger[] remainX = new BigInteger[num - splitNum];
        System.arraycopy(x, 0, subX, 0, splitNum);
        System.arraycopy(x, splitNum, remainX, 0, num - splitNum);
        x = remainX;
        // split t
        BigInteger[] subT = new BigInteger[splitNum];
        BigInteger[] remainT = new BigInteger[num - splitNum];
        System.arraycopy(t, 0, subT, 0, splitNum);
        System.arraycopy(t, splitNum, remainT, 0, num - splitNum);
        t = remainT;

        return ZpVoleSenderOutput.create(zp, subX, subT);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            BigInteger[] remainX = new BigInteger[reduceNum];
            System.arraycopy(x, 0, remainX, 0, reduceNum);
            x = remainX;
            BigInteger[] remainT = new BigInteger[reduceNum];
            System.arraycopy(t, 0, remainT, 0, reduceNum);
            t = remainT;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        ZpVoleSenderOutput that = (ZpVoleSenderOutput) other;
        assert this.zp.equals(that.zp) : "merged " + this.getClass().getSimpleName()
            + " must have the same " + zp.getClass().getSimpleName() + " instance:"
            + " (" + this.zp + " : " + that.zp + ")";
        // merge x
        BigInteger[] mergeX = new BigInteger[this.x.length + that.x.length];
        System.arraycopy(this.x, 0, mergeX, 0, this.x.length);
        System.arraycopy(that.x, 0, mergeX, this.x.length, that.x.length);
        x = mergeX;
        // merge t
        BigInteger[] mergeT = new BigInteger[this.t.length + that.t.length];
        System.arraycopy(this.t, 0, mergeT, 0, this.t.length);
        System.arraycopy(that.t, 0, mergeT, this.t.length, that.t.length);
        t = mergeT;
    }

    /**
     * Gets the Zp instance.
     *
     * @return the Zp instance.
     */
    public Zp getZp() {
        return zp;
    }

    /**
     * Gets x_i.
     *
     * @param index the index.
     * @return x_i.
     */
    public BigInteger getX(int index) {
        return x[index];
    }

    /**
     * Gets x.
     *
     * @return x.
     */
    public BigInteger[] getX() {
        return x;
    }

    /**
     * Gets t_i.
     *
     * @param index the index.
     * @return t_i.
     */
    public BigInteger getT(int index) {
        return t[index];
    }

    /**
     * Gets t.
     *
     * @return t.
     */
    public BigInteger[] getT() {
        return t;
    }
}
