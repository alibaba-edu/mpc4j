package edu.alibaba.mpc4j.s2pc.pcg.vole.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

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
    private final Zp zp;
    /**
     * Δ
     */
    private final BigInteger delta;
    /**
     * q_i
     */
    private BigInteger[] q;

    /**
     * Creates an output.
     *
     * @param zp    the Zp instance.
     * @param delta Δ.
     * @param q     q.
     * @return an output.
     */
    public static ZpVoleReceiverOutput create(Zp zp, BigInteger delta, BigInteger[] q) {
        ZpVoleReceiverOutput receiverOutput = new ZpVoleReceiverOutput(zp, delta);
        receiverOutput.q = Arrays.stream(q)
            .peek(qi -> Preconditions.checkArgument(zp.validateElement(qi)))
            .toArray(BigInteger[]::new);
        return receiverOutput;
    }

    /**
     * Creates an empty output.
     *
     * @param zp    the Zp instance.
     * @param delta Δ.
     * @return an empty output.
     */
    public static ZpVoleReceiverOutput createEmpty(Zp zp, BigInteger delta) {
        ZpVoleReceiverOutput receiverOutput = new ZpVoleReceiverOutput(zp, delta);
        receiverOutput.q = new BigInteger[0];

        return receiverOutput;
    }

    /**
     * Creates a random receiver output.
     *
     * @param zp           Zp.
     * @param num          num.
     * @param secureRandom random state.
     * @return a random receiver output.
     */
    public static ZpVoleReceiverOutput createRandom(Zp zp, int num, BigInteger delta, SecureRandom secureRandom) {
        ZpVoleReceiverOutput receiverOutput = new ZpVoleReceiverOutput(zp, delta);
        receiverOutput.q = IntStream.range(0, num)
            .mapToObj(index -> zp.createRandom(secureRandom))
            .toArray(BigInteger[]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     *
     * @param zp    Zp.
     * @param delta Δ.
     */
    private ZpVoleReceiverOutput(Zp zp, BigInteger delta) {
        this.zp = zp;
        Preconditions.checkArgument(zp.validateRangeElement(delta));
        this.delta = delta;
    }

    @Override
    public int getNum() {
        return q.length;
    }

    @Override
    public ZpVoleReceiverOutput copy() {
        ZpVoleReceiverOutput copy = new ZpVoleReceiverOutput(zp, delta);
        copy.q = BigIntegerUtils.clone(q);
        return copy;
    }

    @Override
    public ZpVoleReceiverOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, num);
        // split q
        BigInteger[] subQ = new BigInteger[splitNum];
        BigInteger[] remainQ = new BigInteger[num - splitNum];
        System.arraycopy(q, num - splitNum, subQ, 0, splitNum);
        System.arraycopy(q, 0, remainQ, 0, num - splitNum);
        q = remainQ;

        return ZpVoleReceiverOutput.create(zp, delta, subQ);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduce_num", reduceNum, num);
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
        Preconditions.checkArgument(this.zp.equals(that.zp));
        MathPreconditions.checkEqual("this.delta", "that.delta", this.delta, that.delta);
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(zp)
            .append(delta)
            .append(q)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZpVoleReceiverOutput that) {
            return new EqualsBuilder()
                .append(this.zp, that.zp)
                .append(this.delta, that.delta)
                .append(this.q, that.q)
                .isEquals();
        }
        return false;
    }
}
