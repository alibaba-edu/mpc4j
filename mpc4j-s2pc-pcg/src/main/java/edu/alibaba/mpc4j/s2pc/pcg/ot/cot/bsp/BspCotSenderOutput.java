package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * BSP-COT协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class BspCotSenderOutput {
    /**
     * SSPCOT协议发送方输出数组
     */
    private SspCotSenderOutput[] senderOutputs;
    /**
     * 关联值Δ
     */
    private byte[] delta;
    /**
     * 每个SSPCOT协议发送方输出的数量
     */
    private int num;

    /**
     * 创建发送方输出。
     *
     * @param sspCotSenderOutputs SSPCOT协议发送方输出数组。
     * @return 发送方输出。
     */
    public static BspCotSenderOutput create(SspCotSenderOutput[] sspCotSenderOutputs) {
        BspCotSenderOutput senderOutput = new BspCotSenderOutput();
        assert sspCotSenderOutputs.length > 0;
        // 取第一个输出的参数
        senderOutput.delta = BytesUtils.clone(sspCotSenderOutputs[0].getDelta());
        senderOutput.num = sspCotSenderOutputs[0].getNum();
        // 设置其余输出
        senderOutput.senderOutputs = Arrays.stream(sspCotSenderOutputs)
            // 验证所有Δ相等，且数量均为num
            .peek(sspcotSenderOutput -> {
                assert BytesUtils.equals(senderOutput.delta, sspcotSenderOutput.getDelta());
                assert sspcotSenderOutput.getNum() == senderOutput.num;
            })
            .toArray(SspCotSenderOutput[]::new);
        return senderOutput;
    }

    /**
     * 私有构造函数。
     */
    private BspCotSenderOutput() {
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
     * 返回SSPCOT协议发送方输出。
     *
     * @param index 索引值。
     * @return 发送方输出。
     */
    public SspCotSenderOutput get(int index) {
        return senderOutputs[index];
    }

    /**
     * 返回数量。
     *
     * @return 数量。
     */
    public int getNum() {
        return num;
    }

    /**
     * 返回批处理数量。
     *
     * @return 批处理数量。
     */
    public int getBatch() {
        return senderOutputs.length;
    }
}
