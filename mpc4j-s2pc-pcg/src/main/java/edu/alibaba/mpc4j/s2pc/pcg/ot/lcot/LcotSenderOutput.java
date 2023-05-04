package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.Arrays;

/**
 * 2^l选1-COT协议发送方输出。服务端输入Δ，得到q_1,...,q_k，客户端输入选择元组(m_1,...,m_k)，得到t_0,...,t_k。各个参数满足：
 * <p>
 * q_i ⊕ (C(m_i) ⊙ Δ) = t_i，其中C是一个编码函数。
 * </p>
 * 发送方输出结果满足同态性，即给定q_i ⊕ (C(m_i) ⊙ Δ) = t_i, q_j ⊕ (C(m_j) ⊙ Δ) = t_j，有
 * <p>
 * t_{i ⊕ j} = q_{i ⊕ j} ⊕ (C(m_{i ⊕ j}) ⊙ Δ) = q_i ⊕ (C(m_i) ⊙ Δ)) ⊕ (q_j ⊕ (C(m_j) ⊙ Δ))
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class LcotSenderOutput implements MergedPcgPartyOutput {
    /**
     * 输入比特长度
     */
    private final int inputBitLength;
    /**
     * 输入字节长度
     */
    private final int inputByteLength;
    /**
     * 输出比特长度
     */
    private final int outputBitLength;
    /**
     * 输出字节长度
     */
    private final int outputByteLength;
    /**
     * 线性编码器
     */
    private final LinearCoder linearCoder;
    /**
     * 关联值Δ
     */
    private byte[] delta;
    /**
     * 矩阵Q
     */
    private byte[][] qsArray;

    /**
     * 创建发送方输出。
     *
     * @param inputBitLength 输入比特长度。
     * @param delta          关联值Δ。
     * @param qsArray        矩阵Q。
     * @return 发送方输出。
     */
    public static LcotSenderOutput create(int inputBitLength, byte[] delta, byte[][] qsArray) {
        LcotSenderOutput senderOutput = new LcotSenderOutput(inputBitLength);
        assert delta.length == senderOutput.outputByteLength
            && BytesUtils.isReduceByteArray(delta, senderOutput.outputBitLength);
        senderOutput.delta = BytesUtils.clone(delta);
        assert qsArray.length > 0 : "QS Length must be greater than 0";
        senderOutput.qsArray = Arrays.stream(qsArray)
            .peek(q -> {
                assert q.length == senderOutput.outputByteLength
                    && BytesUtils.isReduceByteArray(q, senderOutput.outputBitLength);
            })
            .toArray(byte[][]::new);

        return senderOutput;
    }

    /**
     * 创建数量为0的发送方输出。
     *
     * @param inputBitLength 输入比特长度。
     * @param delta          关联值Δ。
     * @return 数量为0的发送方输出。
     */
    public static LcotSenderOutput createEmpty(int inputBitLength, byte[] delta) {
        LcotSenderOutput senderOutput = new LcotSenderOutput(inputBitLength);
        assert delta.length == senderOutput.outputByteLength
            && BytesUtils.isReduceByteArray(delta, senderOutput.outputBitLength);
        senderOutput.delta = BytesUtils.clone(delta);
        senderOutput.qsArray = new byte[0][senderOutput.outputByteLength];

        return senderOutput;
    }

    /**
     * private constructor.
     */
    private LcotSenderOutput(int inputBitLength) {
        assert inputBitLength > 0 : "InputBitLength must be greater than 0: " + inputBitLength;
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
    }

    @Override
    public LcotSenderOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "split length must be in range (0, " + num + "]: " + splitNum;
        byte[][] qsSubArray = new byte[splitNum][];
        byte[][] qsRemainArray = new byte[num - splitNum][];
        System.arraycopy(qsArray, 0, qsSubArray, 0, splitNum);
        System.arraycopy(qsArray, splitNum, qsRemainArray, 0, num - splitNum);
        qsArray = qsRemainArray;

        return LcotSenderOutput.create(inputBitLength, delta, qsSubArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
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
        assert this.inputBitLength == that.inputBitLength : "InputBitLength mismatch";
        assert Arrays.equals(this.delta, that.delta) : "Δ mismatch";
        byte[][] mergeQsArray = new byte[this.qsArray.length + that.qsArray.length][];
        System.arraycopy(this.qsArray, 0, mergeQsArray, 0, this.qsArray.length);
        System.arraycopy(that.qsArray, 0, mergeQsArray, this.qsArray.length, that.qsArray.length);
        qsArray = mergeQsArray;
    }

    /**
     * Gets Rb.
     *
     * @param index the index.
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
}
