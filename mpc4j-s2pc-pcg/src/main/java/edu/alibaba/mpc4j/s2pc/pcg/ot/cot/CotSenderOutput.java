package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.OtSenderOutput;

/**
 * COT协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public class CotSenderOutput implements OtSenderOutput, MergedPcgPartyOutput {
    /**
     * 关联值Δ
     */
    private byte[] delta;
    /**
     * R0数组
     */
    private byte[][] r0Array;

    /**
     * 创建发送方输出。
     *
     * @param delta   关联值Δ。
     * @param r0Array R0数组。
     * @return 发送方输出。
     */
    public static CotSenderOutput create(byte[] delta, byte[][] r0Array) {
        CotSenderOutput senderOutput = new CotSenderOutput();
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH
            : "Δ byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + delta.length;
        senderOutput.delta = BytesUtils.clone(delta);
        assert r0Array.length > 0 : "num must be greater than 0: " + r0Array.length;
        senderOutput.r0Array = Arrays.stream(r0Array)
            .peek(r0 -> {
                assert r0.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "r0 byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + r0.length;
            })
            .toArray(byte[][]::new);

        return senderOutput;
    }

    /**
     * 创建长度为0的发送方输出。
     *
     * @param delta 关联值Δ。
     * @return 发送方输出。
     */
    public static CotSenderOutput createEmpty(byte[] delta) {
        CotSenderOutput senderOutput = new CotSenderOutput();
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        senderOutput.delta = BytesUtils.clone(delta);
        senderOutput.r0Array = new byte[0][];

        return senderOutput;
    }

    /**
     * 私有构造函数。
     */
    private CotSenderOutput() {
        // empty
    }

    @Override
    public CotSenderOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
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
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
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
        assert Arrays.equals(this.delta, that.delta) : "merged outputs must have the same Δ";
        byte[][] mergeR0Array = new byte[this.r0Array.length + that.r0Array.length][];
        System.arraycopy(this.r0Array, 0, mergeR0Array, 0, this.r0Array.length);
        System.arraycopy(that.r0Array, 0, mergeR0Array, this.r0Array.length, that.r0Array.length);
        r0Array = mergeR0Array;
    }

    /**
     * 返回关联值Δ。
     *
     * @return 关联值Δ。
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
