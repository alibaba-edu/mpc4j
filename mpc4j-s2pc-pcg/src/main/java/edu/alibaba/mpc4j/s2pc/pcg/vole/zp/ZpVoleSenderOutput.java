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
 * Zp-VOLE sender output. The sender gets (x, t) with t = q + Δ·x, where Δ and q are owned by the receiver.
 *
 * @author Hanwen Feng
 * @date 2022/06/07
 */
public class ZpVoleSenderOutput implements MergedPcgPartyOutput {
    /**
     * the Zp instance
     */
    private final Zp zp;
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
        ZpVoleSenderOutput senderOutput = new ZpVoleSenderOutput(zp);
        MathPreconditions.checkEqual("x.length", "t.length", x.length, t.length);
        senderOutput.x = Arrays.stream(x)
            .peek(xi -> Preconditions.checkArgument(zp.validateElement(xi)))
            .toArray(BigInteger[]::new);
        senderOutput.t = Arrays.stream(t)
            .peek(ti -> Preconditions.checkArgument(zp.validateElement(ti)))
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
        ZpVoleSenderOutput senderOutput = new ZpVoleSenderOutput(zp);
        senderOutput.x = new BigInteger[0];
        senderOutput.t = new BigInteger[0];
        return senderOutput;
    }

    /**
     * Creates a random sender output.
     *
     * @param receiverOutput receiver output.
     * @param secureRandom   random state.
     * @return a random sender output.
     */
    public static ZpVoleSenderOutput createRandom(ZpVoleReceiverOutput receiverOutput, SecureRandom secureRandom) {
        int num = receiverOutput.getNum();
        Zp zp = receiverOutput.getZp();
        ZpVoleSenderOutput senderOutput = new ZpVoleSenderOutput(zp);
        senderOutput.x = IntStream.range(0, num)
            .mapToObj(i -> zp.createRandom(secureRandom))
            .toArray(BigInteger[]::new);
        BigInteger delta = receiverOutput.getDelta();
        senderOutput.t = IntStream.range(0, num)
            .mapToObj(i -> {
                BigInteger ti = zp.mul(senderOutput.x[i], delta);
                ti = zp.add(ti, receiverOutput.getQ(i));
                return ti;
            })
            .toArray(BigInteger[]::new);
        return senderOutput;
    }

    /**
     * private constructor.
     *
     * @param zp Zp.
     */
    private ZpVoleSenderOutput(Zp zp) {
        this.zp = zp;
    }

    @Override
    public int getNum() {
        return x.length;
    }

    @Override
    public ZpVoleSenderOutput copy() {
        ZpVoleSenderOutput copy = new ZpVoleSenderOutput(zp);
        copy.x = BigIntegerUtils.clone(x);
        copy.t = BigIntegerUtils.clone(t);
        return copy;
    }

    @Override
    public ZpVoleSenderOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, num);
        // split x
        BigInteger[] subX = new BigInteger[splitNum];
        BigInteger[] remainX = new BigInteger[num - splitNum];
        System.arraycopy(x, num - splitNum, subX, 0, splitNum);
        System.arraycopy(x, 0, remainX, 0, num - splitNum);
        x = remainX;
        // split t
        BigInteger[] subT = new BigInteger[splitNum];
        BigInteger[] remainT = new BigInteger[num - splitNum];
        System.arraycopy(t, num - splitNum, subT, 0, splitNum);
        System.arraycopy(t, 0, remainT, 0, num - splitNum);
        t = remainT;

        return ZpVoleSenderOutput.create(zp, subX, subT);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduce_num", reduceNum, num);
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
        Preconditions.checkArgument(this.zp.equals(that.zp));
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(zp)
            .append(x)
            .append(t)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZpVoleSenderOutput that) {
            return new EqualsBuilder()
                .append(this.zp, that.zp)
                .append(this.x, that.x)
                .append(this.t, that.t)
                .isEquals();
        }
        return false;
    }
}
