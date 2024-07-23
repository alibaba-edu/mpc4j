package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtReceiverOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * COT receiver output.
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public class CotReceiverOutput implements OtReceiverOutput, MergedPcgPartyOutput {
    /**
     * choice bits.
     */
    private boolean[] choices;
    /**
     * Rb array.
     */
    private byte[][] rbArray;

    /**
     * Creates a receiver output.
     *
     * @param choices choice bits.
     * @param rbArray Rb array.
     * @return a receiver output.
     */
    public static CotReceiverOutput create(boolean[] choices, byte[][] rbArray) {
        CotReceiverOutput receiverOutput = new CotReceiverOutput();
        int num = choices.length;
        MathPreconditions.checkEqual("num", "RbArray.length", num, rbArray.length);
        receiverOutput.choices = BinaryUtils.clone(choices);
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb ->
                MathPreconditions.checkEqual("Rb.length", "λ in bytes", rb.length, CommonConstants.BLOCK_BYTE_LENGTH)
            )
            .toArray(byte[][]::new);

        return receiverOutput;
    }

    /**
     * Creates an empty receiver output.
     *
     * @return an empty receiver output.
     */
    public static CotReceiverOutput createEmpty() {
        CotReceiverOutput receiverOutput = new CotReceiverOutput();
        receiverOutput.choices = new boolean[0];
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
    public static CotReceiverOutput createRandom(CotSenderOutput senderOutput, SecureRandom secureRandom) {
        int num = senderOutput.getNum();
        CotReceiverOutput receiverOutput = new CotReceiverOutput();
        receiverOutput.choices = BinaryUtils.randomBinary(num, secureRandom);
        receiverOutput.rbArray = IntStream.range(0, num)
            .mapToObj(index -> {
                if (receiverOutput.choices[index]) {
                    return BytesUtils.clone(senderOutput.getR1(index));
                } else {
                    return BytesUtils.clone(senderOutput.getR0(index));
                }
            })
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private CotReceiverOutput() {
        // empty
    }

    @Override
    public CotReceiverOutput copy() {
        CotReceiverOutput copy = new CotReceiverOutput();
        copy.choices = BinaryUtils.clone(choices);
        copy.rbArray = BytesUtils.clone(rbArray);
        return copy;
    }

    @Override
    public CotReceiverOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        // split choices
        boolean[] subChoices = new boolean[splitNum];
        boolean[] remainChoices = new boolean[num - splitNum];
        System.arraycopy(choices, num - splitNum, subChoices, 0, splitNum);
        System.arraycopy(choices, 0, remainChoices, 0, num - splitNum);
        choices = remainChoices;
        // split Rb array
        byte[][] rbSubArray = new byte[splitNum][];
        byte[][] rbRemainArray = new byte[num - splitNum][];
        System.arraycopy(rbArray, num - splitNum, rbSubArray, 0, splitNum);
        System.arraycopy(rbArray, 0, rbRemainArray, 0, num - splitNum);
        rbArray = rbRemainArray;

        return CotReceiverOutput.create(subChoices, rbSubArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // reduce only when reduceNum < num
            boolean[] remainChoices = new boolean[reduceNum];
            System.arraycopy(choices, 0, remainChoices, 0, reduceNum);
            choices = remainChoices;
            byte[][] rbRemainArray = new byte[reduceNum][];
            System.arraycopy(rbArray, 0, rbRemainArray, 0, reduceNum);
            rbArray = rbRemainArray;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        CotReceiverOutput that = (CotReceiverOutput) other;
        // merge choices
        boolean[] mergeChoices = new boolean[this.choices.length + that.choices.length];
        System.arraycopy(this.choices, 0, mergeChoices, 0, this.choices.length);
        System.arraycopy(that.choices, 0, mergeChoices, this.choices.length, that.choices.length);
        choices = mergeChoices;
        // merge Rb array
        byte[][] mergeRbArray = new byte[this.rbArray.length + that.rbArray.length][];
        System.arraycopy(this.rbArray, 0, mergeRbArray, 0, this.rbArray.length);
        System.arraycopy(that.rbArray, 0, mergeRbArray, this.rbArray.length, that.rbArray.length);
        rbArray = mergeRbArray;
    }

    @Override
    public boolean getChoice(int index) {
        return choices[index];
    }

    @Override
    public boolean[] getChoices() {
        return choices;
    }

    @Override
    public byte[] getRb(int index) {
        return rbArray[index];
    }

    @Override
    public byte[][] getRbArray() {
        return rbArray;
    }

    @Override
    public int getNum() {
        return rbArray.length;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(choices)
            .append(rbArray)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof CotReceiverOutput that) {
            return new EqualsBuilder()
                .append(this.choices, that.choices)
                .append(this.rbArray, that.rbArray)
                .isEquals();
        }
        return false;
    }
}
