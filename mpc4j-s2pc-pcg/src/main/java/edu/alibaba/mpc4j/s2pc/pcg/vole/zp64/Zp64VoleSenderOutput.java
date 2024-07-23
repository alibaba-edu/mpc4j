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
 * Zp64-VOLE sender output. The sender gets (x, t) with t = q + Δ · x, where Δ and q are owned by the receiver.
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public class Zp64VoleSenderOutput implements MergedPcgPartyOutput {
    /**
     * the Zp64 instance
     */
    private final Zp64 zp64;
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
        Zp64VoleSenderOutput senderOutput = new Zp64VoleSenderOutput(zp64);
        MathPreconditions.checkEqual("x.length", "t.length", x.length, t.length);
        senderOutput.x = Arrays.stream(x)
            .peek(xi -> Preconditions.checkArgument(zp64.validateElement(xi)))
            .toArray();
        senderOutput.t = Arrays.stream(t)
            .peek(ti -> Preconditions.checkArgument(zp64.validateElement(ti)))
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
        Zp64VoleSenderOutput senderOutput = new Zp64VoleSenderOutput(zp64);
        senderOutput.x = new long[0];
        senderOutput.t = new long[0];
        return senderOutput;
    }

    /**
     * Creates a random sender output.
     *
     * @param receiverOutput receiver output.
     * @param secureRandom   random state.
     * @return a random sender output.
     */
    public static Zp64VoleSenderOutput createRandom(Zp64VoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        Zp64 zp64 = receiverOutput.getZp64();
        Zp64VoleSenderOutput senderOutput = new Zp64VoleSenderOutput(zp64);
        senderOutput.x = IntStream.range(0, num)
            .mapToLong(i -> zp64.createRandom(secureRandom))
            .toArray();
        long delta = receiverOutput.getDelta();
        senderOutput.t = IntStream.range(0, num)
            .mapToLong(i -> {
                long ti = zp64.mul(senderOutput.x[i], delta);
                ti = zp64.add(ti, receiverOutput.getQ(i));
                return ti;
            })
            .toArray();
        return senderOutput;
    }

    /**
     * private constructor.
     *
     * @param zp64 Zp64.
     */
    private Zp64VoleSenderOutput(Zp64 zp64) {
        this.zp64 = zp64;
    }

    @Override
    public int getNum() {
        return x.length;
    }

    @Override
    public Zp64VoleSenderOutput copy() {
        Zp64VoleSenderOutput copy = new Zp64VoleSenderOutput(zp64);
        copy.x = LongUtils.clone(x);
        copy.t = LongUtils.clone(t);
        return copy;
    }

    @Override
    public Zp64VoleSenderOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, num);
        // split x
        long[] subX = new long[splitNum];
        long[] remainX = new long[num - splitNum];
        System.arraycopy(x, num - splitNum, subX, 0, splitNum);
        System.arraycopy(x, 0, remainX, 0, num - splitNum);
        x = remainX;
        // split t
        long[] subT = new long[splitNum];
        long[] remainT = new long[num - splitNum];
        System.arraycopy(t, num - splitNum, subT, 0, splitNum);
        System.arraycopy(t, 0, remainT, 0, num - splitNum);
        t = remainT;

        return Zp64VoleSenderOutput.create(zp64, subX, subT);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduce_num", reduceNum, num);
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
        Preconditions.checkArgument(this.zp64.equals(that.zp64));
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(zp64)
            .append(x)
            .append(t)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Zp64VoleSenderOutput that) {
            return new EqualsBuilder()
                .append(this.zp64, that.zp64)
                .append(this.x, that.x)
                .append(this.t, that.t)
                .isEquals();
        }
        return false;
    }
}
