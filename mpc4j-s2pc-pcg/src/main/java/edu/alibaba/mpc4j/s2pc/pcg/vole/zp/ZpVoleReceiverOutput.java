package edu.alibaba.mpc4j.s2pc.pcg.vole.zp;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Zp-VOLE receiver output. The receiver gets (Δ, q) with t = q + Δ · x, where x and t are owned by the sender.
 *
 * @author Hanwen Feng
 * @date 2022/06/08
 */
public class ZpVoleReceiverOutput implements MergedPcgPartyOutput {
    /**
     * the Zp instance
     */
    private Zp zp;
    /**
     * Δ
     */
    private BigInteger delta;
    /**
     * q_i
     */
    private BigInteger[] q;

    /**
     * Creates an output.
     *
     * @param zp the Zp instance.
     * @param delta Δ.
     * @param q     q.
     * @return an output.
     */
    public static ZpVoleReceiverOutput create(Zp zp, BigInteger delta, BigInteger[] q) {
        ZpVoleReceiverOutput receiverOutput = new ZpVoleReceiverOutput();
        receiverOutput.zp = zp;
        assert zp.validateRangeElement(delta) : "Δ must be in range [0, " + zp.getRangeBound() + "): " + delta;
        receiverOutput.delta = delta;
        assert q.length > 0 : "# of q must be greater than 0: " + q.length;
        receiverOutput.q = Arrays.stream(q)
            .peek(qi -> {
                assert zp.validateElement(qi) : "qi must be in range [0, " + zp.getPrime() + "): " + qi;
            })
            .toArray(BigInteger[]::new);
        return receiverOutput;
    }

    /**
     * Creates an empty output.
     *
     * @param zp the Zp instance.
     * @param delta Δ.
     * @return an empty output.
     */
    public static ZpVoleReceiverOutput createEmpty(Zp zp, BigInteger delta) {
        ZpVoleReceiverOutput receiverOutput = new ZpVoleReceiverOutput();
        receiverOutput.zp = zp;
        assert zp.validateRangeElement(delta) : "Δ must be in range [0, " + zp.getRangeBound() + "): " + delta;
        receiverOutput.delta = delta;
        receiverOutput.q = new BigInteger[0];

        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private ZpVoleReceiverOutput() {
        // empty
    }

    @Override
    public int getNum() {
        return q.length;
    }

    @Override
    public ZpVoleReceiverOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        // split q
        BigInteger[] subQ = new BigInteger[splitNum];
        BigInteger[] remainQ = new BigInteger[num - splitNum];
        System.arraycopy(q, 0, subQ, 0, splitNum);
        System.arraycopy(q, splitNum, remainQ, 0, num - splitNum);
        q = remainQ;

        return ZpVoleReceiverOutput.create(zp, delta, subQ);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            BigInteger[] remainQ = new BigInteger[reduceNum];
            System.arraycopy(q, 0, remainQ, 0, reduceNum);
            q = remainQ;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        ZpVoleReceiverOutput that = (ZpVoleReceiverOutput) other;
        assert this.zp.equals(that.zp) : "merged " + this.getClass().getSimpleName()
            + " must have the same " + zp.getClass().getSimpleName() + " instance:"
            + " (" + this.zp + " : " + that.zp + ")";
        assert this.delta.equals(that.delta) : "merged outputs must have the same Δ (" + this.delta + " : " + that.delta + ")";
        // merge q
        BigInteger[] mergeQ = new BigInteger[this.q.length + that.q.length];
        System.arraycopy(this.q, 0, mergeQ, 0, this.q.length);
        System.arraycopy(that.q, 0, mergeQ, this.q.length, that.q.length);
        q = mergeQ;
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
     * Gets Δ.
     *
     * @return Δ.
     */
    public BigInteger getDelta() {
        return delta;
    }

    /**
     * Gets q_i.
     *
     * @param index the index.
     * @return q_i.
     */
    public BigInteger getQ(int index) {
        return q[index];
    }

    /**
     * Gets q.
     *
     * @return q.
     */
    public BigInteger[] getQ() {
        return q;
    }
}
