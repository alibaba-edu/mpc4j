package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Zp64-VOLE receiver output. The receiver gets (Δ, q) with t = q + Δ · x, where x and t are owned by the sender.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public class Zp64VoleReceiverOutput implements MergedPcgPartyOutput {
    /**
     * Zp64
     */
    private final Zp64 zp64;
    /**
     * Δ
     */
    private final long delta;
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
        Zp64VoleReceiverOutput receiverOutput = new Zp64VoleReceiverOutput(zp64, delta);
        receiverOutput.q = Arrays.stream(q)
            .peek(qi -> Preconditions.checkArgument(zp64.validateElement(qi)))
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
        Zp64VoleReceiverOutput receiverOutput = new Zp64VoleReceiverOutput(zp64, delta);
        receiverOutput.q = new long[0];

        return receiverOutput;
    }

    /**
     * Creates a random receiver output.
     *
     * @param zp64         Zp64.
     * @param num          num.
     * @param secureRandom random state.
     * @return a random receiver output.
     */
    public static Zp64VoleReceiverOutput createRandom(Zp64 zp64, int num, long delta, SecureRandom secureRandom) {
        Zp64VoleReceiverOutput receiverOutput = new Zp64VoleReceiverOutput(zp64, delta);
        receiverOutput.q = IntStream.range(0, num)
            .mapToLong(index -> zp64.createRandom(secureRandom))
            .toArray();
        return receiverOutput;
    }

    /**
     * private constructor.
     *
     * @param zp64  Zp64.
     * @param delta Δ.
     */
    private Zp64VoleReceiverOutput(Zp64 zp64, long delta) {
        this.zp64 = zp64;
        Preconditions.checkArgument(zp64.validateRangeElement(delta));
        this.delta = delta;
    }

    @Override
    public int getNum() {
        return q.length;
    }

    @Override
    public Zp64VoleReceiverOutput copy() {
        Zp64VoleReceiverOutput copy = new Zp64VoleReceiverOutput(zp64, delta);
        copy.q = LongUtils.clone(q);
        return copy;
    }

    @Override
    public Zp64VoleReceiverOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, num);
        // split q
        long[] subQ = new long[splitNum];
        long[] remainQ = new long[num - splitNum];
        System.arraycopy(q, num - splitNum, subQ, 0, splitNum);
        System.arraycopy(q, 0, remainQ, 0, num - splitNum);
        q = remainQ;

        return Zp64VoleReceiverOutput.create(zp64, delta, subQ);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduce_num", reduceNum, num);
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
        Preconditions.checkArgument(this.zp64.equals(that.zp64));
        MathPreconditions.checkEqual("this.delta", "that.delta", this.delta, that.delta);
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(zp64)
            .append(delta)
            .append(q)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Zp64VoleReceiverOutput that) {
            return new EqualsBuilder()
                .append(this.zp64, that.zp64)
                .append(this.delta, that.delta)
                .append(this.q, that.q)
                .isEquals();
        }
        return false;
    }
}
