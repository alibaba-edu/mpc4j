package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * SSP-COT协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class SspCotSenderOutput {
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
    public static SspCotSenderOutput create(byte[] delta, byte[][] r0Array) {
        SspCotSenderOutput senderOutput = new SspCotSenderOutput();
        assert delta.length == CommonConstants.BLOCK_BYTE_LENGTH;
        senderOutput.delta = BytesUtils.clone(delta);
        assert r0Array.length > 0;
        senderOutput.r0Array = Arrays.stream(r0Array)
            .peek(r0 -> {
                assert r0.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        return senderOutput;
    }

    /**
     * 私有构造函数。
     */
    private SspCotSenderOutput() {
        // empty
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
     * 返回R1。
     *
     * @param index 索引值。
     * @return R1。
     */
    public byte[] getR1(int index) {
        return BytesUtils.xor(delta, getR0(index));
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return r0Array.length;
    }
}
