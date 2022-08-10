package edu.alibaba.mpc4j.s2pc.pcg.ot.no;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * NOT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class NotReceiverOutput {
    /**
     * 最大选择值
     */
    private int n;
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
    private int[] choices;
    /**
     * Ri数组
     */
    private byte[][] rbArray;

    /**
     * 创建接收方输出。
     *
     * @param n               最大选择值n。
     * @param outputBitLength 输出比特长度。
     * @param choices         选择值数组。
     * @param rbArray         Rb数组。
     * @return 接收方输出。
     */
    public static NotReceiverOutput create(int n, int outputBitLength, int[] choices, byte[][] rbArray) {
        NotReceiverOutput receiverOutput = new NotReceiverOutput();
        assert n > 1 : "n must be greater than 1: " + n;
        receiverOutput.n = n;
        assert outputBitLength >= CommonConstants.BLOCK_BIT_LENGTH
            : "OutputBitLength must be greater than " + CommonConstants.BLOCK_BIT_LENGTH + ": " + outputBitLength;
        receiverOutput.outputBitLength = outputBitLength;
        receiverOutput.outputByteLength = CommonUtils.getByteLength(outputBitLength);
        assert choices.length > 0 : "# of choices must be greater than 0";
        assert choices.length == rbArray.length : "# of choices must match # of randomness";
        Arrays.stream(choices).forEach(choice -> {
            assert choice >= 0 && choice < n : "choice must be in range [0, " + n + ")";
        });
        receiverOutput.choices = Arrays.copyOf(choices, choices.length);
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> {
                assert rb.length == receiverOutput.outputByteLength && BytesUtils.isReduceByteArray(rb, outputBitLength);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        return receiverOutput;
    }

    /**
     * 创建数量为0的接收方输出。
     *
     * @param n               最大选择值。
     * @param outputBitLength 输出比特长度。
     * @return 数量为0的接收方输出。
     */
    public static NotReceiverOutput createEmpty(int n, int outputBitLength) {
        NotReceiverOutput receiverOutput = new NotReceiverOutput();
        assert n > 1 : "n must be greater than 1: " + n;
        receiverOutput.n = n;
        assert outputBitLength >= CommonConstants.BLOCK_BIT_LENGTH
            : "OutputBitLength must be greater than " + CommonConstants.BLOCK_BIT_LENGTH + ": " + outputBitLength;
        receiverOutput.outputBitLength = outputBitLength;
        receiverOutput.outputByteLength = CommonUtils.getByteLength(outputBitLength);
        receiverOutput.choices = new int[0];
        receiverOutput.rbArray = new byte[0][receiverOutput.outputByteLength];

        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private NotReceiverOutput() {
        // empty
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public NotReceiverOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        // 拆分选择比特
        int[] subChoices = new int[length];
        int[] remainChoices = new int[num - length];
        System.arraycopy(choices, 0, subChoices, 0, length);
        System.arraycopy(choices, length, remainChoices, 0, num - length);
        choices = remainChoices;
        // 拆分选择密钥
        byte[][] rbSubArray = new byte[length][];
        byte[][] rbRemainArray = new byte[num - length][];
        System.arraycopy(rbArray, 0, rbSubArray, 0, length);
        System.arraycopy(rbArray, length, rbRemainArray, 0, num - length);
        rbArray = rbRemainArray;

        return NotReceiverOutput.create(n, outputBitLength, subChoices, rbSubArray);
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
            int[] remainChoices = new int[length];
            System.arraycopy(choices, 0, remainChoices, 0, length);
            choices = remainChoices;
            byte[][] rbRemainArray = new byte[length][];
            System.arraycopy(rbArray, 0, rbRemainArray, 0, length);
            rbArray = rbRemainArray;
        }
    }

    /**
     * 合并两个NOT接收方输出。
     *
     * @param that 另一个NOT接收方输出。
     */
    public void merge(NotReceiverOutput that) {
        assert this.n == that.n : "n mismatch";
        assert this.outputBitLength == that.outputBitLength : "OutputBitLength mismatch";
        // 拷贝选择比特数组
        int[] mergeChoices = new int[this.choices.length + that.choices.length];
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
    public int getChoice(int index) {
        return choices[index];
    }

    /**
     * 返回选择值数组。
     *
     * @return 选择值数组。
     */
    public int[] getChoices() {
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
     * 返回最大选择值。
     *
     * @return 最大选择值。
     */
    public int getN() {
        return n;
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
     * 返回输出随机量字节长度。
     *
     * @return 输出随机量字节长度。
     */
    public int getOutputByteLength() {
        return outputByteLength;
    }

    /**
     * 返回NOT数量。
     *
     * @return NOT数量。
     */
    public int getNum() {
        return rbArray.length;
    }
}
