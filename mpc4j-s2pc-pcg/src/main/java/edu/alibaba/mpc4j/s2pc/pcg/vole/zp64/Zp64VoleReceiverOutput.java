package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.Arrays;

/**
 * Zp64-VOLE receiver output. The receiver gets (Δ, q) with t = q + Δ · x, where x and t are owned by the sender.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public class Zp64VoleReceiverOutput implements MergedPcgPartyOutput {
    /**
     * the Zp64 instance
     */
    private Zp64 zp64;
    /**
     * Δ
     */
    private long delta;
    /**
     * q_i
     */
    private long[] q;

    /**
     * Creates a receiver output.
     *
     * @param zp64  the Zp64 instance.
     * @param delta Δ.
     * @param q     q.
     * @return a receiver output.
     */
    public static Zp64VoleReceiverOutput create(Zp64 zp64, long delta, long[] q) {
        Zp64VoleReceiverOutput receiverOutput = new Zp64VoleReceiverOutput();
        receiverOutput.zp64 = zp64;
        assert zp64.validateRangeElement(delta) : "Δ must be in range [0, " + zp64.getRangeBound() + "): " + delta;
        receiverOutput.delta = delta;
        assert q.length > 0 : "# of q must be greater than 0: " + q.length;
        receiverOutput.q = Arrays.stream(q)
            .peek(qi -> {
                assert zp64.validateElement(qi) : "qi must be in range [0, " + zp64.getPrime() + "): " + qi;
            })
            .toArray();
        return receiverOutput;
    }

    /**
     * Creates an empty receiver output.
     *
     * @param zp64  the Zp64 instance.
     * @param delta Δ.
     * @return an empty receiver output.
     */
    public static Zp64VoleReceiverOutput createEmpty(Zp64 zp64, long delta) {
        Zp64VoleReceiverOutput receiverOutput = new Zp64VoleReceiverOutput();
        receiverOutput.zp64 = zp64;
        assert zp64.validateRangeElement(delta) : "Δ must be in range [0, " + zp64.getRangeBound() + "): " + delta;
        receiverOutput.delta = delta;
        receiverOutput.q = new long[0];

        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private Zp64VoleReceiverOutput() {
        // empty
    }

    @Override
    public int getNum() {
        return q.length;
    }

    @Override
    public Zp64VoleReceiverOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        // split q
        long[] subQ = new long[splitNum];
        long[] remainQ = new long[num - splitNum];
        System.arraycopy(q, 0, subQ, 0, splitNum);
        System.arraycopy(q, splitNum, remainQ, 0, num - splitNum);
        q = remainQ;

        return Zp64VoleReceiverOutput.create(zp64, delta, subQ);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum  must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            long[] remainQ = new long[reduceNum];
            System.arraycopy(q, 0, remainQ, 0, reduceNum);
            q = remainQ;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Zp64VoleReceiverOutput that = (Zp64VoleReceiverOutput) other;
        assert this.zp64.equals(that.zp64) : "merged " + this.getClass().getSimpleName()
            + " must have the same " + zp64.getClass().getSimpleName() + " instance:"
            + " (" + this.zp64 + " : " + that.zp64 + ")";
        assert this.delta == that.delta : "merged outputs must have the same Δ (" + this.delta + " : " + that.delta + ")";
        // merge q
        long[] mergeQ = new long[this.q.length + that.q.length];
        System.arraycopy(this.q, 0, mergeQ, 0, this.q.length);
        System.arraycopy(that.q, 0, mergeQ, this.q.length, that.q.length);
        q = mergeQ;
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
     * Gets Δ.
     *
     * @return 关联值Δ。
     */
    public long getDelta() {
        return delta;
    }

    /**
     * Gets q_i.
     *
     * @param index the index.
     * @return q_i.
     */
    public long getQ(int index) {
        return q[index];
    }

    /**
     * Gets q.
     *
     * @return q.
     */
    public long[] getQ() {
        return q;
    }
}
