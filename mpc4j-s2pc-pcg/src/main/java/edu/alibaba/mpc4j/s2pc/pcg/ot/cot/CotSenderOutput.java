package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import java.util.Arrays;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtSenderOutput;

/**
 * COT sender output.
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public class CotSenderOutput implements OtSenderOutput, MergedPcgPartyOutput {
    /**
     * Δ
     */
    private byte[] delta;
    /**
     * R0 array
     */
    private byte[][] r0Array;

    /**
     * Creates a sender output.
     *
     * @param delta   Δ.
     * @param r0Array R0 array.
     * @return a sender output.
     */
    public static CotSenderOutput create(byte[] delta, byte[][] r0Array) {
        CotSenderOutput senderOutput = new CotSenderOutput();
        MathPreconditions.checkEqual("delta.length", "λ in bytes", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        senderOutput.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("num", r0Array.length);
        senderOutput.r0Array = Arrays.stream(r0Array)
            .peek(r0 ->
                MathPreconditions.checkEqual("R0.length", "λ in bytes", r0.length, CommonConstants.BLOCK_BYTE_LENGTH)
            )
            .toArray(byte[][]::new);

        return senderOutput;
    }

    /**
     * Creates an empty sender output.
     *
     * @param delta Δ.
     * @return an empty sender output.
     */
    public static CotSenderOutput createEmpty(byte[] delta) {
        CotSenderOutput senderOutput = new CotSenderOutput();
        MathPreconditions.checkEqual("delta.length", "λ in bytes", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        senderOutput.delta = BytesUtils.clone(delta);
        senderOutput.r0Array = new byte[0][];

        return senderOutput;
    }

    /**
     * private constructor.
     */
    private CotSenderOutput() {
        // empty
    }

    @Override
    public CotSenderOutput split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        byte[][] r0SubArray = new byte[splitNum][];
        byte[][] r0RemainArray = new byte[num - splitNum][];
        System.arraycopy(r0Array, 0, r0SubArray, 0, splitNum);
        System.arraycopy(r0Array, splitNum, r0RemainArray, 0, num - splitNum);
        r0Array = r0RemainArray;

        return CotSenderOutput.create(delta, r0SubArray);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // 如果给定的数量小于当前数量，则裁剪，否则保持原样不动
            byte[][] r0RemainArray = new byte[reduceNum][];
            System.arraycopy(r0Array, 0, r0RemainArray, 0, reduceNum);
            r0Array = r0RemainArray;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        CotSenderOutput that = (CotSenderOutput) other;
        Preconditions.checkArgument(BytesUtils.equals(this.delta, that.delta));
        byte[][] mergeR0Array = new byte[this.r0Array.length + that.r0Array.length][];
        System.arraycopy(this.r0Array, 0, mergeR0Array, 0, this.r0Array.length);
        System.arraycopy(that.r0Array, 0, mergeR0Array, this.r0Array.length, that.r0Array.length);
        r0Array = mergeR0Array;
    }

    /**
     * Gets Δ.
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
    }

    @Override
    public byte[] getR0(int index) {
        return r0Array[index];
    }

    @Override
    public byte[][] getR0Array() {
        return r0Array;
    }

    @Override
    public byte[] getR1(int index) {
        return BytesUtils.xor(delta, getR0(index));
    }

    @Override
    public byte[][] getR1Array() {
        return Arrays.stream(r0Array)
            .map(r0 -> BytesUtils.xor(delta, r0))
            .toArray(byte[][]::new);
    }

    @Override
    public int getNum() {
        return r0Array.length;
    }
}
