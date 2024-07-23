package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 1-out-of-2^l COT receiver output.
 * The sender holds Δ and (q_1,...,q_k). The receiver holds (m_1,...,m_k) and (t_0,...,t_k), with the correlation:
 * <p>
 * q_i ⊕ (C(m_i) ⊙ Δ) = t_i, where C is a linear coder.
 * </p>
 * The correlation satisfies homomorphism, namely, given q_i ⊕ (C(m_i) ⊙ Δ) = t_i, q_j ⊕ (C(m_j) ⊙ Δ) = t_j, we have:
 * <p>
 * t_{i ⊕ j} = q_{i ⊕ j} ⊕ (C(m_{i ⊕ j}) ⊙ Δ) = q_i ⊕ (C(m_i) ⊙ Δ)) ⊕ (q_j ⊕ (C(m_j) ⊙ Δ))
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class LcotReceiverOutput implements MergedPcgPartyOutput {
    /**
     * input bit length
     */
    private final int inputBitLength;
    /**
     * input byte length
     */
    private final int inputByteLength;
    /**
     * output bit length
     */
    private final int outputBitLength;
    /**
     * output byte length
     */
    private final int outputByteLength;
    /**
     * choices
     */
    private byte[][] choices;
    /**
     * Rb array
     */
    private byte[][] rbArray;

    /**
     * Creates a receiver output.
     *
     * @param inputBitLength input bit length.
     * @param choices        choices.
     * @param rbArray        Rb array.
     * @return a receiver output.
     */
    public static LcotReceiverOutput create(int inputBitLength, byte[][] choices, byte[][] rbArray) {
        LcotReceiverOutput receiverOutput = new LcotReceiverOutput(inputBitLength);
        MathPreconditions.checkEqual("choices.length", "rbArray.length", choices.length, rbArray.length);
        receiverOutput.choices = Arrays.stream(choices)
            .peek(choice -> Preconditions.checkArgument(
                BytesUtils.isFixedReduceByteArray(choice, receiverOutput.inputByteLength, inputBitLength)
            ))
            .toArray(byte[][]::new);
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> Preconditions.checkArgument(
                BytesUtils.isFixedReduceByteArray(rb, receiverOutput.outputByteLength, receiverOutput.outputBitLength)
            ))
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * Creates an empty receiver output.
     *
     * @param inputBitLength input bit length.
     * @return an empty receiver output.
     */
    public static LcotReceiverOutput createEmpty(int inputBitLength) {
        LcotReceiverOutput receiverOutput = new LcotReceiverOutput(inputBitLength);
        receiverOutput.choices = new byte[0][];
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
    public static LcotReceiverOutput createRandom(LcotSenderOutput senderOutput, SecureRandom secureRandom) {
        int num = senderOutput.getNum();
        LcotReceiverOutput receiverOutput = new LcotReceiverOutput(senderOutput.getInputBitLength());
        receiverOutput.choices = BytesUtils.randomByteArrayVector(
            num, receiverOutput.inputByteLength, receiverOutput.inputBitLength, secureRandom
        );
        receiverOutput.rbArray = IntStream.range(0, num)
            .mapToObj(i -> BytesUtils.clone(senderOutput.getRb(i, receiverOutput.choices[i])))
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * private constructor.
     *
     * @param inputBitLength input bit length.
     */
    private LcotReceiverOutput(int inputBitLength) {
        MathPreconditions.checkPositive("input_bit_length", inputBitLength);
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        LinearCoder linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
    }

    @Override
    public LcotReceiverOutput copy() {
        LcotReceiverOutput copy = new LcotReceiverOutput(inputBitLength);
        copy.choices = BytesUtils.clone(choices);
        copy.rbArray = BytesUtils.clone(rbArray);
        return copy;
    }

    @Override
    public LcotReceiverOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        // 拆分选择比特
        byte[][] splitChoices = new byte[splitNum][];
        byte[][] remainChoices = new byte[num - splitNum][];
        System.arraycopy(choices, num - splitNum, splitChoices, 0, splitNum);
        System.arraycopy(choices, 0, remainChoices, 0, num - splitNum);
        choices = remainChoices;
        // 拆分选择密钥
        byte[][] rbSplitArray = new byte[splitNum][];
        byte[][] rbRemainArray = new byte[num - splitNum][];
        System.arraycopy(rbArray, num - splitNum, rbSplitArray, 0, splitNum);
        System.arraycopy(rbArray, 0, rbRemainArray, 0, num - splitNum);
        rbArray = rbRemainArray;

        return LcotReceiverOutput.create(inputBitLength, splitChoices, rbSplitArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
            byte[][] remainChoices = new byte[reduceNum][];
            System.arraycopy(choices, 0, remainChoices, 0, reduceNum);
            choices = remainChoices;
            byte[][] rbRemainArray = new byte[reduceNum][];
            System.arraycopy(rbArray, 0, rbRemainArray, 0, reduceNum);
            rbArray = rbRemainArray;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        LcotReceiverOutput that = (LcotReceiverOutput) other;
        MathPreconditions.checkEqual("this.inputBitLength", "that.inputBitLength", this.inputBitLength, that.inputBitLength);
        // 拷贝选择比特数组
        byte[][] mergeChoices = new byte[this.choices.length + that.choices.length][];
        System.arraycopy(this.choices, 0, mergeChoices, 0, this.choices.length);
        System.arraycopy(that.choices, 0, mergeChoices, this.choices.length, that.choices.length);
        choices = mergeChoices;
        // 拷贝Rb数组
        byte[][] mergeRbArray = new byte[this.rbArray.length + that.rbArray.length][];
        System.arraycopy(this.rbArray, 0, mergeRbArray, 0, this.rbArray.length);
        System.arraycopy(that.rbArray, 0, mergeRbArray, this.rbArray.length, that.rbArray.length);
        rbArray = mergeRbArray;
    }

    /**
     * 返回选择值。
     *
     * @param index 索引值。
     * @return 选择值。
     */
    public byte[] getChoice(int index) {
        return choices[index];
    }

    /**
     * 返回选择值数组。
     *
     * @return 选择值数组。
     */
    public byte[][] getChoices() {
        return choices;
    }

    /**
     * 返回Rb。
     *
     * @param index 索引值。
     * @return Rb。
     */
    public byte[] getRb(int index) {
        return rbArray[index];
    }

    /**
     * 返回Rb数组。
     *
     * @return Rb数组。
     */
    public byte[][] getRbArray() {
        return rbArray;
    }

    /**
     * 返回输入比特长度。
     *
     * @return 输入比特长度。
     */
    public int getInputBitLength() {
        return inputBitLength;
    }

    /**
     * 返回输入字节长度。
     *
     * @return 输入字节长度。
     */
    public int getInputByteLength() {
        return inputByteLength;
    }

    /**
     * 返回输出随机量字节长度。
     *
     * @return 输出随机量字节长度。
     */
    public int getOutputByteLength() {
        return outputByteLength;
    }

    /**
     * 返回输出随机量比特长度。
     *
     * @return 输出随机量比特长度。
     */
    public int getOutputBitLength() {
        return outputBitLength;
    }

    @Override
    public int getNum() {
        return rbArray.length;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(inputBitLength)
            .append(choices)
            .append(rbArray)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LcotReceiverOutput that) {
            return new EqualsBuilder()
                .append(this.inputBitLength, that.inputBitLength)
                .append(this.choices, that.choices)
                .append(this.rbArray, that.rbArray)
                .isEquals();
        }
        return false;
    }
}
