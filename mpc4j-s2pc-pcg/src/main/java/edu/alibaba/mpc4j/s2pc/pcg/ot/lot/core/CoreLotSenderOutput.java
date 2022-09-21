package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core;

import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotSenderOutput;

import java.util.Arrays;

/**
 * 核2^l选1-OT协议发送方输出。服务端输入Δ，得到q_1,...,q_k，客户端输入选择元组(m_1,...,m_k)，得到t_0,...,t_k。各个参数满足：
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
public class CoreLotSenderOutput implements LotSenderOutput {
    /**
     * 输入比特长度
     */
    private int inputBitLength;
    /**
     * 输入字节长度
     */
    private int inputByteLength;
    /**
     * 输出比特长度
     */
    private int outputBitLength;
    /**
     * 输出字节长度
     */
    private int outputByteLength;
    /**
     * 线性编码器
     */
    private LinearCoder linearCoder;
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
    public static CoreLotSenderOutput create(int inputBitLength, byte[] delta, byte[][] qsArray) {
        CoreLotSenderOutput senderOutput = new CoreLotSenderOutput();
        assert inputBitLength > 0 : "InputBitLength must be greater than 0: " + inputBitLength;
        senderOutput.inputBitLength = inputBitLength;
        senderOutput.inputByteLength = CommonUtils.getByteLength(inputBitLength);
        senderOutput.linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        senderOutput.outputBitLength = senderOutput.linearCoder.getCodewordBitLength();
        senderOutput.outputByteLength = senderOutput.linearCoder.getCodewordByteLength();
        assert delta.length == senderOutput.outputByteLength
            && BytesUtils.isReduceByteArray(delta, senderOutput.outputBitLength);
        senderOutput.delta = BytesUtils.clone(delta);
        assert qsArray.length > 0 : "QS Length must be greater than 0";
        senderOutput.qsArray = Arrays.stream(qsArray)
            .peek(q -> {
                assert q.length == senderOutput.outputByteLength
                    && BytesUtils.isReduceByteArray(q, senderOutput.outputBitLength);
            })
            .map(BytesUtils::clone)
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
    public static CoreLotSenderOutput createEmpty(int inputBitLength, byte[] delta) {
        CoreLotSenderOutput senderOutput = new CoreLotSenderOutput();
        assert inputBitLength > 0 : "InputBitLength must be greater than 0: " + inputBitLength;
        senderOutput.inputBitLength = inputBitLength;
        senderOutput.inputByteLength = CommonUtils.getByteLength(inputBitLength);
        senderOutput.linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        senderOutput.outputBitLength = senderOutput.linearCoder.getCodewordBitLength();
        senderOutput.outputByteLength = senderOutput.linearCoder.getCodewordByteLength();
        assert delta.length == senderOutput.outputByteLength
            && BytesUtils.isReduceByteArray(delta, senderOutput.outputBitLength);
        senderOutput.delta = BytesUtils.clone(delta);
        senderOutput.qsArray = new byte[0][senderOutput.outputByteLength];

        return senderOutput;
    }

    /**
     * 私有构造函数。
     */
    private CoreLotSenderOutput() {
        // empty
    }

    @Override
    public CoreLotSenderOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        byte[][] qsSubArray = new byte[length][];
        byte[][] qsRemainArray = new byte[num - length][];
        System.arraycopy(qsArray, 0, qsSubArray, 0, length);
        System.arraycopy(qsArray, length, qsRemainArray, 0, num - length);
        qsArray = qsRemainArray;

        return CoreLotSenderOutput.create(inputBitLength, delta, qsSubArray);
    }

    @Override
    public void reduce(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "reduce length = " + length + " must be in range (0, " + num + "]";
        if (length < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
            byte[][] qsRemainArray = new byte[length][];
            System.arraycopy(qsArray, 0, qsRemainArray, 0, length);
            qsArray = qsRemainArray;
        }
    }

    @Override
    public void merge(LotSenderOutput that) {
        assert that instanceof CoreLotSenderOutput;
        CoreLotSenderOutput thatOutput = (CoreLotSenderOutput) that;
        assert this.inputBitLength == thatOutput.inputBitLength : "InputBitLength mismatch";
        assert Arrays.equals(this.delta, thatOutput.delta) : "Δ mismatch";
        byte[][] mergeQsArray = new byte[this.qsArray.length + thatOutput.qsArray.length][];
        System.arraycopy(this.qsArray, 0, mergeQsArray, 0, this.qsArray.length);
        System.arraycopy(thatOutput.qsArray, 0, mergeQsArray, this.qsArray.length, thatOutput.qsArray.length);
        qsArray = mergeQsArray;
    }

    @Override
    public byte[] getRb(int index, byte[] choice) {
        assert choice.length == inputByteLength && BytesUtils.isReduceByteArray(choice, inputBitLength);
        // r_i = q_i ⊕ (C(m_i) ⊙ Δ)
        byte[] output = linearCoder.encode(BytesUtils.paddingByteArray(choice, linearCoder.getDatawordByteLength()));
        BytesUtils.andi(output, delta);
        BytesUtils.xori(output, qsArray[index]);
        return output;
    }

    @Override
    public int getInputBitLength() {
        return inputBitLength;
    }

    @Override
    public int getInputByteLength() {
        return inputByteLength;
    }

    @Override
    public int getOutputBitLength() {
        return outputBitLength;
    }

    @Override
    public int getOutputByteLength() {
        return outputByteLength;
    }

    @Override
    public int getNum() {
        return qsArray.length;
    }

    /**
     * 返回关联值Δ。
     *
     * @return 关联值Δ。
     */
    public byte[] getDelta() {
        return delta;
    }

    /**
     * 返回q。
     *
     * @param index 索引值。
     * @return q。
     */
    public byte[] getQ(int index) {
        return qsArray[index];
    }

    /**
     * 返回q数组。
     *
     * @return q数组。
     */
    public byte[][] getQsArray() {
        return qsArray;
    }

    /**
     * 返回线性编码器。
     *
     * @return 线性编码器。
     */
    public LinearCoder getLinearCoder() {
        return linearCoder;
    }
}
