package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 1-out-of-n OT receiver output. The receiver gets (i, r_i).
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class LnotReceiverOutput implements MergedPcgPartyOutput {
    /**
     * choice bit length
     */
    private final int l;
    /**
     * maximal choice
     */
    private final int n;
    /**
     * choices
     */
    private int[] choiceArray;
    /**
     * rb array
     */
    private byte[][] rbArray;

    /**
     * Creates a receiver output.
     *
     * @param l           choice bit length.
     * @param choiceArray choice array.
     * @param rbArray     rb array.
     * @return a receiver output.
     */
    public static LnotReceiverOutput create(int l, int[] choiceArray, byte[][] rbArray) {
        LnotReceiverOutput receiverOutput = new LnotReceiverOutput(l);
        MathPreconditions.checkEqual("choiceArray.length", "rbArray.length", choiceArray.length, rbArray.length);
        receiverOutput.choiceArray = Arrays.stream(choiceArray)
            .peek(choice -> MathPreconditions.checkNonNegativeInRange("choice", choice, receiverOutput.n))
            .toArray();
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> MathPreconditions.checkEqual("rb.length", "λ", rb.length, CommonConstants.BLOCK_BYTE_LENGTH))
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * Creates an empty receiver output.
     *
     * @param l choice bit length.
     * @return an empty receiver output.
     */
    public static LnotReceiverOutput createEmpty(int l) {
        LnotReceiverOutput receiverOutput = new LnotReceiverOutput(l);
        receiverOutput.choiceArray = new int[0];
        receiverOutput.rbArray = new byte[0][];
        return receiverOutput;
    }

    /**
     * Creates a random receiver output.
     *
     * @param senderOutput sender output.
     * @param secureRandom random state.
     * @return a random receiver output.
     */
    public static LnotReceiverOutput createRandom(LnotSenderOutput senderOutput, SecureRandom secureRandom) {
        int num = senderOutput.getNum();
        LnotReceiverOutput receiverOutput = new LnotReceiverOutput(senderOutput.getL());
        receiverOutput.choiceArray = IntStream.range(0, num)
            .map(i -> IntUtils.randomNonNegative(receiverOutput.n, secureRandom))
            .toArray();
        receiverOutput.rbArray = IntStream.range(0, num)
            .mapToObj(i -> BytesUtils.clone(senderOutput.getRb(i, receiverOutput.choiceArray[i])))
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private LnotReceiverOutput(int l) {
        MathPreconditions.checkPositiveInRangeClosed("l", l, IntUtils.MAX_L);
        this.l = l;
        this.n = (1 << l);
    }

    @Override
    public LnotReceiverOutput copy() {
        LnotReceiverOutput copy = new LnotReceiverOutput(l);
        copy.choiceArray = IntUtils.clone(choiceArray);
        copy.rbArray = BytesUtils.clone(rbArray);
        return copy;
    }

    @Override
    public LnotReceiverOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        // split choice array
        int[] subChoiceArray = new int[splitNum];
        int[] remainChoiceArray = new int[num - splitNum];
        System.arraycopy(choiceArray, num - splitNum, subChoiceArray, 0, splitNum);
        System.arraycopy(choiceArray, 0, remainChoiceArray, 0, num - splitNum);
        choiceArray = remainChoiceArray;
        // split rb
        byte[][] subRbArray = new byte[splitNum][];
        byte[][] remainRbArray = new byte[num - splitNum][];
        System.arraycopy(rbArray, num - splitNum, subRbArray, 0, splitNum);
        System.arraycopy(rbArray, 0, remainRbArray, 0, num - splitNum);
        rbArray = remainRbArray;

        return LnotReceiverOutput.create(l, subChoiceArray, subRbArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // we need to reduce only if reduceNum is less than the current num.
            int[] remainChoiceArray = new int[reduceNum];
            System.arraycopy(choiceArray, 0, remainChoiceArray, 0, reduceNum);
            choiceArray = remainChoiceArray;
            byte[][] remainRbArray = new byte[reduceNum][];
            System.arraycopy(rbArray, 0, remainRbArray, 0, reduceNum);
            rbArray = remainRbArray;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        LnotReceiverOutput that = (LnotReceiverOutput) other;
        MathPreconditions.checkEqual("this.l", "that.l", this.l, that.l);
        // copy choice array
        int[] mergeChoiceArray = new int[this.choiceArray.length + that.choiceArray.length];
        System.arraycopy(this.choiceArray, 0, mergeChoiceArray, 0, this.choiceArray.length);
        System.arraycopy(that.choiceArray, 0, mergeChoiceArray, this.choiceArray.length, that.choiceArray.length);
        choiceArray = mergeChoiceArray;
        // copy rb
        byte[][] mergeRbArray = new byte[this.rbArray.length + that.rbArray.length][];
        System.arraycopy(this.rbArray, 0, mergeRbArray, 0, this.rbArray.length);
        System.arraycopy(that.rbArray, 0, mergeRbArray, this.rbArray.length, that.rbArray.length);
        rbArray = mergeRbArray;
    }

    @Override
    public int getNum() {
        return rbArray.length;
    }

    /**
     * Gets the choice.
     *
     * @param index the index.
     * @return the choice.
     */
    public int getChoice(int index) {
        return choiceArray[index];
    }

    /**
     * Gets Rb.
     *
     * @param index the index.
     * @return Rb.
     */
    public byte[] getRb(int index) {
        return rbArray[index];
    }

    /**
     * Gets the choice bit length.
     *
     * @return the choice bit length.
     */
    public int getL() {
        return l;
    }

    /**
     * Gets the maximal choice.
     *
     * @return maximal choice.
     */
    public int getN() {
        return n;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(l)
            .append(choiceArray)
            .append(rbArray)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LnotReceiverOutput that) {
            return new EqualsBuilder()
                .append(this.l, that.l)
                .append(this.choiceArray, that.choiceArray)
                .append(this.rbArray, that.rbArray)
                .isEquals();
        }
        return false;
    }
}
