package edu.alibaba.mpc4j.s2pc.pcg.ot.lot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * 2^l选1-OT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class LotReceiverOutput {
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
     * 选择值数组
     */
    private byte[][] choices;
    /**
     * Ri数组
     */
    private byte[][] rbArray;

    /**
     * 创建接收方输出。
     *
     * @param inputBitLength  输入比特长度。
     * @param outputBitLength 输出比特长度。
     * @param choices         选择值数组。
     * @param rbArray         Rb数组。
     * @return 接收方输出。
     */
    public static LotReceiverOutput create(int inputBitLength, int outputBitLength, byte[][] choices, byte[][] rbArray) {
        LotReceiverOutput receiverOutput = new LotReceiverOutput();
        assert inputBitLength > 0 : "InputBitLength must be greater than 0";
        receiverOutput.inputBitLength = inputBitLength;
        receiverOutput.inputByteLength = CommonUtils.getByteLength(inputBitLength);
        assert outputBitLength >= CommonConstants.BLOCK_BIT_LENGTH
            : "OutputBitLength must be greater than " + CommonConstants.BLOCK_BIT_LENGTH;
        receiverOutput.outputBitLength = outputBitLength;
        receiverOutput.outputByteLength = CommonUtils.getByteLength(outputBitLength);
        assert choices.length > 0 : "# of NOT must be greater than 0";
        assert choices.length == rbArray.length : "# of choices must match # of randomness";
        receiverOutput.choices = Arrays.stream(choices)
            .peek(choice -> {
                assert choice.length == receiverOutput.inputByteLength
                    && BytesUtils.isReduceByteArray(choice, inputBitLength);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> {
                assert rb.length == receiverOutput.outputByteLength
                    && BytesUtils.isReduceByteArray(rb, outputBitLength);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * 创建数量为0的接收方输出。
     *
     * @param inputBitLength  输入比特长度。
     * @param outputBitLength 输出比特长度。
     * @return 数量为0的NOT接收方输出。
     */
    public static LotReceiverOutput createEmpty(int inputBitLength, int outputBitLength) {
        LotReceiverOutput receiverOutput = new LotReceiverOutput();
        assert inputBitLength > 0 : "InputBitLength must be greater than 0: " + inputBitLength;
        receiverOutput.inputBitLength = inputBitLength;
        receiverOutput.inputByteLength = CommonUtils.getByteLength(inputBitLength);
        assert outputBitLength >= CommonConstants.BLOCK_BIT_LENGTH
            : "OutputBitLength must be greater than " + CommonConstants.BLOCK_BIT_LENGTH + ": " + outputBitLength;
        receiverOutput.outputBitLength = outputBitLength;
        receiverOutput.outputByteLength = CommonUtils.getByteLength(outputBitLength);
        receiverOutput.choices = new byte[0][receiverOutput.inputByteLength];
        receiverOutput.rbArray = new byte[0][receiverOutput.outputByteLength];

        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private LotReceiverOutput() {
        // empty
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public LotReceiverOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        // 拆分选择比特
        byte[][] subChoices = new byte[length][];
        byte[][] remainChoices = new byte[num - length][];
        System.arraycopy(choices, 0, subChoices, 0, length);
        System.arraycopy(choices, length, remainChoices, 0, num - length);
        choices = remainChoices;
        // 拆分选择密钥
        byte[][] rbSubArray = new byte[length][];
        byte[][] rbRemainArray = new byte[num - length][];
        System.arraycopy(rbArray, 0, rbSubArray, 0, length);
        System.arraycopy(rbArray, length, rbRemainArray, 0, num - length);
        rbArray = rbRemainArray;

        return LotReceiverOutput.create(inputBitLength, outputBitLength, subChoices, rbSubArray);
    }

    /**
     * 将当前输出结果数量减少至给定的数量。
     *
     * @param length 给定的数量。
     */
    public void reduce(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "reduce length = " + length + " must be in range (0, " + num + "]";
        if (length < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
            byte[][] remainChoices = new byte[length][];
            System.arraycopy(choices, 0, remainChoices, 0, length);
            choices = remainChoices;
            byte[][] rbRemainArray = new byte[length][];
            System.arraycopy(rbArray, 0, rbRemainArray, 0, length);
            rbArray = rbRemainArray;
        }
    }

    /**
     * 合并两个接收方输出。
     *
     * @param that 另一个接收方输出。
     */
    public void merge(LotReceiverOutput that) {
        assert this.inputBitLength == that.inputBitLength : "InputBitLength mismatch";
        assert this.outputBitLength == that.outputBitLength : "OutputBitLength mismatch";
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

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return rbArray.length;
    }
}
