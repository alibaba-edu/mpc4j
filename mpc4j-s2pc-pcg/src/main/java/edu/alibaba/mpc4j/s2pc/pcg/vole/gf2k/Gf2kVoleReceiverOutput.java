package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * GF2K-VOLE协议接收方输出。接收方得到(Δ, q)，满足t = q + Δ · x（x和t由发送方持有）。
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
public class Gf2kVoleReceiverOutput {
    /**
     * 关联值Δ
     */
    private byte[] delta;
    /**
     * q_i
     */
    private byte[][] q;

    /**
     * 创建接收方输出。
     *
     * @param delta 关联值Δ。
     * @param q     q_i。
     * @return 接收方输出。
     */
    public static Gf2kVoleReceiverOutput create(byte[] delta, byte[][] q) {
        Gf2kVoleReceiverOutput receiverOutput = new Gf2kVoleReceiverOutput();
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        receiverOutput.delta = BytesUtils.clone(delta);
        assert q.length > 0 : "# of t must be greater than 0";
        receiverOutput.q = Arrays.stream(q)
            .peek(qi -> {
                assert qi.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);

        return receiverOutput;
    }

    /**
     * 创建长度为0的接收方输出。
     *
     * @param delta 关联值Δ。
     * @return 长度为0的接收方输出。
     */
    public static Gf2kVoleReceiverOutput createEmpty(byte[] delta) {
        Gf2kVoleReceiverOutput receiverOutput = new Gf2kVoleReceiverOutput();
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        receiverOutput.delta = BytesUtils.clone(delta);
        receiverOutput.q = new byte[0][];

        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private Gf2kVoleReceiverOutput() {
        // empty
    }

    /**
     * 从当前输出结果切分出一部分输出结果。
     *
     * @param length 切分输出结果数量。
     * @return 切分输出结果。
     */
    public Gf2kVoleReceiverOutput split(int length) {
        int num = getNum();
        assert length > 0 && length <= num : "split length must be in range (0, " + num + "]";
        byte[][] subQ = new byte[length][];
        byte[][] remainQ = new byte[num - length][];
        System.arraycopy(q, 0, subQ, 0, length);
        System.arraycopy(q, length, remainQ, 0, num - length);
        q = remainQ;

        return Gf2kVoleReceiverOutput.create(delta, subQ);
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
            byte[][] remainQ = new byte[length][];
            System.arraycopy(q, 0, remainQ, 0, length);
            q = remainQ;
        }
    }

    /**
     * 合并两个发送方输出。
     *
     * @param that 另一个发送方输出。
     */
    public void merge(Gf2kVoleReceiverOutput that) {
        assert Arrays.equals(this.delta, that.delta) : "merged outputs must have the same Δ";
        byte[][] mergeQ = new byte[this.q.length + that.q.length][];
        System.arraycopy(this.q, 0, mergeQ, 0, this.q.length);
        System.arraycopy(that.q, 0, mergeQ, this.q.length, that.q.length);
        q = mergeQ;
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
     * 返回q_i。
     *
     * @param index 索引值。
     * @return q_i。
     */
    public byte[] getQ(int index) {
        return q[index];
    }

    /**
     * 返回q。
     *
     * @return q。
     */
    public byte[][] getQ() {
        return q;
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return q.length;
    }
}
