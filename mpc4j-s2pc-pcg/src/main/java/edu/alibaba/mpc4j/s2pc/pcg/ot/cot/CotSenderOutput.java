package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * COT协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
public class CotSenderOutput {
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
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        senderOutput.delta = BytesUtils.clone(delta);
        assert r0Array.length > 0 : "R0 Array Length must be greater than 0";
        senderOutput.r0Array = Arrays.stream(r0Array)
            .peek(r0 -> {
                assert r0.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
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

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public CotSenderOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        byte[][] r0SubArray = new byte[length][];
        byte[][] r0RemainArray = new byte[num - length][];
        System.arraycopy(r0Array, 0, r0SubArray, 0, length);
        System.arraycopy(r0Array, length, r0RemainArray, 0, num - length);
        r0Array = r0RemainArray;

        return CotSenderOutput.create(delta, r0SubArray);
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
            byte[][] r0RemainArray = new byte[length][];
            System.arraycopy(r0Array, 0, r0RemainArray, 0, length);
            r0Array = r0RemainArray;
        }
    }

    /**
     * 合并两个发送方输出。
     *
     * @param that 另一个发送方输出。
     */
    public void merge(CotSenderOutput that) {
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

    /**
     * 返回R0。
     *
     * @param index 索引值。
     * @return R0。
     */
    public byte[] getR0(int index) {
        return r0Array[index];
    }

    /**
     * 返回R0数组。
     *
     * @return R0数组。
     */
    public byte[][] getR0Array() {
        return r0Array;
    }

    /**
     * 返回R1。
     *
     * @param index 索引值。
     * @return R1。
     */
    public byte[] getR1(int index) {
        return BytesUtils.xor(delta, getR0(index));
    }

    /**
     * 返回R1数组。
     *
     * @return R1数组。
     */
    public byte[][] getR1Array() {
        return Arrays.stream(r0Array)
            .map(r0 -> BytesUtils.xor(delta, r0))
            .toArray(byte[][]::new);
    }

    /**
     * 返回COT数量。
     *
     * @return COT数量。
     */
    public int getNum() {
        return r0Array.length;
    }
}
