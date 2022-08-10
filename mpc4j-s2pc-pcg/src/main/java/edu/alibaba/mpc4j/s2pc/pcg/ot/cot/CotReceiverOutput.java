package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * COT协议接收方输出。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public class CotReceiverOutput {
    /**
     * 选择比特
     */
    private boolean[] choices;
    /**
     * Rb数组
     */
    private byte[][] rbArray;

    /**
     * 创建接收方输出。
     *
     * @param choices 选择比特数组。
     * @param rbArray Rb数组。
     * @return 接收方输出。
     */
    public static CotReceiverOutput create(boolean[] choices, byte[][] rbArray) {
        CotReceiverOutput receiverOutput = new CotReceiverOutput();
        assert choices.length == rbArray.length;
        assert choices.length > 0;
        receiverOutput.choices = BinaryUtils.clone(choices);
        receiverOutput.rbArray = Arrays.stream(rbArray)
            .peek(rb -> {
                assert rb.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);

        return receiverOutput;
    }

    /**
     * 创建长度为0的接收方输出。
     *
     * @return 长度为0的接收方输出。
     */
    public static CotReceiverOutput createEmpty() {
        CotReceiverOutput receiverOutput = new CotReceiverOutput();
        receiverOutput.choices = new boolean[0];
        receiverOutput.rbArray = new byte[0][];

        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private CotReceiverOutput() {
        // empty
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public CotReceiverOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]: " + length;
        // 拆分选择比特
        boolean[] subChoices = new boolean[length];
        boolean[] remainChoices = new boolean[num - length];
        System.arraycopy(choices, 0, subChoices, 0, length);
        System.arraycopy(choices, length, remainChoices, 0, num - length);
        choices = remainChoices;
        // 拆分选择密钥
        byte[][] rbSubArray = new byte[length][];
        byte[][] rbRemainArray = new byte[num - length][];
        System.arraycopy(rbArray, 0, rbSubArray, 0, length);
        System.arraycopy(rbArray, length, rbRemainArray, 0, num - length);
        rbArray = rbRemainArray;

        return CotReceiverOutput.create(subChoices, rbSubArray);
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
            boolean[] remainChoices = new boolean[length];
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
    public void merge(CotReceiverOutput that) {
        // 拷贝选择比特数组
        boolean[] mergeChoices = new boolean[this.choices.length + that.choices.length];
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
     * 返回选择比特。
     *
     * @param index 索引值。
     * @return 选择比特。
     */
    public boolean getChoice(int index) {
        return choices[index];
    }

    /**
     * 返回选择比特数组。
     *
     * @return 选择比特数组。
     */
    public boolean[] getChoices() {
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
     * 返回COT数量。
     *
     * @return COT数量。
     */
    public int getNum() {
        return rbArray.length;
    }
}
