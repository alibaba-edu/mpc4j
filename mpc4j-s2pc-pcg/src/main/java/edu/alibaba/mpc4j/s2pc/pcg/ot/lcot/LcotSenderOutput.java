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

/**
 * 1-out-of-2^l COT sender output.
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
public class LcotSenderOutput implements MergedPcgPartyOutput {
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
     * linear coder
     */
    private final LinearCoder linearCoder;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * Q
     */
    private byte[][] qsArray;

    /**
     * Creates a sender output.
     *
     * @param inputBitLength input bit length.
     * @param delta          Δ.
     * @param qsArray        Q.
     * @return sender output.
     */
    public static LcotSenderOutput create(int inputBitLength, byte[] delta, byte[][] qsArray) {
        LcotSenderOutput senderOutput = new LcotSenderOutput(inputBitLength, delta);
        senderOutput.qsArray = Arrays.stream(qsArray)
            .peek(q -> Preconditions.checkArgument(
                BytesUtils.isFixedReduceByteArray(q, senderOutput.outputByteLength, senderOutput.outputBitLength)
            ))
            .toArray(byte[][]::new);

        return senderOutput;
    }

    /**
     * Creates an empty sender output.
     *
     * @param inputBitLength input bit length.
     * @param delta          Δ.
     * @return an empty sender output.
     */
    public static LcotSenderOutput createEmpty(int inputBitLength, byte[] delta) {
        LcotSenderOutput senderOutput = new LcotSenderOutput(inputBitLength, delta);
        senderOutput.qsArray = new byte[0][];

        return senderOutput;
    }

    /**
     * creates a random sender output.
     *
     * @param num            num.
     * @param inputBitLength input bit length.
     * @param delta          Δ.
     * @param secureRandom   random state.
     * @return a random sender output.
     */
    public static LcotSenderOutput createRandom(int num, int inputBitLength, byte[] delta, SecureRandom secureRandom) {
        LcotSenderOutput senderOutput = new LcotSenderOutput(inputBitLength, delta);
        senderOutput.qsArray = BytesUtils.randomByteArrayVector(
            num, senderOutput.outputByteLength, senderOutput.outputBitLength, secureRandom
        );
        return senderOutput;

    }

    /**
     * private constructor.
     *
     * @param inputBitLength input bit length.
     */
    private LcotSenderOutput(int inputBitLength, byte[] delta) {
        MathPreconditions.checkPositive("input_bit_length", inputBitLength);
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(delta, outputByteLength, outputBitLength));
        this.delta = BytesUtils.clone(delta);
    }

    @Override
    public LcotSenderOutput copy() {
        LcotSenderOutput copy = new LcotSenderOutput(inputBitLength, delta);
        copy.qsArray = BytesUtils.clone(qsArray);
        return copy;
    }

    @Override
    public LcotSenderOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        byte[][] qsSplitArray = new byte[splitNum][];
        byte[][] qsRemainArray = new byte[num - splitNum][];
        System.arraycopy(qsArray, num - splitNum, qsSplitArray, 0, splitNum);
        System.arraycopy(qsArray, 0, qsRemainArray, 0, num - splitNum);
        qsArray = qsRemainArray;

        return create(inputBitLength, delta, qsSplitArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
            byte[][] qsRemainArray = new byte[reduceNum][];
            System.arraycopy(qsArray, 0, qsRemainArray, 0, reduceNum);
            qsArray = qsRemainArray;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        LcotSenderOutput that = (LcotSenderOutput) other;
        MathPreconditions.checkEqual("this.inputBitLength", "that.inputBitLength", this.inputBitLength, that.inputBitLength);
        Preconditions.checkArgument(Arrays.equals(this.delta, that.delta));
        byte[][] mergeQsArray = new byte[this.qsArray.length + that.qsArray.length][];
        System.arraycopy(this.qsArray, 0, mergeQsArray, 0, this.qsArray.length);
        System.arraycopy(that.qsArray, 0, mergeQsArray, this.qsArray.length, that.qsArray.length);
        qsArray = mergeQsArray;
    }

    /**
     * Gets Rb.
     *
     * @param index  the index.
     * @param choice the choice.
     * @return Rb.
     */
    public byte[] getRb(int index, byte[] choice) {
        assert choice.length == inputByteLength && BytesUtils.isReduceByteArray(choice, inputBitLength);
        // r_i = q_i ⊕ (C(m_i) ⊙ Δ)
        byte[] output = linearCoder.encode(BytesUtils.paddingByteArray(choice, linearCoder.getDatawordByteLength()));
        BytesUtils.andi(output, delta);
        BytesUtils.xori(output, qsArray[index]);
        return output;
    }

    /**
     * Gets input bit length.
     *
     * @return input bit length.
     */
    public int getInputBitLength() {
        return inputBitLength;
    }

    /**
     * Gets input byte length.
     *
     * @return input byte length.
     */
    public int getInputByteLength() {
        return inputByteLength;
    }

    /**
     * Gets output bit length.
     *
     * @return output bit length.
     */
    public int getOutputBitLength() {
        return outputBitLength;
    }

    /**
     * Gets output byte length.
     *
     * @return output byte length.
     */
    public int getOutputByteLength() {
        return outputByteLength;
    }

    @Override
    public int getNum() {
        return qsArray.length;
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
     * Gets q.
     *
     * @param index the index.
     * @return q.
     */
    public byte[] getQ(int index) {
        return qsArray[index];
    }

    /**
     * Gets qs.
     *
     * @return qs.
     */
    public byte[][] getQsArray() {
        return qsArray;
    }

    /**
     * Gets the linear coder.
     *
     * @return the linear coder.
     */
    public LinearCoder getLinearCoder() {
        return linearCoder;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(inputBitLength)
            .append(delta)
            .append(qsArray)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LcotSenderOutput that) {
            return new EqualsBuilder()
                .append(this.inputBitLength, that.inputBitLength)
                .append(this.delta, that.delta)
                .append(this.qsArray, that.qsArray)
                .isEquals();
        }
        return false;
    }
}
