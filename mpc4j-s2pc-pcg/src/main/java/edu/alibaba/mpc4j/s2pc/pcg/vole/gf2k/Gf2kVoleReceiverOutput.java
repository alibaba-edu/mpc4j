package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * GF2K-VOLE receiver output. The receiver gets (Δ, q) with t = q + Δ · x, where x and t is owned by the sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Gf2kVoleReceiverOutput implements MergedPcgPartyOutput, Gf2kVolePartyOutput {
    /**
     * field
     */
    private final Sgf2k field;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * q array
     */
    private byte[][] q;

    /**
     * Creates a receiver output.
     *
     * @param field field.
     * @param delta Δ.
     * @param qs    q_i.
     * @return the receiver output.
     */
    public static Gf2kVoleReceiverOutput create(Sgf2k field, byte[] delta, byte[][] qs) {
        Gf2kVoleReceiverOutput receiverOutput = new Gf2kVoleReceiverOutput(field, delta);
        receiverOutput.q = Arrays.stream(qs)
            .peek(qi -> Preconditions.checkArgument(field.validateElement(qi)))
            .toArray(byte[][]::new);

        return receiverOutput;
    }

    /**
     * Creates an empty receiver output.
     *
     * @param field field.
     * @param delta Δ.
     * @return an empty receiver output.
     */
    public static Gf2kVoleReceiverOutput createEmpty(Sgf2k field, byte[] delta) {
        Gf2kVoleReceiverOutput receiverOutput = new Gf2kVoleReceiverOutput(field, delta);
        receiverOutput.q = new byte[0][];

        return receiverOutput;
    }

    /**
     * Creates a random receiver output.
     *
     * @param field        field.
     * @param num          num.
     * @param secureRandom random state.
     * @return a random receiver output.
     */
    public static Gf2kVoleReceiverOutput createRandom(Sgf2k field, int num, byte[] delta, SecureRandom secureRandom) {
        Gf2kVoleReceiverOutput receiverOutput = new Gf2kVoleReceiverOutput(field, delta);
        receiverOutput.q = IntStream.range(0, num)
            .mapToObj(index -> field.createRandom(secureRandom))
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     *
     * @param field field.
     * @param delta Δ.
     */
    private Gf2kVoleReceiverOutput(Sgf2k field, byte[] delta) {
        Preconditions.checkArgument(field.validateElement(delta));
        this.delta = BytesUtils.clone(delta);
        this.field = field;
    }

    @Override
    public int getNum() {
        return q.length;
    }

    @Override
    public Gf2kVoleReceiverOutput copy() {
        Gf2kVoleReceiverOutput copy = new Gf2kVoleReceiverOutput(field, delta);
        copy.q = BytesUtils.clone(q);
        return copy;
    }

    @Override
    public Gf2kVoleReceiverOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, num);
        // split q
        byte[][] subQ = new byte[splitNum][];
        byte[][] remainQ = new byte[num - splitNum][];
        System.arraycopy(q, num - splitNum, subQ, 0, splitNum);
        System.arraycopy(q, 0, remainQ, 0, num - splitNum);
        q = remainQ;

        return create(field, delta, subQ);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduce_num", reduceNum, num);
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            byte[][] remainQ = new byte[reduceNum][];
            System.arraycopy(q, 0, remainQ, 0, reduceNum);
            q = remainQ;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Gf2kVoleReceiverOutput that = (Gf2kVoleReceiverOutput) other;
        Preconditions.checkArgument(this.field.equals(that.field));
        Preconditions.checkArgument(Arrays.equals(this.delta, that.delta));
        // merge q
        byte[][] mergeQ = new byte[this.q.length + that.q.length][];
        System.arraycopy(this.q, 0, mergeQ, 0, this.q.length);
        System.arraycopy(that.q, 0, mergeQ, this.q.length, that.q.length);
        q = mergeQ;
    }

    @Override
    public Sgf2k getField() {
        return field;
    }

    /**
     * Gets Δ.
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
    }

    /**
     * Gets q_i.
     *
     * @param index the index.
     * @return q_i.
     */
    public byte[] getQ(int index) {
        return q[index];
    }

    /**
     * Gets q.
     *
     * @return q.
     */
    public byte[][] getQ() {
        return q;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(field)
            .append(delta)
            .append(q)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Gf2kVoleReceiverOutput that) {
            return new EqualsBuilder()
                .append(this.field, that.field)
                .append(this.delta, that.delta)
                .append(this.q, that.q)
                .isEquals();
        }
        return false;
    }
}
