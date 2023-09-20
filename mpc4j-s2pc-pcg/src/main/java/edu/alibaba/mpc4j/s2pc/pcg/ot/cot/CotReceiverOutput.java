package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtReceiverOutput;

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
        MathPreconditions.checkPositive("num", choices.length);
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
     * private constructor.
     */
    private CotReceiverOutput() {
        // empty
    }

    @Override
    public CotReceiverOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        // 拆分选择比特
        boolean[] subChoices = new boolean[splitNum];
        boolean[] remainChoices = new boolean[num - splitNum];
        System.arraycopy(choices, 0, subChoices, 0, splitNum);
        System.arraycopy(choices, splitNum, remainChoices, 0, num - splitNum);
        choices = remainChoices;
        // 拆分选择密钥
        byte[][] rbSubArray = new byte[splitNum][];
        byte[][] rbRemainArray = new byte[num - splitNum][];
        System.arraycopy(rbArray, 0, rbSubArray, 0, splitNum);
        System.arraycopy(rbArray, splitNum, rbRemainArray, 0, num - splitNum);
        rbArray = rbRemainArray;

        return CotReceiverOutput.create(subChoices, rbSubArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
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
}
