package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * MSP-COT协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class MspCotSenderOutput {
    /**
     * 关联值Δ
     */
    private byte[] delta;
    /**
     * R0数组
     */
    private byte[][] r0Array;

    public static MspCotSenderOutput create(byte[] delta, byte[][] r0Array) {
        MspCotSenderOutput senderOutput = new MspCotSenderOutput();
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
    private MspCotSenderOutput() {
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
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return r0Array.length;
    }

}
