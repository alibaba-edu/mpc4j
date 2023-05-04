package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.util.Arrays;

/**
 * 2^l选1-COT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class LcotReceiverOutput implements MergedPcgPartyOutput {
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
    public static LcotReceiverOutput create(int inputBitLength, int outputBitLength, byte[][] choices, byte[][] rbArray) {
        LcotReceiverOutput receiverOutput = new LcotReceiverOutput(inputBitLength, outputBitLength);
        assert choices.length > 0 : "# of NOT must be greater than 0";
        assert choices.length == rbArray.length : "# of choices must match # of randomness";
        receiverOutput.choices = Arrays.stream(choices)
            .peek(choice -> {
                assert choice.length == receiverOutput.inputByteLength
                    && BytesUtils.isReduceByteArray(choice, inputBitLength);
            })
            .toArray(byte[][]::new);
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> {
                assert rb.length == receiverOutput.outputByteLength
                    && BytesUtils.isReduceByteArray(rb, outputBitLength);
            })
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
    public static LcotReceiverOutput createEmpty(int inputBitLength, int outputBitLength) {
        LcotReceiverOutput receiverOutput = new LcotReceiverOutput(inputBitLength, outputBitLength);
        receiverOutput.choices = new byte[0][receiverOutput.inputByteLength];
        receiverOutput.rbArray = new byte[0][receiverOutput.outputByteLength];

        return receiverOutput;
    }

    /**
     * private constructor.
     */
    private LcotReceiverOutput(int inputBitLength, int outputBitLength) {
        assert inputBitLength > 0 : "InputBitLength must be greater than 0";
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        assert outputBitLength >= CommonConstants.BLOCK_BIT_LENGTH
            : "OutputBitLength must be greater than " + CommonConstants.BLOCK_BIT_LENGTH;
        this.outputBitLength = outputBitLength;
        outputByteLength = CommonUtils.getByteLength(outputBitLength);
    }

    @Override
    public LcotReceiverOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        // 拆分选择比特
        byte[][] subChoices = new byte[splitNum][];
        byte[][] remainChoices = new byte[num - splitNum][];
        System.arraycopy(choices, 0, subChoices, 0, splitNum);
        System.arraycopy(choices, splitNum, remainChoices, 0, num - splitNum);
        choices = remainChoices;
        // 拆分选择密钥
        byte[][] rbSubArray = new byte[splitNum][];
        byte[][] rbRemainArray = new byte[num - splitNum][];
        System.arraycopy(rbArray, 0, rbSubArray, 0, splitNum);
        System.arraycopy(rbArray, splitNum, rbRemainArray, 0, num - splitNum);
        rbArray = rbRemainArray;

        return LcotReceiverOutput.create(inputBitLength, outputBitLength, subChoices, rbSubArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
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

    @Override
    public int getNum() {
        return rbArray.length;
    }
}
